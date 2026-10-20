// 中心通信生命周期：按逻辑服务器管理 RPC 客户端归属
// 配置无效时停用中心通信，服务器停止时撤销归属并释放连接

package io.github.helantianshen.realmmesh.neoforge.rpc;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按逻辑服务器实例管理连接，独立服和内置服共用这组生命周期事件
 * 客户端主菜单不会触发服务器启动事件，因此不会创建 RPC 客户端
 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class NeoForgeRpcLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmMesh.MOD_ID);

    // RPC 回调线程也会查询归属关系，容器必须支持与服务器停机并发访问
    private static final ConcurrentHashMap<MinecraftServer, RealmMeshRpcClient> CLIENTS = new ConcurrentHashMap<>();

    /** 生命周期由事件总线调用，不创建此类实例 */
    private NeoForgeRpcLifecycle() {}

    /** 校验启动快照后建立客户端，不在游戏线程等待中心连接 */
    @SubscribeEvent
    public static void starting(ServerStartingEvent event) {
        // 先移除旧归属，即使本次配置无效，也不会留下旧凭据对应的客户端
        var previous = CLIENTS.remove(event.getServer());
        if (previous != null) {
            previous.close();
        }

        RpcConfig config;
        try {
            config = NeoForgeRpcConfig.snapshot();
        } catch (RuntimeException error) {
            // 不记录配置值或异常对象，凭据缺失时只停用中心通信
            LOGGER.error("RealmMesh 中心通信未启用：请检查 config/realmmesh/connection.toml 的连接参数和服务器凭据，修正后重启服务器");
            return;
        }

        var client = new RealmMeshRpcClient(config);
        CLIENTS.put(event.getServer(), client);
    }

    /** 撤销客户端归属并发起关闭，不等待网络资源回收 */
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
