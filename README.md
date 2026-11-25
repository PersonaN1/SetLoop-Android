# SetLoop for Android

Android version of [SetLoop](https://github.com/PersonaN1/SetLoop) - interval timer with blocks, sets and any combination of steps.

Status: in development. Android 8+ (API 26). Kotlin, Jetpack Compose, Room, DataStore; distribution via RuStore.

- [docs/TECH.md](docs/TECH.md) - stack, modules, workout runtime, build commands.
- [docs/PARITY.md](docs/PARITY.md) - audit against the iOS app.

Build and test:

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```
