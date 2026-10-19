# 配置隔离真实客户端验证

日期：2026-10-19。状态：独立服与内置服局域网路径均验证通过，用户审查通过，测试产物已清理

## 授权与实现边界

用户审查通过两端日志实现，要求清理该步产物并由 Agent 自行验证配置隔离。日志任务列出的临时目录、日志和 build 验证目录已清理，摘要保留

本步只新增验证脚本/夹具、更新说明与任务记录；未修改正式 Java/Go 功能代码、用户游戏目录或正式配置，未提交或推送

## 验证方法

- 使用 Minecraft 1.21.1、NeoForge 21.1.262、Java 21 和现有图形会话启动真实客户端进程，不以模拟 CommandSource 或源码推断代替网络连接
- 所有项目副本、游戏目录、世界和配置均在 /tmp 下隔离。服务器连接配置设置唯一私有主机标记、端口 43123、超时 13579；加入方本地配置设置 client-local.invalid、19001、3210
- 临时项目副本注入 IsolationProbe，注册独立的 SERVER 对照配置 isolation-positive.toml，唯一对照标记为 SYNC_POSITIVE_CONTROL_7b9f。正式配置仍使用 COMMON，正式构建产物不含测试类
- 客户端通过回环 TCP 代理加入游戏，记录完整服务端到客户端数据流；独立服关闭 Minecraft 网络压缩，内置服保持默认压缩，脚本解析 VarInt 帧与 zlib 后检查内容
- 客户端 tick 验证实际进入远端世界、SERVER 对照值已收到、RPC 配置快照仍为自己的三个值，并检查远程客户端的 RPC 生命周期映射为空
- 验证网络数据包含对照配置文件名和标记，但不包含私有 RPC 配置文件名及私有主机标记；检查客户端配置文件、日志和游戏目录文件均未出现服务端私有标记
- 核对当前 NeoForge ConfigSync.syncConfigs 源码只遍历 ModConfig.Type.SERVER，ConfigFilePayload 使用 UTF-8 文件名和原始配置字节数组；与实际网络结果一致

## 独立服结果

- scripts/verify-config-isolation.py 通过
- 真实客户端 IsolationProbe 成功加入独立服并加载世界，接收到对照配置
- 捕获服务端到客户端 517005 字节，私有配置文件名与标记均未出现
- 客户端内存快照和文件三个字段均保持本地值，远端玩家客户端未创建 RPC 客户端
- 客户端和独立服务器均正常关闭，进程退出码为 0
- 证据目录：/tmp/realmmesh-isolation-check-tiwa_4l9

## 内置服局域网结果

- scripts/verify-integrated-config-isolation.py /tmp/realmmesh-isolation-check-tiwa_4l9 通过
- 两个独立 Minecraft 客户端进程，IsolationHost 加载隔离世界并 publishServer；IsolationProbe 通过 TCP 代理加入并加载世界
- 内置服测试副本关闭认证以使用本地测试账号，没有修改日常游戏配置或访问真实服务器
- 捕获压缩网络数据 106487 字节，解帧/解压后 669783 字节；同步对照存在，私有配置文件名和标记均不存在
- 加入方的内存配置和文件均保持本地值，生命周期映射为空；两客户端均正常退出，内置服保存测试世界
- 首次尝试第二客户端发生 NeoForge early-window 交接失败，尚未进入网络验证；仅在临时游戏目录设置 earlyWindowControl=false 后复跑成功，未改系统或用户配置
- 最终证据目录：/tmp/realmmesh-lan-isolation-12i5wu26
- 首次失败目录：/tmp/realmmesh-lan-isolation-22wpomo1

## 结果与限制

当前版本两条真实网络加入路径中，RealmMesh 的 COMMON 连接配置没有下发给加入方。此前待确认的配置隔离验收现已通过

测试为同机不同进程、不同游戏目录、真实 TCP 连接，使用测试标记和离线账号；不是跨机器生产部署测试或所有第三方模组的安全审计，也不代表房主无法读取其自己持有的配置。未进行 UI 视觉验收，不声称人工查看聊天画面

## 证据与后续清理

- 原始网络数据、日志、隔离项目/游戏目录位于上述三个 /tmp 目录
- neoforge/build/config-isolation-validation 保存结果摘要、客户端断言和运行日志副本
- 用户审查通过后清理以上本轮产物，保留两个脚本、两个 fixtures、本文和 README 说明
- 正式 JAR 检查不含 IsolationProbe/IsolationClientDiagnostic，git diff --check 通过

## 审查与清理完成

用户已审查通过。三个 /tmp 隔离目录和 build/config-isolation-validation 已清理，保留验证脚本、测试夹具、正式源码和本文摘要。历史证据路径现已删除
