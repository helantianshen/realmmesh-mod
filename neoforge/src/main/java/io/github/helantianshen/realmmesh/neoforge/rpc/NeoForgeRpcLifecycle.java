// 中心通信生命周期：按逻辑服务器管理 RPC 客户端归属
// 启动检查失败时中止本次服务器启动，服务器停止时撤销归属并释放连接

package io.github.helantianshen.realmmesh.neoforge.rpc;

import io.github.helantianshen.realmmesh.RealmMesh;
import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import io.github.helantianshen.realmmesh.rpc.CenterConnection;
import io.github.helantianshen.realmmesh.rpc.CenterStartupException;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按逻辑服务器实例管理连接，独立服和内置服共用这组生命周期事件
 * 客户端主菜单不会触发服务器启动事件，因此不会创建 RPC 客户端
 */
@EventBusSubscriber(modid = RealmMesh.MOD_ID)
public final class NeoForgeRpcLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmMesh.MOD_ID);

    // RPC 回调线程也会查询归属关系，容器必须支持与服务器停机并发访问
    private static final ConcurrentHashMap<MinecraftServer, CenterConnection> CLIENTS = new ConcurrentHashMap<>();

    /** 生命周期由事件总线调用，不创建此类实例 */
    private NeoForgeRpcLifecycle() {}

    /** 在服务器就绪前有界等待健康通信与配置，两者通过后才发布客户端 */
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
            // 不记录配置值或异常对象，本地配置无效时中止启动
            LOGGER.error("RealmMesh 启动失败 stage=本地配置：请检查 config/realmmesh/connection.toml 的连接参数和服务器凭据");
            throw new CenterStartupException("RealmMesh 本地连接配置无效，请检查 connection.toml 和服务器日志");
        }

        // NeoForge 的 debug.log 默认接收 DEBUG，HTTP/2 帧日志会包含认证头及请求字节
        // 只限制 gRPC 客户端帧记录器，保留项目的请求关联日志和传输错误日志
        Configurator.setLevel("io.grpc.netty.shaded.io.grpc.netty.NettyClientHandler", Level.INFO);

        var client = new CenterConnection(config);
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
        var connection = CLIENTS.get(server);
        return connection == null ? null : connection.client();
    }

    /** 在服务器线程应用配置刷新，网络等待不会阻塞 tick */
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var connection = CLIENTS.get(event.getServer());
        if (connection != null) connection.tick();
    }

    /** 异常停止也会触发此事件，清理未经过正常 stopping 的连接 */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var connection = CLIENTS.remove(event.getServer());
        if (connection != null) connection.close();
    }

    /** 提供当前中心登录时限，未就绪时不返回默认值 */
    public static int loginTimeoutSeconds(MinecraftServer server) {
        var connection = CLIENTS.get(server);
        if (connection == null) throw new IllegalStateException("中心配置尚未就绪");
        return connection.loginTimeoutSeconds();
    }
}
