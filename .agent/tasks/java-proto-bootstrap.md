# Java 协议生成接入

日期：2026-10-18。状态：用户已确认统一使用 Java gRPC 1.60.2；生成、完整构建、依赖打包以及隔离开发服和成品服的 Java → Go 通信验证通过。

## 范围

用户授权执行协议 Submodule、固定工具版本、Gradle 生成、共享源码导出、生成快照和编译验证，后续明确授权统一 gRPC 1.60.2、调整打包配置并进行对接测试。实际 Java 业务代码由用户手写，诊断 Java 代码只存在于隔离测试目录。未授权 Commit、Push 或发布。

## 当前实现

- `proto/` 固定到 `a9daf901a4e129a4a228cd75de2b7ef7f22c9ab4`，`.gitmodules` 和 gitlink 已暂存；保留原有 IDEA 暂存文件。
- Protobuf Gradle Plugin 0.9.5、protoc/protobuf-java Maven 4.36.1、gRPC Java 与生成插件 1.60.2。
- `common` 从协议 Submodule 生成 Java 消息和 gRPC stub；`commonJava` 分别导出手写及两份生成源码目录，生成目录带任务依赖。
- `:common:exportProtoSources` 将生成代码同步到根目录 `gen/`，用于纳入 Git 的快照；编译仍使用 build 内的生成目录，避免重复类。`gen/` 仅用于生成快照，Sync 会清理陈旧文件。
- 未修改任何手写 Java 文件；已接入 grpc-netty-shaded 1.60.2、NeoForge additionalRuntimeClasspath 和 Jar-in-Jar。显式列出 11 个运行 JAR，使用精确版本范围；Guava、failureaccess 和 Gson 使用游戏提供的版本。

## 验证

- `:common:exportProtoSources` 通过，生成 `Probe.java` 和 `ProbeServiceGrpc.java`；强制重复生成后两文件 SHA-256 均一致，`gen/` 未被忽略。
- 原工程编译失败：Minecraft 严格固定 Guava 32.1.2-jre / failureaccess 1.0.1，gRPC 1.84.0 声明 Guava 33.6.0-android，后者需要 failureaccess 1.0.3。
- 此处为首次检查记录，后续游戏运行验证见下文。

## Guava 兼容调查

用户要求调查共用 Minecraft Guava 的 gRPC 版本，未授权切换正式版本。

- Maven Central POM 核对：1.60.2 声明 Guava 32.0.1-android；1.61.0、1.61.1、1.62.2、1.63.0、1.64.0、1.65.1 声明 32.1.3-android；1.66.0、1.67.1、1.68.3 声明 33.2.1-android；1.69.1–1.75.0 本次抽查版本声明 33.3.1-android；1.76.0 声明 33.4.8-android；1.84.0 声明 33.6.0-android。1.60 分支最新发布为 1.60.2。
- 隔离副本 `/tmp/realmmesh-grpc-compat-b85dba7p` 仅将 gRPC 版本改为 1.60.2，保持 Protobuf 4.36.1，Java 生成、common 编译与 NeoForge 编译全部通过。未排除或强制替换 Minecraft 依赖。
- `guava-resolution.log` 确认 NeoForge 编译类路径实际选择 Guava 32.1.2-jre。
- 独立 JVM 网络实验 `runtime-check/` 使用 grpc-netty-shaded 1.60.2、Guava 32.1.2-jre、failureaccess 1.0.1；打印类来源确认实际版本；复用 Channel 验证 ASCII、中文和空字符串，blocking stub 与 Guava future stub 共 6 次 Echo 均通过。日志为 `runtime-check.log`。
- 以上是编译和普通 JVM 运行证据，不是 NeoForge 游戏类加载、成品 Jar-in-Jar 或 Java ↔ Go 验收。正式工程仍保留 grpc_version=1.84.0，Java 手写代码未修改。

## 采用 1.60.2 后的实现与验收

- 用户已批准 1.60.2，正式工程版本已更新，Go 端依赖不变。
- 配置修正：`additionalRuntimeClasspath` 必须在 `neoForge.version` 设置并建立运行配置之后声明；首次配置阶段报错已修复。
- `./gradlew :common:exportProtoSources build :neoforge:prepareServerRun --console=plain` 通过。Gradle 单元测试任务为 NO-SOURCE，不表述为单元测试通过。
- 成品内包含生成类和 11 个 Jar-in-Jar 项，均固定版本，未嵌入 Guava、failureaccess、Gson。元数据保存到 `neoforge/build/rpc-validation/jarjar-metadata.json`。
- 成品路径验证：在 `/tmp/realmmesh-rpc-integration-wvt9vwjx` 的成品副本中加入临时诊断类，复用本项目已接受 EULA 的测试环境和现有服务端 libraries，新建隔离世界与回环端口。正式成品未注入诊断类。NeoForge 1.21.262 加载成功，世界启动到 Done。
- 开发路径验证：在 `/tmp/realmmesh-rpc-dev-sc059h65/dev-project` 的工程副本中加入同一临时诊断类，通过 `:neoforge:runServer` 启动；仅加载源码模组，不额外放入成品 JAR。
- 两条路径都打印实际 Guava 32.1.2-jre 和 failureaccess 1.0.1 的游戏库来源。独立线程通过一个复用 Channel 对临时启动的 Go 中心发起 ASCII、中文、空字符串请求，blocking 与 future stub 共 6 次均原样返回。
- 两条路径均验证：只停止测试拥有的 Go 中心，2 秒 deadline 内返回 UNAVAILABLE；重启同一测试中心端口，用同一 Channel 在 15 秒 deadline 内恢复 Echo。最后关闭 Channel，游戏服接受 list/stop，世界保存，进程正常退出。未停止用户原有的 8000/9000 Go 服务。
- 日志：`neoforge/build/rpc-validation/build.log`、`game.log`、`center.log`、`result.json`、`dev-game.log`、`dev-center.log`、`dev-result.json`。临时测试使用回环地址、明文 gRPC 与离线游戏服；测试世界平坦世界参数缺省产生一条 layers 日志，但未阻止启动与测试。
- 边界：诊断线程验证不阻塞游戏主线程的调用方式；尚无正式生命周期客户端、玩家交互、TLS、认证和多服负载测试。旧版 gRPC 的完整维护/安全评估未在本轮执行。
- 下一步：开发者在 common 编写正式 RPC 客户端，在 NeoForge 生命周期事件中初始化与关闭，复用已验证的运行和打包配置。
