package io.github.helantianshen.realmmesh.neoforge.config;

import io.github.helantianshen.realmmesh.config.RpcConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeRpcConfig {
    public static final String FILE_NAME = "realmmesh/connection.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> HOST;
    private static final ModConfigSpec.ConfigValue<Integer> PORT;
    private static final ModConfigSpec.ConfigValue<Integer> TIMEOUT_MILLIS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("rpc");
        HOST = builder
                .comment("中心主机名或 IP 地址，不包含协议和端口")
                .define("host", "127.0.0.1", value -> value instanceof String host && !host.isBlank());
        // 使用校验器使非法数字恢复默认值，范围配置会将越界数字修正到边界值
        PORT = builder
                .comment("中心 gRPC 端口，范围为 1–65535")
                .define("port", 9000, value -> value instanceof Integer port && port >= 1 && port <= 65535);
        TIMEOUT_MILLIS = builder
                .comment("单次 RPC 请求的超时时间，单位为毫秒，范围为 1–2147483647")
                .define("timeoutMillis", 2000, value -> value instanceof Integer timeout && timeout > 0);
        builder.pop();
        SPEC = builder.build();
    }

    private NeoForgeRpcConfig() {}

    // 配置加载后调用，返回的快照不随配置文件重载而变化
    public static RpcConfig snapshot() {
        return new RpcConfig(HOST.get(), PORT.get(), TIMEOUT_MILLIS.get());
    }
}
