# 两端 RPC 关联日志

日期：2026-10-19。状态：用户审查通过，测试产物已清理

## 授权和范围

用户批准 Java 与 Go 的 RPC 关联日志、单次 request_id、接收/发起与完成日志、耗时与状态，以及日志和玩家反馈不泄漏配置。未授权提交或推送；不修改 proto，不接入完整链路平台，不推进后续功能

## 实现

- RealmMeshRpcClient 每次 echo 生成 UUID，在独立 Metadata 中设置 x-request-id，通过每次调用的新 stub interceptor 发送，避免并发修改共享 stub
- 使用 SLF4J 记录 rpc_client_start/end；正常 INFO、失败 WARN；只记录关联 ID、固定 RPC 方法、状态枚举和耗时。结果记录不依赖游戏命令反馈，包含取消与本地拒绝
- 日志观察器不改变原 Future 的成功/异常语义；主动取消仍传播到底层 RPC，初始化调用阶段 RuntimeException 也完成异常 Future
- Go internal/server/rpc_logging.go 提供 Unary interceptor，接收 ID 限定为单个 36 字符小写规范 UUID；缺失、重复、超长或非法时替换，不记录原输入
- Go 使用标准库 slog.Default 控制台日志，记录 rpc_server_start/end；处理器的原始响应和错误不变，context 错误对应到 gRPC 状态。已存在的 google/uuid 依赖标记为直接使用，未改变版本
- RealmMeshCommand 移除原始异常日志和重复失败日志，玩家反馈仍为状态枚举与固定文案；响应不匹配只记录固定警告
- 请求/响应正文、配置、完整 metadata、异常描述均不进入这些日志；没有向玩家发送关联 ID 或日志载荷。Kratos 和 Minecraft 自身其他启动日志不在本次改动范围
- Java/Go 状态名遵循各自原生拼写。中心处理完成不等于客户端成功收到响应，未到中心的请求不应出现中心接收日志

## 验证

- 本地 Linux x86_64、Java 21.0.12.1、Go go1.27.1-X:nodwarf5；Gradle 完整 build 通过，正式 JAR 不含诊断类
- go test -race ./... 通过。新增 Go 测试覆盖正常/缺失/非法/超长/重复 ID、敏感正文和 metadata、错误描述过滤、context 取消/超时、状态级别和并发 ID 成对记录
- python3 scripts/verify-rpc-logging.py 通过。复用隔离 NeoForge 成品服、独立 Go 中心和 Java 临时模拟服务，未使用用户正在运行的服务，测试进程均正常退出
- 客户端测试：37 次 Java 调用全部成对记录，26 次 Go 请求成对记录。其中 25 次两端 ID 匹配，包含 20 个并发请求；另 1 次使用原始测试 stub 发送非法 ID，中心替换后记录独立合法 ID
- 客户端状态覆盖 OK、UNAVAILABLE、DEADLINE_EXCEEDED、CANCELLED、INVALID_ARGUMENT；未到中心的调用或临时服务调用仅有 Java 记录符合预期
- 生命周期/命令测试：23 次 Java 调用成对记录，4 次 Go 请求全部对应；覆盖 OK、UNAVAILABLE、DEADLINE_EXCEEDED、INTERNAL 和停服期间结束记录
- 三个敏感标记 PRIVATE_RPC_PAYLOAD_SECRET、PRIVATE_RPC_METADATA_SECRET、PRIVATE_ENDPOINT_192.0.2.99_SECRET 均未出现在两条测试路径的游戏/中心日志中；命令来源捕获器继续验证反馈无私有标记或中心地址/端口
- 日志解析严格检查字段白名单、UUID、耗时、唯一关联和开始/结束配对；git diff --check 通过

## 证据和待清理产物

- /tmp/realmmesh-client-check-3xsregm1
- /tmp/realmmesh-lifecycle-check-cx6qmoxz
- /tmp/realmmesh-rpc-logging-build.log
- /tmp/realmmesh-rpc-logging-go-test.log
- neoforge/build/client-validation、neoforge/build/lifecycle-validation、neoforge/build/logging-validation

用户审查通过后清理以上本轮测试产物，保留正式源码、Go 测试、验证脚本和 fixtures、安装库以及本文摘要

## 未覆盖

- 用户此前报告独立服/内置服实机通过；本次日志改动没有重做内置服 GUI 或真实玩家实测
- 配置隔离的真实远程客户端网络验收仍待确认，本次日志/命令脱敏不替代该项
- 不保证强制杀进程时日志落盘，不提供生产日志持久化、检索、轮转或完整审计系统

## 清理完成

按用户授权，上述本轮临时目录、日志与三个 build 验证目录已清理；源码、脚本、测试夹具和摘要保留。下一步由 Agent 自行验证配置隔离

## 后续配置隔离验收

独立服与内置服局域网的真实客户端配置隔离均已验证通过，详见 config-isolation.md；本文此前待确认项为当时状态
