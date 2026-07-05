# Changelog

## [0.1.1] - 2026-07-05

- 首个公开发布准备版本。
- 模块命名为 HyperWallpaperMonet，包名为 `org.hdhmc.hyperwallpapermonet`。
- 修复 HyperOS 3 SystemUI 壁纸取色 / Monet 动态主题色不刷新、始终保持默认蓝色的问题。
- 已测试 HyperOS 3 `OS3.0.300.4.WPMCNXM.C05`。
- 通过动态扫描定位 `ThemeOverlayController` 的壁纸颜色监听器，提高不同 HyperOS 3 构建的兼容性。
- 从 `WallpaperManager` 读取当前壁纸颜色并写回 `ThemeOverlayController#mCurrentColors`，必要时强制执行 `reevaluateSystemTheme(true)`。
- 新增 adaptive icon 和 Android themed icon `monochrome` 图层。
- debug 包使用独立包名 `org.hdhmc.hyperwallpapermonet.debug`，可与 release 包并存，并输出更完整的诊断日志。
- 新增 GitHub Actions CI 和 release workflow，支持源码仓库发布和 Xposed Modules Repo 发布。
- 新增 README、英文 README、发布前检查说明和 Apache-2.0 LICENSE。
