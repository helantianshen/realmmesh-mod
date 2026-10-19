package io.github.helantianshen.realmmesh.diagnostic;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class IsolationProbe {
    public static final ModConfigSpec.ConfigValue<String> VALUE;
    public static final ModConfigSpec SPEC;
    static {
        var builder = new ModConfigSpec.Builder();
        VALUE = builder.define("marker", "client-default");
        SPEC = builder.build();
    }
    public static void register(ModContainer container) {
        // 测试对照配置必须同步，借此确认客户端完成了实际配置握手
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "isolation-positive.toml");
    }
}
