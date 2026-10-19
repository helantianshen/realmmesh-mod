# NeoForge 生命周期与测试命令

日期：2026-10-19。状态：代码审查通过；用户报告独立服与内置服实机测试通过，配置隔离待确认

## 授权与边界

用户批准独立服与内置服生命周期、权限等级至少为 2 的 realmmesh ping 命令，并要求关键注释和服务端配置不泄漏给客户端。当前无提交、推送授权；按小步流程等待审查后再推进

## 实现

- NeoForgeRpcLifecycle 按 MinecraftServer 实例维护并发映射，ServerStartingEvent 创建使用配置快照的客户端，ServerStoppingEvent 先移除再关闭
- 逻辑服务器事件覆盖独立服与内置服，未注册客户端专属入口；主菜单不创建客户端。后两项为实现路径判断，尚未做客户端 GUI 实测
- RealmMeshCommand 注册 realmmesh ping，根命令要求权限等级至少为 2，固定 Echo 消息，异步执行，回调切回服务器线程
- 回调入队前、入队后检查当前服务器客户端身份；停机或玩家退出后不发送迟到消息；结果仅回复命令来源，不向其他管理员广播
- COMMON 配置注册保留。核对当前 FML 4.0.45 ModConfig.Type 源码：COMMON 不同步，SERVER 同步。没有添加配置载荷或配置参数/补全接口
- 原始异常只进入服务端本地日志；反馈只包含状态枚举和固定文案。远端响应必须与固定消息一致才显示成功，异常响应原文不回显
- 补充资源归属、线程、停机竞争和信息边界的中文注释，README 更新运行方式和验收边界

## 验证

- JDK 21.0.12.1、NeoForge 21.1.262；完整 Gradle build 通过，test 为 NO-SOURCE；git diff --check 通过
- scripts/verify-rpc-lifecycle.py 使用隔离独立服、动态回环端口、独立编译的 Go 中心和临时注入诊断类的成品副本。未修改正式 Go 源码或用户运行服务，正式 JAR 不含诊断类
- 中心未启动时游戏服成功启动，ping 返回 UNAVAILABLE；启动中心后命令成功；中心再次停止/启动后原客户端恢复，未替换实例
- 真实服务器控制台执行 realmmesh ping，成功输出固定消息及耗时
- 模拟 CommandSource 验证根命令等级 0、1 拒绝，等级 2 允许；消息均在服务器线程发送
- 临时 gRPC 服务分别返回含 PRIVATE_ENDPOINT_192.0.2.99_SECRET 的错误描述与响应，命令反馈不含该标记或中心地址/端口。错误描述出现在本地服务端诊断日志符合预期，未发给命令来源
- 不回复的临时服务触发 DEADLINE_EXCEEDED；等待期间服务器仍能执行提交的任务
- 在同一独立服上手动调用启停处理器验证旧 Channel 终止、创建新客户端；此项不是实际内置服换世界测试
- 实际停服时仍有请求在途，ServerStoppedEvent 检查映射已清理、Channel 在 5 秒内终止、没有迟到反馈，游戏进程退出码 0
- 首轮各行为检查通过，但诊断线程写最终标记前 JVM 正常退出，导致驱动报错。将最终标记移入停服事件并关闭临时测试服务，复跑全部通过
- 复跑结果：offline-pass、online-pass、down-pass、recovery-pass、privacy-timeout-pass、stopped-pass、all-pass 均通过

## 尚未实际验证

- 内置服 GUI 进入世界、退回主菜单、再次进入世界
- 真实玩家登录后的权限和聊天显示，以及断线时反馈丢弃
- 远程客户端加入独立服/局域网内置服后的网络配置同步检查；当前依据 COMMON 类型源码及代码路径检查，不声称已经抓包验收
- 没有实施 TLS、认证、负载或生产环境验证

## 审查通过后清理

- /tmp/realmmesh-lifecycle-check-hpyfkmt8
- /tmp/realmmesh-lifecycle-check-pn50afq6
- /tmp/realmmesh-lifecycle-build.log
- neoforge/build/lifecycle-validation

本步测试进程均已退出。保留 scripts/verify-rpc-lifecycle.py、scripts/fixtures/LifecycleDiagnostic.java、正式源码、安装库及本文摘要，用户审查通过后清理上述测试产物

## 用户实机反馈与清理

用户报告单人世界内置服和独立服实机测试全部通过，但未确认配置隔离；此项单独保留为待验收，不扩展为网络隔离通过。用户同时提出两端缺少 RPC 控制台日志，下一步确认关联日志方案后实施

上述两个临时目录、临时构建日志和 lifecycle-validation 已清理；保留正式源码、安装库、复验脚本及本文摘要

## 后续配置隔离验收

独立服与内置服局域网的真实客户端配置隔离均已验证通过，详见 config-isolation.md；本文此前待确认项为当时状态
