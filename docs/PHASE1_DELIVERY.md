# 第一阶段交付与复现

## 范围与当前状态

第一阶段闭环为 NeoForge → gRPC Echo → Go/Kratos，包括配置加载、异步客户端、服务器生命周期、管理员测试命令、两端关联日志和配置隔离。支持 Minecraft 1.21.1 / NeoForge 21.1.262 的独立服和内置服。Fabric、Forge、账户、资产、TLS 和生产认证不在本次交付中。

协议唯一来源是 realmmesh-proto；两端 proto gitlink 固定到 `a9daf901a4e129a4a228cd75de2b7ef7f22c9ab4`。Go 的 gen 与 Java 的 gen 均纳入候选清单，生成后不得手改。

2026-10-19 整理时，server 和 mod 均尚无首次提交。因此本次只能验证候选文件隔离快照，不能声称完成了真实 Git 克隆验收。本记录的隔离快照验证发生在首次提交之前；用户随后授权清理 Go 测试文件并进行本地首次提交，未授权推送。

## 环境与构建顺序

将 realmmesh-server 与 realmmesh-mod 放在同一父目录。当前流程针对 Linux x86_64、Bash、Git、JDK 21、Go 1.27.1、protoc 36.1、Python 3.12+，Go race 检查需要 C 编译器。Java 生成器由 Gradle 固定 Maven 坐标，使用 gRPC Java 1.60.2；完整版本表见两端 README 和构建配置。

1. 在两个仓库分别执行 `git submodule update --init --recursive`，核对上述协议提交。
2. 按 server README 安装两个固定 Go 生成插件、生成、运行 race/vet 并构建中心和 probe。
3. 在 mod 执行 `./gradlew :common:exportProtoSources build --console=plain`。
4. 启动中心，检查 HTTP `/healthz` 与 probe Echo，再启动游戏服务端执行 `/realmmesh ping`。命令需要权限等级 2；中心不可达时应给出固定失败反馈而不阻塞主线程。

两端依赖版本与生成命令分别见 [Mod README](../README.md) 和相邻 server 的 README。默认地址仅用于回环联调。Mod 的 COMMON 连接配置保存在游戏目录 config/realmmesh/connection.toml；加入远端服务器的客户端不得接收服主的连接配置。日志只包含关联 ID、方法、状态和耗时，玩家反馈不包含连接参数或原始异常。

## 自动隔离生成与构建

在 mod 根目录执行：

```bash
python3 scripts/verify-phase1-build.py
```

脚本读取两端 docs/PHASE1_FILES.txt，将候选文件复制到新的 /tmp/realmmesh-phase1-build-*；协议从本地已提交对象导出。删除快照中的 gen 后重新生成，逐文件比较 SHA-256，执行 Go 模块校验、race、vet、双入口构建，以及 Java 完整重新构建。脚本校验两端原暂存区未变化，日志和 result.json 留在输出的 TEST_DIR。

验证复用本机 Go/Gradle 依赖缓存，并可能联网补齐依赖；Java 禁用构建缓存并强制任务重跑。这不是空缓存新机器安装，也不是 JAR 字节级可复现构建。快照没有消费者 Git 历史，协议对象导出也不能替代未来的 Submodule 克隆验证。

## 游戏联调复验准备

已有的验证脚本需要 neoforge/build/bootstrap-validation/packaged-server 下的 NeoForge 安装。首次准备时使用 NeoForge 官方 Maven 中固定的 21.1.262 installer，在空测试目录安装：

```bash
mkdir -p neoforge/build/bootstrap-validation/packaged-server
curl --fail --location \
  https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.262/neoforge-21.1.262-installer.jar \
  --output neoforge/build/bootstrap-validation/neoforge-installer.jar
(cd neoforge/build/bootstrap-validation/packaged-server && java -jar ../neoforge-installer.jar --installServer)
```

使用者阅读并接受 Minecraft EULA 后，在此测试目录创建 eula.txt，内容为 eula=true。不要复用生产世界。安装命令此次未在全新游戏环境重跑；先前已有安装环境与联调结果见历史任务记录。

先完成两端构建，再在 mod 根目录执行：

```bash
python3 scripts/verify-rpc-logging.py
python3 scripts/verify-config-isolation.py
# 使用上一条输出的实际 TEST_DIR
python3 scripts/verify-integrated-config-isolation.py /tmp/realmmesh-isolation-check-实际目录
```

日志脚本复用客户端与生命周期检查。配置隔离检查需要可用图形会话，并启动真实游戏窗口；内置服检查依赖上一条独立服检查目录，不能提前清理。测试使用同机回环、离线测试账号和隔离目录；不等同于生产或第三方模组安全审计。

## 验收依据与清理

- 用户已确认独立服和单人世界内置服实机通过。
- 关联日志、并发、失败恢复、配置边界等历史结果见 [.agent/tasks/rpc-logging.md](../.agent/tasks/rpc-logging.md) 等分步记录。
- 两条真实网络加入路径的配置隔离与清理记录见 [.agent/tasks/config-isolation.md](../.agent/tasks/config-isolation.md)。历史原始测试产物已清理，现保留脚本、夹具和文字摘要。
- 本次生成与构建的实际结果见 [.agent/tasks/phase1-delivery.md](../.agent/tasks/phase1-delivery.md)。本次不重复宣称执行了游戏联调。
- 本次 /tmp 快照及日志已在用户授权后清理；源码、测试脚本与夹具保留。不要对 /tmp 或 build 做无范围的批量删除。

## 待提交清单与首次提交后验收

两端 docs/PHASE1_FILES.txt 是逐文件候选清单，其中 proto 表示 gitlink。清单不是当前暂存区内容，也不执行暂存。阶段记录、生成源码、Gradle wrapper JAR 和验证脚本在清单内；本机配置、运行产物不在清单内。空目录无法由 Git 单独保存。

目前暂存区含 server 的 .idea/.gitignore，以及 mod 的 .idea/.gitignore 和 .idea/misc.xml，均应排除本次交付；新增忽略规则不会自动移除已暂存的文件。用户已授权按清单整理暂存内容，本机 IDE 文件仅从索引移除并保留磁盘文件，避免提交原有空骨架 blob 或本机 IDE 设置。.codex/ 下的本机 MCP 配置也不交付。proto 源仓库与 web 本次没有待交付修改。

本地首次提交后，待从该提交克隆两个消费者到同级目录，恢复 Submodule，执行上述生成与构建流程，核对 `git status --porcelain` 和 `git diff --exit-code -- gen` 无生成漂移。真实克隆验收通过后才能将该项标记完成；推送仍需单独授权。

Go 临时测试已在最新 request ID 规则下通过 race 验证后删除，当前清单不包含 *_test.go；后续 go test 通过只代表包编译通过。旧快照的测试结果属于删除前的历史证据。
