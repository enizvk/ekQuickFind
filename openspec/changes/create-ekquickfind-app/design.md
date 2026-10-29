# Design

## Context

Greenfield repository: no code exists yet, only the OpenSpec scaffold. The target is Android 17 (API 37) and newer only, which removes every compatibility shim and lets us assume: 16 KB pages, predictive back on by default, Material 3 dynamic colour, Photo Picker and partial media access, foreground service restrictions, and a Compose-first toolchain. See proposal.md for motivation and the `specs/` folder for behaviour.

Constraints that shape the design:
- A third-party app cannot replace the launcher's search bar. Entry must go through widget, tile, assistant role and icon (see `specs/search-entry`).
- "Smoother than Samsung Finder" is a latency budget, not a feeling: first keystroke to app results in one frame, cold start to interactive under 400 ms, no list jank while sections arrive asynchronously.
- The developer is primarily a C#/.NET engineer working solo; favour a small number of modules and conventional Android tooling over clever architecture.
- TDD is the default workflow, so the search engine and all provider logic must be testable on the JVM without an emulator.

## Goals / Non-Goals

**Goals:**
- One `SearchProvider` contract that every data source implements, so adding a provider never touches the engine or the UI.
- Engine, ranking, query parsing, calculator and unit conversion live in a pure-Kotlin JVM module with zero Android dependencies and sub-second unit tests.
- Progressive, cancellation-safe result delivery using structured concurrency; stale results can never reach the screen.
- Hilt-based composition root so each provider's Android dependencies (ContentResolver, LauncherApps, PackageManager) are injected and replaceable with fakes.
- Measurable performance: Macrobenchmark startup test and a baseline profile from day one.

**Non-Goals:**
- Supporting any API level below 37.
- Semantic/LLM search, SMS/calendar/notes providers, notification-derived cards (future changes).
- A backend, accounts, sync or analytics.
- Replacing the launcher or acting as a launcher.

## Decisions

### D1. Kotlin + Jetpack Compose + Material 3 Expressive, not Flutter/.NET MAUI/Views
Native Kotlin and Compose are the only stack that gets first-class access to predictive back, shared element transitions, Glance widgets, TileService and VoiceInteractionService without bridging. Material 3 1.4+ ships the Expressive motion scheme and new components (loading indicator, button groups, floating toolbars) that make the "more modern than One UI" goal concrete. MAUI was considered because of the author's .NET background, but it lags on Android system integrations and Compose-level animation control. Views/XML rejected: Compose is the Android 17 default and shared-element and predictive-back APIs are Compose-first.

### D2. Four Gradle modules
```
:app            Android application. Compose UI, entry points (activity, widget, tile,
                assistant service), Hilt wiring, theme, navigation.
:core:search    Pure Kotlin/JVM. SearchProvider contract, SearchResult model, SearchEngine
                (orchestration + merge), Ranker, QueryMatcher (fuzzy), calculator + unit
                conversion. No Android imports. JUnit 5 + Turbine + kotlinx-coroutines-test.
:providers      Android library. Apps, Contacts, DeviceSettings, FilesMedia, Web providers
                and their Android data sources behind small interfaces.
:data           Android library. Room database for history/pins/launch counts, DataStore
                for settings. Exposes repositories as Flows.
```
Alternatives: single module (fastest to start, but drags Android into engine tests and slows Compose builds as it grows); one module per provider (clean, but eight modules for one developer is ceremony). Four modules is the smallest split that keeps the engine Android-free and keeps Compose recompilation out of provider edits.

### D3. Provider contract and result model
```kotlin
interface SearchProvider {
    val id: ProviderId
    val section: Section                        // Apps, Contacts, Settings, Files, QuickAnswers, Web
    val requirements: Set<Requirement>          // permissions / network, checked by engine
    fun search(query: Query): Flow<List<SearchResult>>   // may emit more than once
}

data class SearchResult(
    val id: ResultId,            // stable across runs, used for history + pins
    val providerId: ProviderId,
    val title: String,
    val subtitle: String?,
    val icon: ResultIcon,        // sealed: PackageIcon, ContentUri, Vector, Thumbnail
    val score: Float,            // provider-local 0..1 text match quality
    val primaryAction: Action,
    val secondaryActions: List<Action>,
    val payload: Any? = null,
)
```
`search` returns a `Flow` so a provider can emit a cheap first page then enrich (for example, file names first, thumbnails resolved lazily by the UI via Coil). The engine, not the provider, applies timeouts, history boosts and section caps. Providers never touch UI or history.

### D4. Engine pipeline
```
queryFlow (TextField) ─► normalise ─► mapLatest { q ->
    channelFlow {
        enabledProviders.forEach { p ->
            launch { p.search(q).withTimeoutOrNull(p.budget).collect { send(p.id to it) } }
        }
    }
    .scan(emptyResultSet) { acc, (id, items) -> acc.replaceSection(id, items) }
    .map { ranker.rank(it, historyBoosts) }
}
```
- `flatMapLatest` (shown as `mapLatest` above) cancels the whole provider fan-out on each keystroke, which is what makes stale results impossible by construction.
- Per-provider debounce lives in the provider (`Web` waits 250 ms, `FilesMedia` 120 ms, `Apps`/`QuickAnswers` none) so the engine stays generic.
- Per-provider budget: 700 ms default. When it expires the provider stops counting as pending so the surface settles; late results are still appended while the query is unchanged (per `specs/unified-search`), and a new keystroke cancels them.
- Ranking: `finalScore = textScore * sectionWeight + log1p(launchCount) * recencyDecay + pinBoost`. Pure function, table-driven tests.
- Fuzzy matching: own small implementation (prefix, initials, substring, Damerau-Levenshtein ≤ 1 for ≥ 4-char queries) over pre-normalised labels (lowercase, diacritics stripped). No external fuzzy library: the corpus is a few thousand short strings and we need deterministic ranking tie-breaks.

### D5. Apps index
`LauncherApps.getActivityList(null, userHandle)` for every profile from `UserManager.userProfiles`, held in memory as a `StateFlow<AppIndex>`. Refreshed by `LauncherApps.Callback` (package added/removed/changed, shortcuts changed), which also delivers work-profile events; no manifest broadcast receiver. Labels are normalised once on index build. Icons loaded through Coil with a custom `Fetcher` that calls `LauncherApps.getActivityList(...).getIcon(density)`, cached in memory keyed by component + user. `PACKAGE_USAGE_STATS` is optional: if granted, `UsageStatsManager` seeds "suggested apps" for the zero-query state; if not, that section is hidden. We declare `<queries><intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent></queries>` rather than `QUERY_ALL_PACKAGES`, which app stores reject for this use case.

### D6. Content-backed providers (Contacts, Files)
Each wraps `ContentResolver` behind a one-method interface (`ContactsSource.query(text): List<ContactRow>`), so provider logic is unit-tested with fakes and the Android query is covered by a thin Robolectric/instrumented test. Queries run on `Dispatchers.IO`, limited to 50 rows, selected columns only. Files use `MediaStore.Files` with `DISPLAY_NAME LIKE` plus per-type permission checks; thumbnails come from `ContentResolver.loadThumbnail` through a Coil fetcher. Partial photo access (`READ_MEDIA_VISUAL_USER_SELECTED`) is treated as "granted for selected" and surfaced as a row offering full access.

Files have a platform limit: since Android 13 the media permissions (`READ_MEDIA_IMAGES/VIDEO/AUDIO`) only expose photos, video and audio. Documents and downloads created by other apps are visible only with "All files access" (`MANAGE_EXTERNAL_STORAGE`). The provider therefore treats documents as a fourth permission tier with its own opt-in row that opens the system "All files access" page. Media search works without it.

### D7. Device settings index
A static Kotlin table (`SettingsEntry(title, breadcrumb, keywords, intentAction, extras, fallbackAction)`), roughly 80 entries, validated at startup by `PackageManager.resolveActivity`; unresolvable entries fall back to their parent action. Live toggles (Bluetooth, Wi‑Fi, DND, flashlight, rotation, battery saver) read state through the respective managers and open the system `Settings.Panel` intents, except flashlight which uses `CameraManager.setTorchMode` directly. The table is data, so adding a screen is a one-line change and the table has a test that every entry resolves on the emulator image.

### D8. Web provider
OkHttp + kotlinx.serialization; one `SuggestEngine` interface with implementations for DuckDuckGo (`/ac/?q=`), Google (`suggestqueries`), Bing, Brave and a custom template. Debounce 250 ms, 5 results, request cancelled on query change via coroutine cancellation. A `NetworkPolicy` object is the single place that knows whether suggestions are enabled; it is checked before any call so "suggestions off means zero network" is enforceable and testable. DuckDuckGo default is a privacy decision; it costs nothing to switch.

### D9. History and settings storage
Room with two history tables: `launch_event(result_id, provider_id, at)` and `pin(result_id, order)`. Typed queries are not stored (a `recent_query` table existed until schema v3, which drops it). Boosts are computed from events at query time (cheap, few thousand rows max; events older than 90 days pruned on launch). Preferences (provider toggles, engine, theme, hidden apps) in Proto DataStore. `android:allowBackup="true"` with a backup rule excluding the Room database by default; a settings switch flips the rule via `BackupAgent` include list. Alternative considered: SQLDelight (fine, but Room's Compose/Flow integration and Hilt support are the path of least surprise on Android).

### D10. Entry points
- **Activity**: single `SearchActivity`, theme `Theme.Translucent` with `windowIsTranslucent`, launch mode `singleInstance` so every entry point reuses one surface. `enableEdgeToEdge()`; keyboard shown via `WindowInsetsControllerCompat` in `onResume`; `WindowInsets.ime` drives the bottom-anchored layout so the field tracks the keyboard animation.
- **Widget**: Glance `GlanceAppWidget` rendering a pill search bar with dynamic colour; tap sends an explicit intent to `SearchActivity` with `ActivityOptions.makeScaleUpAnimation` for the "grows from widget" feel.
- **Tile**: `TileService` with `startActivityAndCollapse(PendingIntent)`; lockscreen handled by `unlockAndRun`.
- **Assistant**: `VoiceInteractionService` + `VoiceInteractionSessionService` whose session immediately starts `SearchActivity` with `ACTION_ASSIST` extras and finishes. Manifest `<intent-filter>` for `android.intent.action.ASSIST`, `android.intent.action.SEARCH`, `android.intent.action.WEB_SEARCH`.
- Settings exposes `Settings.ACTION_VOICE_INPUT_SETTINGS`, `AppWidgetManager.requestPinAppWidget`, and `StatusBarManager.requestAddTileService`.

### D11. UI and motion
Single Compose screen: `SearchSurface` = dim scrim + bottom sheet-like container holding `ResultList` (LazyColumn with `animateItem`, section headers sticky) above `QueryField`. Material 3 Expressive `MotionScheme.expressive()` for spring-based transitions, `PredictiveBackHandler` driving the shrink-to-edge preview, `LoadingIndicator` for the pending Apps section on cold start, haptics via `HapticFeedbackConstants.CONFIRM` on top-result launch. Settings is a second destination in Navigation Compose with shared-element transition of the top app bar. Baseline profile generated with Macrobenchmark covers: launch, type three characters, launch top result.

### D12. Testing strategy
- `:core:search`: JUnit 5, parameterised tests for `QueryMatcher`, `Ranker`, `Calculator`, `UnitConverter`; Turbine for `SearchEngine` cancellation and ordering.
- `:providers`: provider logic with fake sources on the JVM; one Robolectric test per `ContentResolver` wrapper; settings table resolution test on an API 37 emulator.
- `:app`: Compose UI tests for the surface (typing, section appearance, long-press menu, predictive back), screenshot tests via Compose Preview Screenshot Testing for light/dark/dynamic.
- Macrobenchmark module `:benchmark` for startup and keystroke-to-frame timing; CI fails if P50 startup regresses by more than 15 percent.

### D13. Personalisation (background, sizing, keyboard behaviour)
All options live in `UserPreferences` and flow into the UI as one immutable `SurfaceStyle` value read through a `CompositionLocal`, so composables never read DataStore directly and changes apply live.
- **Background**: the activity window is always translucent. Blur sets `WindowManager.LayoutParams.blurBehindRadius` plus `FLAG_BLUR_BEHIND` at runtime (not theme attributes) so the style can switch without recreating the activity. Frosted glass adds the same blur and renders panels with a translucent `surfaceContainer` tint and a thin light border. Solid colour clears the blur and paints a full-screen colour at the chosen alpha. A `CrossWindowBlurEnabledListener` swaps to a dim fallback while blur is unavailable (power saving, some OEM settings). Alternative considered: Compose `Modifier.blur` on a screenshot of the app below, rejected because third-party apps can't capture other apps' windows.
- **Selection model**: `selectedIndex` lives in the view model over a flattened list of visible rows; it resets to the top result on every new state, moves with hardware Up/Down, and Enter runs it only when the setting is on.
- **Sizing**: icon size and density map to a small token table (icon dp, row vertical padding). Section cap maps onto `EngineConfig.sectionCap`, so the engine stays the single owner of capping.
- **Motion**: reduce motion swaps `MotionScheme.expressive()` for `MotionScheme.standard()`, disables `animateItem` and replaces enter slides with fades.

### D14. Grid layout and handler icons
- **Grid**: sections declare a presentation (`Grid` or `Rows`) chosen in the UI from the section and the grid preference; the engine is unaware of layout. Grid sections are chunked into `LazyColumn` rows of N tiles (5 on phones, more on wide screens via window width) rather than nesting a `LazyVerticalGrid`, so the single list keeps sticky headers and one scroll position. Grid sections cap at two rows before "More".
- **Handler icons**: `SearchResult` gains an optional `handler` (package to show), filled by providers that know it (settings -> Settings package, contacts -> default contacts app, files -> `resolveActivity` for the MIME type, web -> default browser via `ACTION_VIEW http`). Resolution happens once per provider (cached), never per keystroke. The UI renders the handler icon as the main icon for vector rows and as a badge over thumbnails and avatars.

### D15. Match highlighting
`MatchTarget` keeps a normalised-to-raw index map built while normalising character by character, so ranges found on normalised text translate back exactly. `QueryMatcher.highlight(query, target)` follows the same match order as `score` and returns raw-text ranges. Providers attach no highlight data; the UI calls `highlight` with the current query when it renders a title, which keeps `SearchResult` unchanged and costs one cheap match per visible row.

### D16. Live engine configuration
`SearchEngine` takes `Flow<EngineConfig>` (section order, per-section caps, budget) next to the enabled-providers flow; a change re-runs the current query like a toggle does. Section order only affects how sections are listed; the top result stays the best match across all sections so the keyboard action is predictable.

## Risks / Trade-offs

- [Android 17-only cuts the addressable user base sharply in year one] → Accepted by the user's requirement. Record the minSdk in one place (version catalog) so lowering later is a single change, and avoid API 37-only calls where a 35-stable equivalent exists.
- [Assistant role competes with Gemini; users may not switch] → Widget and tile cover the common path; assistant is additive. Settings clearly shows how to set it.
- [`LauncherApps` label loading for hundreds of apps can take > 300 ms on cold start] → Build the index on a background dispatcher started from `Application.onCreate`, persist a label cache in Room to render instantly and refresh behind it.
- [MediaStore `LIKE` queries are slow on devices with 50k+ files] → Hard row limit, 120 ms debounce, 700 ms budget, and a future FTS index if measurements show it is needed.
- [Fuzzy matching produces surprising ranks] → Table-driven ranking tests with real-world fixtures; typo matching only for queries of 4+ characters.
- [Web suggestion endpoints are undocumented and may change] → Isolated behind `SuggestEngine`; failures are swallowed into "no Web section", never an error.
- [Store policies on `QUERY_ALL_PACKAGES`] → Not used; `<queries>` with launcher intent instead. Shortcut access requires the app to be a default launcher or hold the host role; if `LauncherApps.hasShortcutHostPermission()` is false, shortcut search is hidden rather than broken.
- [Android 13+ blocks apps from switching Bluetooth directly] → Use the system's REQUEST_ENABLE / REQUEST_DISABLE confirmation dialogs (one extra tap). REQUEST_DISABLE isn't public API; when the system finishes it instantly without a dialog, the switch falls back to Bluetooth settings.
- [One UI delivers Quick Settings tile added/removed callbacks late or not at all for some changes] → The tile also marks itself added whenever it starts listening; the settings status is best-known state, and tests drive the reported state instead of relying on SystemUI timing.
- [Cross-window blur is off on some devices or in power saving] → Live fallback to a dimmed solid background via the blur-enabled listener; the setting stays as chosen.
- [Translucent single-instance activity can look wrong over landscape or multi-window] → Test on tablet and freeform; the surface uses `WindowSizeClass` to switch to a centred dialog layout on expanded widths.

## Migration Plan

Greenfield: no migration. Rollout order inside this change: engine + tests, apps provider, UI surface, then remaining providers, then entry points, then history and settings. Each step leaves a runnable app.

## Open Questions

- Which exact Settings screens exist on the Android 17 emulator vs. Pixel vs. OEM builds; the table test will decide the final list.
- Whether launch-count boosts should decay over 30 or 90 days; tunable constant, decided after dogfooding.
- App display name and icon (placeholder "ekQuickFind").
