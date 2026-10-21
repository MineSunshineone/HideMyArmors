# 兼容验证（2026-10-21）

版本：HideMyArmors 1.0.4、NBT-API 2.16.1、ProtocolLib 5.5.0-SNAPSHOT-d89a5de。

| 检查 | 环境 | 结果 |
| --- | --- | --- |
| 构建、3 项单元测试 | Java 21 / Gradle 9.6.1 | 通过 |
| 后台线程操作真实装备包、原包隔离、自身与非玩家包保护 | Luminol 1.21.8 / Java 21 | 通过 |
| 同上 | Shiroha 26.2 / Java 25 | 通过 |
| 清除 lore、名称、附魔，保留皮革颜色、完整 CustomModelData、光泽 | 上述两个区域调度服务端 | 通过 |
| 头颅皮肤保留、头颅 lore 清除、显式关闭光泽、MagicCosmetic 标记保护 | 上述两个区域调度服务端 | 通过 |
| 主副手不隐藏、不清除元数据；旧隐藏标记与旧包处理入口均跳过手持物品 | 上述两个区域调度服务端 | 通过 |
| 三个协议客户端：两个观察者、隐藏/显示、重连后的 PDC、权限刷新 | Luminol 1.21.8 | 通过 |

服务端测试用真实的 ProtocolLib/NMS 装备包，在后台线程直接调用本插件监听器；接收者代理只允许读取实体 ID，读取玩家世界等状态会使检查失败。客户端测试连接真实服务端，检查收到的装备包。它们不能替代真人客户端的视觉检查或所有第三方插件联动测试。

本地测试目录为 `C:\Users\MineSunshine\Desktop\Test-McServer\Survival\tmp\hidemyarmors-compat`，使用独立测试世界和仅绑定本机的端口。原生 Folia、1.21 的其他小版本、26.3 及后续版本没有逐个启动测试，不作无 bug 保证。

## 重跑

1. `./gradlew build compatibilityProbeJar`。
2. 在独立 Folia / Paper 测试服安装主插件、对应版本 ProtocolLib，以及 `build/compatibility-probe/*-compatibility-probe.jar`。测试探针会自动检查并关闭服务器，**不要放进实际使用的服务器**。
3. 查看日志中的 `COMPATIBILITY PROBE PASSED` 或 `COMPATIBILITY PROBE FAILED`，不能只看进程退出码。
4. 客户端检查：以 `-Dhidemyarmors.probe.keepRunning=true` 启动独立的 1.21.8 测试服，端口 `25598`；在仓库执行 `npm install --prefix build/compat-client minecraft-protocol --no-audit --no-fund`，然后 `node src/test/client.cjs <minecraft-protocol 模块的绝对路径>`。测试完成后正常停止测试服。

本机 CodeMC 仓库 TLS 连接失败，因此本次构建从 NBT-API 官方 `2.16.1` GitHub 标签构建依赖，并用临时 Gradle init script 指向该本地制品。正常网络环境仍使用 `build.gradle.kts` 中的官方 Maven 坐标。临时源码与脚本保存在 `build/compat-deps`，未提交到项目源码。

Windows 的 Java NIO 在本机默认临时目录遇到 loopback 错误时，本次验证使用 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=E:\GitHub-Repo\HideMyArmors\build` 绕开；它不是插件运行要求。

上游版本依据：[ProtocolLib 发布记录](https://github.com/dmulloy2/ProtocolLib/releases)、[NBT-API 2.16.1](https://github.com/tr7zw/Item-NBT-API/releases/tag/2.16.1)、[Folia 调度说明](https://docs.papermc.io/paper/dev/folia-support/)。
