# Mod 最小 RPC 配置接入

日期：2026-10-19。状态：实现与隔离服务器验证完成，用户审查通过，测试产物已清理

## 授权与范围

用户已将工作方式调整为每次确认一小步需求，由 Agent 生成代码、验证并讲解，用户审查通过后再继续。本步已批准 NeoForge ModConfigSpec、COMMON 类型、config/realmmesh/connection.toml 和三个连接字段。未授权提交或推送

## 实现

- common/config/RpcConfig 是不可变 record，验证非空白 host、1–65535 的 port 和正整数 timeoutMillis
- neoforge/config/NeoForgeRpcConfig 定义默认值 127.0.0.1、9000、2000，提供配置加载后使用的 snapshot()
- RealmMeshNeoForge 构造器接收 ModContainer，注册 COMMON 配置，不向客户端同步；客户端本地也会生成文件
- 已取得的快照不会随原生文件重载变化；正式 RPC 生命周期尚未接入，后续按服务器启动时获取快照的约定实现
- README 记录配置行为和验证命令；scripts/verify-rpc-config.py 在临时目录验证成品加载，不修改原成品、旧世界或运行配置

## 验证过程

- Linux x86_64，本地 Bash 命令环境，JDK 21.0.12.1，NeoForge 21.1.262
- 初次构建成功；默认值、自定义值验证成功。非法值验证发现 defineInRange 将数字夹到边界，而非恢复默认值；已换用带 Integer 校验器的 define，维持批准的默认值恢复行为
- 测试诊断类仅注入临时成品副本，在 ServerStartedEvent 调用真实 snapshot() 并验证 RpcConfig 的边界和非法参数
- 最终 `./gradlew build --console=plain` 通过，Gradle test 为 NO-SOURCE，不称为单元测试通过
- `python3 scripts/verify-rpc-config.py neoforge/build/bootstrap-validation/packaged-server` 三项通过：defaults=127.0.0.1:9000:2000、custom=localhost:19000:3500、invalid=127.0.0.1:9000:2000。非法配置警告和备份均已验证，三个游戏服正常 stop，退出码均为 0
- 最终证据位于 /tmp/realmmesh-config-check-x_b4pek1，构建日志、三项游戏日志和结果摘要复制到 neoforge/build/config-validation；build 和临时目录可能被清理，可用仓库脚本复验
- 正式 JAR 检查不含诊断类；git diff --check 通过，未变更既有暂存状态和 IDEA 文件
- 尚未进行正式 RPC 请求、客户端 GUI、热重载运行连接或多服验证

## 后续边界

用户已审查通过。按用户要求，后续每次审查通过后清理上一步临时测试产物，保留复验脚本和文字验收摘要

已清理本步两个 /tmp/realmmesh-config-check-* 目录（c96tcg6n、x_b4pek1）、/tmp/realmmesh-config-build.log 和 neoforge/build/config-validation。删除前确认无进程以这些目录为工作目录；未删除复用的 NeoForge 安装环境、正式构建产物、源码或测试脚本。上述日志路径为历史记录，文件现已删除

下一步 RPC 客户端尚在需求确认阶段，未授权实现生命周期接线
