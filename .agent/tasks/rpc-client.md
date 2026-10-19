# common RPC 客户端

日期：2026-10-19。状态：用户审查通过，已按反馈补充注释并清理测试产物

## 范围

用户批准本步生成 RPC 客户端，完成讲解后等待审查。未接入 NeoForge 生命周期、命令、认证或 TLS，未授权提交和推送

## 实现

- common/rpc/RealmMeshRpcClient 接收 RpcConfig，持有并复用一个 ManagedChannel 和生成的 future stub
- echo 返回 CompletableFuture，逐请求 deadline，空字符串允许，null 和关闭后的请求异步失败
- Guava FutureCallback 将生成 stub 的结果转为字符串 Future，保留 gRPC 失败异常；用户取消 Future 时取消底层请求
- close 使用 shutdownNow，可重复调用，不在调用线程等待资源释放；回调不保证在游戏线程，禁止直接操作游戏状态
- 明文连接、禁用 gRPC 重试；恢复后由调用方发起新请求，正式代码没有轮询或请求重放

## 验证

- 本机 Linux x86_64、JDK 21.0.12.1；./gradlew build --console=plain 通过，test 仍为 NO-SOURCE，不称为单元测试通过
- scripts/verify-rpc-client.py 使用已有 NeoForge 21.1.262 安装库，在临时目录启动回环游戏服和独立 Go 中心。正式 Go 源码未修改，未操作用户原有服务
- scripts/fixtures/RpcClientDiagnostic.java 只编译并注入临时 JAR 副本，调用正式 RealmMeshRpcClient；正式成品包含客户端而不含诊断类
- ASCII、中文、空字符串、20 个并发请求、null 拒绝全部通过；中心停止返回 UNAVAILABLE，重启后原客户端及同一个 Channel 恢复成功
- 临时 Java gRPC 服务接收请求而不回复，验证调用及时返回、实际 DEADLINE_EXCEEDED、deadline 和主动取消传播到服务端
- 在途关闭、重复关闭、关闭后新请求失败通过；两个客户端的 Channel 均在 5 秒内 awaitTermination 成功，临时 Java 服务也正常终止；游戏服 stop 后退出码为 0
- 首轮测试错误地断言 shutdownNow 导致 CANCELLED，实际为 UNAVAILABLE: Channel shutdownNow invoked。修正测试预期，保留底层真实状态，并补充服务端取消通知断言；正式实现未为测试改变异常语义
- 最终测试全部通过，game.log 包含 initial-pass、down-pass、reconnect-pass、timeout-pass、close-pass、all-pass；git diff --check 通过

## 产物与清理

- 首轮目录 /tmp/realmmesh-client-check-lpjrv0lv
- 最终目录 /tmp/realmmesh-client-check-2_wz9pvo
- /tmp/realmmesh-client-build.log 及 neoforge/build/client-validation 保存构建、游戏、中心日志和结果
- 用户审查通过后已清理以上两个临时目录、构建日志和 client-validation 目录，保留源码、复验脚本、安装库和本文验收摘要。清理前确认无进程以这些目录为工作目录；上述产物路径仅作为历史记录

## 后续

已按用户反馈补充客户端资源所有权、deadline、Future 适配、取消传播、异常保留与关闭语义的中文注释；未修改运行逻辑，git diff --check 通过，未重复运行集成测试。本轮不继续实现游戏生命周期或命令入口
