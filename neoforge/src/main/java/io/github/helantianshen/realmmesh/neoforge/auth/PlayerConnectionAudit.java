// 玩家连接审计：观察服务端进服和离服事件，尽力上报，不参与认证和门禁决策
package io.github.helantianshen.realmmesh.neoforge.auth;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import realmmesh.audit.v1.Audit;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** 全部状态仅由服务器生命周期线程访问，按服务器和实际玩家实例隔离 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class PlayerConnectionAudit {
    private static final Map<MinecraftServer, Map<ServerPlayer, UUID>> CONNECTIONS = new IdentityHashMap<>();

    /** 事件总线通过静态入口调用，不创建实例 */
    private PlayerConnectionAudit() {}

    /** 每次进服生成随机 UUID；未注册或未登录玩家同样记录，不等待中心响应 */
    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID connectionId = UUID.randomUUID();
        CONNECTIONS.computeIfAbsent(player.getServer(), ignored -> new IdentityHashMap<>()).put(player, connectionId);
        report(player, connectionId, Audit.PlayerConnectionEventType.PLAYER_CONNECTION_EVENT_TYPE_JOINED);
    }

    /** 离服先移除关联，再上报一次；认证被提前清除不影响这里保存的连接 ID */
    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var players = CONNECTIONS.get(player.getServer());
        if (players == null) return;
        UUID connectionId = players.remove(player);
        if (connectionId != null) {
            report(player, connectionId, Audit.PlayerConnectionEventType.PLAYER_CONNECTION_EVENT_TYPE_LEFT);
        }
    }

    /** 仅发起异步调用，统一 RPC 日志记录结果，不等待、不重试，也不保存待补报队列 */
    private static void report(ServerPlayer player, UUID connectionId, Audit.PlayerConnectionEventType eventType) {
        var client = NeoForgeRpcLifecycle.clientFor(player.getServer());
        // 停服时通信可能已经关闭，此时允许缺失记录，不延长游戏服退出过程
        if (client != null) client.reportPlayerConnectionEvent(eventType, player.getGameProfile().getName(), connectionId);
    }

    /** 停服释放关联，不根据剩余玩家集合伪造离服事件 */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CONNECTIONS.remove(event.getServer());
    }
}
