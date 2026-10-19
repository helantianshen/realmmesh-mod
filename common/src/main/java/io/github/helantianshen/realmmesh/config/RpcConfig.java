package io.github.helantianshen.realmmesh.config;

public record RpcConfig(String host, int port, int timeoutMillis) {
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
    }
}
