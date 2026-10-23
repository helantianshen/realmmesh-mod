# 从源码构建

## 工具链

| 工具 | 固定版本 |
| --- | --- |
| Java | 21 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.262 |
| ModDevGradle | 2.0.148 |
| Gradle Wrapper | 9.2.1 |
| NeoForm | 1.21.1-20240808.144430 |
| Parchment | 1.21.1 / 2024.11.17 |
| Protobuf Gradle Plugin | 0.9.5 |
| protoc / protobuf-java Maven 版本 | 4.36.1 |
| gRPC Java / protoc-gen-grpc-java | 1.60.2 |

工程组织参考 [MultiLoader-Template 固定提交](https://github.com/jaredlll08/MultiLoader-Template/tree/7bd74e027936c3ecefb38a518c5d7ac71476d543)，工具链版本参考 [NeoForge 1.21.1 官方 MDK 固定提交](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/tree/00b49d3f81a257858ca425792d59b9e5ca0f7e56)。Gradle Wrapper 来自 Gradle v9.2.1，JAR 已核对官方 SHA-256，发行 ZIP 校验和写入 wrapper 配置。构建脚本仅保留当前所需约定；不引入 Architectury API。

## 构建与启动

在本仓库目录执行，使用本机 JDK 21。本轮第三阶段协议尚在相邻 `realmmesh-proto` 工作区，消费者 Submodule 指针保持原状，因此当前必须显式指定协议源目录：

```bash
git submodule update --init --recursive
./gradlew -PrealmmeshProtoDir=../realmmesh-proto :common:exportProtoSources build --console=plain
./gradlew -PrealmmeshProtoDir=../realmmesh-proto :neoforge:runClient
./gradlew -PrealmmeshProtoDir=../realmmesh-proto :neoforge:runServer
```

IntelliJ IDEA 打开仓库根目录并导入 Gradle。`gradle/gradle-daemon-jvm.properties` 将 Gradle 自身的 JVM 约束为 Java 21，与 Java 编译 Toolchain 分别声明；本机需安装 JDK 21。在 Gradle 设置中选择 JVM criteria 21 或本机 JDK 21。首次构建需要下载工具链依赖及游戏资源。

运行游戏服前，需准备有效的中心连接配置、服务器凭据并启动中心；内置服同样需要完成中心启动检查。连接配置见 [配置说明](CONFIGURATION.md)。

服务端启动后可在当前终端输入 `list`、`stop` 等控制台命令。

客户端与服务端数据分别在 `neoforge/runs/client` 和 `neoforge/runs/server`，均忽略提交。独立服务端需要使用者阅读并接受 Minecraft EULA 后，才能将其测试目录内的 `eula.txt` 设置为 `eula=true`。

成品为 `neoforge/build/libs/realmmesh-neoforge-1.21.1-0.1.0-SNAPSHOT.jar`。共享源码和资源随加载器 JAR 打包；不将 `common` JAR 或 `-sources.jar` 放入 mods 目录。

## 协议与生成

`proto/` 是协议仓库的 Submodule，当前固定到 `ec166842c43ddbb84335d4f22b703552d93fe3ea`。干净检出后先恢复协议引用：

```bash
git submodule update --init --recursive
```

Gradle 根据固定 Maven 坐标获取生成工具，无需使用系统 protoc。正式生成和编译检查命令：

```bash
./gradlew -PrealmmeshProtoDir=../realmmesh-proto :common:exportProtoSources :common:compileJava :neoforge:compileJava --console=plain
```

`common` 从 `proto/probe/v1/probe.proto`、`proto/auth/v1/auth.proto`、`proto/config/v1/config.proto` 、`proto/audit/v1/audit.proto` 和 `proto/warehouse/v1/warehouse.proto` 生成 Java 消息与 gRPC stub。生成文件位于 `common/build/generated/sources/proto/main/`，由 common 与 NeoForge 编译；`commonJava` 导出包含生成任务依赖的源码目录。

`:common:exportProtoSources` 将结果同步到 `gen/java/` 与 `gen/grpc/`，这两份快照纳入 Git 供版本审查，不再次加入编译源码集。`gen/` 是专用生成目录，Sync 会删除陈旧文件，不在其中存放手写代码。更新协议后重新生成，并将 Submodule 引用和生成快照一起提交。不要手改生成代码，也不要复制 `.proto`。

Java 生成包沿用协议命名空间 `realmmesh.probe.v1`，类型为 `Probe.EchoRequest`、`Probe.EchoResponse` 和 `ProbeServiceGrpc`。

玩家认证生成包为 `realmmesh.auth.v1`，包含 `Auth` 消息类型及 `AuthServiceGrpc`。这些类型由共享 RPC 客户端使用，游戏线程与玩家连接状态由 NeoForge 层管理。

NeoForge 使用 `grpc-netty-shaded`，通过 `additionalRuntimeClasspath` 配置 1.21.1 开发运行类路径，并通过 Jar-in-Jar 显式嵌入运行库。ModDevGradle 的 Jar-in-Jar 配置不传递解析依赖，因此 `neoforge/build.gradle` 中逐项列出 8 个 gRPC JAR、protobuf-java、proto-google-common-protos 和 perfmark-api，并使用本次验证的精确版本范围。

Guava 32.1.2-jre、failureaccess 1.0.1 和 Gson 2.10.1 由 Minecraft 提供，不重复打包。采用 gRPC 1.60.2 是为了满足 Minecraft 1.21.1 的依赖约束；升级 gRPC 或 Minecraft 前需要重新核对完整依赖图和实际运行行为。

仓库生成包为 `realmmesh.warehouse.v1`，包含 `Warehouse` 消息类型和 `WarehouseServiceGrpc`。`realmmeshProtoDir` 只覆盖本次构建的协议输入目录，不复制协议或修改 Submodule 指针。待协议提交及消费者指针按授权更新后，才可使用默认 `proto/` 输入。

仓库隔离诊断脚本及夹具已按用户要求清理；历史验证记录不等同第三方模组验收。

当前仅启用 common 与 neoforge；Fabric、Forge 为预留目录。客户端专属类不能从独立服务端加载路径引用。项目许可证尚未决定，模组元数据使用 All Rights Reserved。
