# Release build

Target stores: Xiaomi GetApps (signed APK, primary), RuStore (APK), optionally an AAB.

## Build

Release signing reads four values from Gradle properties (`~/.gradle/gradle.properties`) or from environment
variables with the same names (for CI):

- `SETLOOP_KEYSTORE_FILE` - absolute path to the keystore
- `SETLOOP_KEYSTORE_PASSWORD`
- `SETLOOP_KEY_ALIAS` - `setloop`
- `SETLOOP_KEY_PASSWORD`

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleRelease :app:bundleRelease :app:lintRelease
```

Outputs:

- `app/build/outputs/apk/release/app-release.apk` - signed APK
- `app/build/outputs/bundle/release/app-release.aab` - signed bundle

If `SETLOOP_KEYSTORE_FILE` is not set, Gradle prints a warning and the release APK is named
`app-release-unsigned.apk`. Debug builds are not affected.

Verify the signature:

```sh
apksigner verify -v --print-certs app/build/outputs/apk/release/app-release.apk
```

Release builds use R8 (minify and resource shrinking). Keep rules are in `app/proguard-rules.pro`
(kotlinx.serialization, type-safe navigation routes, the shortcut activity). Dependency metadata blobs are
disabled (`dependenciesInfo`) because third-party stores do not need them.

## Signing key

- Keystore: `~/Keys/setloop/setloop-release.jks` (PKCS12, RSA 4096, alias `setloop`, valid 30 years),
  outside the repository.
- Credentials: `~/.gradle/gradle.properties` and the macOS Keychain (service `SetLoop Android keystore`,
  account `setloop-release`). Never commit them.

**Back up the keystore and its password in a safe place (offline copy plus a password manager). If the key is
lost, the app cannot be updated in the stores: users would have to install a new app under a new signature.**

Certificate SHA-256:

```
23:A1:3E:19:97:E6:B7:34:83:3E:1C:67:A9:E9:06:C1:73:A0:65:DE:21:CC:48:05:AA:40:49:CE:90:AF:D0:35
```

APK signature schemes: v2 (minSdk 26 does not need v1). Stores that re-sign (for example Google Play App
Signing) are not used here.

## Version bump

1. Set `versionName` in `app/build.gradle.kts` (keep it equal to the iOS `MARKETING_VERSION`).
2. Increase `versionCode` by 1 on every upload to any store. It never decreases.
3. Build, verify the signature, run the smoke test (list, preview, editor save, workout, summary, history,
   settings, shortcut, restart), then upload.

Current: `versionName` 1.1, `versionCode` 1.

## Permissions

- `FOREGROUND_SERVICE` - run the workout as a foreground service so the timer survives the screen being off.
- `FOREGROUND_SERVICE_SPECIAL_USE` - required service type on Android 14+ for an interval timer (subtype is
  declared in the manifest property).
- `POST_NOTIFICATIONS` - show the ongoing workout notification with Pause and Skip on Android 13+.
- `WAKE_LOCK` - keep the CPU awake for step signals while the workout runs.
- `VIBRATE` - haptic signal on step changes.

There is no network, storage, phone or location permission.

## Backup

`allowBackup` is on, so timers, history and settings are restored on a new device. `data_extraction_rules.xml`
and `backup_rules.xml` exclude the transient `workout_snapshot` and `start_flow` preferences.
