# linux-ime-fcitx

让 Linux 上的 Minecraft（Fabric）文本框支持 **fcitx5** 输入法（中文 / 日文 / 韩文）。

本模组通过 fcitx5 的 D-Bus portal（`org.freedesktop.portal.Fcitx`）连接输入法，
并把预编辑与候选词**全部交给客户端自绘**——fcitx5 自身不再弹出候选窗，
从根本上避免了 Wayland 下 fcitx 窗口位置错乱、以及它遮挡游戏内 UI 的问题。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | >= 0.19.3 |
| Java | >= 25 |
| fcitx5 | 已运行，并启用 D-Bus portal |

> 该 Minecraft/Fabric 组合使用 **official（Mojang 官方）命名空间**运行，
> 因此本模组无需 intermediary 重映射。

## 安装

1. 安装 Fabric Loader（26.2）。
2. 把 `linux-ime-fcitx-1.0.0.jar` 放进 `.minecraft/mods`。
3. 启动游戏，在任意文本框（聊天、命令、告示牌等）中即可用输入法输入。

无需额外配置。

## 功能

- **客户端自绘候选框**：候选词与预编辑完全由模组绘制，fcitx5 不画窗。
- **动态定位**：候选框跟随光标；靠近屏幕底部时自动翻到输入框上方，并做屏幕内约束，
  命令输入框等底部场景不会再看不到。
- **输入法提示**：显示 fcitx 的 aux 文本（如切换输入法时的「拼音 / en」）。
- **Esc 行为**：输入未完成时按一次 Esc 清空组词与自绘 UI，再按一次才关闭界面。

## 从源码构建

工程为标准的 Fabric Loom 项目：

```bash
# 构建，产物在 build/libs/
./gradlew build

# 构建并复制到 mods 目录
./gradlew deploy
```

`deploy` 默认复制到 `/mnt/ssd/MC/.minecraft/versions/26.2-Fabric/mods`，
可用属性覆盖：

```bash
./gradlew deploy -PmodsDir=/path/to/.minecraft/mods
```

## 项目结构

```
src/main/java/linuxime/
  LinuxImeMod.java          模组入口（Fabric 客户端初始化）
  FcitxIntegration.java     输入法集成与候选框渲染
  fcitx/FcitxClient.java    fcitx5 D-Bus 客户端
  dbus/                     自实现的 D-Bus 协议栈
  mixin/                    EditBox / Screen 注入
src/main/resources/
  fabric.mod.json
  linux-ime-fcitx.client.mixins.json
```

## 许可证

[GPL-3.0-only](LICENSE)
