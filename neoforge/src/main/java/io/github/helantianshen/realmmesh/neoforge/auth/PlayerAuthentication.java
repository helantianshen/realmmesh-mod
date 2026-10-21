// 玩家认证：维护当前游戏连接的认证状态并处理中心结果
// 全部状态和游戏操作在服务器线程执行，不实现移动或交互门禁

package io.github.helantianshen.realmmesh.neoforge.auth;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import io.grpc.Status;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import realmmesh.auth.v1.Auth;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** 按逻辑服务器和玩家对象实例区分连接，不将 UUID 作为中心账户身份 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class PlayerAuthentication {
    private static final Map<MinecraftServer, Map<ServerPlayer, ConnectionState>> STATES = new IdentityHashMap<>();

    /** 本次连接的认证结果及在途请求，不保存密码或认证请求消息 */
    private static final class ConnectionState {
        private boolean authenticated;
        private CompletableFuture<?> pending;
    }

    /** 仅通过服务器事件与命令调用 */
    private PlayerAuthentication() {}

    /** 进服时建立全新的未登录状态，休息期不会阻止进服本身 */
    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var players = STATES.computeIfAbsent(player.getServer(), ignored -> new IdentityHashMap<>());
            var previous = players.put(player, new ConnectionState());
            cancel(previous);
            reply(player, "请使用 /realmmesh register <密码> <确认密码> 注册，或 /realmmesh login <密码> 登录");
        }
    }

    /** 离线先移除归属，再取消请求，迟到响应不能命中另一连接 */
    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var players = STATES.get(player.getServer());
            if (players != null) cancel(players.remove(player));
        }
    }

    /** 停服时清理全部状态，取消操作不保证中心尚未提交事务 */
    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        var players = STATES.remove(event.getServer());
        if (players != null) players.values().forEach(PlayerAuthentication::cancel);
    }

    /** 只读取本次连接的状态，调用方必须位于服务器线程 */
    public static boolean isAuthenticated(ServerPlayer player) {
        var state = stateFor(player);
        return state != null && state.authenticated;
    }

    /** 注册成功直接认证本次连接，不再额外发送登录请求 */
    public static void register(ServerPlayer player, String password) {
        var state = ready(player);
        if (state == null) return;

        var client = NeoForgeRpcLifecycle.clientFor(player.getServer());
        if (client == null) {
            reply(player, "中心通信未启用，请联系服务器管理员");
            return;
        }

        var request = client.register(player.getGameProfile().getName(), password);
        await(player, state, client, request, response -> {
            if (!validIdentity(player, response.getUserId(), response.getPlayerName())) {
                reply(player, "中心认证响应无效，请联系服务器管理员");
                return;
            }

            state.authenticated = true;
            reply(player, "注册成功，已登录");
        });
    }

    /** 中心负责密码规则与失败计数，本地只处理协议结果和当前连接 */
    public static void login(ServerPlayer player, String password) {
        var state = ready(player);
        if (state == null) return;

        var client = NeoForgeRpcLifecycle.clientFor(player.getServer());
        if (client == null) {
            reply(player, "中心通信未启用，请联系服务器管理员");
            return;
        }

        var request = client.login(player.getGameProfile().getName(), password);
        await(player, state, client, request, response -> applyLogin(player, state, response));
    }

    /** 取得可提交请求的连接，已登录或已有请求时不重复调用中心 */
    private static ConnectionState ready(ServerPlayer player) {
        var state = stateFor(player);
        if (state == null) {
            reply(player, "当前连接认证状态不可用，请重新进入服务器");
            return null;
        }
        if (state.authenticated) {
            reply(player, "你已经登录");
            return null;
        }
        if (state.pending != null) {
            reply(player, "认证请求正在处理中，请稍候");
            return null;
        }
        return state;
    }

    /**
     * 登记在途请求后立即返回，完成时切回服务器线程
     * 连接实例、状态对象、客户端和请求均须仍属于原请求，避免重连或停机竞争
     * 归属有效时先释放请求中状态，再反馈失败或应用业务结果，允许失败后手动重试
     */
    private static <T> void await(ServerPlayer player, ConnectionState state, RealmMeshRpcClient client,
            CompletableFuture<T> request, Consumer<T> accept) {
        var server = player.getServer();
        state.pending = request;
        reply(player, "正在向中心提交认证请求");

        request.whenComplete((response, error) -> {
            // 网络回调只负责投递，状态与玩家对象的检查仍在游戏线程执行
            if (NeoForgeRpcLifecycle.clientFor(server) != client) return;
            server.execute(() -> {
                if (stateFor(player) != state || state.pending != request
                        || server.getPlayerList().getPlayer(player.getUUID()) != player) return;

                state.pending = null;
                if (NeoForgeRpcLifecycle.clientFor(server) != client) return;
                if (error != null) {
                    reply(player, errorMessage(error));
                    return;
                }
                accept.accept(response);
            });
        });
    }

    /**
     * 先区分未注册结果，再核对已存在账户的身份，最后按状态执行反馈
     * 协议字段必须相互一致；未知或矛盾响应只提示错误，不能认证玩家
     * 只有 SUCCEEDED 可以置为已登录，休息结果始终执行本次连接的踢出操作
     */
    private static void applyLogin(ServerPlayer player, ConnectionState state, Auth.LoginResponse response) {
        boolean idleFields = !response.getKick() && !response.hasCooldownUntil() && response.getRetryAfterSeconds() == 0;
        if (response.getStatus() == Auth.LoginStatus.LOGIN_STATUS_UNREGISTERED) {
            if (idleFields && response.getUserId() == 0 && response.getPlayerName().isEmpty()
                    && response.getRemainingAttempts() == 0) {
                reply(player, "账户尚未注册，请先使用 /realmmesh register <密码> <确认密码>");
            } else {
                reply(player, "中心认证响应无效，请联系服务器管理员");
            }
            return;
        }

        if (!validIdentity(player, response.getUserId(), response.getPlayerName())) {
            reply(player, "中心认证响应无效，请联系服务器管理员");
            return;
        }

        switch (response.getStatus()) {
            case LOGIN_STATUS_SUCCEEDED, LOGIN_STATUS_WRONG_PASSWORD -> {
                if (!idleFields || response.getRemainingAttempts() <= 0) {
                    reply(player, "中心认证响应无效，请联系服务器管理员");
                    return;
                }
                if (response.getStatus() == Auth.LoginStatus.LOGIN_STATUS_SUCCEEDED) {
                    state.authenticated = true;
                    reply(player, "登录成功");
                } else {
                    reply(player, "密码错误，还可尝试 " + response.getRemainingAttempts() + " 次");
                }
            }
            case LOGIN_STATUS_COOLING_DOWN -> {
                var deadline = response.getCooldownUntil();
                // Timestamp 的上限为公元 9999 年末，纳秒部分必须处于单秒范围内
                boolean validDeadline = response.hasCooldownUntil()
                        && deadline.getSeconds() > 0 && deadline.getSeconds() <= 253402300799L
                        && deadline.getNanos() >= 0 && deadline.getNanos() <= 999999999;
                if (!response.getKick() || response.getRemainingAttempts() != 0
                        || response.getRetryAfterSeconds() <= 0 || !validDeadline) {
                    reply(player, "中心认证响应无效，请联系服务器管理员");
                    return;
                }
                // 使用中心计算的秒数提示，每次休息结果均断开本次连接，不本地延长截止时间
                player.connection.disconnect(Component.literal("登录尝试过多，请在 "
                        + response.getRetryAfterSeconds() + " 秒后重新进入服务器"));
            }
            default -> reply(player, "中心认证响应无效，请联系服务器管理员");
        }
    }

    /** 当前游戏名字与中心原始大小写名字可以不同，但必须属于同一账户 */
    private static boolean validIdentity(ServerPlayer player, long userId, String playerName) {
        return userId > 0 && player.getGameProfile().getName().equalsIgnoreCase(playerName);
    }

    /** 按实例取得状态，不会因相同 UUID 或名字复用其他玩家的认证结果 */
    private static ConnectionState stateFor(ServerPlayer player) {
        var players = STATES.get(player.getServer());
        return players == null ? null : players.get(player);
    }

    /** 撤销在途请求的本地归属后取消 Future，不阻塞等待网络退出 */
    private static void cancel(ConnectionState state) {
        if (state != null && state.pending != null) {
            var request = state.pending;
            state.pending = null;
            request.cancel(false);
        }
    }

    /** 将状态码映射为固定提示，不显示远端错误描述或异常对象 */
    private static String errorMessage(Throwable error) {
        return switch (Status.fromThrowable(error).getCode()) {
            case ALREADY_EXISTS -> "玩家名字已被占用，请使用登录命令";
            case INVALID_ARGUMENT -> "玩家名字或注册密码格式不符合要求";
            case UNAUTHENTICATED -> "游戏服凭据无效，请联系服务器管理员";
            case UNAVAILABLE, DEADLINE_EXCEEDED -> "中心暂时不可用或请求超时，请稍后手动重试";
            default -> "认证请求未完成，请稍后手动重试或联系服务器管理员";
        };
    }

    /** 只发送给当前玩家，不广播或记录包含敏感参数的命令 */
    private static void reply(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}
