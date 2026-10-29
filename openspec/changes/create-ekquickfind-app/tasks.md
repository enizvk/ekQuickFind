# Tasks

Each group follows red-green-refactor: write the failing test first, make it pass, then tidy. Every group leaves the app building and installable.

## 1. Project scaffold

- [x] 1.1 Create the Gradle project with modules `:app`, `:core:search` (JVM), `:providers`, `:data` and a `libs.versions.toml` pinning Kotlin 2.x, AGP 9.x, Compose BOM, Material 3 1.4+, Hilt, Room, Coil, OkHttp, kotlinx.serialization, Glance; verify `./gradlew assembleDebug` succeeds with an empty activity
- [x] 1.2 Set `minSdk = 37`, `targetSdk = 37`, `compileSdk = 37` in the version catalog only, enable the Compose compiler in `:app` (the only UI module) and KSP in all Android modules; verify the debug APK installs and launches on an Android 17 device
- [x] 1.3 Configure JUnit 5, Turbine, kotlinx-coroutines-test for `:core:search` and Robolectric for `:providers`/`:data`; verify a trivial test in each module runs with `./gradlew test`
- [x] 1.4 Add `.editorconfig`, ktlint (or Compose-aware lint) and a README describing modules and how to run tests; verify `./gradlew lint ktlintCheck` passes

- [ ] 1.5 Use the supplied app artwork as the adaptive launcher icon (white surround removed, magnifier inside the safe zone for circle and squircle masks, white glyph kept for themed icons); verify mask previews and the icon on the device home screen

## 2. Core search engine (`:core:search`, pure JVM)

- [x] 2.1 Define `SearchProvider`, `SearchResult`, `Query`, `Section`, `Requirement`, `Action` and `ResultIcon` types per design D3; verify the module compiles with no Android imports (add a test that scans dependencies for `android.*`)
- [x] 2.2 Implement text normalisation (lowercase, strip diacritics, collapse spaces) with tests for "Café" → "cafe" and multi-space input
- [x] 2.3 Implement `QueryMatcher` scoring prefix > word-initials > substring > edit-distance-1 (only for 4+ char queries), test-first with fixtures from `specs/unified-search` ("gm" → Gmail above Google Maps, "whatsap" → WhatsApp, no typo match for "ma"; short-query rules later tightened in 15.14)
- [x] 2.4 Implement `Ranker` combining text score, section weight, launch-count boost with recency decay and pin boost per design D4, with table-driven tests covering the "Spotify ranks above untouched apps" scenario and pinned-first ordering
- [x] 2.5 Implement `SearchEngine` using `mapLatest` + `channelFlow` fan-out, per-section replace, per-provider budget (default 700 ms) after which the provider no longer blocks settling and late results are appended only while the query is unchanged; verify with Turbine tests for: progressive emission order, stale query cancellation ("ma" then "map"), a timed-out provider no longer holds the state unsettled and its late results are appended, disabled providers never invoked
- [x] 2.6 Implement section capping and "best match" selection (top result falls back to a web-search result when empty); verify tests for cap = 4 with 12 results, hidden empty sections, and web fallback

## 3. Quick answers provider (`:core:search`)

- [x] 3.1 Implement an arithmetic expression parser/evaluator (+ − × ÷, parentheses, powers, percent, decimals) test-first; verify "12*8+3" = 99, "15% of 240" = 36, "12*" yields no result, division by zero yields no result
- [x] 3.2 Implement `UnitConverter` for length, mass, temperature, volume, speed, data size, time with natural phrasing ("5 km in miles", "72f to c"); verify parameterised tests including magnitude-based rounding (2 decimals below 10, 1 below 1000, whole numbers above, so "3.11 mi" and "22.2 °C" as in the spec) and unknown or mismatched units yielding no result
- [x] 3.3 Implement `QuickAnswersProvider` emitting synchronously with copy-to-clipboard as primary action; verify engine test that it emits in the same tick as the query

## 4. Apps provider (`:providers`)

- [x] 4.1 Define `AppsSource` interface (list launcher activities per profile, icon loading, shortcut listing, change callbacks) and implement it on `LauncherApps` + `UserManager`; verify Robolectric test enumerates fake packages across two profiles
- [x] 4.2 Implement in-memory `AppIndex` as `StateFlow` built on a background dispatcher, refreshed from `LauncherApps.Callback`; verify JVM tests with a fake source for initial build, package added/removed, label update
- [x] 4.3 Implement `AppsProvider` matching label and package name, work badge, hidden-apps filter and shortcut matching gated on `hasShortcutHostPermission()`; verify tests for package-name match, hidden app excluded, shortcut result "New tab · Chrome"
- [x] 4.4 Add a Coil `Fetcher` for app icons keyed by component + user with memory cache; verify an instrumented test loads a real icon within 50 ms on second request
- [x] 4.5 Persist a label cache in `:data` so the index renders before `LauncherApps` finishes on cold start; verify a test that the provider serves cached labels while the live index is still loading

- [x] 4.6 Add an "Excluded apps" settings page listing all launchable apps with a per-app switch and a filter field, backed by the same hidden-results store as long-press Hide; verify a view-model test that the list reflects hidden ids and a UI test that excluding Calculator removes it from results and re-including restores it

## 5. Search surface UI (`:app`)

- [x] 5.1 Create `SearchActivity` (translucent, singleInstance, edge-to-edge, keyboard shown on resume) and Hilt `Application`; verify instrumented test that the activity shows the keyboard and the field is focused on launch
- [x] 5.2 Build `SearchSurface` composable: scrim, bottom-anchored `QueryField` tracking `WindowInsets.ime`, `ResultList` with sticky section headers and `animateItem`; verify Compose UI test that typing shows an Apps section and a top result
- [x] 5.3 Wire `SearchViewModel` to `SearchEngine` with `AppsProvider` and `QuickAnswersProvider` registered; verify end-to-end UI test "cam" → Camera as top result, keyboard action launches it and finishes the activity
- [x] 5.4 Implement result long-press menu with provider-supplied secondary actions (App info, Uninstall, Hide, Pin placeholder); verify UI test that long-press shows the menu and "Hide" removes the row
- [x] 5.5 Implement dismissal: `PredictiveBackHandler` shrink preview, tap-on-scrim, swipe-down at scroll top, and keyboard hide on upward scroll; verify UI tests for scrim tap, back, pull-down and keyboard hide, plus a predictive back preview screenshot taken by dispatching back progress events (the test phone uses 3-button navigation, so there is no back swipe to perform by hand)
- [x] 5.6 Apply Material 3 Expressive theme with dynamic colour, `MotionScheme.expressive()`, `LoadingIndicator` for the Apps section before index load, haptic on top-result launch; verify on-device screenshot tests for light, dark and dynamic colour (saved PNGs plus luminance checks; the alpha Compose Preview Screenshot plugin was not used)
- [x] 5.7 Add `WindowSizeClass`-based centred dialog layout for expanded widths; verify screenshot test at tablet width

## 6. Contacts provider (`:providers`)

- [x] 6.1 Define `ContactsSource` and implement it on `ContactsContract` (name, nickname, organisation, normalised phone, email, starred, times contacted, 50-row limit); verify Robolectric test against an in-memory contacts provider
- [x] 6.2 Implement `ContactsProvider` with permission gating that emits a single "Enable contacts search" row when ungranted, call/message/open actions, multi-number chooser action and starred tie-break; verify JVM tests for partial phone match "0871", organisation match, starred ordering, ungranted row
- [x] 6.3 Wire permission request flow in `:app` (dialog on first denial, app-settings deep link after permanent denial); verify UI test that tapping the enable row triggers the permission launcher

## 7. Device settings provider (`:providers`)

- [x] 7.1 Create the `SettingsEntry` table (~80 entries: title, breadcrumb, keywords, intent action, extras, fallback action) including synonyms like "dark mode" and "hotspot" ("torch" lives with the flashlight toggle in 7.3); verify a JVM test that every entry has at least two keywords and a fallback
- [x] 7.2 Implement `DeviceSettingsProvider` with keyword matching and resolve-or-fallback launching via an injected `IntentResolver`; verify tests for "dark mode" → Dark theme screen and unresolvable action falling back to parent
- [x] 7.3 Implement live toggle results (Wi‑Fi, Bluetooth, DND, rotation, battery saver via `Settings.Panel`; flashlight via `CameraManager`) showing current state; verify Robolectric tests for state rendering and an instrumented test toggling flashlight on a device with flash
- [x] 7.5 Show an inline switch on single-toggle results (Wi‑Fi, Bluetooth, Do Not Disturb, auto-rotate, battery saver, flashlight): flip in place where Android allows it (flashlight; Do Not Disturb with notification-policy access; auto-rotate with modify-system-settings access, asking for that access on first use), and open the system quick panel or settings page for toggles only the system may change (Wi‑Fi, Bluetooth, battery saver); verify unit tests per toggle for direct flip, access request and panel fallback, and an on-device test flipping flashlight from the switch
- [x] 7.6 Switch Bluetooth through the system confirmation dialogs (request Nearby devices once; REQUEST_ENABLE / REQUEST_DISABLE via activity result; wait for the new state, then refresh; open Bluetooth settings when the system refuses without showing a dialog); verify unit tests for permission, allow, deny and refusal paths, and an on-device check that the switch turns Bluetooth on and off without opening settings
- [x] 7.4 Add an instrumented test that resolves every table entry on the API 37 emulator and reports unresolvable ones; verify it passes or the entries are moved to fallback-only

## 8. Files and media provider (`:providers`)

- [x] 8.1 Define `MediaSource` over `MediaStore.Files` (display name LIKE, type filter, modified date, 50-row limit, selected columns) and implement it; verify Robolectric test with seeded media rows
- [x] 8.2 Implement `FilesMediaProvider` with per-type permission gating (full, partial-selected, none) emitting enable rows, extension matching ("pdf"), recency tie-break and default-handler open with chooser fallback; verify JVM tests for each permission state and recency ordering
- [x] 8.3 Add a Coil fetcher using `ContentResolver.loadThumbnail` for image/video results and type icons for others; verify UI test that an image result shows a thumbnail
- [x] 8.4 Implement Share and Show-in-folder secondary actions; verify UI test that long-press shows both for a file result

## 9. Web provider (`:providers`)

- [x] 9.1 Define `WebEngine` (search + suggestion URL templates) for DuckDuckGo, Google, Bing, Brave and custom templates, with one kotlinx.serialization parser for the OpenSearch format they all return; verify JVM tests parsing recorded fixture responses for each engine
- [x] 9.2 Implement `NetworkPolicy` and `WebProvider` with 250 ms debounce, 5-result cap, cancellation on query change, offline → hidden section; verify tests with a MockWebServer that no request is made when suggestions are off and in-flight requests are cancelled on new input
- [x] 9.3 Implement the always-present "Search the web for <query>" fallback result opening the chosen engine in the default browser; verify UI test that the fallback appears when no provider matches and intent is fired on tap

## 10. History, pins and ranking feedback (`:data`, `:app`)

- [x] 10.1 Create Room database with `launch_event`, `pin`, `recent_query` tables and DAOs exposing Flows, plus 90-day pruning on launch; verify Robolectric DAO tests for insert, query by result id, prune
- [x] 10.2 Implement `HistoryRepository` providing launch-count boosts to the `Ranker` and recording opens and queries from the ViewModel; verify engine integration test that opening Spotify five times reorders results
- [x] 10.3 Implement Pin/Unpin action and the zero-query state (Pinned, Recent results, Recent queries, Suggested apps from `UsageStatsManager` when granted, fresh-install hint); verify UI tests for pin/unpin flow and empty-history hint
- [x] 10.4 Implement per-item removal and "Clear search history" (keeps pins, resets boosts) with confirmation; verify DAO test and UI test that recents are empty afterwards and pins remain
- [ ] 10.5 Add backup rules excluding the Room database by default; verify `adb shell bmgr` full backup on emulator omits the database file

- [x] 10.6 Isolate instrumented tests from the user's data: a Hilt test module swaps in an in-memory database, so tests can't erase real pins or history; verify the device suite leaves the real pin and launch tables unchanged
- [x] 10.7 Mark pinned results with a pin icon in rows, tiles, chips and the strip; verify a UI test that pinning shows the mark and unpinning removes it
- [x] 10.8 (removed by 15.20) Add "Remember recent searches" (on by default): when off, queries aren't recorded, the Recent searches list is hidden, and existing recent searches are deleted; verify repository and DataStore tests and a UI test that no Recent searches appear after searching with it off

## 11. Settings screen (`:app`, `:data`)

- [x] 11.1 Implement typed DataStore `UserPreferences` (kotlinx.serialization JSON instead of protobuf) (provider toggles, search engine + custom template, theme override, dynamic colour, hidden apps, web suggestions on/off, backup include); verify DataStore round-trip tests
- [x] 11.2 Build the Settings destination with Navigation Compose and shared-element top bar: provider switches with live permission status and grant/revoke links; verify UI test that disabling Files hides the section and the engine never calls the provider
- [x] 11.3 Add Appearance section (system/light/dark, dynamic colour) applied to surface, widget and settings; verify screenshot test for forced dark over light system
- [x] 11.4 Add Entry points section (pin widget via `requestPinAppWidget`, add tile via `requestAddTileService`, set default assistant via `ACTION_VOICE_INPUT_SETTINGS`) each showing current status; verify instrumented test that status reflects tile added/removed
- [x] 11.5 Add Privacy section stating stored data, network use (web suggestions state) and the clear-history action; verify UI test that the text reflects the suggestions toggle

## 12. Additional entry points (`:app`)

- [ ] 12.1 Implement Glance search-bar widget with dynamic colour, resizable, launching `SearchActivity` with scale-up animation; verify widget preview renders in light/dark and instrumented test that tapping launches the activity
- [x] 12.2 Implement `TileService` with `startActivityAndCollapse` and `unlockAndRun` on lockscreen; verify instrumented test on emulator that the tile opens search and manual check on locked emulator
- [x] 12.3 Implement `VoiceInteractionService` + session service launching `SearchActivity` with `ACTION_ASSIST`, and manifest filters for `ASSIST`, `SEARCH`, `WEB_SEARCH`; verify that ekQuickFind appears in the system default-assistant picker and the assistant gesture opens search on the emulator
- [x] 12.4 Handle incoming `SEARCH`/`WEB_SEARCH` intents by pre-filling the query; verify instrumented test with `am start -a android.intent.action.WEB_SEARCH -e query cats`

- [x] 12.5 Add launcher shortcuts: static "New search" and "Settings", and dynamic shortcuts for up to three pinned results that open the item directly and follow pin changes; verify a Robolectric test of the published shortcuts, an on-device check that the shortcuts are registered, and a UI test that the open-result intent launches the pinned app

## 13. Performance and release readiness

- [x] 13.1 Add `:benchmark` Macrobenchmark module measuring cold start to first frame and keystroke-to-results; verify baseline numbers are recorded in `docs/performance.md`
- [x] 13.2 Generate and ship a baseline profile covering launch, typing, top-result launch; verify Macrobenchmark shows startup improvement vs. no profile
- [x] 13.3 Enable R8 with resource shrinking and Compose stability config; verify the minified build with a UI Automator smoke suite in `:benchmark` (the Hilt-based UI tests need unobfuscated test hooks, so they keep running on debug)
- [ ] 13.4 Add a GitHub Actions workflow running unit tests, lint, and Macrobenchmark regression check (fail if P50 startup regresses > 15 %); verify the workflow passes on the main branch

## 14. Integration verification

- [ ] 14.1 Run the full scenario checklist from every `specs/**/spec.md` on an Android 17 emulator and a physical device, recording pass/fail in `docs/acceptance.md`; verify all scenarios pass or are listed with an explicit follow-up
- [ ] 14.2 Dogfood for one week with all entry points enabled; verify no ANRs or crashes in the device bug reports and that ranking boosts behave as expected

## 15. Personalisation (background, sizing, keyboard behaviour)

Requested after planning; builds on the settings screen from group 11. Covers the personalisation requirements added to `specs/search-settings`.

- [x] 15.1 Extend `UserPreferences` with background style (Blur, Frosted glass, Solid colour), blur strength, dim amount (all styles), solid colour, icon size, density, results per section, position, keyboard auto-open, Enter-runs-selected, haptics and reduce motion; verify DataStore round-trip tests for every new field and defaults
- [x] 15.2 Map preferences to a `SurfaceStyle` value provided through a `CompositionLocal`, and wire results-per-section into `EngineConfig`; verify unit tests for the token mapping and an engine test that the cap follows the preference
- [x] 15.3 Implement the window background controller (runtime blur radius and flag, solid colour, frosted panels, blur-unavailable fallback via `CrossWindowBlurEnabledListener`); verify on-device screenshots for Blur, Frosted glass and Solid colour and a test that the fallback applies when blur is disabled
- [x] 15.4 Apply icon size, density, top/bottom position and reduced motion in the surface; verify on-device screenshots for Large icons, Compact density and Top position, and a UI test that reduce motion disables item animations
- [x] 15.5 Add the selection model (default top result, hardware Up/Down, highlight), Enter-runs-selected toggle, keyboard auto-open toggle and haptics toggle; verify UI tests for Enter on/off, arrow-key selection and keyboard not opening when auto-open is off
- [x] 15.6 Add a Personalisation section to Settings with live preview of the background style; verify a UI test that changing each option updates the surface without restarting
- [x] 15.7 Add the compact grid layout (on by default for Apps, available for every section, Files tiles showing thumbnails) with a setting to switch back to list rows; verify UI tests for both layouts and an on-device screenshot showing more results per screen in grid mode
- [x] 15.8 Show the icon of the app that opens each result: rows without their own app icon (settings, contacts, files, web) show the handling app's icon, and rows with thumbnails or avatars show it as a small corner badge; verify unit tests for handler resolution per result type and an on-device screenshot of a mixed result list
- [x] 15.9 Highlight matched characters in result titles: `QueryMatcher` returns highlight ranges mapped to the raw text (accents, collapsed spaces), and rows render them in the primary colour and bold; verify unit tests for prefix, word prefix ("notif" in "My Notification"), initials, all-words, typo and accented text, plus an on-device screenshot
- [x] 15.10 Add quick, smooth motion everywhere it helps: surface exit animation before the activity finishes, Coil cross-fade for icons and thumbnails, `animateContentSize` on section expand ("More"), animated clear button and top-result content, loading-to-results cross-fade, toggle state change, dialog and menu transitions, settings navigation transitions; all durations at most ~250 ms and all of it switched to fades by reduce motion; verify a UI test that the exit animation runs before finish and that reduce motion removes slide and item animations
- [x] 15.11 Make section order user-configurable: `EngineConfig` becomes a live flow from preferences (order and per-section caps), and Settings gets a reorderable section list with move up/down and reset; verify an engine test that order follows the config flow without restarting, a DataStore round-trip test, and a UI test that moving Settings above Apps reorders the surface
- [x] 15.12 Add per-section layout (Rows, Grid, Chips) with defaults Apps=Grid, Recent=Chips, Recent searches=Chips, others=Rows, configurable in Settings; chips keep icon, title and long-press menu; verify UI tests for Settings as chips and Recent as chips, and a DataStore round-trip test for the per-section layout map
- [x] 15.13 Add the optional recents strip (small chips next to the search bar, off by default, visible before and while typing, horizontally scrollable); verify a DataStore round-trip test and UI tests that the strip is absent by default and shows recent chips when on
- [x] 15.14 Make fuzzy matching start at three characters: one- and two-character queries match only by prefix or word prefix; initials, all-words and substring from three; typos from four; verify matcher and highlight tests for "s" (no substring hits), "vsc" initials and unchanged longer-query behaviour
- [x] 15.15 Align icons and text consistently in rows, tiles, chips, the strip and settings (icons centred on the text's first line box, equal icon slots for app and glyph icons); verify on-device screenshots of each layout
- [x] 15.16 Web suggestions emphasise the completion rather than the typed text, and identical suggestions (after normalising) are collapsed; verify unit tests for completion ranges and de-duplication
- [x] 15.17 Add the "Selected result by the search bar" option: the selected result renders in a fixed slot at the bottom of the panel, outside the scrolling list; verify a DataStore round-trip test and a UI test that the top result sits below the sections when on
- [ ] 15.18 Regroup settings into tinted panels with compact rows (`surfaceContainerLow` panels on `surface`); verify an on-device screenshot review and that the existing settings UI tests still pass
- [x] 15.19 The recents strip shows recently opened items only (pinned items keep their own section), and its setting says it replaces the Recent list; verify a UI test that a pinned item isn't repeated in the strip
- [x] 15.20 Remove Recent searches on request: typed queries are no longer stored, the "Remember recent searches" setting and the Recent searches layout choice go, and schema v3 drops `recent_query` (auto-migration keeps pins and launches). Recent (recently opened, with each item's own icon) and the optional strip stay; verify unit and UI tests, and a device screenshot of the empty search screen
- [ ] 15.21 Swipes that start on the selected result by the search bar behave like swipes on the list: up hides the keyboard, pulling down closes; device tests run on default settings so the owner's layout can't move swipe targets; verify a UI test that swipes on the slot
- [x] 15.22 Use One UI's own window blur (`View.semSetBlurInfo`, by reflection) when Android's cross-window blur isn't supported, as on Samsung phones; verify on device that the compositor shows a blur region on the search window and a screenshot over Settings
- [x] 15.23 Don't draw an empty panel on the empty screen when the strip holds the recents and nothing else is left; verify unit tests for when the panel shows
- [x] 15.24 Shorten the search placeholder to "Search..." in the search bar, settings preview and wide widget; verify the widget unit test
- [ ] 15.25 Add an About panel at the end of settings with the author and app version; verify a UI test that both are shown
- [x] 15.26 New defaults: Frosted glass, blur 29, dim 45% (Medium icons and Comfortable density already were); verify the DataStore defaults test
- [ ] 15.27 Add a "Web search" switch that turns off the web provider, its fallback row and the web options; verify an engine test that a turned-off provider drops its fallback, and a UI test
- [ ] 15.28 Replace the up/down buttons in Result order with drag handles (accessibility move actions kept); verify unit tests for the reorder maths and a UI drag test
- [x] 15.29 (dropped) Double-tap Home through an accessibility service: tried on the phone, but navigation bar buttons send injected key events that bypass the accessibility key filter, so the service never sees them; removed
- [ ] 15.30 Web results show as a compact link list (smaller icon, tighter one-line rows) and ignore saved grid or chips layouts; the Web layout choice is gone from settings; verify unit tests for the dense style and a device screenshot
- [ ] 15.31 Show a fade and a "More below: <sections>" line between the results and the selected slot while more results are below; tapping it hides the keyboard and scrolls down; verify unit tests for the label, UI tests that it appears and scrolls when results overflow and stays hidden when they fit, and a device screenshot
- [ ] 15.32 Recent strip position setting, Top (above the results, default) or Bottom (below them); verify DataStore default and round-trip tests, unit tests for the stacking order, and a device screenshot

## 16. Rename, distribution and self-update

- [x] 16.1 Rename everything to ekQuickFind: packages `com.ekquickfind.*`, application ID, class names, project name, database file, OpenSpec change; verify all unit tests, lint, ktlint and every variant build
- [x] 16.2 Split into `store` and `direct` flavors; only `direct` has the update feed, `REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS` and the install receiver; benchmarks target `store`; one baseline profile in `src/main`; verify both release APKs carry the profile and only `direct` declares the permissions
- [x] 16.3 Release signing from `keystore.properties` (key outside the project, git-ignored), debug key as fallback; verify both release APKs are signed with the release certificate
- [x] 16.4 Update checker and downloader: feed parsing, version compare, https-only APK links (localhost allowed), SHA-256 check, cancellation passes through; verify MockWebServer unit tests
- [x] 16.5 Installer, status receiver, notifications, daily WorkManager job, Updates settings panel, `autoUpdate` preference (default on); verify DataStore tests and a clean build of all variants
- [x] 16.6 `updateFeedDirectDebug` / `updateFeedDirectRelease` tasks write the APK and `update.json`; versions come from `gradle.properties`; verify the generated feed
- [x] 16.7 End-to-end on the phone: version 1 installed, version 2 served from a computer over `adb reverse`; check offered 0.2.0, download passed the checksum, Android asked once (adb was the installer of record), and after the owner tapped Update the phone runs 0.2.0 (build 2) with ekQuickFind as its installer, so later updates can install without a prompt
- [x] 16.8 README landing page with fresh screenshots of the renamed app (contacts and files off so no personal data shows); developer notes in `docs/development.md`, release steps in `docs/updates.md`
- [x] 16.10 Flat, modern launcher icon: vector magnifier and spark on solid cyan (adaptive, themed icon kept); `docs/icon.png` rendered from the same shapes; verify on the phone
- [x] 16.11 About shows the GitHub page (opens in the browser) and the plain version name, no build number; verify on the phone and an updated UI test
- [x] 16.12 Accent colour setting (System + 9 colours) re-tints the accent roles of the theme; verify unit tests for hue, light/dark tones and contrast, a DataStore test, a UI test, and screenshots with Red and System
- [x] 16.13 Clearer settings: panels use `surfaceContainerHigh` for more contrast, headings get icons, Layout splits into Results and Search bar, Appearance sits next to Background; verify on-device screenshots and the entry-points test scrolling by tag instead of list position

## 17. Photo tags, Ask AI, settings menu

- [x] 17.1 Photo tag index (Room v4: `photo_tag`, `photo_scan`) with word-start search, replace and forget; verify store tests
- [x] 17.2 XMP keyword reader (dc:subject, lr:hierarchicalSubject, entities); verify unit tests
- [x] 17.3 Resumable, incremental tagger (ML Kit bundled labeller on 384 px thumbnails + keywords; newest first; failures keep keywords and aren't retried); verify unit tests
- [x] 17.4 Files search matches photo tags for 3+ letter queries, one row per photo, tag shown in the subtitle; verify provider tests
- [x] 17.5 "Photo tags" switch under Files (off by default), progress "Tagged x of y photos", WorkManager run now plus daily while charging, off deletes tags; verify a clean build of all variants
- [ ] 17.6 Photo tags on the phone: grant photo access, turn on, wait for tagging, search "dog" or "beach"
- [x] 17.7 Ask AI section (ChatGPT, Claude, Gemini chips; app icons when installed; per-service switches in Web and AI); verify provider tests and an on-device screenshot
- [x] 17.8 Settings menu with pages (icon, title, summary, arrow) and per-page panels; device tests navigate through pages; verify on-device screenshots
- [ ] 17.9 Direct APK size: ML Kit's native libraries take it from ~5 MB to ~50 MB; consider per-ABI direct APKs (store bundles split by ABI anyway)
- [x] 17.10 Photo tagging keeps going after settings or the app closes: the run is an expedited WorkManager job promoted to a foreground `dataSync` service with a "Tagging photos x of y" notification (falls back to background when Android refuses); reopening settings resumes an unfinished run; verify on the phone that the foreground service runs with the notification
- [ ] 17.11 Settings rows are easier to tell apart: hairline dividers between rows in panels, Photo tags indented under Files with an accent bar; verify an on-device screenshot
- [x] 17.12 Direct release APK limited to arm64-v8a (18.7 MB instead of 50 MB); first release built into `dist/v0.1.0/` (APK + update.json), signed with the release key
- [x] 17.13 README: full feature list, Settings section shows the menu; on-request `SettingsScreenshotsTest` captures every settings page from the app window
- [x] 17.14 Floating button: overlay window from a `specialUse` foreground service, tap opens search, drag with snap to the nearer edge (saved), size and opacity sliders that apply live, hidden while the app is open except on its settings page, restarts after boot; verify geometry and preference unit tests and a clean build of all variants
- [ ] 17.15 Floating button on the phone: user grants "Display over other apps", then checks tap, drag, snap, size and opacity
- [x] 17.17 Floating button: opacity down to 10%, neutral gray instead of the accent, and it restarts after app updates (MY_PACKAGE_REPLACED) as well as after a reboot; verify a preference test and, on the phone, that the service is back in the foreground after an update
- [x] 17.18 `scripts/release.sh`: checks key, gh login, repo and tag, builds the signed direct APK + update.json into dist/, asks, publishes the GitHub release, then bumps gradle.properties; verify a dry run (signed by the release key, feed for build 2) and that publishing stops early when the repo is not visible
- [x] 17.19 Public-repo hygiene: neutral sample data (Acme, example.com), no device model or Mac tooling in docs and scripts, `.claude/` git-ignored, release key regenerated with a neutral certificate (`CN=ekQuickFind`), README search screenshots retaken from the app window over a neutral backdrop (no status bar, overlays, personal apps, photos or locale)
- [x] 17.20 Neutral open-source repo: `play` flavor renamed `store`, no store or trial mentions, Apache 2.0 `LICENSE` with an ML Kit terms note, one `ekquickfind.githubRepo` setting drives the About link, update feed and release script, clean README images without metadata
- [x] 17.21 Two-letter initials ("gm" finds Google Maps, G and M highlighted); single letters still match only word starts and mid-word matches still need three letters; verify matcher and apps provider tests
- [x] 17.22 Exact-match toggle in the search bar (quote-mark button, remembered): `Query.exact` matches whole words only (no partial words, initials, all-words or typo matching) in apps, settings, contacts, files and photo tags, and highlighting follows; verify matcher, apps provider and preference tests and a UI test for the toggle
- [x] 17.23 Search polish: Results settings (order, layout, icon size, density) moved onto Search sources with a new Text size (Small/Default/Large/Larger); Effects panel on Look and feel with Active item (Tinted card default, Soft glow, Gradient; even edges, fades between results), an Animations switch and Search bar animation (Light sweep default, Colour pulse) played once per open; Reduce motion turns animations off; verify preference, style and glow tests and settings UI tests
- [x] 17.24 Active item fix and more effects: the top result's card (and the selected slot) carries the highlight instead of clipping it, the Tinted card edge is visible and glows fit the list padding; three more search bar animations (Shimmer ring, Breathe, Twin lights) chosen from chips, with a live bar and active-item preview in Effects; About and Updates merged into "About app & updates"; verify glow tests and settings UI tests
- [x] 17.25 Result font (Default, Serif, Mono, Condensed) next to Text size on Search sources, applied to the results panel and recent strip only; verify preference, style and typography tests and a settings UI test
- [x] 17.26 Top result: Featured (big card, default) or In its section (stays in its section, selected, highlighted, plays the bar animation once when it becomes active); in the Effects panel with the preview; verify regrouping, preference and style tests and settings/search UI tests
- [x] 17.27 Look and feel regrouped: Text panel (text size, font, new weight, sample results), Search bar panel (opening animation switch, type and its preview under it), Active item panel (top result, selected result by the bar, style, separate item animation switch with Settle, Lift and the border glows, and a layout-following preview); verify preference, style, typography and highlight tests and settings UI tests
- [x] 17.28 Fonts that work with system font styles: Lora, JetBrains Mono, Roboto Condensed and Nunito bundled as variable fonts (SIL OFL, licences in assets/licenses/fonts), picked from a list shown in each font; text size is a slider (80–160 % in 10 % steps); previews get a "Preview" label over a coloured backdrop and take no taps; verify preference, style tests and settings UI tests
- [x] 17.29 "Reset all settings" on the About page with a confirmation: every setting back to default, keeping history, pins, hidden results and excluded apps; photo tags off clears their index; verify the reset preference test and a settings UI test
- [x] 17.30 New defaults: top result In its section, search bar animation Twin lights, floating button on, Settings section as chips, Google as search engine (selected result by the bar stays off); "Exact match button" switch in the Search bar panel (hiding it turns exact match off); "About app & updates" renamed "App"; UI tests start from a fixed baseline (`testBaseline`) so defaults can change without rewriting them; verify preference and style tests
- [x] 17.31 "Recent items shown" number field (1–20, default 5) in the Search bar panel, limiting recents in the zero-query list and the strip; verify preference and zero-query tests and a settings UI test
- [x] 17.32 Smart suggestions: launches keep the connected audio device (Room 4→5), `SmartSuggestions` ranks by time of day, kind of day and device (last 30 days), Suggested row puts them before usage apps; switch in Before you type; verify scorer, history and zero-query tests
- [x] 17.33 Calendar search (`CalendarProvider`, READ_CALENDAR, new Calendar section after Contacts) and Next up before typing; verify provider tests
- [x] 17.34 Clipboard chip: shown only when text was copied (description only, no "pasted" message), read on tap, with Search, Open link, Call and Share; nothing kept; verify clip-kind tests and a settings UI test
- [x] 17.35 Clipboard fixes: the chip is re-checked once the window has focus (Android denied the early check, so it only appeared after visiting settings); clips opened with the chip are remembered (last 10, 7 days, no-backup folder, never sensitive) and shown as swipeable pages with dots and a Forget action; Clear history clears them; verify clip history tests
- [x] 17.36 An open clipboard pane picks up new clips: it reads again when search gets focus back and when the clip changes while search is in front, and jumps to the newest; UI test for a clip copied after opening
