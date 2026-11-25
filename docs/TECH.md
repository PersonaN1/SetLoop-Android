# SetLoop for Android - technical notes

Product decisions are in the iOS repo ([SPEC](https://github.com/PersonaN1/SetLoop/blob/main/docs/SPEC.md),
[DESIGN](https://github.com/PersonaN1/SetLoop/blob/main/docs/DESIGN.md),
[TECH](https://github.com/PersonaN1/SetLoop/blob/main/docs/TECH.md)). This file records how the Android app
implements them. What differs from iOS is listed in [PARITY.md](PARITY.md).

## Stack and versions

- Kotlin 2.4.21, Android Gradle Plugin 9.5.2, JDK 17 target.
- `minSdk` 26 (Android 8), `targetSdk` 37, compile against android-37.1.
- Jetpack Compose (BOM 2026.11.01), Material 3, Navigation Compose 2.10.2 (type-safe routes), Lifecycle 2.10.0.
- Room 2.8.5 (KSP), DataStore Preferences 1.3.0.
- kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0.
- No analytics, no crash SDK, no network permission. Only AndroidX, Kotlin and Material libraries.
- Fonts: Nunito for the timer digits (OFL, license shown in Settings).

## Module layout

- `:core` - plain Kotlin/JVM, no Android: `TimerDefinition` (value types, JSON, limits, merge into block), plan
  builder, `WorkoutEngine`, summary, palette and step colors, strip layout, sound synthesis (`SoundSynth`), names,
  presets. Compiles with warnings as errors; tested on the JVM on JUnit 5.
- `:app` - the Android app, package `io.github.personan1.setloop`:
  - `data` - Room entities, DAOs and repositories (timers, history), `AppSettings` and `PresetSeeder` on
    DataStore, `AppContainer` (manual DI, created in `SetLoopApplication`).
  - `workout` - `WorkoutController`, `WorkoutSession`, `WorkoutService`, audio, haptics, notification, start flow.
  - `tips` - the tip engine: `Tip`, `TipsState` (rules), `TipsStore` (DataStore).
  - `shortcuts` - launcher shortcuts and their intents.
  - `ui` - `navigation`, `timers`, `history`, `settings`, `summary`, `workout`, `tips`, shared `components`, `theme`.

## Persistence

- Room database (`SetLoopDatabase`, version 1, schemas exported to `app/schemas`): `SavedTimerEntity` (list fields,
  cached total duration, JSON `TimerDefinition`) and `WorkoutRecordEntity` (summary fields, JSON steps and a
  definition snapshot for "Repeat"). Every change is written at once.
- Preferences DataStore `settings`: user settings (`AppSettings`), the example seeding flag and the tips state
  (`tips.dismissed`, `tips.introDone`, `tips.openedTimer`, `tips.blockTipDone`). "Show Tips Again" clears the tips keys.
- SharedPreferences `workout_snapshot`: one JSON value rewritten on every workout command, so a killed process can
  rebuild the running workout. `start_flow` stores that the notification permission was already asked.
- Limits: 100 timers, 3000 records, 10 blocks, 30 steps per block (`Limits`).

## Workout runtime

- `WorkoutController` (process-wide, main thread) owns the one running `WorkoutSession`. State is time marks, not
  tick counters; the controller sleeps until the next cue (countdown tick, step end) and publishes `WorkoutUiState`.
  The UI derives the countdown from the step end, so frozen or backgrounded UI is always right on return.
- `WorkoutService` is a foreground service of type `specialUse` (subtype "Interval workout timer with step
  signals"), declared with `FOREGROUND_SERVICE_SPECIAL_USE`. It hosts the effects only: notification, wake lock,
  audio and haptics. It is started from the screen the user tapped Start on (foreground-service start rules).
- Wake lock: a partial wake lock held for the workout remaining time plus one minute, released on pause and finish.
- Notification: one ongoing, colorized notification in the step color; the countdown is a system chronometer, so it
  is rebuilt only on step change, pause and resume. Actions: Pause / Resume and Skip. Small icon `ic_stat_workout` is
  a monochrome silhouette. Visibility follows "Show on Lock Screen".
- Audio: signals are PCM rendered in code and played from static `AudioTrack`s. Usage is `USAGE_ALARM` when "Ignore
  Silent Mode" is on, otherwise `USAGE_NOTIFICATION_EVENT`; content type sonification. Audio focus
  (`GAIN_TRANSIENT_MAY_DUCK`) is taken around a signal and abandoned after a quiet second. Nothing is played or
  focused while paused or when sounds and voice are off. Voice is the system `TextToSpeech` in the interface language.
- Calls: on Android 12+ `CallModeMonitor` listens to `AudioManager` mode changes (ringtone, in call, in
  communication) without `READ_PHONE_STATE`; below that, a transient focus loss during a call pauses. Pause on call
  is optional; after the interruption the workout stays paused.
- Snapshot restore: the service is sticky; a restart with a null intent restores the session from the snapshot,
  an unreadable snapshot is dropped.
- Start flow (`WorkoutStartFlow`): notification permission (asked once, the workout starts if it is denied), then a
  warning when `ActivityManager.isBackgroundRestricted` is true, then the service start.
- Background restriction warning: with the "Restricted" battery setting the system stops the foreground service. The
  app warns before the start and offers the app settings page (Battery > Unrestricted) or "Start anyway".

## Tips

`TipsState` holds the dismissed ids and the three flags the iOS rules use (`introDone`, `openedTimer`,
`blockTipDone`). `visible(...)` returns the first eligible tip of a screen, so a screen shows at most one. Rules:
card actions need the intro closed and a timer opened; the merge tip needs the block tip closed and one editor
visit after that. `TipCard` is the inline card, `TipPopup` is a persistent `TooltipBox` with `RichTooltip` and a
caret; tapping outside closes the tip for good. Events are reported on the application scope so that leaving a
screen still saves.

## App shortcuts

- `res/xml/shortcuts.xml` declares two static shortcuts, Pause and Resume. Their target `WorkoutCommandActivity`
  (no-display theme) acts on `WorkoutController` in the same process and finishes at once.
- `TimerShortcuts` publishes dynamic "Start <timer>" shortcuts for the first timers of the list (system maximum per
  activity minus the two static ones), refreshed by a flow over the timer list started in `AppContainer`.
- A start shortcut opens `MainActivity` (`singleTask`) with `START_TIMER` and the timer id. `ShortcutIntents.parse`
  accepts only known actions and a well-formed id; `ShortcutRequests` then checks that the timer exists and that no
  workout runs, and starts through the normal start flow.

## Icon

Adaptive icon: background color `#E8452C`, foreground vector converted from `docs/icon/AppIcon.svg` of the iOS repo (same strokes,
scaled to fit the 66 dp safe circle of the 108 dp canvas), monochrome vector for themed icons. The store export is
`docs/store/icon-512.png`, opaque and full-bleed.

## Emulator verification (known results)

- Deep Doze plus the restricted standby bucket: a full Tabata ran on schedule, with signals and step changes on time.
- The user's "Restricted" battery setting stops the foreground service. This is by design of Android; the app warns
  before the start.
- Parity pass (API 37 emulator): bottom bar hidden in selection mode; tips shown, dismissed and kept after a restart;
  "Show Tips Again"; launcher long-press menu with Pause, Resume and Start entries; Start through the shortcut
  intent; Pause and Resume from the launcher menu confirmed by the notification action.

## Testing

- `:core:test` - engine, plan, summary, editing, sound synthesis (JUnit 5, kotlin-test).
- `:app:testDebugUnitTest` - JUnit 4, kotlin-test and Robolectric: storage, settings, view-model and list logic,
  workout session, controller and start flow, tips rules and store, shortcut intents, navigation bar visibility.
- Lint: `:app:lintDebug`.

## Build commands

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # macOS with Android Studio
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
