// 游戏命令：玩家注册、登录、自助改密及管理员中心连通性探测
// 在游戏线程返回固定反馈，不向玩家暴露连接配置或原始异常

package io.github.helantianshen.realmmesh.neoforge.command;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.auth.PlayerAuthentication;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import io.grpc.Status;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class RealmMeshCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmMesh.MOD_ID);
    private static final String PING_MESSAGE = "RealmMesh ping";

    /** 命令通过事件注册，不创建实例 */
    private RealmMeshCommand() {}

    /** 注册管理员探测和普通玩家认证命令 */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        // 玩家认证只限制实际玩家，管理员权限单独约束 ping 子命令
        event.getDispatcher().register(Commands.literal("realmmesh")
                .then(Commands.literal("ping").requires(source -> source.hasPermission(2))
                        .executes(context -> ping(context.getSource())))
                .then(Commands.literal("register").requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> usage(context.getSource(), true))
                        .then(Commands.argument("credentials", StringArgumentType.greedyString())
                                .executes(context -> registerPlayer(context.getSource(),
                                        StringArgumentType.getString(context, "credentials")))))
                .then(Commands.literal("password").requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> passwordUsage(context.getSource()))
                        .then(Commands.argument("credentials", StringArgumentType.greedyString())
                                .executes(context -> changePassword(context.getSource(),
                                        StringArgumentType.getString(context, "credentials")))))
                .then(Commands.literal("login").requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(context -> usage(context.getSource(), false))
                        .then(Commands.argument("password", StringArgumentType.greedyString())
                                .executes(context -> loginPlayer(context.getSource(),
                                        StringArgumentType.getString(context, "password"))))));
    }

    /** greedyString 保留允许的特殊符号，不使用 Brigadier word 的受限字符集 */
    private static int registerPlayer(CommandSourceStack source, String credentials) {
        // 密码规则禁止空格，两个参数只按分隔空格拆分，不修剪或改变密码内容
        String[] parts = credentials.split(" ", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) return usage(source, true);
        if (!parts[0].equals(parts[1])) {
            source.sendFailure(Component.literal("两次输入的密码不一致"));
            return 0;
        }
        PlayerAuthentication.register((ServerPlayer) source.getEntity(), parts[0]);
        return 1;
    }

    /** 只比较新密码确认，不在本地验证旧密码或累计失败次数 */
    private static int changePassword(CommandSourceStack source, String credentials) {
        // greedyString 保留特殊字符，参数不修剪、不转义，也不回显到用法提示
        String[] parts = credentials.split(" ", -1);
        if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
            return passwordUsage(source);
        }
        if (!parts[1].equals(parts[2])) {
            source.sendFailure(Component.literal("两次输入的新密码不一致"));
            return 0;
        }
        PlayerAuthentication.changePassword((ServerPlayer) source.getEntity(), parts[0], parts[1]);
        return 1;
    }

    /** 改密用法仅包含参数名称，不包含用户输入 */
    private static int passwordUsage(CommandSourceStack source) {
        source.sendFailure(Component.literal("用法：/realmmesh password <旧密码> <新密码> <确认新密码>"));
        return 0;
    }

    /** 原样提交登录密码，格式错误也由中心按现有规则计数 */
    private static int loginPlayer(CommandSourceStack source, String password) {
        PlayerAuthentication.login((ServerPlayer) source.getEntity(), password);
        return 1;
    }

    /** 固定用法提示不包含原始命令或密码 */
    private static int usage(CommandSourceStack source, boolean registration) {
        source.sendFailure(Component.literal(registration
                ? "用法：/realmmesh register <密码> <确认密码>"
                : "用法：/realmmesh login <密码>"));
        return 0;
    }

    /** 异步探测中心，只在原连接仍有效时反馈 */
    private static int ping(CommandSourceStack source) {
        var server = source.getServer();
        var client = NeoForgeRpcLifecycle.clientFor(server);
        if (client == null) {
            source.sendFailure(Component.literal("中心通信未启用，请检查服务端连接配置与服务器凭据，修正后重启服务器"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("正在测试中心连接"), false);
        long started = System.nanoTime();
        // 此处只注册回调，立即将游戏线程交还给服务器，不等待网络结果
        client.echo(PING_MESSAGE).whenComplete((response, error) -> {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            // 关闭后的回调不再入队；入队后仍须在游戏线程重新检查，覆盖停机竞争窗口
            if (NeoForgeRpcLifecycle.clientFor(server) != client) {
                return;
            }
            server.execute(() -> {
                if (NeoForgeRpcLifecycle.clientFor(server) != client) {
                    return;
                }
                if (source.getEntity() instanceof ServerPlayer player
                        && server.getPlayerList().getPlayer(player.getUUID()) != player) {
                    return;
                }
                if (error != null) {
                    // 调用结果由 RPC 层统一记录，玩家反馈不包含可能携带配置的原始异常
                    String code = Status.fromThrowable(error).getCode().name();
                    source.sendFailure(Component.literal("中心连接测试失败（" + code + "），请联系服务器管理员"));
                } else if (!PING_MESSAGE.equals(response)) {
                    // 不回显任意远端内容，避免错误响应携带服务端信息进入聊天消息
                    LOGGER.warn("RealmMesh ping response did not match the request");
                    source.sendFailure(Component.literal("中心响应校验失败，请联系服务器管理员"));
                } else {
                    // false 表示只回复命令来源，不向其他管理员广播
                    source.sendSuccess(() -> Component.literal("中心连接正常：" + PING_MESSAGE + "（" + elapsedMillis + " ms）"), false);
                }
            });
        });
        return 1;
    }
}
