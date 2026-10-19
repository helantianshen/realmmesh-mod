# 第一阶段交付整理

范围：两端 README、候选文件清单、隔离生成构建脚本和复现说明。不暂存、提交或推送，不改业务行为。

2026-10-19 状态：整理与隔离构建完成，等待用户审查。原有暂存区与工作前保存的 git ls-files --stage 完全一致，已有 .idea 文件不属于交付候选。协议固定 a9daf901a4e129a4a228cd75de2b7ef7f22c9ab4。

## 实际验证

- /tmp/realmmesh-phase1-build-w2rvr7gz/result.json 为通过结果，分步日志在同一目录
- 删除快照 gen 后重新生成，两端所有生成文件 SHA-256 与源文件一致
- Go 模块下载、校验、race 测试、vet、中心及 probe 构建通过，go.mod/go.sum 无漂移
- Java 禁用构建缓存并强制重跑，24 个任务执行，BUILD SUCCESSFUL
- 成品 JAR 包含 11 个内嵌依赖，无重复 Guava/Gson，无 Diagnostic 或 IsolationProbe 测试类
- 两端 git diff --check 通过；本机 MCP 配置与 Go 本地配置、bin 忽略规则有效
- GoLand 对 rpc_logging.go、IDEA 对 RealmMeshRpcClient.java 的错误检查均无报错；不是全项目 IDE 审计
- NeoForge 固定版本 installer 官方下载地址 HEAD 返回 HTTP 200，本次未重新安装游戏环境

## 限制与待办

消费者仓库均未首次提交，本次为候选文件快照而非真实克隆。复用了本机依赖缓存，不代表空缓存安装或 JAR 字节级复现。游戏联调采用此前已审查结果，本次未重跑。

Gradle 9.2.1 构建成功，但提示存在 Gradle 10 不兼容的弃用功能，本次未升级或扩展修复。

候选清单为 server 21 项、mod 51 项，各含一个 proto gitlink。没有暂存、提交或推送。用户审查后清理上述 /tmp 快照及日志；首次提交后再执行真实克隆、Submodule 恢复与生成无漂移验收。

## 本地提交准备

用户授权处理 request ID 校验与 Go 测试清理后提交，不推送。当前规则为单个、非空、最多 128 字节，不要求 UUID 格式。临时测试使用非 UUID 标识验证保留行为，并完成 race、vet、build 后删除唯一的 rpc_logging_test.go。联调夹具的拒绝用例调整为超长标识，本轮未重跑游戏联调。

server 候选清单更新为 20 项。Go 临时测试不进入提交，后续功能验证完成后清理该类文件。IDE 文件仅从暂存区移除，磁盘内容保留。本次隔离构建快照及日志已清理，保留本文历史结果；真实克隆验收待执行。

首次完整暂存检查发现 protoc 生成的 Probe.java 有 8 处行尾空格；保留生成器原始输出，避免破坏再生成一致性。排除 gen 后的暂存差异空白检查通过，未手改生成代码。
