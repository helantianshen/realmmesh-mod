package realmmesh.probe.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * ProbeService 提供通信验证接口，不处理业务数据
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.2)",
    comments = "Source: probe/v1/probe.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ProbeServiceGrpc {

  private ProbeServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "realmmesh.probe.v1.ProbeService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<realmmesh.probe.v1.Probe.EchoRequest,
      realmmesh.probe.v1.Probe.EchoResponse> getEchoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Echo",
      requestType = realmmesh.probe.v1.Probe.EchoRequest.class,
      responseType = realmmesh.probe.v1.Probe.EchoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.probe.v1.Probe.EchoRequest,
      realmmesh.probe.v1.Probe.EchoResponse> getEchoMethod() {
    io.grpc.MethodDescriptor<realmmesh.probe.v1.Probe.EchoRequest, realmmesh.probe.v1.Probe.EchoResponse> getEchoMethod;
    if ((getEchoMethod = ProbeServiceGrpc.getEchoMethod) == null) {
      synchronized (ProbeServiceGrpc.class) {
        if ((getEchoMethod = ProbeServiceGrpc.getEchoMethod) == null) {
          ProbeServiceGrpc.getEchoMethod = getEchoMethod =
              io.grpc.MethodDescriptor.<realmmesh.probe.v1.Probe.EchoRequest, realmmesh.probe.v1.Probe.EchoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Echo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.probe.v1.Probe.EchoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.probe.v1.Probe.EchoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ProbeServiceMethodDescriptorSupplier("Echo"))
              .build();
        }
      }
    }
    return getEchoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ProbeServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProbeServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProbeServiceStub>() {
        @java.lang.Override
        public ProbeServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProbeServiceStub(channel, callOptions);
        }
      };
    return ProbeServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ProbeServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProbeServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProbeServiceBlockingStub>() {
        @java.lang.Override
        public ProbeServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProbeServiceBlockingStub(channel, callOptions);
        }
      };
    return ProbeServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ProbeServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProbeServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProbeServiceFutureStub>() {
        @java.lang.Override
        public ProbeServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProbeServiceFutureStub(channel, callOptions);
        }
      };
    return ProbeServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * ProbeService 提供通信验证接口，不处理业务数据
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Echo 原样返回请求中的消息
     * </pre>
     */
    default void echo(realmmesh.probe.v1.Probe.EchoRequest request,
        io.grpc.stub.StreamObserver<realmmesh.probe.v1.Probe.EchoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEchoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ProbeService.
   * <pre>
   * ProbeService 提供通信验证接口，不处理业务数据
   * </pre>
   */
  public static abstract class ProbeServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ProbeServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ProbeService.
   * <pre>
   * ProbeService 提供通信验证接口，不处理业务数据
   * </pre>
   */
  public static final class ProbeServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ProbeServiceStub> {
    private ProbeServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProbeServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProbeServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Echo 原样返回请求中的消息
     * </pre>
     */
    public void echo(realmmesh.probe.v1.Probe.EchoRequest request,
        io.grpc.stub.StreamObserver<realmmesh.probe.v1.Probe.EchoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEchoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ProbeService.
   * <pre>
   * ProbeService 提供通信验证接口，不处理业务数据
   * </pre>
   */
  public static final class ProbeServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ProbeServiceBlockingStub> {
    private ProbeServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProbeServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProbeServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Echo 原样返回请求中的消息
     * </pre>
     */
    public realmmesh.probe.v1.Probe.EchoResponse echo(realmmesh.probe.v1.Probe.EchoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEchoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ProbeService.
   * <pre>
   * ProbeService 提供通信验证接口，不处理业务数据
   * </pre>
   */
  public static final class ProbeServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ProbeServiceFutureStub> {
    private ProbeServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProbeServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProbeServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Echo 原样返回请求中的消息
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.probe.v1.Probe.EchoResponse> echo(
        realmmesh.probe.v1.Probe.EchoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEchoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ECHO = 0;

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
        case METHODID_ECHO:
          serviceImpl.echo((realmmesh.probe.v1.Probe.EchoRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.probe.v1.Probe.EchoResponse>) responseObserver);
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
          getEchoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.probe.v1.Probe.EchoRequest,
              realmmesh.probe.v1.Probe.EchoResponse>(
                service, METHODID_ECHO)))
        .build();
  }

  private static abstract class ProbeServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ProbeServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return realmmesh.probe.v1.Probe.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ProbeService");
    }
  }

  private static final class ProbeServiceFileDescriptorSupplier
      extends ProbeServiceBaseDescriptorSupplier {
    ProbeServiceFileDescriptorSupplier() {}
  }

  private static final class ProbeServiceMethodDescriptorSupplier
      extends ProbeServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ProbeServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ProbeServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ProbeServiceFileDescriptorSupplier())
              .addMethod(getEchoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
