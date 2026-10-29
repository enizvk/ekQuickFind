# Acceptance checklist

Run on 2026-10-28 on a physical Android 17 phone (no
Android 17 emulator run yet). "Device" means an instrumented test or a manual check on that device;
"JVM" means a unit or Robolectric test. Totals at sign-off: 301 JVM tests, 53 app UI tests and 5
provider device tests on the phone, 3 UI Automator smoke tests against the minified build. All green.

| Spec | Scenario | Result | Evidence |
| --- | --- | --- | --- |
| search-entry | Cold launch from icon | Pass | `docs/performance.md`: cold start median 256–364 ms (target 400 ms); `SearchActivityTest` focus and keyboard |
| search-entry | Warm launch keeps nothing stale | Pass | Device manual: typed "maps", left, reopened with empty query |
| search-entry | Widget tap | Partial | `SearchWidgetTest` asserts the click intent; tapping a placed widget needs the user to add it to the home screen |
| search-entry | Widget follows system theme | Pass | `SearchWidgetTest` light, dark and dynamic renders; widgets refresh on theme change |
| search-entry | Tile tap over another app | Pass | `SearchTileTest` (shell add-tile, click-tile) |
| search-entry | Tile tap on locked device | Partial | Device manual: locked tap asks to unlock and doesn't open search; opening after unlock needs the owner's PIN |
| search-entry | Assistant gesture | Pass | Device manual: temporary assistant role, KEYCODE_ASSIST opened search; role restored to None |
| search-entry | Not the default assistant | Pass | Settings "Ways to open" shows status and opens the picker (`EntryPointsTest`) |
| search-entry | Incoming web search intent | Pass | `IncomingIntentTest`, and `am start -a WEB_SEARCH -e query cats` |
| search-entry | Predictive back preview | Pass | `DismissTest` dispatches back progress; screenshot reviewed (phone uses 3-button nav) |
| search-entry | Tap outside | Pass | `DismissTest` |
| search-entry | Keyboard dismissed by scrolling | Pass | `DismissTest` (real swipe) |
| search-entry | Closing animates | Pass | `DismissTest.theSurfaceAnimatesOutBeforeTheWindowCloses` |
| search-entry | Icons fade in | Pass | Coil cross-fade 150 ms (code review) |
| search-entry | Settings shortcut / pinned shortcut / unpinning | Pass | `LauncherShortcutsTest` |
| unified-search | Fast provider shows first; stale results discarded | Pass | `SearchEngineTest` |
| unified-search | Section cap; mixed query; empty sections hidden | Pass | `SearchEngineTest`; device screenshots ("camera": Apps, Files, Web together) |
| unified-search | Keyboard action opens top result | Pass | `SearchActivityTest` ("cam" → Camera) |
| unified-search | No top result falls back to web | Pass | `WebFallbackTest` |
| unified-search | Short queries; initials; typo | Pass | `QueryMatcherTest` |
| unified-search | Previously launched app ranks higher | Pass | `HistoryRepositoryTest` through the real engine |
| unified-search | Long-press on an app | Pass | `ResultMenuTest`; grid tile long-press checked manually |
| unified-search | Fresh install | Pass | `ZeroQueryTest.freshInstallShowsTheHint` |
| unified-search | Provider timeout | Pass | `SearchEngineTest` |
| unified-search | Settings / file / web result shows its app | Pass | `HandlerIconsTest`; device screenshots |
| unified-search | Highlighting (prefix, initials, completion, accents) | Pass | `HighlightTest`; device screenshots ("notif", "sound") |
| search-providers/apps | Work profile app; package name | Pass | `LauncherAppsSourceDeviceTest` (real work profile), `AppsProviderTest` |
| search-providers/apps | App installed while search is open | Pass | `AppIndexTest` (JVM) |
| search-providers/apps | Shortcut match | Pass (JVM) | `AppsProviderTest`; on the phone shortcut search is hidden because only the default launcher may read shortcuts |
| search-providers/apps | Index not yet loaded | Pass | `AppIndexCacheTest`; loading indicator screenshot |
| search-providers/apps | Hide an app; exclude from list; in sync | Pass | `ResultMenuTest`, `ExcludedAppsUiTest`, `ExcludedAppsTest` |
| search-providers/contacts | Permission not granted / denied permanently | Pass | `ContactsPermissionTest` (dialog or app settings) |
| search-providers/contacts | Partial phone; organisation; actions; chooser; starred; often opened | Pass | `ContactsProviderTest`, `ContentResolverContactsSourceTest`, `ContactsSourceDeviceTest` (real provider) |
| search-providers/device-settings | Keywords; synonyms; unavailable screen | Pass | `DeviceSettingsProviderTest`, `SettingsDeviceTest` (7 One UI gaps fall back) |
| search-providers/device-settings | Bluetooth toggle; flashlight | Pass | `DeviceSettingsProviderTest`, `SettingsDeviceTest`, `InlineSwitchTest` |
| search-providers/device-settings | Bluetooth through the system dialog; system refuses | Pass | `DeviceSettingsProviderTest` (permission, allow, deny, refusal); device manual: Nearby devices prompt, then "turn on" and "turn off" dialogs over search, state flipped in place both ways |
| search-providers/device-settings | Flip in place; access needed first; system-only | Pass | `DeviceSettingsProviderTest`, `AndroidToggleStatesTest`, `InlineSwitchTest`; Wi‑Fi panel checked manually |
| search-providers/files-media | Partial / no permission; extension; thumbnail; recency; no handler | Pass | `FilesMediaProviderTest`, `MediaStoreSourceTest`, `FileResultsTest` |
| search-providers/quick-answers | Expressions, percentage, invalid, conversions, same tick | Pass | `CalculatorTest`, `UnitConverterTest`, `QuickAnswersProviderTest`, `ReleaseSmokeTest` |
| search-providers/web | Open web search; suggestions; offline; custom; off | Pass | `WebProviderTest`, `SuggestEngineTest`, `WebFallbackTest` |
| search-history | Recent; query recorded; pin/unpin; clear all | Pass | `HistoryDaoTest`, `HistoryRepositoryTest`, `ZeroQueryTest` |
| search-history | Default backup | Partial | `BackupPolicyTest` checks the rules and opt-in logic; a real `bmgr` backup wasn't run (it would change the phone's backup transport or upload to the owner's account) |
| search-settings | Provider toggle; permission status; force dark; privacy | Pass | `SettingsFlowTest` |
| search-settings | Add widget prompt | Not run | Button calls `requestPinAppWidget`; not tapped on the phone to avoid changing the home screen |
| search-settings | Background styles, dim, blur unavailable | Pass | `SettingsFlowTest`, `ThemeScreenshotTest`; this phone has system blur off (`pms_settings_blur_enabled=0`), fallback confirmed |
| search-settings | Icon size, results per section, position, keyboard, haptics, motion | Pass | `SettingsFlowTest` (haptics by code review) |
| search-settings | Grid, order, per-section layout, recents strip | Pass | `SettingsFlowTest`, `SurfaceStyleTest`, `PersonalisationPreferencesTest` |

## Follow-ups

- Run this checklist on an Android 17 emulator as well (task 14.1 asks for both).
- Place the widget on a home screen and tap it; tap "Add to home screen" in settings.
- Finish the locked-tile flow with a real unlock.
- Confirm on a test device that a real backup leaves out `ekquickfind.db` (task 10.5).
- One-week dogfood (task 14.2).
