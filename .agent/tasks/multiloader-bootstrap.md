# MultiLoader 最小工程初始化

日期：2026-10-18。状态：基础工程构建与启动验证通过。

## 授权与范围

用户已确认 A 方案并授权搭建、启动。仅 common / neoforge 启用，Fabric / Forge 只预留说明。RPC 后续接入。未执行暂存、提交、Push 或发布，保留用户原有的两项 IDEA 暂存文件。

许可证未定，模组必填元数据暂用 All Rights Reserved；未创建项目 LICENSE。用户明确同意 Minecraft EULA，仅限本项目测试实例。

## 实现

- Java 21、Minecraft 1.21.1、NeoForge 21.1.262、ModDevGradle 2.0.148、Gradle 9.2.1。
- NeoForm 1.21.1-20240808.144430，Parchment 1.21.1 / 2024.11.17。
- realmmesh / RealmMesh / io.github.helantianshen.realmmesh / 0.1.0-SNAPSHOT。
- buildSrc 共享约定，common 使用 Vanilla 编译环境，NeoForge 编译并打包共享源码和资源。
- 仅共享初始化日志与 NeoForge 模组入口，没有业务、协议、Mixin 或示例物品。
- Wrapper JAR 官方 SHA-256 校验通过，ZIP 校验和已固定。
- 上游参考提交见 README；未改写现有 .idea 文件或系统配置。

## 已执行验证

1. `./gradlew build` 成功，首次用时 3m 48s。
2. 排除资源占位文件后，`./gradlew build :neoforge:prepareClientRun :neoforge:prepareServerRun --console=plain` 成功。
3. 成品 JAR 内容检查通过：共享类、NeoForge 入口和已展开 TOML 均存在，无示例类或 .gitkeep。
4. `./gradlew :neoforge:runClient`：识别 RealmMesh 0.1.0-SNAPSHOT，共享初始化成功。实际截图已查看，主菜单显示 Minecraft 1.21.1、NeoForge 21.1.262 和 3 mods。
5. `./gradlew :neoforge:runServer`：共享初始化成功，127.0.0.1:25585，生成隔离测试世界并到达 Done (3.362s)。首次 stop 因 Gradle 未转发输入而无效，TERM 后保存退出。补上 runServer 的 standardInput 后，重跑 `./gradlew build :neoforge:runServer --console=plain`，再次到达 Done (0.982s)，list 与 stop 均生效，所有维度保存，Gradle exit 0 / BUILD SUCCESSFUL。
6. 官方安装器创建独立服务端，将成品 JAR 放入 mods：识别模组并初始化，127.0.0.1:25586，生成世界并到达 Done (4.287s)。list 命令成功，stop 后所有维度保存完成。

成品 SHA-256：`8c33c3f7d01109c0329a5de439756e7a1037eb89f971ead858cb0f8293f9344b`。

## 证据位置

- `neoforge/build/bootstrap-validation/initial-build.log`
- `neoforge/build/bootstrap-validation/build-and-prepare.log`
- `neoforge/build/bootstrap-validation/client-launch.log`
- `neoforge/build/bootstrap-validation/client-menu.png`
- `neoforge/build/bootstrap-validation/installer.log`
- `neoforge/build/bootstrap-validation/packaged-server/logs/latest.log`
- `neoforge/runs/server/logs/latest.log`

上述运行与证据目录忽略提交，执行 clean 会删除 build 内证据。

## 限制与后续

- 客户端主菜单和服务端世界初始化已验证；未验证玩家加入、单人世界操作或真实业务。
- 当前无单元测试，Gradle test 为 NO-SOURCE；运行验收不能描述成单元测试通过。
- 客户端旁白因本机缺少 flite 无法加载，未阻止主菜单启动；未安装或修改系统库。另有原版资源 schema / shader 警告。
- 官方安装器首次直连超时，随后仅给安装器进程显式指定现有 127.0.0.1:7897 HTTP/HTTPS 代理并完成安装；该本机代理未写入工程配置。
- 开发服务端运行环境提示 offline mode，测试仅绑定回环地址；不作为部署配置。
- 客户端窗口保留供用户检视；两个测试服务端验证后关闭。
- Fabric / Forge、RPC、协议 Submodule、客户端业务与资产功能均未实现。

## IDEA Gradle JVM 修正（2026-10-18）

用户截图显示 IDEA 使用 Java 26.0.2 启动 Gradle 9.2.1，导致同步兼容检查失败。原工程只有 Java 编译 Toolchain 21，缺少 Gradle Daemon JVM 约束。

新增 `gradle/gradle-daemon-jvm.properties`，指定 `toolchainVersion=21`，不写机器绝对路径、不升级 Gradle。更新 README 中 IDEA 重新加载与旧 JVM 设置的处理方法。

验证：显式设置 `JAVA_HOME=/home/helan/.jdks/openjdk-26.0.2.1` 执行 `./gradlew --version`，Launcher JVM 为 26，Daemon JVM criteria 为 21；同样环境执行 `./gradlew build --console=plain` 成功（增量任务均 up-to-date）。尚未确认 IDEA UI 重新同步结果。

生成命令 `updateDaemonJvm --jvm-version=21` 曾因未配置自动下载仓库失败；随后直接写入 Gradle 支持的 JVM criteria 属性，使用已安装 JDK 21，无需增加 JDK 下载插件。
