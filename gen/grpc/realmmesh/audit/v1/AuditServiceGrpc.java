package realmmesh.audit.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * AuditService 接收尽力上报的连接记录，不维护中心在线会话
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.2)",
    comments = "Source: audit/v1/audit.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuditServiceGrpc {

  private AuditServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "realmmesh.audit.v1.AuditService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest,
      realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse> getReportPlayerConnectionEventMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportPlayerConnectionEvent",
      requestType = realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest.class,
      responseType = realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest,
      realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse> getReportPlayerConnectionEventMethod() {
    io.grpc.MethodDescriptor<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest, realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse> getReportPlayerConnectionEventMethod;
    if ((getReportPlayerConnectionEventMethod = AuditServiceGrpc.getReportPlayerConnectionEventMethod) == null) {
      synchronized (AuditServiceGrpc.class) {
        if ((getReportPlayerConnectionEventMethod = AuditServiceGrpc.getReportPlayerConnectionEventMethod) == null) {
          AuditServiceGrpc.getReportPlayerConnectionEventMethod = getReportPlayerConnectionEventMethod =
              io.grpc.MethodDescriptor.<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest, realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportPlayerConnectionEvent"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuditServiceMethodDescriptorSupplier("ReportPlayerConnectionEvent"))
              .build();
        }
      }
    }
    return getReportPlayerConnectionEventMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuditServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditServiceStub>() {
        @java.lang.Override
        public AuditServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditServiceStub(channel, callOptions);
        }
      };
    return AuditServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuditServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditServiceBlockingStub>() {
        @java.lang.Override
        public AuditServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditServiceBlockingStub(channel, callOptions);
        }
      };
    return AuditServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuditServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditServiceFutureStub>() {
        @java.lang.Override
        public AuditServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditServiceFutureStub(channel, callOptions);
        }
      };
    return AuditServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * AuditService 接收尽力上报的连接记录，不维护中心在线会话
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * ReportPlayerConnectionEvent 每次写入一条记录，成功返回空响应
     * 非法参数返回 InvalidArgument，凭据无效返回 Unauthenticated，存储失败返回 Internal
     * 失败或超时不阻断游戏流程，不自动重试；超时不保证记录未写入
     * </pre>
     */
    default void reportPlayerConnectionEvent(realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest request,
        io.grpc.stub.StreamObserver<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportPlayerConnectionEventMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AuditService.
   * <pre>
   * AuditService 接收尽力上报的连接记录，不维护中心在线会话
   * </pre>
   */
  public static abstract class AuditServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuditServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuditService.
   * <pre>
   * AuditService 接收尽力上报的连接记录，不维护中心在线会话
   * </pre>
   */
  public static final class AuditServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AuditServiceStub> {
    private AuditServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * ReportPlayerConnectionEvent 每次写入一条记录，成功返回空响应
     * 非法参数返回 InvalidArgument，凭据无效返回 Unauthenticated，存储失败返回 Internal
     * 失败或超时不阻断游戏流程，不自动重试；超时不保证记录未写入
     * </pre>
     */
    public void reportPlayerConnectionEvent(realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest request,
        io.grpc.stub.StreamObserver<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportPlayerConnectionEventMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuditService.
   * <pre>
   * AuditService 接收尽力上报的连接记录，不维护中心在线会话
   * </pre>
   */
  public static final class AuditServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuditServiceBlockingStub> {
    private AuditServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * ReportPlayerConnectionEvent 每次写入一条记录，成功返回空响应
     * 非法参数返回 InvalidArgument，凭据无效返回 Unauthenticated，存储失败返回 Internal
     * 失败或超时不阻断游戏流程，不自动重试；超时不保证记录未写入
     * </pre>
     */
    public realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse reportPlayerConnectionEvent(realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportPlayerConnectionEventMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuditService.
   * <pre>
   * AuditService 接收尽力上报的连接记录，不维护中心在线会话
   * </pre>
   */
  public static final class AuditServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuditServiceFutureStub> {
    private AuditServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * ReportPlayerConnectionEvent 每次写入一条记录，成功返回空响应
     * 非法参数返回 InvalidArgument，凭据无效返回 Unauthenticated，存储失败返回 Internal
     * 失败或超时不阻断游戏流程，不自动重试；超时不保证记录未写入
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse> reportPlayerConnectionEvent(
        realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportPlayerConnectionEventMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REPORT_PLAYER_CONNECTION_EVENT = 0;

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
        case METHODID_REPORT_PLAYER_CONNECTION_EVENT:
          serviceImpl.reportPlayerConnectionEvent((realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse>) responseObserver);
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
          getReportPlayerConnectionEventMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.audit.v1.Audit.ReportPlayerConnectionEventRequest,
              realmmesh.audit.v1.Audit.ReportPlayerConnectionEventResponse>(
                service, METHODID_REPORT_PLAYER_CONNECTION_EVENT)))
        .build();
  }

  private static abstract class AuditServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuditServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return realmmesh.audit.v1.Audit.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuditService");
    }
  }

  private static final class AuditServiceFileDescriptorSupplier
      extends AuditServiceBaseDescriptorSupplier {
    AuditServiceFileDescriptorSupplier() {}
  }

  private static final class AuditServiceMethodDescriptorSupplier
      extends AuditServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuditServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AuditServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuditServiceFileDescriptorSupplier())
              .addMethod(getReportPlayerConnectionEventMethod())
              .build();
        }
      }
    }
    return result;
  }
}
