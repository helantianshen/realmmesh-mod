package realmmesh.warehouse.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * WarehouseService 的取出准备不会扣库，提交成功才允许游戏服发放
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.60.2)",
    comments = "Source: warehouse/v1/warehouse.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class WarehouseServiceGrpc {

  private WarehouseServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "realmmesh.warehouse.v1.WarehouseService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.ListItemsRequest,
      realmmesh.warehouse.v1.Warehouse.ListItemsResponse> getListItemsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListItems",
      requestType = realmmesh.warehouse.v1.Warehouse.ListItemsRequest.class,
      responseType = realmmesh.warehouse.v1.Warehouse.ListItemsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.ListItemsRequest,
      realmmesh.warehouse.v1.Warehouse.ListItemsResponse> getListItemsMethod() {
    io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.ListItemsRequest, realmmesh.warehouse.v1.Warehouse.ListItemsResponse> getListItemsMethod;
    if ((getListItemsMethod = WarehouseServiceGrpc.getListItemsMethod) == null) {
      synchronized (WarehouseServiceGrpc.class) {
        if ((getListItemsMethod = WarehouseServiceGrpc.getListItemsMethod) == null) {
          WarehouseServiceGrpc.getListItemsMethod = getListItemsMethod =
              io.grpc.MethodDescriptor.<realmmesh.warehouse.v1.Warehouse.ListItemsRequest, realmmesh.warehouse.v1.Warehouse.ListItemsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListItems"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.ListItemsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.ListItemsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WarehouseServiceMethodDescriptorSupplier("ListItems"))
              .build();
        }
      }
    }
    return getListItemsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.DepositRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getDepositMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Deposit",
      requestType = realmmesh.warehouse.v1.Warehouse.DepositRequest.class,
      responseType = realmmesh.warehouse.v1.Warehouse.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.DepositRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getDepositMethod() {
    io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.DepositRequest, realmmesh.warehouse.v1.Warehouse.Operation> getDepositMethod;
    if ((getDepositMethod = WarehouseServiceGrpc.getDepositMethod) == null) {
      synchronized (WarehouseServiceGrpc.class) {
        if ((getDepositMethod = WarehouseServiceGrpc.getDepositMethod) == null) {
          WarehouseServiceGrpc.getDepositMethod = getDepositMethod =
              io.grpc.MethodDescriptor.<realmmesh.warehouse.v1.Warehouse.DepositRequest, realmmesh.warehouse.v1.Warehouse.Operation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Deposit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.DepositRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.Operation.getDefaultInstance()))
              .setSchemaDescriptor(new WarehouseServiceMethodDescriptorSupplier("Deposit"))
              .build();
        }
      }
    }
    return getDepositMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.WithdrawalRequest,
      realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse> getPrepareWithdrawalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PrepareWithdrawal",
      requestType = realmmesh.warehouse.v1.Warehouse.WithdrawalRequest.class,
      responseType = realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.WithdrawalRequest,
      realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse> getPrepareWithdrawalMethod() {
    io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.WithdrawalRequest, realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse> getPrepareWithdrawalMethod;
    if ((getPrepareWithdrawalMethod = WarehouseServiceGrpc.getPrepareWithdrawalMethod) == null) {
      synchronized (WarehouseServiceGrpc.class) {
        if ((getPrepareWithdrawalMethod = WarehouseServiceGrpc.getPrepareWithdrawalMethod) == null) {
          WarehouseServiceGrpc.getPrepareWithdrawalMethod = getPrepareWithdrawalMethod =
              io.grpc.MethodDescriptor.<realmmesh.warehouse.v1.Warehouse.WithdrawalRequest, realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PrepareWithdrawal"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.WithdrawalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WarehouseServiceMethodDescriptorSupplier("PrepareWithdrawal"))
              .build();
        }
      }
    }
    return getPrepareWithdrawalMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getCommitWithdrawalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CommitWithdrawal",
      requestType = realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest.class,
      responseType = realmmesh.warehouse.v1.Warehouse.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getCommitWithdrawalMethod() {
    io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest, realmmesh.warehouse.v1.Warehouse.Operation> getCommitWithdrawalMethod;
    if ((getCommitWithdrawalMethod = WarehouseServiceGrpc.getCommitWithdrawalMethod) == null) {
      synchronized (WarehouseServiceGrpc.class) {
        if ((getCommitWithdrawalMethod = WarehouseServiceGrpc.getCommitWithdrawalMethod) == null) {
          WarehouseServiceGrpc.getCommitWithdrawalMethod = getCommitWithdrawalMethod =
              io.grpc.MethodDescriptor.<realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest, realmmesh.warehouse.v1.Warehouse.Operation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CommitWithdrawal"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.Operation.getDefaultInstance()))
              .setSchemaDescriptor(new WarehouseServiceMethodDescriptorSupplier("CommitWithdrawal"))
              .build();
        }
      }
    }
    return getCommitWithdrawalMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getCompleteWithdrawalMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CompleteWithdrawal",
      requestType = realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest.class,
      responseType = realmmesh.warehouse.v1.Warehouse.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getCompleteWithdrawalMethod() {
    io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest, realmmesh.warehouse.v1.Warehouse.Operation> getCompleteWithdrawalMethod;
    if ((getCompleteWithdrawalMethod = WarehouseServiceGrpc.getCompleteWithdrawalMethod) == null) {
      synchronized (WarehouseServiceGrpc.class) {
        if ((getCompleteWithdrawalMethod = WarehouseServiceGrpc.getCompleteWithdrawalMethod) == null) {
          WarehouseServiceGrpc.getCompleteWithdrawalMethod = getCompleteWithdrawalMethod =
              io.grpc.MethodDescriptor.<realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest, realmmesh.warehouse.v1.Warehouse.Operation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CompleteWithdrawal"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.Operation.getDefaultInstance()))
              .setSchemaDescriptor(new WarehouseServiceMethodDescriptorSupplier("CompleteWithdrawal"))
              .build();
        }
      }
    }
    return getCompleteWithdrawalMethod;
  }

  private static volatile io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.GetOperationRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getGetOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetOperation",
      requestType = realmmesh.warehouse.v1.Warehouse.GetOperationRequest.class,
      responseType = realmmesh.warehouse.v1.Warehouse.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.GetOperationRequest,
      realmmesh.warehouse.v1.Warehouse.Operation> getGetOperationMethod() {
    io.grpc.MethodDescriptor<realmmesh.warehouse.v1.Warehouse.GetOperationRequest, realmmesh.warehouse.v1.Warehouse.Operation> getGetOperationMethod;
    if ((getGetOperationMethod = WarehouseServiceGrpc.getGetOperationMethod) == null) {
      synchronized (WarehouseServiceGrpc.class) {
        if ((getGetOperationMethod = WarehouseServiceGrpc.getGetOperationMethod) == null) {
          WarehouseServiceGrpc.getGetOperationMethod = getGetOperationMethod =
              io.grpc.MethodDescriptor.<realmmesh.warehouse.v1.Warehouse.GetOperationRequest, realmmesh.warehouse.v1.Warehouse.Operation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.GetOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  realmmesh.warehouse.v1.Warehouse.Operation.getDefaultInstance()))
              .setSchemaDescriptor(new WarehouseServiceMethodDescriptorSupplier("GetOperation"))
              .build();
        }
      }
    }
    return getGetOperationMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WarehouseServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WarehouseServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WarehouseServiceStub>() {
        @java.lang.Override
        public WarehouseServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WarehouseServiceStub(channel, callOptions);
        }
      };
    return WarehouseServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WarehouseServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WarehouseServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WarehouseServiceBlockingStub>() {
        @java.lang.Override
        public WarehouseServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WarehouseServiceBlockingStub(channel, callOptions);
        }
      };
    return WarehouseServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WarehouseServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WarehouseServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WarehouseServiceFutureStub>() {
        @java.lang.Override
        public WarehouseServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WarehouseServiceFutureStub(channel, callOptions);
        }
      };
    return WarehouseServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * WarehouseService 的取出准备不会扣库，提交成功才允许游戏服发放
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void listItems(realmmesh.warehouse.v1.Warehouse.ListItemsRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.ListItemsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListItemsMethod(), responseObserver);
    }

    /**
     */
    default void deposit(realmmesh.warehouse.v1.Warehouse.DepositRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDepositMethod(), responseObserver);
    }

    /**
     */
    default void prepareWithdrawal(realmmesh.warehouse.v1.Warehouse.WithdrawalRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPrepareWithdrawalMethod(), responseObserver);
    }

    /**
     */
    default void commitWithdrawal(realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCommitWithdrawalMethod(), responseObserver);
    }

    /**
     */
    default void completeWithdrawal(realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCompleteWithdrawalMethod(), responseObserver);
    }

    /**
     */
    default void getOperation(realmmesh.warehouse.v1.Warehouse.GetOperationRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetOperationMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service WarehouseService.
   * <pre>
   * WarehouseService 的取出准备不会扣库，提交成功才允许游戏服发放
   * </pre>
   */
  public static abstract class WarehouseServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return WarehouseServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service WarehouseService.
   * <pre>
   * WarehouseService 的取出准备不会扣库，提交成功才允许游戏服发放
   * </pre>
   */
  public static final class WarehouseServiceStub
      extends io.grpc.stub.AbstractAsyncStub<WarehouseServiceStub> {
    private WarehouseServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WarehouseServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WarehouseServiceStub(channel, callOptions);
    }

    /**
     */
    public void listItems(realmmesh.warehouse.v1.Warehouse.ListItemsRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.ListItemsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListItemsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deposit(realmmesh.warehouse.v1.Warehouse.DepositRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDepositMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void prepareWithdrawal(realmmesh.warehouse.v1.Warehouse.WithdrawalRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPrepareWithdrawalMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commitWithdrawal(realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCommitWithdrawalMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void completeWithdrawal(realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCompleteWithdrawalMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getOperation(realmmesh.warehouse.v1.Warehouse.GetOperationRequest request,
        io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetOperationMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service WarehouseService.
   * <pre>
   * WarehouseService 的取出准备不会扣库，提交成功才允许游戏服发放
   * </pre>
   */
  public static final class WarehouseServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<WarehouseServiceBlockingStub> {
    private WarehouseServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WarehouseServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WarehouseServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public realmmesh.warehouse.v1.Warehouse.ListItemsResponse listItems(realmmesh.warehouse.v1.Warehouse.ListItemsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListItemsMethod(), getCallOptions(), request);
    }

    /**
     */
    public realmmesh.warehouse.v1.Warehouse.Operation deposit(realmmesh.warehouse.v1.Warehouse.DepositRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDepositMethod(), getCallOptions(), request);
    }

    /**
     */
    public realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse prepareWithdrawal(realmmesh.warehouse.v1.Warehouse.WithdrawalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPrepareWithdrawalMethod(), getCallOptions(), request);
    }

    /**
     */
    public realmmesh.warehouse.v1.Warehouse.Operation commitWithdrawal(realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCommitWithdrawalMethod(), getCallOptions(), request);
    }

    /**
     */
    public realmmesh.warehouse.v1.Warehouse.Operation completeWithdrawal(realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCompleteWithdrawalMethod(), getCallOptions(), request);
    }

    /**
     */
    public realmmesh.warehouse.v1.Warehouse.Operation getOperation(realmmesh.warehouse.v1.Warehouse.GetOperationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetOperationMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service WarehouseService.
   * <pre>
   * WarehouseService 的取出准备不会扣库，提交成功才允许游戏服发放
   * </pre>
   */
  public static final class WarehouseServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<WarehouseServiceFutureStub> {
    private WarehouseServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WarehouseServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WarehouseServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.warehouse.v1.Warehouse.ListItemsResponse> listItems(
        realmmesh.warehouse.v1.Warehouse.ListItemsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListItemsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.warehouse.v1.Warehouse.Operation> deposit(
        realmmesh.warehouse.v1.Warehouse.DepositRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDepositMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse> prepareWithdrawal(
        realmmesh.warehouse.v1.Warehouse.WithdrawalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPrepareWithdrawalMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.warehouse.v1.Warehouse.Operation> commitWithdrawal(
        realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCommitWithdrawalMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.warehouse.v1.Warehouse.Operation> completeWithdrawal(
        realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCompleteWithdrawalMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<realmmesh.warehouse.v1.Warehouse.Operation> getOperation(
        realmmesh.warehouse.v1.Warehouse.GetOperationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetOperationMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_ITEMS = 0;
  private static final int METHODID_DEPOSIT = 1;
  private static final int METHODID_PREPARE_WITHDRAWAL = 2;
  private static final int METHODID_COMMIT_WITHDRAWAL = 3;
  private static final int METHODID_COMPLETE_WITHDRAWAL = 4;
  private static final int METHODID_GET_OPERATION = 5;

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
        case METHODID_LIST_ITEMS:
          serviceImpl.listItems((realmmesh.warehouse.v1.Warehouse.ListItemsRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.ListItemsResponse>) responseObserver);
          break;
        case METHODID_DEPOSIT:
          serviceImpl.deposit((realmmesh.warehouse.v1.Warehouse.DepositRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation>) responseObserver);
          break;
        case METHODID_PREPARE_WITHDRAWAL:
          serviceImpl.prepareWithdrawal((realmmesh.warehouse.v1.Warehouse.WithdrawalRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse>) responseObserver);
          break;
        case METHODID_COMMIT_WITHDRAWAL:
          serviceImpl.commitWithdrawal((realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation>) responseObserver);
          break;
        case METHODID_COMPLETE_WITHDRAWAL:
          serviceImpl.completeWithdrawal((realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation>) responseObserver);
          break;
        case METHODID_GET_OPERATION:
          serviceImpl.getOperation((realmmesh.warehouse.v1.Warehouse.GetOperationRequest) request,
              (io.grpc.stub.StreamObserver<realmmesh.warehouse.v1.Warehouse.Operation>) responseObserver);
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
          getListItemsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.warehouse.v1.Warehouse.ListItemsRequest,
              realmmesh.warehouse.v1.Warehouse.ListItemsResponse>(
                service, METHODID_LIST_ITEMS)))
        .addMethod(
          getDepositMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.warehouse.v1.Warehouse.DepositRequest,
              realmmesh.warehouse.v1.Warehouse.Operation>(
                service, METHODID_DEPOSIT)))
        .addMethod(
          getPrepareWithdrawalMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.warehouse.v1.Warehouse.WithdrawalRequest,
              realmmesh.warehouse.v1.Warehouse.PrepareWithdrawalResponse>(
                service, METHODID_PREPARE_WITHDRAWAL)))
        .addMethod(
          getCommitWithdrawalMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.warehouse.v1.Warehouse.CommitWithdrawalRequest,
              realmmesh.warehouse.v1.Warehouse.Operation>(
                service, METHODID_COMMIT_WITHDRAWAL)))
        .addMethod(
          getCompleteWithdrawalMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.warehouse.v1.Warehouse.CompleteWithdrawalRequest,
              realmmesh.warehouse.v1.Warehouse.Operation>(
                service, METHODID_COMPLETE_WITHDRAWAL)))
        .addMethod(
          getGetOperationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              realmmesh.warehouse.v1.Warehouse.GetOperationRequest,
              realmmesh.warehouse.v1.Warehouse.Operation>(
                service, METHODID_GET_OPERATION)))
        .build();
  }

  private static abstract class WarehouseServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WarehouseServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return realmmesh.warehouse.v1.Warehouse.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WarehouseService");
    }
  }

  private static final class WarehouseServiceFileDescriptorSupplier
      extends WarehouseServiceBaseDescriptorSupplier {
    WarehouseServiceFileDescriptorSupplier() {}
  }

  private static final class WarehouseServiceMethodDescriptorSupplier
      extends WarehouseServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    WarehouseServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (WarehouseServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WarehouseServiceFileDescriptorSupplier())
              .addMethod(getListItemsMethod())
              .addMethod(getDepositMethod())
              .addMethod(getPrepareWithdrawalMethod())
              .addMethod(getCommitWithdrawalMethod())
              .addMethod(getCompleteWithdrawalMethod())
              .addMethod(getGetOperationMethod())
              .build();
        }
      }
    }
    return result;
  }
}
