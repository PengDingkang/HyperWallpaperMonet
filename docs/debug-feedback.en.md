# Debug APK and Log Collection

The debug APK is for troubleshooting compatibility issues across devices and HyperOS 3 builds. Use the release APK for daily use.

## Using the Debug APK

1. Install `HyperWallpaperMonet-<version>-debug.apk`.
2. Enable `HyperWallpaperMonet Debug` in LSPosed.
3. Select System UI / `com.android.systemui` as the scope.
4. If the release APK is also installed, disable the release module in LSPosed and keep only the debug module enabled.
5. Restart SystemUI, or reboot the device.
6. Reproduce the issue, for example by applying a wallpaper with a clear dominant color and checking whether SystemUI dynamic colors update.

The debug APK uses the separate package name `org.hdhmc.hyperwallpapermonet.debug`, so it can coexist with the release APK, but do not enable both modules at the same time.

## Collecting Logs

ADB logcat is recommended:

```bash
adb logcat -c
# Reproduce: change wallpaper / restart SystemUI / reboot and enter launcher
adb logcat -d -v time -s HyperWallpaperMonet:D LSPosed-Bridge:D '*:S' > HyperWallpaperMonet-debug.log
```

Exported LSPosed logs are also acceptable, but please include the complete SystemUI logs around the reproduction.

## Please Include

- Device model.
- HyperOS version and full build number.
- LSPosed version.
- Whether the release APK or debug APK is installed and enabled.
- Whether only one HyperWallpaperMonet module is enabled.
- Reproduction steps, such as changing wallpaper, restarting SystemUI, or rebooting.
- Logcat output or exported LSPosed logs.

Debug logs include SystemUI environment details, listener class resolution, wallpaper color reads, overlay field state, and forced refresh decisions.
