# Debug 包和日志反馈说明

debug 包用于排查不同设备或不同 HyperOS 3 构建上的兼容问题。日常使用请安装 release 包。

## 使用 debug 包

1. 安装 `HyperWallpaperMonet-<version>-debug.apk`。
2. 在 LSPosed 中启用 `HyperWallpaperMonet Debug`。
3. 作用域选择系统界面 / `com.android.systemui`。
4. 如果同时安装了 release 包，请先在 LSPosed 中禁用 release 包，只保留 debug 包启用。
5. 重启 SystemUI，或直接重启设备。
6. 复现问题，例如更换明显颜色的壁纸后观察系统主题色是否刷新。

debug 包使用独立包名 `org.hdhmc.hyperwallpapermonet.debug`，可以和 release 包同时安装，但不要同时启用两个模块。

## 收集日志

推荐使用 ADB 收集日志：

```bash
adb logcat -c
# 复现问题：换壁纸 / 重启 SystemUI / 重启设备后进入桌面
adb logcat -d -v time -s HyperWallpaperMonet:D LSPosed-Bridge:D '*:S' > HyperWallpaperMonet-debug.log
```

也可以在 LSPosed 中导出日志，但请尽量包含复现问题前后的完整 SystemUI 日志。

## 反馈时请提供

- 设备型号。
- HyperOS 版本和完整系统版本号。
- LSPosed 版本。
- 安装并启用的是 release 包还是 debug 包。
- 是否只启用了一个 HyperWallpaperMonet 模块。
- 复现步骤，例如换壁纸、重启 SystemUI、重启设备后的表现。
- 日志文件或 LSPosed 导出的日志。

debug 日志会包含 SystemUI 版本环境、监听器类解析、壁纸颜色读取、overlay 字段状态和强制刷新原因。
