# iOS parity audit

Audit of the Android app against the iOS SetLoop decisions (`SPEC.md` D1..D23, `DESIGN.md` V1..V12,
`TECH.md` T1..T12) and the iOS screens. Every item is one user-visible behavior, marked:

- **Done** - same behavior as on iOS.
- **Different** - deliberate or platform-driven difference, with the reason.
- **Missing** - not implemented, with the reason.

Status of the checks: items were verified by reading the Android code and, where marked "(device)", on the
emulator. Unit tests cover the core engine, storage, view-model logic, tips and shortcut parsing.

## Timer model and editor (D1, D2, D18, V11)

- Done: one constructor for every timer; a simple timer is one block without repeats (D1).
- Done: structure Prepare, sets x N with rest between sets, blocks x M, steps, Cooldown (D2).
- Done: six step types (Prepare, Work A/B/C, Rest, Cooldown) with the type driving color, sound and history category (D2, V2).
- Done: "Merge into Block" on neighboring steps of one block, never a whole block; steps around stay separate blocks (D2).
- Done: step duration 1 s to 99:59, repeats and sets 1..99, up to 10 blocks and 30 steps per block (D18); limit hints under the sections.
- Done: "Skip Last Rest" off by default; removes only the final rest, keeps rest between blocks and sets (D18).
- Done: name field with the "New Timer" prompt; an empty name gets the numbered default (D9, V11); names are capped at 60 characters.
- Done: editor pinned panel with total time, strip and "Select Steps" mode (circles on steps, drag handles) (V11).
- Done: step sheet with name, type list (hidden for Prepare and Cooldown), minute and second wheels; Cancel for a new step, Delete with confirmation for an existing one (V11).
- Done: Cancel with changes asks to discard; back is handled the same way (V11).
- Done: the pinned panel hides while the keyboard is open (V7).
- Done (device): block tip card in the editor and the "Select steps" popup (D23).

## Timer list and preview (D9, D21, V4, V9)

- Done: card with name, total time, short structure and the colored strip; tap opens the preview, the play button starts at once (D9, V4).
- Done: three example timers on first launch, editable, deletable and counted in the limit (D9).
- Done: new and copied names follow the iOS numbering ("New Timer (2)", "Tabata (3)") (D9).
- Done: limit of 100 timers with no counter; adding or duplicating at the limit shows the "delete old ones" hint (D9).
- Done (device): "Select" mode with checkboxes, "Select All" / "Deselect All", "Delete (N)" with confirmation, "Done"; drag handles reorder in this mode (D9, V6).
- Done (device): the bottom navigation bar is hidden in selection mode so the selection bar replaces it, in the timer list and in History (V6).
- Different: edit, duplicate and delete are in a long-press menu and in TalkBack custom actions, not on a swipe. Reason: swipe-to-reveal actions are an iOS list convention; Android lists use long press. The card actions tip says so.
- Done: delete asks for confirmation and says workouts in History stay (D9).
- Done: preview with the name as large title, structure line, summary card (total, strip, Work / Rest / Other) and read-only structure; Start pinned at the bottom (V9).
- Done: preview toolbar with edit and a menu with Edit, Duplicate, Delete (V9).
- Done: "Nothing to run" message when the only step is the skipped last rest.
- Done: default step and example names follow the interface language (T11, `DefaultNames`).

## Workout (D3, D4, D5, D6, T2, T6, T9)

- Done: Pause / Resume as the big center button, Skip marks the step skipped, Back restarts the step or goes to the previous one under 2 s, Finish without confirmation (D3).
- Done: after the last step the summary shows "Completed" (D3).
- Done: full-screen color of the step type, step name, large countdown with a progress ring, cycle and set counters, "Next:", total remaining (D4).
- Done: landscape layout with the digits on the left and the rest in a column (D4).
- Done: signals 3 s before the end of a step, a long signal on step change, a final signal; sets Glass, Marimba, Note and Off, synthesized in code (D5, T6).
- Done: voice prompts (system TTS in the interface language) with the step name and the 3-2-1 countdown, off by default (D5).
- Done: music is ducked around signals and given back after a quiet second, not for the whole workout (D5, T6).
- Done: "Ignore Silent Mode" on by default (D5); implemented with the alarm audio usage, off uses the notification usage.
- Done: vibration with the signals (D5).
- Done: "Pause on Calls" on by default; after the interruption the workout stays paused (D6).
- Different: call detection by audio mode runs on Android 12+. On Android 8 to 11 a call pauses the workout only when it takes audio focus while a signal is playing. Reason: the `READ_PHONE_STATE` permission was rejected on privacy grounds.
- Done: with the call setting off the workout keeps running through the call (D6).
- Done: "Keep Screen On" on by default, applies only while the workout screen is open (D7).
- Done: engine uses a monotonic clock and sleeps until the next cue; state survives freezing (T2, T9).
- Done: the finished workout opens the summary over the app and survives process death through the snapshot (T9).
- Done (device): starting from a launcher shortcut opens the app and runs the same start flow, so the notification permission and the battery restriction warning apply (D15).

## Lock screen and background (D7, T10)

- Different: the Live Activity is a foreground-service notification colored with the step color, a system-side countdown chronometer, step position, "Next:" and Pause / Resume and Skip actions. Reason: no Live Activities on Android; Android 16 Live Updates cannot be colorized, and the color is the point.
- Different: no Dynamic Island. Reason: iOS-only surface.
- Done: "Show on Lock Screen" on by default; off hides the notification content on the lock screen (D7).
- Done: sound and vibration work in the background through a foreground service with a wake lock (T2).
- Different: the notification is removed by the service when the workout ends; there is nothing to clean up at the next launch. Reason: no orphaned Live Activities on Android.
- Different: the user's "Restricted" battery setting stops the foreground service. The app warns before the start and offers the app settings page; there is no iOS equivalent.

## Summary and History (D10, V5, V6)

- Done: summary with check icon, real time, status badge, work / rest / steps metrics, strip with skipped steps faded (V5).
- Done: name field (defaults to the timer name) and optional note; "Save" and "Don't Save" with a discard confirmation when edited (D10).
- Done: only the steps actually reached are saved, with completed or skipped marks; the status is complete or partial (D10).
- Done: history limit 3000 records; the summary warns in advance and saving removes the oldest (D10).
- Done: records by day with a week summary (workouts, time), "Partial" label, strip of the reached steps (D10, V6).
- Done: record details with steps, editable name and note, Delete, Repeat; Repeat works after the timer is deleted because the record keeps a definition snapshot (D10).
- Done: bulk delete in History works as in the timer list (D10).
- Done: swiping a record deletes it at once, without confirmation (D10). Added in this audit.
- Done: editing or deleting a timer never changes its records (D10).
- Done (device): history tip card on the summary (D23).

## Settings (D22, V8)

- Done: groups Workout, Sound and Vibration, Appearance, About; every change is saved at once (V8).
- Done: Pause on Calls, Show on Lock Screen, Keep Screen On, Vibration, Voice Prompts, Sounds with a preview on selection, Ignore Silent Mode (D22).
- Done: notes under the sound group (silent mode, no sounds and voice) (T2, T6).
- Done: Theme System / Light / Dark (D17).
- Done: "Step Colors" screen with a palette per type, strip preview and reset; the rest color cannot be used for work and the work colors cannot be used for rest (D17, V8).
- Done: Privacy Policy and Support links (D20, D22).
- Different: extra "Contact on Telegram" and "Open-source licenses" rows. Reason: the support channel from D20 and the font license (Nunito, OFL) must be shown.
- Done (device): "Show Tips Again" resets all tips and switches to the Timers tab (D22, D23). Added in this audit.
- Done: orientation follows the system, no setting (D22).

## Tips (D23, V12)

- Done (device): "Start with an example" card on first launch, then "More actions" after the intro is closed and a timer was opened (D23).
- Done (device): "Start the workout" popup with an arrow above the Start button in the preview.
- Done (device): "Blocks repeat" card in the editor; "Make a block from steps" popup at the next editor visit (D23).
- Done (device): "Keep it in History" card under the name and note fields of the summary.
- Done (device): each tip shows once; dismissal is stored in DataStore and survives a restart; the close button or the action it describes dismisses it.
- Done (device): at most one tip per screen; tips are hidden in selection mode; no tips on the workout screen (V12).
- Done: tip texts are the iOS catalog texts in English and Russian.
- Different: the card actions tip says "touch and hold" and does not mention swiping. Reason: see the list section.
- Different: there is no 1.0 upgrade path that hides the basic tips. Reason: the Android app has no earlier version.
- Different: tips are drawn by a small in-app engine, not TipKit. Reason: TipKit is Apple-only; the rules are ported and unit-tested.

## Shortcuts and integrations (D15, T11)

- Different: the iOS App Shortcuts (Siri, Spotlight, Shortcuts, Action button) are launcher shortcuts on Android: "Start <timer>" for the first timers of the list, plus "Pause" and "Resume" (D15).
- Done (device): "Start <timer>" shortcuts are dynamic, follow the list order and are updated when timers change; their number is limited by the system maximum.
- Done (device): "Pause" and "Resume" act on the running workout without opening a screen; with nothing to pause or resume a short message says so, as the iOS errors do.
- Done: a shortcut for a deleted timer or while a workout is running shows the iOS messages and does nothing.
- Done: shortcut intents are validated before use (known action, well-formed timer id, existing timer).
- Done: Russian labels from `AppShortcuts.xcstrings`.
- Missing: voice phrases ("Start Tabata in SetLoop"). Reason: needs Google Assistant App Actions with `shortcuts.xml` capabilities and Play services; not available for RuStore distribution.

## Appearance and accessibility (D17, T11)

- Done: one app icon: adaptive icon from `AppIcon.svg` with a monochrome layer for themed icons; 512 x 512 export in `docs/store/icon-512.png` (D17).
- Different: in the monochrome layer the thin gaps at the crossing of the loop are filled. Reason: a vector layer cannot cut a gap; the color icon keeps them.
- Different: Material 3 with step colors on the workout screen instead of iOS system styling. Reason: platform conventions (V7 asks for system text styles; on Android that is the Material type scale).
- Done: font scale support with layouts that stack rows on large sizes (`StepLine`, `MetricRow`), and a cap for the timer digits (T11).
- Done: TalkBack labels, custom actions and spoken durations ("1 minute 30 seconds") (T11).
- Done: tip close button has a label.
- Done: English and Russian strings have the same key set (checked by script), plurals for Russian included (D12).
- Done: names and text fields wrap long text instead of cutting it (V7).

## Data and platform (D11, D13, D14, D16, T1, T5, T7, T8)

- Done: no ads, no analytics, no data collection (D11).
- Done: data stays on the device in Room; timers and records keep a JSON definition snapshot with a version field (T5).
- Different: Android 8+ (API 26) instead of iOS 17+ (D8, T8). Reason: platform reach.
- Different: the Room database is not CloudKit-ready; backup is the Android auto backup. Reason: sync is out of v1 on both platforms (D13).
- Done: v1 has no Health, Watch, widget, sharing or tip jar on either platform (D14, D16, "Deferred").
- Different: Bundle ID counterpart is the application id `io.github.personan1.setloop` (T7).

## Distribution (App Store checklist)

- Done: 512 x 512 store icon without transparency.
- Missing: RuStore listing text, screenshots, privacy questionnaire and review notes for the foreground service. Reason: store work is the next step after parity, not an app feature.
- Missing: CI for the Android repo. Reason: out of scope here; build and test commands are in `TECH.md`.

## Big missing features (not implemented here)

- Google Assistant voice phrases through App Actions (see Shortcuts).
- Android 16 Live Updates (promoted ongoing notification) for the workout. Rejected for now: a promoted notification cannot be colorized.
- Home screen widget. Deferred on iOS too (v1.1).
- iCloud-like sync and Apple Health counterpart (Health Connect). Deferred on iOS too (v1.1).
- RuStore listing assets and the review notes.
