// 中心通信：复用带服务器凭据的 gRPC Channel，异步处理请求和关闭
// 认证信息由启动快照提供，请求正文、凭据和原始异常不写入业务日志

package io.github.helantianshen.realmmesh.rpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.grpc.ManagedChannel;
import io.grpc.CallOptions;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import realmmesh.auth.v1.Auth;
import realmmesh.config.v1.Config;
import realmmesh.config.v1.ConfigServiceGrpc;
import realmmesh.auth.v1.AuthServiceGrpc;
import realmmesh.probe.v1.Probe;
import realmmesh.probe.v1.ProbeServiceGrpc;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 中心通信客户端，每个实例独占并复用一个 Channel，由实例持有者负责关闭
 * 运行期请求异步执行；仅启动检查允许在服务器就绪前有界等待
 */
public final class RealmMeshRpcClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RealmMeshRpcClient.class);
    private static final Metadata.Key<String> REQUEST_ID = Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> SERVER_ID = Metadata.Key.of("x-realmmesh-server-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String ECHO_METHOD = ProbeServiceGrpc.getEchoMethod().getFullMethodName();
    private final ManagedChannel channel;
    private final ProbeServiceGrpc.ProbeServiceFutureStub stub;
    private final AuthServiceGrpc.AuthServiceFutureStub authStub;
    private final ConfigServiceGrpc.ConfigServiceFutureStub configStub;
    private final int timeoutMillis;

    /**
     * 从配置快照创建客户端，创建成功不代表中心已经连接或可用
     * 当前连接使用明文传输，客户端不自动重放失败请求
     */
    public RealmMeshRpcClient(RpcConfig config) {
        Objects.requireNonNull(config, "config");

        // 凭据在实例生命周期内保持不变，拦截器为每次调用附加相同身份
        // Metadata 创建后不再修改，独立请求的 request ID 仍由调用方法设置
        var credentials = new Metadata();
        credentials.put(SERVER_ID, Long.toString(config.serverId()));
        credentials.put(AUTHORIZATION, "Bearer " + config.serverSecret());

        channel = ManagedChannelBuilder.forAddress(config.host(), config.port())
                .usePlaintext()
                .disableRetry()
                .intercept(MetadataUtils.newAttachHeadersInterceptor(credentials))
                .build();
        stub = ProbeServiceGrpc.newFutureStub(channel);
        authStub = AuthServiceGrpc.newFutureStub(channel);
        configStub = ConfigServiceGrpc.newFutureStub(channel);
        timeoutMillis = config.timeoutMillis();
    }

    /**
     * 异步发送消息并返回中心的原样响应，允许空字符串
     * 参数错误、关闭后调用和 RPC 失败均通过返回的 Future 表达
     * 完成回调不保证在游戏线程执行，操作游戏状态前必须切回游戏线程
     */
    public CompletableFuture<String> echo(String message) {
        return invoke(ECHO_METHOD, headers -> stub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                .echo(Probe.EchoRequest.newBuilder().setMessage(Objects.requireNonNull(message, "message")).build()),
                Probe.EchoResponse::getMessage);
    }

    /** 异步提交注册，密码仅用于构造本次请求，不记录或存入客户端字段 */
    public CompletableFuture<Auth.RegisterResponse> register(String playerName, String password) {
        return invoke(AuthServiceGrpc.getRegisterMethod().getFullMethodName(), headers -> authStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                .register(Auth.RegisterRequest.newBuilder().setPlayerName(playerName).setPassword(password).build()),
                Function.identity());
    }

    /** 异步提交登录，业务拒绝由响应状态表达，不自动重试以免重复计数 */
    public CompletableFuture<Auth.LoginResponse> login(String playerName, String password) {
        return invoke(AuthServiceGrpc.getLoginMethod().getFullMethodName(), headers -> authStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                .login(Auth.LoginRequest.newBuilder().setPlayerName(playerName).setPassword(password).build()),
                Function.identity());
    }

    /** 提交当前玩家的旧密码和新密码，复用短期限且不自动重试冲突或未知结果 */
    public CompletableFuture<Auth.ChangePasswordResponse> changePassword(String playerName, String oldPassword, String newPassword) {
        return invoke(AuthServiceGrpc.getChangePasswordMethod().getFullMethodName(), headers -> authStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                .changePassword(Auth.ChangePasswordRequest.newBuilder()
                        .setPlayerName(playerName)
                        .setOldPassword(oldPassword)
                        .setNewPassword(newPassword)
                        .build()), Function.identity());
    }

    /** 拉取中心登录时限，拒绝零值和负数，不使用本地默认值 */
    public CompletableFuture<Integer> getLoginTimeoutSeconds() {
        return invoke(ConfigServiceGrpc.getGetConfigMethod().getFullMethodName(), headers -> configStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                .getConfig(Config.GetConfigRequest.getDefaultInstance()), response -> {
                    int seconds = response.getLoginTimeoutSeconds();
                    if (seconds <= 0) {
                        throw new IllegalArgumentException("中心登录时限必须为正整数秒数");
                    }
                    return seconds;
                });
    }

    /** 建立独立通知流，复用服务器凭据，不套用普通请求的短期限或自动重试 */
    public AccountEventSubscription subscribeAccountEvents() {
        var headers = new Metadata();
        headers.put(REQUEST_ID, UUID.randomUUID().toString());
        var call = channel.newCall(AuthServiceGrpc.getSubscribeAccountEventsMethod(), CallOptions.DEFAULT);
        return new AccountEventSubscription(call, headers);
    }

    /**
     * 为各类调用统一记录关联日志，并将 Guava Future 适配为 CompletableFuture
     * 每次调用独立 metadata，取消向底层传播；日志不读取消息或异常正文
     */
    private <R, T> CompletableFuture<T> invoke(String method,
            Function<Metadata, ListenableFuture<R>> send, Function<R, T> decode) {
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        LOGGER.info("event=rpc_client_start request_id={} method={}", requestId, method);
        var result = new CompletableFuture<T>();
        result.whenComplete((value, error) -> {
            Status.Code code = error == null ? Status.Code.OK : Status.fromThrowable(error).getCode();
            if (result.isCancelled()) code = Status.Code.CANCELLED;
            else if (error instanceof NullPointerException) code = Status.Code.INVALID_ARGUMENT;
            else if (error instanceof IllegalStateException) code = Status.Code.UNAVAILABLE;
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            String format = "event=rpc_client_end request_id={} method={} status={} duration_ms={}";
            if (code == Status.Code.OK) LOGGER.info(format, requestId, method, code, elapsedMillis);
            else LOGGER.warn(format, requestId, method, code, elapsedMillis);
        });
        if (channel.isShutdown()) {
            result.completeExceptionally(new IllegalStateException("RPC client is closed"));
            return result;
        }

        var headers = new Metadata();
        headers.put(REQUEST_ID, requestId);
        try {
            var call = send.apply(headers);
            result.whenComplete((value, error) -> {
                // gRPC Future 只有 cancel(true) 才会调用 ClientCall.cancel 终止网络请求
                if (result.isCancelled()) call.cancel(true);
            });
            // 直接执行器不创建线程池，回调只能转换结果，游戏操作由调用方调度
            Futures.addCallback(call, new FutureCallback<R>() {
                @Override
                public void onSuccess(R response) {
                    try {
                        result.complete(decode.apply(response));
                    } catch (RuntimeException error) {
                        result.completeExceptionally(error);
                    }
                }

                @Override
                public void onFailure(Throwable error) {
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
