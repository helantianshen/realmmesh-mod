package io.github.helantianshen.realmmesh.neoforge.rpc;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按逻辑服务器实例管理连接，独立服和内置服共用这组生命周期事件
 * 客户端主菜单不会触发服务器启动事件，因此不会创建 RPC 客户端
 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class NeoForgeRpcLifecycle {
    // RPC 回调线程也会查询归属关系，容器必须支持与服务器停机并发访问
    private static final ConcurrentHashMap<MinecraftServer, RealmMeshRpcClient> CLIENTS = new ConcurrentHashMap<>();

    private NeoForgeRpcLifecycle() {}

    @SubscribeEvent
    public static void starting(ServerStartingEvent event) {
        // 读取本地配置快照，不等待中心连接；中心离线不影响游戏服务器启动
        var client = new RealmMeshRpcClient(NeoForgeRpcConfig.snapshot());
        var previous = CLIENTS.put(event.getServer(), client);
        if (previous != null) {
            previous.close();
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        // 先撤销归属，再关闭连接，使关闭触发的回调无法向已停止的服务器发送反馈
        var client = CLIENTS.remove(event.getServer());
        if (client != null) {
            client.close();
        }
    }

    /** 返回当前服务器拥有的客户端，未启动或正在停止时返回 null */
    public static RealmMeshRpcClient clientFor(MinecraftServer server) {
        return CLIENTS.get(server);
    }
}
