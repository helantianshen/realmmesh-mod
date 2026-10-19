package io.github.helantianshen.realmmesh.neoforge.command;

import io.github.helantianshen.realmmesh.RealmMesh;
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

    private RealmMeshCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        // 权限约束放在根命令上，同时限制执行和命令树可见性
        event.getDispatcher().register(Commands.literal("realmmesh")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ping").executes(context -> ping(context.getSource()))));
    }

    private static int ping(CommandSourceStack source) {
        var server = source.getServer();
        var client = NeoForgeRpcLifecycle.clientFor(server);
        if (client == null) {
            source.sendFailure(Component.literal("中心通信尚未就绪"));
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
