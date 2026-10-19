# RealmMesh Mod

Minecraft 1.21.1 的 MultiLoader 最小工程。当前仅启用 `common` 与 `neoforge`；Fabric、Forge 为目录预留，尚未实现和验证。已接入 Echo 协议生成、异步 RPC 客户端、服务器生命周期和测试命令，尚无登录、资产或界面功能。

第一阶段交付、复现顺序与验收边界见 [docs/PHASE1_DELIVERY.md](docs/PHASE1_DELIVERY.md)，待提交候选文件见 [docs/PHASE1_FILES.txt](docs/PHASE1_FILES.txt)。

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

在本仓库目录执行，使用本机 JDK 21：

```bash
git submodule update --init --recursive
./gradlew :common:exportProtoSources build --console=plain
./gradlew :neoforge:runClient
./gradlew :neoforge:runServer
```

IntelliJ IDEA 打开仓库根目录并导入 Gradle。`gradle/gradle-daemon-jvm.properties` 将 Gradle 自身的 JVM 约束为 Java 21，与 Java 编译 Toolchain 分别声明；本机需安装 JDK 21。若 IDEA 保留了旧的 Java 26 设置，在 Gradle 设置中选择 JVM criteria 21（或本机 JDK 21），然后重新加载 Gradle 项目。首次构建需要下载工具链依赖及游戏资源。

服务端启动后可在当前终端输入 `list`、`stop` 等控制台命令。

客户端与服务端数据分别在 `neoforge/runs/client` 和 `neoforge/runs/server`，均忽略提交。独立服务端需要使用者阅读并接受 Minecraft EULA 后，才能将其测试目录内的 `eula.txt` 设置为 `eula=true`。

成品为 `neoforge/build/libs/realmmesh-neoforge-1.21.1-0.1.0-SNAPSHOT.jar`。共享源码和资源随加载器 JAR 打包；不将 `common` JAR 或 `-sources.jar` 放入 mods 目录。

## 协议与生成

`proto/` 是协议仓库的 Submodule，当前固定到 `a9daf901a4e129a4a228cd75de2b7ef7f22c9ab4`。干净检出后先恢复协议引用：

```bash
git submodule update --init --recursive
```

Gradle 根据固定 Maven 坐标获取生成工具，无需使用系统 protoc。正式生成和编译检查命令：

```bash
./gradlew :common:exportProtoSources :common:compileJava :neoforge:compileJava --console=plain
```

`common` 从 `proto/probe/v1/probe.proto` 生成 Java 消息与 gRPC stub。生成文件位于 `common/build/generated/sources/proto/main/`，由 common 与 NeoForge 编译；`commonJava` 导出包含生成任务依赖的源码目录。

`:common:exportProtoSources` 将结果同步到 `gen/java/` 与 `gen/grpc/`，这两份快照纳入 Git 供版本审查，不再次加入编译源码集。`gen/` 是专用生成目录，Sync 会删除陈旧文件，不在其中存放手写代码。更新协议后重新生成，并将 Submodule 引用和生成快照一起提交。不要手改生成代码，也不要复制 `.proto`。

Java 生成包沿用协议命名空间 `realmmesh.probe.v1`，类型为 `Probe.EchoRequest`、`Probe.EchoResponse` 和 `ProbeServiceGrpc`。

NeoForge 使用 `grpc-netty-shaded`，通过 `additionalRuntimeClasspath` 配置 1.21.1 开发运行类路径，并通过 Jar-in-Jar 显式嵌入运行库。ModDevGradle 的 Jar-in-Jar 配置不传递解析依赖，因此 `neoforge/build.gradle` 中逐项列出 8 个 gRPC JAR、protobuf-java、proto-google-common-protos 和 perfmark-api，并使用本次验证的精确版本范围。

Guava 32.1.2-jre、failureaccess 1.0.1 和 Gson 2.10.1 由 Minecraft 提供，不重复打包。采用 gRPC 1.60.2 是为了满足 Minecraft 1.21.1 的依赖约束；升级 gRPC 或 Minecraft 前需要重新核对完整依赖图和实际运行行为。

已在隔离的 NeoForge 开发服与成品服中，用临时诊断代码验证 Java → Go Echo、中心停止后的失败返回、中心重启后复用 Channel 恢复及正常停服。此处为依赖接入阶段记录，见 [.agent/tasks/java-proto-bootstrap.md](.agent/tasks/java-proto-bootstrap.md)。正式客户端的范围和验证命令见下文。

## RPC 连接配置

NeoForge 启动时通过 `ModConfigSpec` 加载游戏实例下的 `config/realmmesh/connection.toml`，首次启动自动生成：

```toml
[rpc]
host = "127.0.0.1"
port = 9000
timeoutMillis = 2000
```

`host` 为非空白主机名或 IP，`port` 范围为 1–65535，`timeoutMillis` 范围为 1–2147483647 毫秒。非法字段由 NeoForge 记录警告、备份原文件并恢复默认值。TOML 语法损坏也由原生配置系统处理，不能将错误配置视为成功应用。

注册类型为 `COMMON`，不向玩家同步；客户端安装 Mod 时也会生成自己的本地配置。`NeoForgeRpcConfig.snapshot()` 必须在配置加载后调用，返回不依赖加载器的不可变 `RpcConfig`。文件重载不会修改已经取得的快照。RPC 客户端在游戏服务器启动时读取一次快照，修改连接配置后需重启游戏服务器。

配置验证脚本使用 JDK 21、Python 3.11+ 和已有的 NeoForge 21.1.262 服务端安装目录（包含 libraries 且已接受 EULA）：

```bash
./gradlew build --console=plain
python3 scripts/verify-rpc-config.py /path/to/installed-test-server
```

脚本在临时目录建立三个隔离世界和回环监听端口，分别验证默认文件、自定义值和非法值校正，并正常停服。诊断类只注入临时 JAR 副本，不修改正式成品或已有世界。终端打印日志所在目录。脚本同时检查 `RpcConfig` 的有效边界和无效参数；它不执行 RPC 通信。

## RPC 客户端

`common` 中的 `RealmMeshRpcClient` 接收 `RpcConfig`，持有一个复用的 Channel 和生成的 future stub。`echo(String)` 返回 `CompletableFuture<String>`，每次请求使用配置中的 deadline，允许空字符串；null 参数和关闭后调用通过异常 Future 返回。

gRPC 请求失败保留原异常，可用 `Status.fromThrowable(error)` 取得状态码。调用方取消 Future 会取消底层 RPC。完成回调不保证运行在游戏线程，不能直接操作游戏状态，也不应在普通完成回调中执行阻塞工作。游戏生命周期和命令入口负责实例持有及线程切换。

客户端使用明文连接并关闭 gRPC 重试机制，不自动重放失败请求。中心恢复后，调用方可以使用同一客户端发起新的请求。`close()` 可重复调用，以 `shutdownNow()` 终止在途 RPC 并异步释放资源，不阻塞等待；gRPC 1.60.2 下此类在途请求以 `UNAVAILABLE` 结束，关闭后的新请求则返回 `IllegalStateException`。

正式客户端的隔离游戏服复验命令：

```bash
./gradlew build --console=plain
python3 scripts/verify-rpc-client.py
```

该脚本需要本仓库已有的 `neoforge/build/bootstrap-validation/packaged-server` 安装环境（含 libraries 和已接受的 EULA）、相邻 Go 服务仓库、Go 工具链及 JDK 21。它在临时目录构建并启动独立 Go 中心、隔离游戏服，在临时 JAR 副本中注入 `scripts/fixtures/RpcClientDiagnostic.java`。测试包含 Echo、20 个并发请求、中心断线恢复、延迟响应触发 deadline、取消传播、重复关闭和 Channel 终止。恢复探测循环只属于测试驱动，不属于正式客户端。结果位于 `neoforge/build/client-validation` 和终端输出的临时目录；审查通过后清理这些测试产物，保留脚本与文字摘要。

## 生命周期与测试命令

`NeoForgeRpcLifecycle` 在 `ServerStartingEvent` 读取本地配置并为当前 MinecraftServer 创建客户端；在 `ServerStoppingEvent` 先移除归属关系，再关闭客户端。连接不等待中心就绪。逻辑服务器事件适用于独立服和内置服；主菜单不会创建客户端。

控制台输入 `realmmesh ping`，玩家输入 `/realmmesh ping`，要求权限等级至少为 2。命令立即返回等待提示，异步调用 Echo，并在服务器线程反馈固定探测消息与耗时或简短失败提示。结果不广播给其他管理员；玩家退出或服务器停止后丢弃迟到反馈。耗时包括整个 RPC 调用，不代表纯网络延迟。

连接配置保持 `COMMON` 类型，不经 NeoForge 配置同步下发。客户端安装后生成的是自己的本地配置，并非远端服务器配置。命令不接收或提供配置参数，不注册配置网络消息；失败反馈仅含 gRPC 状态枚举与固定文案。原始异常不写入 RPC 或命令日志，远端响应先与固定探测消息比对，不匹配时不回显原文。内置服的配置与日志属于运行它的本机用户，不同步给加入游戏的其他玩家。

复验命令为 `python3 scripts/verify-rpc-lifecycle.py`（先执行 `./gradlew build --console=plain`），环境要求同 RPC 客户端验证脚本。脚本使用隔离的实际 NeoForge 独立服、独立 Go 中心和临时诊断 JAR 副本，验证离线启动、命令恢复、权限谓词、游戏线程反馈、错误/响应脱敏、deadline 和停机清理。诊断代码仅位于 scripts/fixtures，不进入正式构建。

自动化通过模拟 CommandSource 检查权限和消息内容，通过在独立服实例上调用启停处理器检查旧实例关闭与新实例创建；这些不等同于真实玩家登录或内置服 GUI 验收。用户已报告独立服和内置服实机测试通过；配置隔离也已通过真实客户端加入独立服与内置服局域网的网络验证，见下文。

## RPC 关联日志

Java 每次 `echo` 调用生成 UUID，通过独立的 gRPC metadata `x-request-id` 发往中心。Go Unary 拦截器保留单个非空且不超过 128 字节的关联标识，不限制 UUID 格式；缺失、空值、超长或重复时生成新 ID。关联标识属于公开日志字段，不得承载配置或密钥。ID 只用于关联，不代表认证身份，也不会发送到玩家聊天。

Mod 使用现有 SLF4J 控制台日志，中心使用 Go 标准库 slog 默认控制台输出。两端分别记录 `rpc_client_start/end` 和 `rpc_server_start/end`，字段限定为 `request_id`、`method`，结束事件额外记录 `status`、`duration_ms`。正常结果为 INFO，失败为 WARN。没有引入日志平台或新版本依赖。

请求正文、响应正文、配置值、完整 metadata、令牌和原始异常描述均不进入这些日志。命令不重复打印 RPC 失败堆栈，玩家反馈仍仅使用固定文案和状态码。本规则针对本项目 RPC/命令日志，不改变 Minecraft 或 Kratos 的其他启动日志。

中心日志表示处理器接收和处理结果，不代表响应已送达调用方；客户端超时、中心处理成功可能同时出现。未到达中心的请求只有客户端日志。Java 和 Go 保留各自状态枚举的拼写，例如 `UNAVAILABLE` 与 `Unavailable`。正常完成的调用每端各有一条开始和结束记录；进程被强制终止时不能保证结束日志落盘。

验证命令：

```bash
# 在 realmmesh-server 中执行
go test -race ./...
# 在 realmmesh-mod 中执行
./gradlew build --console=plain
python3 scripts/verify-rpc-logging.py
```

日志验证脚本复用隔离客户端与生命周期测试，核对并发关联、敏感标记过滤、字段白名单、完成事件和状态。原始测试产物在审查通过后清理，验证脚本和文字摘要保留。

## 配置隔离验证

已用独立游戏目录的真实客户端分别加入独立服、内置服开放的局域网世界，确认远端连接配置不下发：客户端保留自己的 host、port、timeoutMillis，且不会创建属于远端服务器的 RPC 客户端。

测试在隔离项目副本中注册额外的 `SERVER` 对照配置，客户端必须成功收到对照标记并进入世界，才进行隔离断言。TCP 转发捕获中可见对照配置；独立服原始流量和内置服解压后的流量均不含 RealmMesh 私有连接配置文件名或主机标记。加入方配置、日志和游戏目录文件也未出现私有标记。正式源码与 JAR 不包含此测试配置。

复验需要已有的 NeoForge 测试安装环境、JDK 21、Python 3.11+ 和可用图形会话，会启动并自动关闭隔离 Minecraft 窗口：

```bash
python3 scripts/verify-config-isolation.py
# 将上一条命令输出的 TEST_DIR 作为参数
python3 scripts/verify-integrated-config-isolation.py /tmp/realmmesh-isolation-check-实际目录
```

局域网验证使用两个独立客户端进程；测试游戏目录设置 `earlyWindowControl=false` 以避开本机多窗口启动交接问题，不修改日常游戏配置。测试使用回环连接和离线测试账号，不等同于跨机器生产部署或第三方模组审计。结果和清理范围见 [.agent/tasks/config-isolation.md](.agent/tasks/config-isolation.md)。

## 开发边界

- `common` 表示跨加载器共享，可以访问原版 Minecraft API，不引用加载器专属 API。
- `neoforge` 承担 NeoForge 入口和平台适配；当前入口注册连接配置并调用共享初始化。
- 客户端专属类应通过客户端入口隔离，不能从独立服务端加载的路径引用。
- Fabric、Forge 未加入 Gradle settings，也没有对应插件与依赖。
- 已接入协议 Submodule、Java 生成和 gRPC 运行依赖打包，并完成隔离游戏服的通信探测；正式 Java 客户端已接入服务器生命周期与测试命令，后续按小步确认、生成实现、用户审查的流程推进。
- 项目许可证尚未决定，模组元数据暂用 `All Rights Reserved`，不代表采用了模板的 CC0 许可。

实际验收记录见 [.agent/tasks/multiloader-bootstrap.md](.agent/tasks/multiloader-bootstrap.md)。构建、开发环境启动和成品加载分别记录，不互相替代。

Go 功能验证使用的临时 *_test.go 文件在验证完成后删除，不纳入提交；当前 go test 命令只检查包编译，不能视为持续回归测试覆盖。
