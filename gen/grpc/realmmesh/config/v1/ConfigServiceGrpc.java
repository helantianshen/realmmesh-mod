package realmmesh.config.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * ConfigService 提供中心统一管理的游戏服配置，不返回凭据或中心连接配置
 * 每次调用均要求有效服务器凭据，并读取中心数据库中的当前配置
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.2)",
    comments = "Source: config/v1/config.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ConfigServiceGrpc {

  private ConfigServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "realmmesh.config.v1.ConfigService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<realmmesh.config.v1.Config.GetConfigRequest,
      realmmesh.config.v1.Config.GetConfigResponse> getGetConfigMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetConfig",
      requestType = realmmesh.config.v1.Config.GetConfigRequest.class,
      responseType = realmmesh.config.v1.Config.GetConfigResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.config.v1.Config.GetConfigRequest,
      realmmesh.config.v1.Config.GetConfigResponse> getGetConfigMethod() {
    io.grpc.MethodDescriptor<realmmesh.config.v1.Config.GetConfigRequest, realmmesh.config.v1.Config.GetConfigResponse> getGetConfigMethod;
    if ((getGetConfigMethod = ConfigServiceGrpc.getGetConfigMethod) == null) {
      synchronized (ConfigServiceGrpc.class) {
        if ((getGetConfigMethod = ConfigServiceGrpc.getGetConfigMethod) == null) {
          ConfigServiceGrpc.getGetConfigMethod = getGetConfigMethod =
              io.grpc.MethodDescriptor.<realmmesh.config.v1.Config.GetConfigRequest, realmmesh.config.v1.Config.GetConfigResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetConfig"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.config.v1.Config.GetConfigRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.config.v1.Config.GetConfigResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ConfigServiceMethodDescriptorSupplier("GetConfig"))
              .build();
        }
      }
    }
    return getGetConfigMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ConfigServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ConfigServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ConfigServiceStub>() {
        @java.lang.Override
        public ConfigServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ConfigServiceStub(channel, callOptions);
        }
      };
    return ConfigServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ConfigServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ConfigServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ConfigServiceBlockingStub>() {
        @java.lang.Override
        public ConfigServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ConfigServiceBlockingStub(channel, callOptions);
        }
      };
    return ConfigServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ConfigServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ConfigServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ConfigServiceFutureStub>() {
        @java.lang.Override
        public ConfigServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ConfigServiceFutureStub(channel, callOptions);
        }
      };
    return ConfigServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * ConfigService 提供中心统一管理的游戏服配置，不返回凭据或中心连接配置
   * 每次调用均要求有效服务器凭据，并读取中心数据库中的当前配置
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * GetConfig 返回当前配置，供游戏服启动检查及运行期间定期拉取
     * 配置缺失、非法或读取失败时返回 gRPC 错误，不以默认值代替
     * </pre>
     */
    default void getConfig(realmmesh.config.v1.Config.GetConfigRequest request,
        io.grpc.stub.StreamObserver<realmmesh.config.v1.Config.GetConfigResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetConfigMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ConfigService.
   * <pre>
   * ConfigService 提供中心统一管理的游戏服配置，不返回凭据或中心连接配置
   * 每次调用均要求有效服务器凭据，并读取中心数据库中的当前配置
   * </pre>
   */
  public static abstract class ConfigServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ConfigServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ConfigService.
   * <pre>
   * ConfigService 提供中心统一管理的游戏服配置，不返回凭据或中心连接配置
   * 每次调用均要求有效服务器凭据，并读取中心数据库中的当前配置
   * </pre>
   */
  public static final class ConfigServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ConfigServiceStub> {
    private ConfigServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ConfigServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ConfigServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * GetConfig 返回当前配置，供游戏服启动检查及运行期间定期拉取
     * 配置缺失、非法或读取失败时返回 gRPC 错误，不以默认值代替
     * </pre>
     */
    public void getConfig(realmmesh.config.v1.Config.GetConfigRequest request,
        io.grpc.stub.StreamObserver<realmmesh.config.v1.Config.GetConfigResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetConfigMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ConfigService.
   * <pre>
   * ConfigService 提供中心统一管理的游戏服配置，不返回凭据或中心连接配置
   * 每次调用均要求有效服务器凭据，并读取中心数据库中的当前配置
   * </pre>
   */
  public static final class ConfigServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ConfigServiceBlockingStub> {
    private ConfigServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ConfigServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ConfigServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * GetConfig 返回当前配置，供游戏服启动检查及运行期间定期拉取
     * 配置缺失、非法或读取失败时返回 gRPC 错误，不以默认值代替
     * </pre>
     */
    public realmmesh.config.v1.Config.GetConfigResponse getConfig(realmmesh.config.v1.Config.GetConfigRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetConfigMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ConfigService.
   * <pre>
   * ConfigService 提供中心统一管理的游戏服配置，不返回凭据或中心连接配置
   * 每次调用均要求有效服务器凭据，并读取中心数据库中的当前配置
   * </pre>
   */
  public static final class ConfigServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ConfigServiceFutureStub> {
    private ConfigServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ConfigServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ConfigServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * GetConfig 返回当前配置，供游戏服启动检查及运行期间定期拉取
     * 配置缺失、非法或读取失败时返回 gRPC 错误，不以默认值代替
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.config.v1.Config.GetConfigResponse> getConfig(
        realmmesh.config.v1.Config.GetConfigRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetConfigMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_CONFIG = 0;

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
        case METHODID_GET_CONFIG:
          serviceImpl.getConfig((realmmesh.config.v1.Config.GetConfigRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.config.v1.Config.GetConfigResponse>) responseObserver);
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
          getGetConfigMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.config.v1.Config.GetConfigRequest,
              realmmesh.config.v1.Config.GetConfigResponse>(
                service, METHODID_GET_CONFIG)))
        .build();
  }

  private static abstract class ConfigServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ConfigServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return realmmesh.config.v1.Config.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ConfigService");
    }
  }

  private static final class ConfigServiceFileDescriptorSupplier
      extends ConfigServiceBaseDescriptorSupplier {
    ConfigServiceFileDescriptorSupplier() {}
  }

  private static final class ConfigServiceMethodDescriptorSupplier
      extends ConfigServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ConfigServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ConfigServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ConfigServiceFileDescriptorSupplier())
              .addMethod(getGetConfigMethod())
              .build();
        }
      }
    }
    return result;
  }
}
