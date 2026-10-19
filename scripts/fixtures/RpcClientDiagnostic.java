package io.github.helantianshen.realmmesh.diagnostic;

import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import realmmesh.probe.v1.Probe;
import realmmesh.probe.v1.ProbeServiceGrpc;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.concurrent.*;

@EventBusSubscriber(modid = "realmmesh")
public final class RpcClientDiagnostic {
    private static final Path DIR = Path.of(System.getProperty("rpc.test.dir"));

    private static void mark(String name) throws Exception {
        Files.writeString(DIR.resolve(name), name);
        System.out.println("RPC_TEST_" + name);
    }

    private static void waitFor(String name) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (!Files.exists(DIR.resolve(name))) {
            if (System.nanoTime() > end) throw new AssertionError("timeout " + name);
            Thread.sleep(50);
        }
    }

    private static Throwable failure(CompletableFuture<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected failure");
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    private static void status(CompletableFuture<?> future, Status.Code code) throws Exception {
        var error = failure(future);
        if (Status.fromThrowable(error).getCode() != code) throw new AssertionError(error);
    }

    private static ManagedChannel channel(RealmMeshRpcClient client) throws Exception {
        var field = RealmMeshRpcClient.class.getDeclaredField("channel");
        field.setAccessible(true);
        return (ManagedChannel) field.get(client);
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        var thread = new Thread(RpcClientDiagnostic::run, "realmmesh-client-test");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run() {
        try {
            var client = new RealmMeshRpcClient(new RpcConfig("127.0.0.1", Integer.getInteger("rpc.test.port"), 2000));
            var originalChannel = channel(client);
            try (client) {
                for (String message : new String[]{"hello", "你好 RealmMesh", "", "PRIVATE_RPC_PAYLOAD_SECRET"}) {
                    if (!message.equals(client.echo(message).get(5, TimeUnit.SECONDS))) throw new AssertionError(message);
                }
                var calls = new CompletableFuture<?>[20];
                for (int i = 0; i < calls.length; i++) {
                    String message = "parallel-" + i;
                    calls[i] = client.echo(message).thenAccept(value -> {
                        if (!message.equals(value)) throw new AssertionError(value);
                    });
                }
                CompletableFuture.allOf(calls).get(5, TimeUnit.SECONDS);
                // 故意发送非法关联 ID 和敏感 metadata，检查中心是否替换 ID 且不记录原值
                var headers = new Metadata();
                headers.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "PRIVATE_RPC_METADATA_SECRET".repeat(8));
                headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "PRIVATE_RPC_METADATA_SECRET");
                var raw = ProbeServiceGrpc.newBlockingStub(originalChannel)
                        .withInterceptors(io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(headers))
                        .withDeadlineAfter(3, TimeUnit.SECONDS)
                        .echo(Probe.EchoRequest.newBuilder().setMessage("PRIVATE_RPC_PAYLOAD_SECRET").build());
                if (!raw.getMessage().equals("PRIVATE_RPC_PAYLOAD_SECRET")) throw new AssertionError("Echo mismatch");
                if (!(failure(client.echo(null)) instanceof NullPointerException)) throw new AssertionError("null");
                mark("initial-pass");
                waitFor("center-stopped");
                status(client.echo("offline"), Status.Code.UNAVAILABLE);
                mark("down-pass");
                waitFor("center-restarted");
                long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                while (true) {
                    try {
                        if (!"recovered".equals(client.echo("recovered").get(3, TimeUnit.SECONDS))) throw new AssertionError();
                        break;
                    } catch (ExecutionException e) {
                        if (Status.fromThrowable(e.getCause()).getCode() != Status.Code.UNAVAILABLE || System.nanoTime() > end) throw e;
                        Thread.sleep(100);
                    }
                }
                if (originalChannel != channel(client)) throw new AssertionError("Channel replaced");
                mark("reconnect-pass");
            }
            if (!originalChannel.awaitTermination(5, TimeUnit.SECONDS)) throw new AssertionError("Channel not terminated");
            slowAndClose();
            mark("all-pass");
        } catch (Throwable e) {
            e.printStackTrace();
            try { Files.writeString(DIR.resolve("failed"), e.toString()); } catch (Exception ignored) {}
        }
    }

    private static void slowAndClose() throws Exception {
        var received = new LinkedBlockingQueue<String>();
        var cancelled = new LinkedBlockingQueue<String>();
        var server = NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0))
                .addService(new ProbeServiceGrpc.ProbeServiceImplBase() {
                    @Override
                    public void echo(Probe.EchoRequest request, StreamObserver<Probe.EchoResponse> observer) {
                        Context.current().addListener(context -> cancelled.add(request.getMessage()), Runnable::run);
                        received.add(request.getMessage());
                    }
                }).build().start();
        try (var slow = new RealmMeshRpcClient(new RpcConfig("127.0.0.1", server.getPort(), 1000))) {
            long start = System.nanoTime();
            var pending = slow.echo("timeout");
            if (pending.isDone() || TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) > 500) throw new AssertionError("Blocking call");
            if (!"timeout".equals(received.poll(3, TimeUnit.SECONDS))) throw new AssertionError("Not received");
            status(pending, Status.Code.DEADLINE_EXCEEDED);
            if (!"timeout".equals(cancelled.poll(3, TimeUnit.SECONDS))) throw new AssertionError("Deadline not propagated");
            mark("timeout-pass");
            var cancel = slow.echo("cancel");
            if (!"cancel".equals(received.poll(3, TimeUnit.SECONDS))) throw new AssertionError();
            if (!cancel.cancel(false)) throw new AssertionError();
            if (!"cancel".equals(cancelled.poll(3, TimeUnit.SECONDS))) throw new AssertionError("Cancellation not propagated");
            var closing = slow.echo("close");
            if (!"close".equals(received.poll(3, TimeUnit.SECONDS))) throw new AssertionError();
            slow.close();
            slow.close();
            status(closing, Status.Code.UNAVAILABLE);
            if (!"close".equals(cancelled.poll(3, TimeUnit.SECONDS))) throw new AssertionError("Close not propagated");
            if (!(failure(slow.echo("closed")) instanceof IllegalStateException)) throw new AssertionError("Closed client accepted call");
            if (!channel(slow).awaitTermination(5, TimeUnit.SECONDS)) throw new AssertionError("Channel leaked");
            mark("close-pass");
        } finally {
            server.shutdownNow();
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) throw new AssertionError("Test server leaked");
        }
    }
}
