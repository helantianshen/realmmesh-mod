// 仓库转移：游戏线程内协调暂扣、中心授权、整笔发放与明确失败归还
// 暂存只覆盖当前进程，未知结果仅保留证据，不自动补发或回滚

package io.github.helantianshen.realmmesh.neoforge.warehouse;

import com.google.protobuf.ByteString;
import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthentication;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import io.grpc.Status;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import realmmesh.warehouse.v1.Warehouse;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 服务器线程串行访问暂存；RPC 回调只投递任务，不直接操作 Map 或背包 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class WarehouseTransfers {
    public static final int MAX_TRANSFER_AMOUNT = 2304;
    private static final int MAX_PENDING = 256;
    private static final int MAX_PENDING_BYTES = 64 * 1024 * 1024;
    private static final int MAX_COMPLETED = 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmMesh.MOD_ID);
    private static final Map<MinecraftServer, Transfers> SERVERS = new IdentityHashMap<>();

    /** 正常进行中、明确可归还和未知结果不能互相混用 */
    private enum Phase { DEPOSITING, RETURNABLE, PREPARING, COMMITTING, GIVING, ACKNOWLEDGING, UNKNOWN }

    /** 每个逻辑服务器独立预算，不持有离线玩家对象 */
    private static final class Transfers {
        private final Map<String, Transfer> pending = new LinkedHashMap<>();
        private final Map<String, Completed> completed = new LinkedHashMap<>();
        private int payloadBytes;
    }

    /** 完成记录只保存轻量结果，迟到或重复响应不能重新取得发放资格 */
    private record Completed(long userId, String result) {}

    /** 一笔操作只保留一份载荷和当前进程内的身份关联 */
    private static final class Transfer {
        private final String operationId = UUID.randomUUID().toString();
        private final Warehouse.PlayerIdentity identity;
        private final int amount;
        private final boolean deposit;
        private final long serverId;
        private long inventoryId;
        private String itemId = "";
        private byte[] payload;
        private ByteString hash = ByteString.EMPTY;
        private Phase phase;

        /** 身份只取自已经完成认证的连接 */
        private Transfer(Warehouse.PlayerIdentity identity, long serverId, int amount, boolean deposit) {
            this.identity = identity;
            this.serverId = serverId;
            this.amount = amount;
            this.deposit = deposit;
            phase = deposit ? Phase.DEPOSITING : Phase.PREPARING;
        }
    }

    /** 所有入口均由命令或生命周期调用 */
    private WarehouseTransfers() {}

    /** 列表只显示当前账户摘要和后续游标，不获取其他玩家载荷 */
    public static void list(ServerPlayer player, long afterId) {
        var client = authenticatedClient(player);
        if (client == null) return;
        long userId = PlayerAuthentication.userId(player);
        client.listWarehouseItems(Warehouse.ListItemsRequest.newBuilder()
                .setPlayer(identity(player)).setAfterId(afterId).setLimit(10).build())
                .whenComplete((response, error) -> player.getServer().execute(() -> {
                    if (!current(player, userId)) return;
                    if (error != null) {
                        reply(player, "仓库查询失败，请稍后重试");
                        return;
                    }
                    if (response.getItemsCount() == 0) reply(player, "没有更多库存");
                    for (var item : response.getItemsList()) {
                        reply(player, item.getInventoryId() + " | " + item.getItemId() + " × " + item.getQuantity());
                    }
                    if (response.getNextAfterId() > 0) {
                        reply(player, "下一页：/realmmesh warehouse list " + response.getNextAfterId());
                    }
                }));
    }

    /** 游戏线程中编码并暂扣主手，中心明确成功后才释放暂存 */
    public static void deposit(ServerPlayer player, int amount) {
        var client = ready(player, amount);
        if (client == null) return;
        var state = state(player.getServer());
        var source = player.getMainHandItem();
        if (source.isEmpty() || amount > source.getCount() || (!source.isStackable() && amount != 1)) {
            reply(player, "主手物品数量不足，或该物品必须逐件存入");
            return;
        }
        WarehouseItemCodec.Payload payload;
        try {
            payload = WarehouseItemCodec.encode(player.registryAccess(), source);
        } catch (RuntimeException error) {
            reply(player, "该物品无法完整保存，未扣除物品");
            return;
        }
        if (!capacity(state, payload.data().length)) {
            reply(player, "服务器仓库暂存已满，未扣除物品");
            return;
        }

        var transfer = new Transfer(identity(player), client.serverId(), amount, true);
        transfer.itemId = payload.itemId();
        var request = Warehouse.DepositRequest.newBuilder().setPlayer(transfer.identity)
                .setOperationId(transfer.operationId).setItemId(payload.itemId())
                .setItemData(ByteString.copyFrom(payload.data())).setStackable(payload.stackable())
                .setQuantity(amount).build();
        setPayload(state, transfer, payload.data());
        state.pending.put(transfer.operationId, transfer);
        source.shrink(amount);
        log(transfer, "temporarily_removed");
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        reply(player, "物品已暂扣，操作 ID：" + transfer.operationId);
        await(player.getServer(), state, transfer, Phase.DEPOSITING, () -> client.depositWarehouse(request), result -> {
            if (!validOperation(transfer, result) || result.getState() != Warehouse.OperationState.OPERATION_STATE_DEPOSITED) {
                unknown(transfer, player, "invalid_deposit_response");
                return;
            }
            transfer.inventoryId = result.getInventoryId();
            transfer.hash = result.getItemDataHash();
            finish(state, transfer, "deposited");
            tellCurrent(player, transfer, "存入完成");
        }, error -> {
            if (!explicitRejection(error)) {
                unknown(transfer, player, "deposit_result_unknown");
                return;
            }
            transfer.phase = Phase.RETURNABLE;
            log(transfer, "deposit_rejected");
            if (!tryReturn(player, state, transfer) && transfer.phase == Phase.RETURNABLE) {
                tellCurrent(player, transfer, "存入被拒绝，物品仍在服务器暂存；清空背包后使用 /realmmesh warehouse return");
            }
        });
    }

    /** 先取得只读载荷并检验容量，准备完成后才请求中心扣库 */
    public static void withdraw(ServerPlayer player, long inventoryId, int amount) {
        var client = ready(player, amount);
        if (client == null) return;
        var state = state(player.getServer());
        // 预检查前先检查预算，取得实际载荷后再次核对，扣库后不再增加载荷
        if (!capacity(state, WarehouseItemCodec.MAX_PAYLOAD_BYTES)) {
            reply(player, "服务器仓库暂存已满，请稍后重试");
            return;
        }
        var transfer = new Transfer(identity(player), client.serverId(), amount, false);
        transfer.inventoryId = inventoryId;
        state.pending.put(transfer.operationId, transfer);
        reply(player, "正在准备取出，操作 ID：" + transfer.operationId);
        var request = withdrawalRequest(transfer);
        await(player.getServer(), state, transfer, Phase.PREPARING, () -> client.prepareWithdrawal(request), prepared -> {
            var item = prepared.getItem();
            if (!prepared.getOperationId().equals(transfer.operationId) || prepared.getQuantity() != amount
                    || !prepared.hasItem() || item.getInventoryId() != inventoryId || item.getQuantity() < amount
                    || item.getItemDataHash().size() != 32 || item.getItemData().isEmpty()
                    || item.getItemData().size() > WarehouseItemCodec.MAX_PAYLOAD_BYTES) {
                finish(state, transfer, "prepare_rejected");
                tellCurrent(player, transfer, "中心预检查响应无效，未请求扣库");
                return;
            }
            transfer.itemId = item.getItemId();
            transfer.hash = item.getItemDataHash();
            byte[] payload = item.getItemData().toByteArray();
            if (!capacityBytes(state, payload.length) || !canReceive(player, transfer, payload, item.getStackable())) {
                finish(state, transfer, "not_ready");
                tellCurrent(player, transfer, "物品无法还原、背包不足或连接状态变化，未请求扣库");
                return;
            }
            setPayload(state, transfer, payload);
            transfer.phase = Phase.COMMITTING;
            log(transfer, "ready_not_delivered");
            var commit = Warehouse.CommitWithdrawalRequest.newBuilder()
                    .setRequest(request).setItemDataHash(transfer.hash).build();
            await(player.getServer(), state, transfer, Phase.COMMITTING, () -> client.commitWithdrawal(commit),
                    operation -> authorize(player, client, state, transfer, operation), error -> {
                        if (explicitRejection(error)) {
                            finish(state, transfer, "withdrawal_rejected");
                            tellCurrent(player, transfer, "库存不足或条件已变化，未给予物品");
                        } else {
                            unknown(transfer, player, "debit_result_unknown");
                        }
                    });
        }, error -> {
            // 预检查没有资产副作用，通信失败可安全结束本地准备状态
            finish(state, transfer, "prepare_failed");
            tellCurrent(player, transfer, "取出预检查失败，未请求扣库");
        });
    }

    /** 只有已知在途请求的扣库成功响应可以进入一次性发放 */
    private static void authorize(ServerPlayer player, RealmMeshRpcClient client, Transfers state,
            Transfer transfer, Warehouse.Operation operation) {
        if (!authorizesWithdrawal(transfer, operation)) {
            unknown(transfer, player, "invalid_authorization_response");
            return;
        }
        transfer.phase = Phase.GIVING;
        boolean delivered = false;
        String reason = "player_unavailable";
        net.minecraft.world.item.ItemStack restored = null;
        if (current(player, transfer.identity.getUserId())) {
            try {
                restored = WarehouseItemCodec.decode(player.registryAccess(), transfer.itemId, transfer.payload);
                reason = WarehouseInventory.available(player) ? "inventory_full" : "state_changed";
            } catch (RuntimeException error) {
                // 还原发生在修改背包之前，失败可以确定本次没有发放
                reason = "restore_failed";
                log(transfer, "restore_failed_not_delivered");
            }
        }
        if (restored != null) {
            try {
                delivered = WarehouseInventory.give(player, restored, transfer.amount);
            } catch (RuntimeException error) {
                // 发放方法中的异常可能发生在槽位更新后，不能声称完全未发放
                unknown(transfer, player, "delivery_exception");
                return;
            }
        }
        log(transfer, delivered ? "delivered" : "definitely_not_delivered");
        transfer.phase = Phase.ACKNOWLEDGING;
        setPayload(state, transfer, null);
        boolean confirmedDelivered = delivered;
        var complete = Warehouse.CompleteWithdrawalRequest.newBuilder().setPlayer(transfer.identity)
                .setOperationId(transfer.operationId).setDelivered(delivered)
                .setReason(delivered ? "delivered" : reason).build();
        await(player.getServer(), state, transfer, Phase.ACKNOWLEDGING, () -> client.completeWithdrawal(complete), result -> {
            var expected = confirmedDelivered ? Warehouse.OperationState.OPERATION_STATE_DELIVERED
                    : Warehouse.OperationState.OPERATION_STATE_REFUNDED;
            if (!validOperation(transfer, result) || result.getState() != expected) {
                unknown(transfer, player, "invalid_completion_response");
                return;
            }
            finish(state, transfer, confirmedDelivered ? "completed" : "refunded");
            tellCurrent(player, transfer, confirmedDelivered ? "取出完成" : "未给予物品，数量已退回仓库");
        }, error -> unknown(transfer, player, confirmedDelivered ? "delivered_ack_unknown" : "refund_result_unknown"));
    }

    /** 由当前已认证账户手动领取明确被中心拒绝的暂存物品 */
    public static void returnItems(ServerPlayer player) {
        if (authenticatedClient(player) == null) return;
        var state = state(player.getServer());
        long userId = PlayerAuthentication.userId(player);
        for (var transfer : state.pending.values().toArray(Transfer[]::new)) {
            if (transfer.identity.getUserId() != userId || transfer.phase != Phase.RETURNABLE) continue;
            if (!tryReturn(player, state, transfer) && transfer.phase == Phase.RETURNABLE) {
                reply(player, "暂存归还需要可用的主背包空间和生存/冒险状态");
            }
            return;
        }
        reply(player, "没有明确可归还的暂存物品；未知结果请联系管理员核查");
    }

    /** 状态查询仅展示结果，绝不借查询响应自动发放或退回物品 */
    public static void status(ServerPlayer player, String operationId) {
        var client = authenticatedClient(player);
        if (client == null) return;
        var state = state(player.getServer());
        long userId = PlayerAuthentication.userId(player);
        if (operationId.isEmpty()) {
            boolean found = false;
            for (var transfer : state.pending.values()) {
                if (transfer.identity.getUserId() != userId) continue;
                reply(player, transfer.operationId + " | " + transfer.phase);
                found = true;
            }
            if (!found) reply(player, "当前没有在途或待核查操作");
            return;
        }
        var completed = state.completed.get(operationId);
        if (completed != null && completed.userId() == userId) reply(player, "本服记录：" + completed.result());
        client.getWarehouseOperation(Warehouse.GetOperationRequest.newBuilder().setPlayer(identity(player))
                .setOperationId(operationId).build()).whenComplete((result, error) -> player.getServer().execute(() -> {
                    if (!current(player, userId)) return;
                    if (error != null) reply(player, "中心操作查询未完成或不存在，不能据此补发或归还");
                    else reply(player, "中心记录：" + result.getOperationId() + " | " + operationLabel(result.getState()));
                }));
    }

    /** 将中心终态翻译为提示，查询结果不改变本地资产状态 */
    private static String operationLabel(Warehouse.OperationState state) {
        return switch (state) {
            case OPERATION_STATE_DEPOSITED -> "已存入";
            case OPERATION_STATE_DEBITED -> "已扣库，等待发放确认";
            case OPERATION_STATE_DELIVERED -> "已发放";
            case OPERATION_STATE_REFUNDED -> "已退回仓库";
            case OPERATION_STATE_REJECTED -> "已拒绝";
            default -> "未知状态，请管理员核查";
        };
    }

    /** 明确失败归还也重新规划整笔容量，离线或背包满时继续保留暂存 */
    private static boolean tryReturn(ServerPlayer player, Transfers state, Transfer transfer) {
        if (transfer.phase != Phase.RETURNABLE || !current(player, transfer.identity.getUserId())) return false;
        net.minecraft.world.item.ItemStack item;
        try {
            item = WarehouseItemCodec.decode(player.registryAccess(), transfer.itemId, transfer.payload);
        } catch (RuntimeException error) {
            log(transfer, "return_restore_failed");
            return false;
        }
        try {
            if (!WarehouseInventory.give(player, item, transfer.amount)) return false;
            finish(state, transfer, "returned");
            reply(player, "暂存物品已全部归还");
            return true;
        } catch (RuntimeException error) {
            unknown(transfer, player, "return_exception");
            return false;
        }
    }

    /** 预检查只读取游戏状态，不保存可能过期的槽位方案 */
    private static boolean canReceive(ServerPlayer player, Transfer transfer, byte[] payload, boolean stackable) {
        if (!current(player, transfer.identity.getUserId()) || !WarehouseInventory.available(player)) return false;
        try {
            var item = WarehouseItemCodec.decode(player.registryAccess(), transfer.itemId, payload);
            return item.isStackable() == stackable && (stackable || transfer.amount == 1)
                    && WarehouseInventory.plan(player.getInventory(), item, transfer.amount) != null;
        } catch (RuntimeException error) {
            return false;
        }
    }

    /** 只有固定业务拒绝状态可以解释为中心未修改资产 */
    private static boolean explicitRejection(Throwable error) {
        return switch (Status.fromThrowable(error).getCode()) {
            case INVALID_ARGUMENT, FAILED_PRECONDITION, NOT_FOUND, RESOURCE_EXHAUSTED -> true;
            default -> false;
        };
    }

    /** 核对原操作主体参数，错误或重复阶段响应不能获得资产应用资格 */
    private static boolean validOperation(Transfer transfer, Warehouse.Operation operation) {
        return operation.getOperationId().equals(transfer.operationId) && operation.getQuantity() == transfer.amount
                && operation.getInventoryId() > 0 && operation.getItemId().equals(transfer.itemId)
                && operation.getItemDataHash().size() == 32
                && (transfer.deposit || operation.getInventoryId() == transfer.inventoryId);
    }

    /** 只有匹配准备载荷的已扣库结果可以授权发放 */
    private static boolean authorizesWithdrawal(Transfer transfer, Warehouse.Operation operation) {
        return validOperation(transfer, operation)
                && operation.getState() == Warehouse.OperationState.OPERATION_STATE_DEBITED
                && operation.getItemDataHash().equals(transfer.hash);
    }

    /** 响应必须仍拥有原暂存条目和预期阶段，终态记录不授予发放资格 */
    private static boolean ownsResponse(Transfers state, Transfer transfer, Phase expected) {
        return state.pending.get(transfer.operationId) == transfer && transfer.phase == expected;
    }

    /** 回调始终切回游戏线程，归属与阶段验证实现单次领取 */
    private static <T> void await(MinecraftServer server, Transfers state, Transfer transfer, Phase expected,
            Supplier<CompletableFuture<T>> send, Consumer<T> success, Consumer<Throwable> failure) {
        startRequest(send).whenComplete((result, error) -> server.execute(() -> {
            if (SERVERS.get(server) != state || !ownsResponse(state, transfer, expected)) return;
            if (error == null) success.accept(result);
            else failure.accept(error);
        }));
    }

    /** 同步发起异常也进入统一失败回调，已暂扣操作不会停留在无日志状态 */
    private static <T> CompletableFuture<T> startRequest(Supplier<CompletableFuture<T>> send) {
        try {
            return java.util.Objects.requireNonNull(send.get());
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    /** 新资产操作要求当前账户无本服未完成记录，未知结果不允许重复扣物 */
    private static RealmMeshRpcClient ready(ServerPlayer player, int amount) {
        var client = authenticatedClient(player);
        if (client == null) return null;
        if (amount <= 0 || amount > MAX_TRANSFER_AMOUNT || !WarehouseInventory.available(player)) {
            reply(player, "数量须为 1–2304，且须处于生存/冒险、背包关闭和鼠标未持物状态");
            return null;
        }
        for (var transfer : state(player.getServer()).pending.values()) {
            if (transfer.identity.getUserId() == PlayerAuthentication.userId(player)) {
                reply(player, "本服仍有未完成操作，请先查看 /realmmesh warehouse status");
                return null;
            }
        }
        return client;
    }

    /** 只接受当前已认证玩家，不复用任意命令参数中的账户 ID */
    private static RealmMeshRpcClient authenticatedClient(ServerPlayer player) {
        if (PlayerAuthentication.userId(player) <= 0) {
            reply(player, "请先完成登录");
            return null;
        }
        var client = NeoForgeRpcLifecycle.clientFor(player.getServer());
        if (client == null) reply(player, "中心通信不可用，请联系管理员");
        return client;
    }

    /** 按当前认证连接构造中心身份 */
    private static Warehouse.PlayerIdentity identity(ServerPlayer player) {
        return Warehouse.PlayerIdentity.newBuilder().setUserId(PlayerAuthentication.userId(player))
                .setPlayerName(player.getGameProfile().getName()).build();
    }

    /** 预检查和准备确认必须引用完全相同的操作参数 */
    private static Warehouse.WithdrawalRequest withdrawalRequest(Transfer transfer) {
        return Warehouse.WithdrawalRequest.newBuilder().setPlayer(transfer.identity)
                .setOperationId(transfer.operationId).setInventoryId(transfer.inventoryId).setQuantity(transfer.amount).build();
    }

    /** 原连接离线、重连或重生后不能将迟到响应应用到新的玩家实例 */
    private static boolean current(ServerPlayer player, long userId) {
        return !player.hasDisconnected() && player.connection.isAcceptingMessages()
                && player.getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && PlayerAuthentication.userId(player) == userId;
    }

    /** 游戏线程按逻辑服务器延迟创建暂存 */
    private static Transfers state(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, ignored -> new Transfers());
    }

    /** 条目和载荷预算在扣物前共同检查 */
    private static boolean capacity(Transfers state, int bytes) {
        return state.pending.size() < MAX_PENDING && capacityBytes(state, bytes);
    }

    /** 不使用压缩比估计内存，只按实际持有的载荷字节计费 */
    private static boolean capacityBytes(Transfers state, int bytes) {
        return bytes <= MAX_PENDING_BYTES - state.payloadBytes;
    }

    /** 替换或释放一份载荷，并同步服务器预算 */
    private static void setPayload(Transfers state, Transfer transfer, byte[] payload) {
        if (transfer.payload != null) state.payloadBytes -= transfer.payload.length;
        transfer.payload = payload;
        if (payload != null) state.payloadBytes += payload.length;
    }

    /** 完成后释放大载荷，只保留有界的轻量终态记录 */
    private static void finish(Transfers state, Transfer transfer, String result) {
        log(transfer, result);
        setPayload(state, transfer, null);
        state.pending.remove(transfer.operationId);
        state.completed.put(transfer.operationId, new Completed(transfer.identity.getUserId(), result));
        if (state.completed.size() > MAX_COMPLETED) state.completed.remove(state.completed.keySet().iterator().next());
    }

    /** 未知结果只封存本地状态和日志，不推测失败并归还 */
    private static void unknown(Transfer transfer, ServerPlayer player, String reason) {
        transfer.phase = Phase.UNKNOWN;
        log(transfer, reason);
        tellCurrent(player, transfer, "操作结果需要核查，请勿重复存取；操作 ID：" + transfer.operationId);
    }

    /** 日志只含资产关联字段，不输出物品大载荷、凭据或玩家密码 */
    private static void log(Transfer transfer, String result) {
        LOGGER.info("event=warehouse_transfer operation_id={} server_id={} user_id={} inventory_id={} item_id={} quantity={} item_data_hash={} phase={} result={}",
                transfer.operationId, transfer.serverId, transfer.identity.getUserId(), transfer.inventoryId,
                transfer.itemId, transfer.amount, java.util.HexFormat.of().formatHex(transfer.hash.toByteArray()), transfer.phase, result);
    }

    /** 反馈只发送给原请求仍有效的认证连接 */
    private static void tellCurrent(ServerPlayer player, Transfer transfer, String message) {
        if (current(player, transfer.identity.getUserId())) reply(player, message);
    }

    /** 仓库反馈不广播 */
    private static void reply(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    /** 停服前输出全部尚未完成操作，不尝试把未知结果当失败补偿 */
    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        clear(event.getServer());
    }

    /** 异常停止也释放本进程暂存，重启后只能依靠日志人工核查 */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        clear(event.getServer());
    }

    /** 撤销状态归属后，迟到网络回调不会继续发物 */
    private static void clear(MinecraftServer server) {
        var state = SERVERS.remove(server);
        if (state != null) state.pending.values().forEach(transfer -> log(transfer, "server_stopped_unresolved"));
    }
}
