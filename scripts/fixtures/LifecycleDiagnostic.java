package io.github.helantianshen.realmmesh.diagnostic;

import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import io.github.helantianshen.realmmesh.rpc.RealmMeshRpcClient;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.server.*;
import realmmesh.probe.v1.Probe;
import realmmesh.probe.v1.ProbeServiceGrpc;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber(modid="realmmesh")
public final class LifecycleDiagnostic {
    static final Path DIR = Path.of(System.getProperty("rpc.test.dir"));
    static final String SECRET = "PRIVATE_ENDPOINT_192.0.2.99_SECRET";
    static volatile RealmMeshRpcClient lastClient;
    static volatile Capture lastCapture;
    static volatile io.grpc.Server testService;
    static void mark(String name) throws Exception { Files.writeString(DIR.resolve(name), name); }
    static void waitFor(String name) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);
        while (!Files.exists(DIR.resolve(name))) {
            if (System.nanoTime()>end) throw new AssertionError(name);
            Thread.sleep(50);
        }
    }
    static ManagedChannel channel(RealmMeshRpcClient client) throws Exception {
        var field=RealmMeshRpcClient.class.getDeclaredField("channel"); field.setAccessible(true);
        return (ManagedChannel)field.get(client);
    }
    static final class Capture implements CommandSource {
        final MinecraftServer server;
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        Capture(MinecraftServer server) { this.server=server; }
        public void sendSystemMessage(Component message) {
            if (!server.isSameThread()) throw new AssertionError("Feedback off server thread");
            String text=message.getString();
            if (text.contains(SECRET)||text.contains("127.0.0.1")||text.contains(System.getProperty("rpc.test.port"))) throw new AssertionError("Config leaked: "+text);
            messages.add(text);
        }
        public boolean acceptsSuccess() { return true; }
        public boolean acceptsFailure() { return true; }
        public boolean shouldInformAdmins() { return false; }
    }
    static Capture invoke(MinecraftServer server) throws Exception {
        var capture=new Capture(server);
        server.submit(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(capture),"realmmesh ping")).get(3,TimeUnit.SECONDS);
        if (!"正在测试中心连接".equals(capture.messages.poll(3,TimeUnit.SECONDS))) throw new AssertionError("Missing pending reply");
        return capture;
    }
    static String reply(Capture capture) throws Exception {
        var text=capture.messages.poll(5,TimeUnit.SECONDS);
        if(text==null)throw new AssertionError("Missing reply");
        return text;
    }
    static void expect(MinecraftServer server,String expected) throws Exception {
        String text=reply(invoke(server));
        if(!text.contains(expected))throw new AssertionError(text);
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var thread=new Thread(() -> run(event.getServer()),"lifecycle-test");thread.setDaemon(true);thread.start();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) throws Exception {
        if(NeoForgeRpcLifecycle.clientFor(event.getServer())!=null)throw new AssertionError("Client retained");
        if(lastClient!=null && !channel(lastClient).awaitTermination(5,TimeUnit.SECONDS))throw new AssertionError("Leaked channel");
        if(lastCapture!=null && !lastCapture.messages.isEmpty())throw new AssertionError("Late feedback");
        if(testService!=null){testService.shutdownNow();if(!testService.awaitTermination(5,TimeUnit.SECONDS))throw new AssertionError("Test service leaked");}
        mark("stopped-pass");
        mark("all-pass");
    }
    static void run(MinecraftServer server) {
        try {
            var first=NeoForgeRpcLifecycle.clientFor(server);
            if(first==null)throw new AssertionError("Lifecycle not started");
            server.submit(() -> {
                var node=server.getCommands().getDispatcher().getRoot().getChild("realmmesh");
                if(node.canUse(server.createCommandSourceStack().withPermission(0)))throw new AssertionError("Permission 0 allowed");
                if(node.canUse(server.createCommandSourceStack().withPermission(1)))throw new AssertionError("Permission 1 allowed");
                if(!node.canUse(server.createCommandSourceStack().withPermission(2)))throw new AssertionError("Permission 2 rejected");
            }).get(3,TimeUnit.SECONDS);
            expect(server,"UNAVAILABLE");mark("offline-pass");waitFor("center-started");
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(true) {
                String text=reply(invoke(server));
                if(text.contains("中心连接正常"))break;
                if(System.nanoTime()>end)throw new AssertionError(text);
                Thread.sleep(150);
            }
            expect(server,"中心连接正常");
            if(first!=NeoForgeRpcLifecycle.clientFor(server))throw new AssertionError("Client replaced");
            mark("online-pass");waitFor("center-stopped");
            expect(server,"UNAVAILABLE");mark("down-pass");waitFor("center-restarted");
            end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(true) {
                String text=reply(invoke(server));
                if(text.contains("中心连接正常"))break;
                if(System.nanoTime()>end)throw new AssertionError(text);
                Thread.sleep(150);
            }
            if(first!=NeoForgeRpcLifecycle.clientFor(server))throw new AssertionError("Client replaced");
            mark("recovery-pass");
            mock(server,first);
        }catch(Throwable e){e.printStackTrace();try{Files.writeString(DIR.resolve("failed"),e.toString());}catch(Exception ignored){}}
    }
    static void mock(MinecraftServer server,RealmMeshRpcClient first) throws Exception {
        var mode=new AtomicInteger();var received=new LinkedBlockingQueue<Integer>();
        var mock=NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1",0)).addService(new ProbeServiceGrpc.ProbeServiceImplBase(){
            public void echo(Probe.EchoRequest req,StreamObserver<Probe.EchoResponse> observer){
                int current=mode.get();received.add(current);
                if(current==0)observer.onError(Status.INTERNAL.withDescription(SECRET).asRuntimeException());
                if(current==1){observer.onNext(Probe.EchoResponse.newBuilder().setMessage(SECRET).build());observer.onCompleted();}
            }
        }).build().start();
        testService=mock;
        try {
            server.submit(() -> {
                NeoForgeRpcLifecycle.stopping(new ServerStoppingEvent(server));
                try {
                    var port=NeoForgeRpcConfig.class.getDeclaredField("PORT");port.setAccessible(true);
                    ((ModConfigSpec.ConfigValue<Integer>)port.get(null)).set(mock.getPort());
                }catch(Exception e){throw new RuntimeException(e);}
                NeoForgeRpcLifecycle.starting(new ServerStartingEvent(server));
            }).get(3,TimeUnit.SECONDS);
            if(!channel(first).awaitTermination(5,TimeUnit.SECONDS))throw new AssertionError("Old client leaked");
            if(first==NeoForgeRpcLifecycle.clientFor(server))throw new AssertionError("Old client reused");
            expect(server,"INTERNAL");mode.set(1);expect(server,"响应校验失败");
            received.clear();mode.set(2);
            var pending=invoke(server);
            if(received.poll(3,TimeUnit.SECONDS)==null)throw new AssertionError();
            server.submit(() -> {}).get(1,TimeUnit.SECONDS);
            if(!pending.messages.isEmpty())throw new AssertionError("Premature completion");
            if(!reply(pending).contains("DEADLINE_EXCEEDED"))throw new AssertionError();
            mark("privacy-timeout-pass");
            received.clear();lastCapture=invoke(server);
            if(received.poll(3,TimeUnit.SECONDS)==null)throw new AssertionError();
            lastClient=NeoForgeRpcLifecycle.clientFor(server);
            server.submit(() -> server.halt(false)).get(3,TimeUnit.SECONDS);
            waitFor("stopped-pass");mark("all-pass");
        } finally {mock.shutdownNow();mock.awaitTermination(5,TimeUnit.SECONDS);}
    }
}
