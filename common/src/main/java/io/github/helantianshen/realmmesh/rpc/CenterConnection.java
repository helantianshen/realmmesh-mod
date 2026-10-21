// 中心连接状态：启动检查、配置快照和运行期间定期刷新

package io.github.helantianshen.realmmesh.rpc;

import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 每个逻辑服务器持有一个实例，刷新与关闭均在服务器线程执行 */
public final class CenterConnection implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CenterConnection.class);
    private static final long REFRESH_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);
    private final RpcConfig config;
    private final RealmMeshRpcClient client;
    private int loginTimeoutSeconds;
    private long nextRefreshNanos;
    private CompletableFuture<Integer> pendingRefresh;
    private boolean closed;

    /** 创建连接并依次检查 Echo 与配置，任一步失败立即关闭客户端 */
    public CenterConnection(RpcConfig config) {
        this.config = config;
        this.client = new RealmMeshRpcClient(config);
        String stage = "健康通信";
        try {
            String probe = "realmmesh-startup";
            if (!probe.equals(await(client.echo(probe)))) {
                throw new IllegalArgumentException("Echo 响应不匹配");
            }

            stage = "配置拉取";
            loginTimeoutSeconds = await(client.getLoginTimeoutSeconds());
            nextRefreshNanos = System.nanoTime() + REFRESH_INTERVAL_NANOS;
            LOGGER.info("中心启动检查通过 host={} port={} server_id={} login_timeout_seconds={}",
                    config.host(), config.port(), config.serverId(), loginTimeoutSeconds);
        } catch (Exception error) {
            client.close();
            String reason = safeReason(error);
            logFailure(stage, reason);
            throw new CenterStartupException("RealmMesh " + stage + "失败：" + reason + "，请检查服务器日志");
        }
    }

    /** 有界等待仅用于启动；超时或中断时主动取消底层请求 */
    private <T> T await(CompletableFuture<T> request) throws ExecutionException, TimeoutException, InterruptedException {
        try {
            return request.get(config.timeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException error) {
            request.cancel(true);
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            throw error;
        }
    }

    /** 返回启动检查通过后共享的 RPC 客户端 */
    public RealmMeshRpcClient client() {
        return client;
    }

    /** 返回当前配置，调用方在玩家进入时取一次值以固定该连接期限 */
    public int loginTimeoutSeconds() {
        return loginTimeoutSeconds;
    }

    /**
     * 每个服务器 tick 调用，网络请求异步执行，不阻塞游戏线程
     * 完成结果在本线程应用；失败保留旧值，在下一周期继续拉取
     */
    public void tick() {
        if (closed) return;

        if (pendingRefresh != null && pendingRefresh.isDone()) {
            try {
                // 已完成的 Future 不会阻塞；校验在 RPC 解码时完成
                loginTimeoutSeconds = pendingRefresh.join();
            } catch (RuntimeException error) {
                logFailure("配置刷新", safeReason(error));
            }
            pendingRefresh = null;
        }

        long now = System.nanoTime();
        if (pendingRefresh == null && now - nextRefreshNanos >= 0) {
            nextRefreshNanos = now + REFRESH_INTERVAL_NANOS;
            pendingRefresh = client.getLoginTimeoutSeconds();
        }
    }

    /** 使用固定原因说明状态，不把远端异常正文或凭据写入日志 */
    private static String safeReason(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return switch (cause) {
            case TimeoutException timeoutException -> "DEADLINE_EXCEEDED：请求超时";
            case InterruptedException interruptedException -> "CANCELLED：启动等待已中断";
            case IllegalArgumentException illegalArgumentException -> "INVALID_RESPONSE：中心返回值无效";
            default -> switch (Status.fromThrowable(error).getCode()) {
                case UNAUTHENTICATED -> "UNAUTHENTICATED：服务器凭据无效或服务器已停用";
                case UNAVAILABLE -> "UNAVAILABLE：中心或依赖暂不可用，请检查地址、端口和服务状态";
                case DEADLINE_EXCEEDED -> "DEADLINE_EXCEEDED：请求超时";
                case INTERNAL -> "INTERNAL：中心配置读取或处理失败";
                case CANCELLED -> "CANCELLED：请求已取消";
                default -> "UNKNOWN：中心通信失败";
            };
        };
    }

    /** 记录失败阶段和连接定位信息，关联 ID 由 RPC 客户端日志提供 */
    private void logFailure(String stage, String reason) {
        LOGGER.error("中心通信失败 stage={} host={} port={} server_id={} reason={}",
                stage, config.host(), config.port(), config.serverId(), reason);
    }

    /** 关闭后不再应用结果，取消在途刷新并释放 Channel */
    @Override
    public void close() {
        closed = true;
        if (pendingRefresh != null) {
            pendingRefresh.cancel(true);
            pendingRefresh = null;
        }
        client.close();
    }
}
