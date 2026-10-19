package io.github.helantianshen.realmmesh.diagnostic;

import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import io.github.helantianshen.realmmesh.neoforge.rpc.NeoForgeRpcLifecycle;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.Map;
import net.minecraft.world.level.GameType;

@EventBusSubscriber(modid="realmmesh", value=Dist.CLIENT)
public final class IsolationClientDiagnostic {
    private static boolean checked;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if ("host".equals(System.getProperty("isolation.role"))) {
            hostTick(mc);
            return;
        }
        if (checked || mc.player == null || mc.level == null) return;
        checked = true;
        Path evidence = Path.of(System.getProperty("isolation.evidence"));
        try {
            if (mc.hasSingleplayerServer()) throw new AssertionError("Expected remote server");
            var config = NeoForgeRpcConfig.snapshot();
            if (!config.host().equals("client-local.invalid") || config.port() != 19001 || config.timeoutMillis() != 3210)
                throw new AssertionError("Client config was overwritten");
            if (!IsolationProbe.VALUE.get().equals("SYNC_POSITIVE_CONTROL_7b9f")) throw new AssertionError("Positive control not synced");
            var field = NeoForgeRpcLifecycle.class.getDeclaredField("CLIENTS");
            field.setAccessible(true);
            if (!((Map<?, ?>)field.get(null)).isEmpty()) throw new AssertionError("Remote client opened RPC connection");
            Files.writeString(evidence.resolve("client-pass"), "remote_world_loaded=true\npositive_control_synced=true\nlocal_rpc_config_preserved=true\nrpc_clients=0\n");
        } catch (Throwable error) {
            error.printStackTrace();
            try { Files.writeString(evidence.resolve("failed"), error.toString()); } catch (Exception ignored) {}
        } finally {
            mc.stop();
        }
    }

    private static void hostTick(Minecraft mc) {
        Path evidence = Path.of(System.getProperty("isolation.evidence"));
        if (Files.exists(evidence.resolve("stop-host"))) {
            mc.stop();
            return;
        }
        if (checked || mc.player == null || mc.level == null) return;
        checked = true;
        try {
            var server = mc.getSingleplayerServer();
            // 隔离测试使用不同本地账号，不连接外部认证服务
            if (server != null) server.setUsesAuthentication(false);
            if (server == null || !server.publishServer(GameType.SURVIVAL, true, Integer.getInteger("isolation.port")))
                throw new AssertionError("LAN publish failed");
            if (!NeoForgeRpcConfig.snapshot().host().equals("private-rpc-6a8b2c-server-only.invalid"))
                throw new AssertionError("Host private config missing");
            Files.writeString(evidence.resolve("host-ready"), "integrated_server_published=true\n");
        } catch (Throwable error) {
            error.printStackTrace();
            try { Files.writeString(evidence.resolve("failed"), error.toString()); } catch (Exception ignored) {}
            mc.stop();
        }
    }
}
