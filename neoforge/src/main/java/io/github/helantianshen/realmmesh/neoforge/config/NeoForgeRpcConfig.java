// 中心通信配置：定义本机 TOML 参数并构造服务端启动快照
// 凭据不参与配置自动纠错，格式错误由快照校验以固定消息报告

package io.github.helantianshen.realmmesh.neoforge.config;

import io.github.helantianshen.realmmesh.config.RpcConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeRpcConfig {
    public static final String FILE_NAME = "realmmesh/connection.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> HOST;
    private static final ModConfigSpec.ConfigValue<Integer> PORT;
    private static final ModConfigSpec.ConfigValue<Integer> TIMEOUT_MILLIS;

    private static final ModConfigSpec.ConfigValue<Object> SERVER_ID;
    private static final ModConfigSpec.ConfigValue<Object> SERVER_SECRET;

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
        // 接受配置原始类型，避免自动纠错日志打印凭据；snapshot 负责严格校验
        SERVER_ID = builder
                .comment("中心登记的服务器 ID，使用引号包围的十进制正整数字符串，重启生效")
                .define("server_id", (Object) "", value -> true);
        SERVER_SECRET = builder
                .comment("中心签发的服务器原始密钥，仅保存在本机，重启生效")
                .define("server_secret", (Object) "", value -> true);
        builder.pop();
        SPEC = builder.build();
    }

    /** 配置定义仅通过静态入口访问 */
    private NeoForgeRpcConfig() {}

    /** 读取一次配置并校验凭据，返回的快照不随配置文件重载而变化 */
    public static RpcConfig snapshot() {
        Object configuredId = SERVER_ID.get();
        Object configuredSecret = SERVER_SECRET.get();
        if (!(configuredId instanceof String idText) || !(configuredSecret instanceof String secret)) {
            throw new IllegalArgumentException("RPC credentials must be strings");
        }

        // parseLong 按十进制读取 64 位身份；重新编码拒绝加号、前导零和空白
        long serverId;
        try {
            serverId = Long.parseLong(idText);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("RPC server_id must be a positive decimal integer");
        }
        if (serverId <= 0 || !Long.toString(serverId).equals(idText)) {
            throw new IllegalArgumentException("RPC server_id must be a positive decimal integer");
        }

        return new RpcConfig(HOST.get(), PORT.get(), TIMEOUT_MILLIS.get(), serverId, secret);
    }
}
