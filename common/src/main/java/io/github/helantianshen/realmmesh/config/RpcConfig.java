// 中心通信配置：保存服务端启动时读取的连接与凭据快照
// 校验失败不包含实际配置值，配置对象的字符串表示始终隐藏密钥

package io.github.helantianshen.realmmesh.config;

import java.util.Base64;

/** 经过校验的不可变配置，修改磁盘配置后需重启逻辑服务器才能生效 */
public record RpcConfig(String host, int port, int timeoutMillis, long serverId, String serverSecret) {
    /** 校验连接参数、正整数身份及中心签发的 32 字节 Base64URL 密钥格式 */
    public RpcConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("RPC host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("RPC port must be between 1 and 65535");
        }
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("RPC timeoutMillis must be positive");
        }
        if (serverId <= 0) {
            throw new IllegalArgumentException("RPC server_id must be positive");
        }

        // Base64URL 无填充编码的 32 字节密钥固定为 43 个字符
        // 解码后重新编码，拒绝填充、非规范尾部位及其他编码形式
        if (serverSecret == null || serverSecret.length() != 43) {
            throw new IllegalArgumentException("RPC server_secret has invalid format");
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(serverSecret);
            String canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded);
            if (decoded.length != 32 || !canonical.equals(serverSecret)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException error) {
            // 不附加解码异常或原始值，避免诊断信息泄露密钥
            throw new IllegalArgumentException("RPC server_secret has invalid format");
        }
    }

    /** 覆盖 record 默认输出，避免日志或调试信息包含原始密钥 */
    @Override
    public String toString() {
        return "RpcConfig[host=" + host + ", port=" + port + ", timeoutMillis=" + timeoutMillis
                + ", serverId=" + serverId + ", serverSecret=<redacted>]";
    }
}
