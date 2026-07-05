# Release Notes for Maintainers

## Local Build

```bash
./gradlew :app:assembleDebug :app:assembleRelease --no-daemon
```

Release signing is read from `local.properties`, Gradle properties, or environment variables.

```properties
releaseStoreFile=release.jks
releaseStorePassword=...
releaseKeyAlias=...
releaseKeyPassword=...
```

Environment variable names:

```text
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

## Pre-Release Checklist

- Local `main` is based on `origin/main`; the remote source repository already has an initial LICENSE commit.
- `gradle.properties` has the intended `appVersionName` and `appVersionCode`.
- `CHANGELOG.md` contains a matching version section, for example `## [0.1.1]`.
- Local debug and release builds pass.
- Release APK metadata matches:
  - package: `org.hdhmc.hyperwallpapermonet`
  - scope: `com.android.systemui`
  - Xposed entry: `org.hdhmc.hyperwallpapermonet.ModernEntry`
- Debug APK metadata matches:
  - package: `org.hdhmc.hyperwallpapermonet.debug`
  - label: `HyperWallpaperMonet Debug`
- GitHub secrets are configured:
  - `RELEASE_KEYSTORE_BASE64`
  - `RELEASE_STORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- To publish to Xposed Modules Repo, `XPOSED_REPO_TOKEN` must have write access to `Xposed-Modules-Repo/org.hdhmc.hyperwallpapermonet`.

## GitHub Release Workflow

Use the `Release` workflow from the source repository.

- `version`: must match `appVersionName`.
- `publish_source_release`: creates `v<version>` and a source repository release.
- `publish_xposed_release`: creates `VERSION_CODE-VERSION_NAME` in the Xposed Modules Repo.
- `xposed_prerelease`: marks the Xposed release as prerelease.

The workflow updates the Xposed repository metadata before creating the Xposed release.

## Xposed Repository Sync Scope

When `publish_xposed_release` is enabled, the workflow writes only these items to `Xposed-Modules-Repo/org.hdhmc.hyperwallpapermonet`:

- Metadata files on the `main` branch:
  - `README.md`
  - `README.en.md`
  - `SCOPE`
  - `SOURCE_URL`
  - `SUMMARY`
- A GitHub release is created with tag `VERSION_CODE-VERSION_NAME`, for example `101-0.1.1`.
- The release title is `VERSION_NAME`, for example `0.1.1`.
- The release notes are generated from the matching `CHANGELOG.md` section and include a source commit link.
- The only release asset is the signed release APK: `HyperWallpaperMonet-VERSION_NAME.apk`.

The workflow does not sync source code, docs, changelog files, debug APKs, local `dist/` files, keystores, or build outputs to the Xposed repository.
