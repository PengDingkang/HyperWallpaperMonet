# HyperWallpaperMonet

HyperWallpaperMonet 是一个用于 HyperOS 3 的 LSPosed 模块，尝试修复 SystemUI 壁纸取色 / Monet 动态主题色不刷新、始终停留在默认蓝色的问题。

[English](README.en.md)

## 功能

HyperOS 3 的 `com.android.systemui` 在部分版本上可能已经从壁纸服务拿到了 `WallpaperColors`，但 `ThemeOverlayController` 没有继续生成或刷新动态主题 overlay。表现通常是：

- 换成明显不同颜色的壁纸后，系统控件仍然保持默认蓝色。
- 使用几乎纯色壁纸，控制中心、设置项等仍然没有跟随取色。
- 重启 SystemUI 或切换壁纸后，取色偶尔恢复或仍然失败。

本模块会在 SystemUI 的 `ThemeOverlayController` 生命周期和壁纸颜色回调中补充一次颜色修复：

- 动态定位 HyperOS 3 中的壁纸颜色监听器类。
- 从 `WallpaperManager` 读取当前系统壁纸颜色，必要时回退读取锁屏壁纸颜色。
- 将颜色写回 `ThemeOverlayController#mCurrentColors`。
- 在 overlay 状态不完整时强制执行 `reevaluateSystemTheme(true)`。

模块不会写死主题色，也不会修改壁纸、主题包或系统设置。

## 适用范围

- 目标系统：HyperOS 3
- 目标进程：`com.android.systemui`
- 需要 LSPosed v2.1.0 (7769) 或更新版本，或兼容 libxposed API 102 的框架
- 仅提供 API 102 版本，不提供 legacy Xposed 版本
- 模块没有设置界面；桌面图标仅用于识别模块

### 已知测试环境

| 系统版本 | 状态 | 说明 |
| --- | --- | --- |
| HyperOS 3 `OS3.0.300.4.WPMCNXM.C05` | 已测试 | 可修复 SystemUI 壁纸取色不刷新的问题 |
| 其他 HyperOS 3 版本 | 尝试兼容 | 模块会动态扫描监听器类，但 SystemUI 字段名或逻辑变化仍可能导致失效 |

不同设备、不同 HyperOS 3 构建中的 SystemUI 可能有差异。未列出的版本如果没有生效，请提供 debug 包日志和系统版本号。

## 下载

日常使用请安装 release APK：

`HyperWallpaperMonet-<version>.apk`

debug APK 只用于排查问题，不建议长期启用。需要反馈问题时请看 [debug 包和日志反馈说明](https://github.com/PengDingkang/HyperWallpaperMonet/blob/main/docs/debug-feedback.md)。

## 使用方法

1. 安装 release APK。
2. 在 LSPosed 中启用模块。
3. 作用域选择系统界面 / `com.android.systemui`。
4. 重启 SystemUI，或直接重启设备。
5. 换一张颜色明显的壁纸，观察控制中心、系统控件等动态主题色是否刷新。

如果桌面上出现模块图标，这是普通应用图标。主题图标的 `monochrome` 图层已经声明，但 HyperOS 是否按壁纸给它染色取决于系统桌面实现。

## 反馈问题

如果模块没有生效，请先确认只启用了一个 HyperWallpaperMonet 模块，并尝试重启 SystemUI 或重启设备。

反馈时请提供设备型号、HyperOS 完整版本号、LSPosed 版本、复现步骤和日志。详细步骤见 [debug 包和日志反馈说明](https://github.com/PengDingkang/HyperWallpaperMonet/blob/main/docs/debug-feedback.md)。

## 许可证

Apache License 2.0
