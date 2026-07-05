# HyperWallpaperMonet

HyperWallpaperMonet is an LSPosed module for HyperOS 3. It tries to restore SystemUI wallpaper Monet / dynamic color refresh when the system keeps using the default blue accent.

[中文](README.md)

## What It Does

On some HyperOS 3 builds, `com.android.systemui` may receive `WallpaperColors` from the wallpaper service, but `ThemeOverlayController` does not continue generating or refreshing the dynamic color overlays. Typical symptoms include:

- System UI controls keep the default blue accent after changing to a clearly different wallpaper.
- Nearly solid-color wallpapers still do not affect Control Center or other SystemUI surfaces.
- Restarting SystemUI or changing wallpapers may only sometimes restore dynamic colors.

This module repairs the color flow around SystemUI `ThemeOverlayController`:

- Dynamically resolves the HyperOS 3 wallpaper color listener class.
- Reads current wallpaper colors from `WallpaperManager`, falling back from system wallpaper to lock wallpaper when needed.
- Writes colors back into `ThemeOverlayController#mCurrentColors`.
- Forces `reevaluateSystemTheme(true)` when overlay state is incomplete.

The module does not hard-code an accent color, change wallpapers, modify theme packages, or write system settings.

## Compatibility

- Target system: HyperOS 3
- Target process: `com.android.systemui`
- Requires LSPosed v2.1.0 (7769) or newer, or a compatible framework with libxposed API 102
- API 102 only; no legacy Xposed build is provided
- No settings UI; the launcher icon is only for identifying the module

### Tested Environments

| System version | Status | Notes |
| --- | --- | --- |
| HyperOS 3 `OS3.0.300.4.WPMCNXM.C05` | Tested | Fixes the observed SystemUI wallpaper color refresh failure |
| Other HyperOS 3 builds | Best effort | The module scans listener classes dynamically, but SystemUI field names or logic may still change |

SystemUI can differ across devices and HyperOS 3 builds. For unlisted versions, please include debug logs and the full system version when reporting issues.

## Download

For daily use, install the release APK:

`HyperWallpaperMonet-<version>.apk`

The debug APK is only for troubleshooting and should not be enabled long-term. See [Debug APK and log collection](https://github.com/PengDingkang/HyperWallpaperMonet/blob/main/docs/debug-feedback.en.md) when reporting issues.

## Usage

1. Install the release APK.
2. Enable the module in LSPosed.
3. Select System UI / `com.android.systemui` as the module scope.
4. Restart SystemUI, or reboot the device.
5. Apply a wallpaper with a clear dominant color and check whether SystemUI dynamic colors update.

The APK declares an Android themed icon `monochrome` layer, but whether HyperOS tints it from wallpaper colors depends on the launcher implementation.

## Reporting Issues

If the module does not work, first make sure only one HyperWallpaperMonet module is enabled, then try restarting SystemUI or rebooting the device.

Please include your device model, full HyperOS build, LSPosed version, reproduction steps, and logs. See [Debug APK and log collection](https://github.com/PengDingkang/HyperWallpaperMonet/blob/main/docs/debug-feedback.en.md) for details.

## License

Apache License 2.0
