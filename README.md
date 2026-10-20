# RealmMesh Mod

面向 Minecraft 1.21.1 / NeoForge 21.1.262 的 Mod，目前提供中心连接配置、服务器凭据认证、异步 gRPC Echo 和管理员探测命令。

游戏内注册登录、玩家门禁、资产及界面功能尚未实现。当前仅支持 NeoForge；Fabric、Forge 为预留目录。

## 安装与配置

1. 使用 Java 21 和兼容的 NeoForge 服务端或客户端。
2. 将成品 `realmmesh-neoforge-1.21.1-0.1.0-SNAPSHOT.jar` 放入实例的 `mods/`，不要安装 common 或 sources JAR。
3. 首次启动后，在游戏服实例的 `config/realmmesh/connection.toml` 填写中心地址、服务器 ID 与原始密钥。
4. 重启游戏服，在控制台执行 `realmmesh ping`，或由权限等级至少为 2 的玩家执行 `/realmmesh ping`。

服务器 ID 与密钥由中心管理员登记后交付，Mod 不自动注册服务器。配置缺失或凭据格式错误时游戏服仍可启动，但中心通信不可用。

- [连接配置、凭据管理与排查](docs/CONFIGURATION.md)
- [从源码构建](docs/BUILDING.md)

当前使用明文 gRPC，不提供传输加密。项目许可证尚未决定，模组元数据使用 All Rights Reserved。
