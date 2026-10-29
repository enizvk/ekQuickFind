# Proposal

## Why

Samsung Finder (the search bar built into One UI's app drawer) is the fastest way to reach anything on a Galaxy phone, but it is Samsung-only, visually dated, and often lags on first keystroke. Pixel and other Android 17 devices have no equivalent of iOS Spotlight. ekQuickFind brings a single, instant, privacy-respecting search surface for apps, contacts, settings, files, quick answers and the web to any Android 17+ device, with a smoother Material 3 Expressive UI than Samsung ships.

## What Changes

- New Android app (Kotlin, Jetpack Compose, Material 3 Expressive) targeting **minSdk 37 / targetSdk 37 (Android 17)**. No lower API levels are supported, by design.
- A bottom-anchored search surface that opens as a translucent overlay in under 400 ms, shows results progressively per keystroke, and dismisses with predictive back or swipe-down.
- Multiple ways to open search, since a third-party app cannot inject into the launcher drawer: app icon, home-screen search-bar widget, Quick Settings tile, and the system assistant role (long-press power / corner swipe) for a Spotlight-like gesture.
- Pluggable search providers behind one `SearchProvider` contract, each with its own ranking signals and permission gating:
  - Installed apps (incl. work profile), with usage-based ranking and app shortcuts.
  - Contacts, with call / message / open actions.
  - Device settings, via a curated keyword index of Settings screens.
  - Files and media from MediaStore (images, video, audio, documents, downloads).
  - Quick answers: calculator and unit conversion, evaluated on device.
  - Web suggestions and "search the web" fallback through a user-selectable search engine (privacy-respecting default).
- Search history and learned ranking: launch counts per result (recently opened items), and user-pinned results, all stored locally. One-tap clear.
- Zero-query state: pinned items, recently opened items and suggested apps, so the overlay is useful before typing.
- Results grouped by type on one screen, shown by default in a compact grid of small icons (switchable to list rows), and every result shows the icon of the app that opens it.
- Personalisation: selectable background behind the surface (Blur, Frosted glass, Solid colour), icon size, list density, results per section, top or bottom placement, keyboard behaviour (auto-open, Enter runs the selected result, arrow-key selection), haptics and reduced motion.
- Privacy by default: no analytics, no network except opt-out-able web suggestions, every provider individually toggleable, permissions requested lazily per provider.

Out of scope for this change (candidate follow-up changes): on-device semantic / natural-language search (Gemini Nano via ML Kit GenAI), SMS and calendar providers, notes, Samsung-style "recall" cards from notifications, cloud sync.

## Capabilities

### New Capabilities
- `search-entry`: how search is opened and closed (app icon, widget, Quick Settings tile, assistant role, shortcuts), overlay behavior, keyboard handling, predictive back.
- `unified-search`: query input, provider orchestration, progressive result delivery, cross-provider ranking and sectioning, result actions, empty and zero-query states.
- `search-providers/apps`: indexing and ranking installed apps and shortcuts, including work profile and package change tracking.
- `search-providers/contacts`: contact matching and actions, gated on the contacts permission.
- `search-providers/device-settings`: curated keyword index of Settings screens and how they are launched.
- `search-providers/files-media`: MediaStore-based search of images, video, audio, documents and downloads, gated on media permissions.
- `search-providers/quick-answers`: calculator and unit-conversion answers computed locally.
- `search-providers/web`: web search suggestions and web-search fallback, search engine selection, network opt-out.
- `search-history`: recently opened results, launch frequency, pinned results, and clearing of all learned data.
- `search-settings`: provider toggles, search engine choice, appearance and behaviour personalisation (background style, icon size, density, placement, keyboard, haptics, motion), and privacy controls.

### Modified Capabilities
<!-- None: greenfield project with no existing specs. -->

## Impact

- New Gradle project under the repository root: `:app`, `:core:search` (pure JVM engine, no Android deps), `:providers` (Android library), `:data` (Room history). Kotlin 2.x, AGP 9.x, version catalog.
- Runtime permissions used: `READ_CONTACTS`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `QUERY_ALL_PACKAGES` (or `<queries>` declarations), `PACKAGE_USAGE_STATS` (optional, for usage ranking), `INTERNET` (web suggestions only).
- System integrations: `VoiceInteractionService` for the assistant role, `TileService`, Glance `AppWidget`, `ACTION_ASSIST` / `ACTION_WEB_SEARCH` intent filters.
- External dependencies: Compose BOM, Material 3 (1.4+ for Expressive), Hilt, Room, Coil (icons), OkHttp + kotlinx.serialization (suggestions), Glance, Turbine/JUnit 5 for tests.
- No backend, no accounts, no analytics SDK.
