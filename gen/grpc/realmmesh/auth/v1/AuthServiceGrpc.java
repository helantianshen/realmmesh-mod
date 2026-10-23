package realmmesh.auth.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * AuthService 校验玩家账户，当前连接的登录状态由游戏服维护
 * 每次调用均要求有效服务器凭据，密码和完整请求不得写入日志
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.2)",
    comments = "Source: auth/v1/auth.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuthServiceGrpc {

  private AuthServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "realmmesh.auth.v1.AuthService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.RegisterRequest,
      realmmesh.auth.v1.Auth.RegisterResponse> getRegisterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Register",
      requestType = realmmesh.auth.v1.Auth.RegisterRequest.class,
      responseType = realmmesh.auth.v1.Auth.RegisterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.RegisterRequest,
      realmmesh.auth.v1.Auth.RegisterResponse> getRegisterMethod() {
    io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.RegisterRequest, realmmesh.auth.v1.Auth.RegisterResponse> getRegisterMethod;
    if ((getRegisterMethod = AuthServiceGrpc.getRegisterMethod) == null) {
      synchronized (AuthServiceGrpc.class) {
        if ((getRegisterMethod = AuthServiceGrpc.getRegisterMethod) == null) {
          AuthServiceGrpc.getRegisterMethod = getRegisterMethod =
              io.grpc.MethodDescriptor.<realmmesh.auth.v1.Auth.RegisterRequest, realmmesh.auth.v1.Auth.RegisterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Register"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.RegisterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.RegisterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthServiceMethodDescriptorSupplier("Register"))
              .build();
        }
      }
    }
    return getRegisterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.LoginRequest,
      realmmesh.auth.v1.Auth.LoginResponse> getLoginMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Login",
      requestType = realmmesh.auth.v1.Auth.LoginRequest.class,
      responseType = realmmesh.auth.v1.Auth.LoginResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.LoginRequest,
      realmmesh.auth.v1.Auth.LoginResponse> getLoginMethod() {
    io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.LoginRequest, realmmesh.auth.v1.Auth.LoginResponse> getLoginMethod;
    if ((getLoginMethod = AuthServiceGrpc.getLoginMethod) == null) {
      synchronized (AuthServiceGrpc.class) {
        if ((getLoginMethod = AuthServiceGrpc.getLoginMethod) == null) {
          AuthServiceGrpc.getLoginMethod = getLoginMethod =
              io.grpc.MethodDescriptor.<realmmesh.auth.v1.Auth.LoginRequest, realmmesh.auth.v1.Auth.LoginResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Login"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.LoginRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.LoginResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthServiceMethodDescriptorSupplier("Login"))
              .build();
        }
      }
    }
    return getLoginMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.ChangePasswordRequest,
      realmmesh.auth.v1.Auth.ChangePasswordResponse> getChangePasswordMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ChangePassword",
      requestType = realmmesh.auth.v1.Auth.ChangePasswordRequest.class,
      responseType = realmmesh.auth.v1.Auth.ChangePasswordResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.ChangePasswordRequest,
      realmmesh.auth.v1.Auth.ChangePasswordResponse> getChangePasswordMethod() {
    io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.ChangePasswordRequest, realmmesh.auth.v1.Auth.ChangePasswordResponse> getChangePasswordMethod;
    if ((getChangePasswordMethod = AuthServiceGrpc.getChangePasswordMethod) == null) {
      synchronized (AuthServiceGrpc.class) {
        if ((getChangePasswordMethod = AuthServiceGrpc.getChangePasswordMethod) == null) {
          AuthServiceGrpc.getChangePasswordMethod = getChangePasswordMethod =
              io.grpc.MethodDescriptor.<realmmesh.auth.v1.Auth.ChangePasswordRequest, realmmesh.auth.v1.Auth.ChangePasswordResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ChangePassword"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.ChangePasswordRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.ChangePasswordResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthServiceMethodDescriptorSupplier("ChangePassword"))
              .build();
        }
      }
    }
    return getChangePasswordMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest,
      realmmesh.auth.v1.Auth.AccountEvent> getSubscribeAccountEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubscribeAccountEvents",
      requestType = realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest.class,
      responseType = realmmesh.auth.v1.Auth.AccountEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest,
      realmmesh.auth.v1.Auth.AccountEvent> getSubscribeAccountEventsMethod() {
    io.grpc.MethodDescriptor<realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest, realmmesh.auth.v1.Auth.AccountEvent> getSubscribeAccountEventsMethod;
    if ((getSubscribeAccountEventsMethod = AuthServiceGrpc.getSubscribeAccountEventsMethod) == null) {
      synchronized (AuthServiceGrpc.class) {
        if ((getSubscribeAccountEventsMethod = AuthServiceGrpc.getSubscribeAccountEventsMethod) == null) {
          AuthServiceGrpc.getSubscribeAccountEventsMethod = getSubscribeAccountEventsMethod =
              io.grpc.MethodDescriptor.<realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest, realmmesh.auth.v1.Auth.AccountEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubscribeAccountEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.auth.v1.Auth.AccountEvent.getDefaultInstance()))
              .setSchemaDescriptor(new AuthServiceMethodDescriptorSupplier("SubscribeAccountEvents"))
              .build();
        }
      }
    }
    return getSubscribeAccountEventsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuthServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthServiceStub>() {
        @java.lang.Override
        public AuthServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthServiceStub(channel, callOptions);
        }
      };
    return AuthServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuthServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthServiceBlockingStub>() {
        @java.lang.Override
        public AuthServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthServiceBlockingStub(channel, callOptions);
        }
      };
    return AuthServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuthServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthServiceFutureStub>() {
        @java.lang.Override
        public AuthServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthServiceFutureStub(channel, callOptions);
        }
      };
    return AuthServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * AuthService 校验玩家账户，当前连接的登录状态由游戏服维护
   * 每次调用均要求有效服务器凭据，密码和完整请求不得写入日志
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Register 创建普通账户，只有持久化成功才返回身份
     * 游戏服收到成功结果后，将发起注册的本次连接标记为已登录
     * </pre>
     */
    default void register(realmmesh.auth.v1.Auth.RegisterRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.RegisterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Login 验证密码并提交失败计数或休息状态，再返回本次认证结果
     * 密码错误、未注册与休息期属于正常业务结果，不作为 gRPC 错误返回
     * </pre>
     */
    default void login(realmmesh.auth.v1.Auth.LoginRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.LoginResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoginMethod(), responseObserver);
    }

    /**
     * <pre>
     * ChangePassword 验证旧密码后修改密码，与登录共用失败次数和休息期
     * 仅允许已登录连接发起；提交成功后广播变更，发起连接也须清除认证并退出
     * </pre>
     */
    default void changePassword(realmmesh.auth.v1.Auth.ChangePasswordRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.ChangePasswordResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getChangePasswordMethod(), responseObserver);
    }

    /**
     * <pre>
     * SubscribeAccountEvents 订阅中心提交成功后的账户变更，仅推送在线通知
     * 建流及每次发送均校验服务器凭据，断线期间的事件不保存、不补发
     * </pre>
     */
    default void subscribeAccountEvents(realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.AccountEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubscribeAccountEventsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AuthService.
   * <pre>
   * AuthService 校验玩家账户，当前连接的登录状态由游戏服维护
   * 每次调用均要求有效服务器凭据，密码和完整请求不得写入日志
   * </pre>
   */
  public static abstract class AuthServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuthServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuthService.
   * <pre>
   * AuthService 校验玩家账户，当前连接的登录状态由游戏服维护
   * 每次调用均要求有效服务器凭据，密码和完整请求不得写入日志
   * </pre>
   */
  public static final class AuthServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AuthServiceStub> {
    private AuthServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Register 创建普通账户，只有持久化成功才返回身份
     * 游戏服收到成功结果后，将发起注册的本次连接标记为已登录
     * </pre>
     */
    public void register(realmmesh.auth.v1.Auth.RegisterRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.RegisterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Login 验证密码并提交失败计数或休息状态，再返回本次认证结果
     * 密码错误、未注册与休息期属于正常业务结果，不作为 gRPC 错误返回
     * </pre>
     */
    public void login(realmmesh.auth.v1.Auth.LoginRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.LoginResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoginMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ChangePassword 验证旧密码后修改密码，与登录共用失败次数和休息期
     * 仅允许已登录连接发起；提交成功后广播变更，发起连接也须清除认证并退出
     * </pre>
     */
    public void changePassword(realmmesh.auth.v1.Auth.ChangePasswordRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.ChangePasswordResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getChangePasswordMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * SubscribeAccountEvents 订阅中心提交成功后的账户变更，仅推送在线通知
     * 建流及每次发送均校验服务器凭据，断线期间的事件不保存、不补发
     * </pre>
     */
    public void subscribeAccountEvents(realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest request,
        io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.AccountEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSubscribeAccountEventsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuthService.
   * <pre>
   * AuthService 校验玩家账户，当前连接的登录状态由游戏服维护
   * 每次调用均要求有效服务器凭据，密码和完整请求不得写入日志
   * </pre>
   */
  public static final class AuthServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuthServiceBlockingStub> {
    private AuthServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Register 创建普通账户，只有持久化成功才返回身份
     * 游戏服收到成功结果后，将发起注册的本次连接标记为已登录
     * </pre>
     */
    public realmmesh.auth.v1.Auth.RegisterResponse register(realmmesh.auth.v1.Auth.RegisterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Login 验证密码并提交失败计数或休息状态，再返回本次认证结果
     * 密码错误、未注册与休息期属于正常业务结果，不作为 gRPC 错误返回
     * </pre>
     */
    public realmmesh.auth.v1.Auth.LoginResponse login(realmmesh.auth.v1.Auth.LoginRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoginMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ChangePassword 验证旧密码后修改密码，与登录共用失败次数和休息期
     * 仅允许已登录连接发起；提交成功后广播变更，发起连接也须清除认证并退出
     * </pre>
     */
    public realmmesh.auth.v1.Auth.ChangePasswordResponse changePassword(realmmesh.auth.v1.Auth.ChangePasswordRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getChangePasswordMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SubscribeAccountEvents 订阅中心提交成功后的账户变更，仅推送在线通知
     * 建流及每次发送均校验服务器凭据，断线期间的事件不保存、不补发
     * </pre>
     */
    public java.util.Iterator<realmmesh.auth.v1.Auth.AccountEvent> subscribeAccountEvents(
        realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSubscribeAccountEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuthService.
   * <pre>
   * AuthService 校验玩家账户，当前连接的登录状态由游戏服维护
   * 每次调用均要求有效服务器凭据，密码和完整请求不得写入日志
   * </pre>
   */
  public static final class AuthServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuthServiceFutureStub> {
    private AuthServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Register 创建普通账户，只有持久化成功才返回身份
     * 游戏服收到成功结果后，将发起注册的本次连接标记为已登录
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.auth.v1.Auth.RegisterResponse> register(
        realmmesh.auth.v1.Auth.RegisterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Login 验证密码并提交失败计数或休息状态，再返回本次认证结果
     * 密码错误、未注册与休息期属于正常业务结果，不作为 gRPC 错误返回
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.auth.v1.Auth.LoginResponse> login(
        realmmesh.auth.v1.Auth.LoginRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoginMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ChangePassword 验证旧密码后修改密码，与登录共用失败次数和休息期
     * 仅允许已登录连接发起；提交成功后广播变更，发起连接也须清除认证并退出
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.auth.v1.Auth.ChangePasswordResponse> changePassword(
        realmmesh.auth.v1.Auth.ChangePasswordRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getChangePasswordMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER = 0;
  private static final int METHODID_LOGIN = 1;
  private static final int METHODID_CHANGE_PASSWORD = 2;
  private static final int METHODID_SUBSCRIBE_ACCOUNT_EVENTS = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REGISTER:
          serviceImpl.register((realmmesh.auth.v1.Auth.RegisterRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.RegisterResponse>) responseObserver);
          break;
        case METHODID_LOGIN:
          serviceImpl.login((realmmesh.auth.v1.Auth.LoginRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.LoginResponse>) responseObserver);
          break;
        case METHODID_CHANGE_PASSWORD:
          serviceImpl.changePassword((realmmesh.auth.v1.Auth.ChangePasswordRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.ChangePasswordResponse>) responseObserver);
          break;
        case METHODID_SUBSCRIBE_ACCOUNT_EVENTS:
          serviceImpl.subscribeAccountEvents((realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.auth.v1.Auth.AccountEvent>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getRegisterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.auth.v1.Auth.RegisterRequest,
              realmmesh.auth.v1.Auth.RegisterResponse>(
                service, METHODID_REGISTER)))
        .addMethod(
          getLoginMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.auth.v1.Auth.LoginRequest,
              realmmesh.auth.v1.Auth.LoginResponse>(
                service, METHODID_LOGIN)))
        .addMethod(
          getChangePasswordMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.auth.v1.Auth.ChangePasswordRequest,
              realmmesh.auth.v1.Auth.ChangePasswordResponse>(
                service, METHODID_CHANGE_PASSWORD)))
        .addMethod(
          getSubscribeAccountEventsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              realmmesh.auth.v1.Auth.SubscribeAccountEventsRequest,
              realmmesh.auth.v1.Auth.AccountEvent>(
                service, METHODID_SUBSCRIBE_ACCOUNT_EVENTS)))
        .build();
  }

  private static abstract class AuthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuthServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return realmmesh.auth.v1.Auth.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuthService");
    }
  }

  private static final class AuthServiceFileDescriptorSupplier
      extends AuthServiceBaseDescriptorSupplier {
    AuthServiceFileDescriptorSupplier() {}
  }

  private static final class AuthServiceMethodDescriptorSupplier
      extends AuthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuthServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AuthServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuthServiceFileDescriptorSupplier())
              .addMethod(getRegisterMethod())
              .addMethod(getLoginMethod())
              .addMethod(getChangePasswordMethod())
              .addMethod(getSubscribeAccountEventsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
