package io.github.helantianshen.realmmesh.rpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import realmmesh.probe.v1.Probe;
import realmmesh.probe.v1.ProbeServiceGrpc;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 中心通信客户端，每个实例独占并复用一个 Channel，由实例持有者负责关闭
 * 请求异步执行，调用方不应在游戏线程通过 get 或 join 等待结果
 */
public final class RealmMeshRpcClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmMeshRpcClient.class);
    private static final Metadata.Key<String> REQUEST_ID = Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final String ECHO_METHOD = ProbeServiceGrpc.getEchoMethod().getFullMethodName();
    private final ManagedChannel channel;
    private final ProbeServiceGrpc.ProbeServiceFutureStub stub;
    private final int timeoutMillis;

    /**
     * 从配置快照创建客户端，创建成功不代表中心已经连接或可用
     * 当前连接使用明文传输，客户端不自动重放失败请求
     */
    public RealmMeshRpcClient(RpcConfig config) {
        Objects.requireNonNull(config, "config");
        channel = ManagedChannelBuilder.forAddress(config.host(), config.port())
                .usePlaintext()
                .disableRetry()
                .build();
        stub = ProbeServiceGrpc.newFutureStub(channel);
        timeoutMillis = config.timeoutMillis();
    }

    /**
     * 异步发送消息并返回中心的原样响应，允许空字符串
     * 参数错误、关闭后调用和 RPC 失败均通过返回的 Future 表达
     * 完成回调不保证在游戏线程执行，操作游戏状态前必须切回游戏线程
     */
    public CompletableFuture<String> echo(String message) {
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        LOGGER.info("event=rpc_client_start request_id={} method={}", requestId, ECHO_METHOD);
        var result = new CompletableFuture<String>();
        // 结果日志不依赖命令层回调，玩家离线或服务器停止也会记录 RPC 完成状态
        result.whenComplete((value, error) -> {
            Status.Code code = error == null ? Status.Code.OK : Status.fromThrowable(error).getCode();
            if (result.isCancelled()) code = Status.Code.CANCELLED;
            else if (error instanceof NullPointerException) code = Status.Code.INVALID_ARGUMENT;
            else if (error instanceof IllegalStateException) code = Status.Code.UNAVAILABLE;
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            // 只记录固定字段与状态枚举，不传入正文、配置、metadata 或异常对象
            String format = "event=rpc_client_end request_id={} method={} status={} duration_ms={}";
            if (code == Status.Code.OK) LOGGER.info(format, requestId, ECHO_METHOD, code, elapsedMillis);
            else LOGGER.warn(format, requestId, ECHO_METHOD, code, elapsedMillis);
        });
        if (message == null) {
            result.completeExceptionally(new NullPointerException("message"));
            return result;
        }
        if (channel.isShutdown()) {
            result.completeExceptionally(new IllegalStateException("RPC client is closed"));
            return result;
        }

        // 每次调用独立携带 ID，不能修改共享 stub 的 metadata，以免并发请求串号
        var headers = new Metadata();
        headers.put(REQUEST_ID, requestId);
        // 为每次调用创建独立 deadline，避免复用一个逐渐过期的截止时间
        try {
            var call = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                    .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                    .echo(Probe.EchoRequest.newBuilder().setMessage(message).build());
            // 将调用方的取消操作传递给底层 RPC，避免只取消包装结果而让网络请求继续运行
            result.whenComplete((value, error) -> {
                if (result.isCancelled()) {
                    call.cancel(false);
                }
            });
            // 适配生成 stub 的 Guava Future，不创建额外线程池或阻塞等待结果
            // directExecutor 也可能执行调用方的同步完成回调，因此回调中不应执行阻塞工作
            Futures.addCallback(call, new FutureCallback<Probe.EchoResponse>() {
                @Override
                public void onSuccess(Probe.EchoResponse response) {
                    result.complete(response.getMessage());
                }

                @Override
                public void onFailure(Throwable error) {
                    // 保留原始异常，调用方可通过 Status.fromThrowable 区分不可用与超时等状态
                    result.completeExceptionally(error);
                }
            }, MoreExecutors.directExecutor());
        } catch (RuntimeException error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    /**
     * 发起关闭并终止在途请求，可重复调用，资源由 gRPC 异步释放
     * 本方法不等待 Channel 完全终止，返回时不代表所有资源已经释放
     * 与关闭并发发起的请求由 gRPC 终止，已完成的请求结果不受影响
     */
    @Override
    public void close() {
        channel.shutdownNow();
    }
}
