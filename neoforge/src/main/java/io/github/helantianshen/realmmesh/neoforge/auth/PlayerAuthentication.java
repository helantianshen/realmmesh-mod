// 玩家认证：维护当前游戏连接的认证状态并处理中心结果
// 全部状态和游戏操作在服务器线程执行，认证结果决定门禁是否解除

package io.github.helantianshen.realmmesh.neoforge.auth;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.network.AuthStatePayload;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import io.grpc.Status;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import realmmesh.auth.v1.Auth;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 按逻辑服务器和玩家对象实例区分连接，不将 UUID 作为中心账户身份 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class PlayerAuthentication {
    private static final Map<MinecraftServer, Map<ServerPlayer, ConnectionState>> STATES = new IdentityHashMap<>();

    /** 本次连接的认证结果及在途请求，不保存密码或认证请求消息 */
    private static final class ConnectionState {
        private boolean authenticated;
        private CompletableFuture<?> pending;
        private final LoginDeadline deadline;
        private Vec3 anchor;
        private long nextHintNanos;

        /** 只在连接建立时取一次中心时限，同时记录临时固定位置 */
        private ConnectionState(ServerPlayer player, int timeoutSeconds) {
            deadline = new LoginDeadline(timeoutSeconds, System.nanoTime());
            anchor = player.position();
        }
    }

    /** 仅通过服务器事件与命令调用 */
    private PlayerAuthentication() {}

    /** 进服时建立全新的未登录状态，休息期不会阻止进服本身 */
    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var players = STATES.computeIfAbsent(player.getServer(), ignored -> new IdentityHashMap<>());
            int timeoutSeconds = NeoForgeRpcLifecycle.loginTimeoutSeconds(player.getServer());
            var previous = players.put(player, new ConnectionState(player, timeoutSeconds));
            cancel(previous);
            player.stopRiding();
            player.stopUsingItem();
            AuthStatePayload.send(player, false);
            reply(player, "请在 " + timeoutSeconds + " 秒内完成登录，超时将断开连接");
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

    /**
     * 在服务器线程处理已校验的账户通知，名字匹配不区分大小写
     * 先撤销状态再取消请求并踢出，避免迟到的认证响应恢复这条连接
     */
    public static void accountChanged(MinecraftServer server, Auth.AccountEvent event) {
        String message = switch (event.getReason()) {
            case ACCOUNT_EVENT_REASON_PASSWORD_CHANGED -> "账户密码已变更，请重新进入并登录";
            case ACCOUNT_EVENT_REASON_ACCOUNT_DELETED -> "账户已删除，连接已断开";
            default -> null;
        };
        if (message == null) return;

        // 复制在线列表，断开连接引发玩家移除时不影响本次遍历
        var players = STATES.get(server);
        for (var player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (!player.getGameProfile().getName().equalsIgnoreCase(event.getPlayerName())) continue;
            var state = players == null ? null : players.remove(player);
            cancel(state);
            player.connection.disconnect(Component.literal(message));
        }
    }

    /** 在 tick 或认证结果落地前检查期限，超时先撤销归属再断开连接 */
    private static boolean expire(ServerPlayer player, ConnectionState state) {
        if (state.authenticated || !state.deadline.expired(System.nanoTime())) return false;
        STATES.get(player.getServer()).remove(player);
        cancel(state);
        player.connection.disconnect(Component.literal("登录超时，请重新进入服务器后完成登录"));
        return true;
    }

    /** 按进服时的期限检查超时，并发送最多两次剩余时间提醒 */
    public static void tick(ServerPlayer player) {
        var state = stateFor(player);
        if (state == null || state.authenticated || expire(player, state)) return;
        int remaining = state.deadline.reminder(System.nanoTime());
        if (remaining > 0) reply(player, "请在剩余 " + remaining + " 秒内完成登录");
    }

    /** 返回本次连接的固定位置，状态尚未建立时仍不放行移动 */
    public static Vec3 anchor(ServerPlayer player) {
        var state = stateFor(player);
        return state == null ? player.position() : state.anchor;
    }

    /** 操作拦截提示最多每两秒一次，不记录玩家命令或密码 */
    public static void remindBlocked(ServerPlayer player) {
        var state = stateFor(player);
        if (state == null) return;
        long now = System.nanoTime();
        if (state.nextHintNanos != 0 && now - state.nextHintNanos < 0) return;
        state.nextHintNanos = now + TimeUnit.SECONDS.toNanos(2);
        reply(player, "请先使用 /realmmesh login 或 /realmmesh register 完成认证");
    }

    /** 成功结果只解除当前连接的门禁，并同步客户端界面状态 */
    private static void authenticate(ServerPlayer player, ConnectionState state) {
        state.authenticated = true;
        AuthStatePayload.send(player, true);
        player.containerMenu.sendAllDataToRemote();
    }

    /** 重生仅在仍使用同一个网络连接时迁移状态，取消旧玩家对象的在途请求 */
    @SubscribeEvent
    public static void cloned(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)
                || !(event.getEntity() instanceof ServerPlayer replacement)) return;
        var players = STATES.get(original.getServer());
        if (players == null) return;
        var state = players.remove(original);
        cancel(state);
        if (state != null && original.connection == replacement.connection) {
            players.put(replacement, state);
        }
    }

    /** 重生位置确定后更新位置锚点，沿用原连接截止时间而不重新计时 */
    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = stateFor(player);
            if (state != null) state.anchor = player.position();
            AuthStatePayload.send(player, isAuthenticated(player));
        }
    }

    /** 异常停服也清理认证状态，迟到结果无法恢复旧连接 */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var players = STATES.remove(event.getServer());
        if (players != null) players.values().forEach(PlayerAuthentication::cancel);
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

            authenticate(player, state);
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
        if (expire(player, state)) return null;
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

                if (expire(player, state)) return;
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
                    authenticate(player, state);
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
            request.cancel(true);
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
