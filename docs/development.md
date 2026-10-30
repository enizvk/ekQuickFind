# Development

A fast, private, Spotlight-style search for Android 13+ (API 33, built against API 37). Planning artifacts live in `openspec/changes/create-ekquickfind-app/` (proposal, specs, design, tasks).

## Modules

| Module | Kind | What lives here |
| --- | --- | --- |
| `:app` | Android app | Compose UI, entry points (activity, widget, tile, assistant), self-update, Hilt wiring, theme |
| `:core:search` | Pure Kotlin/JVM | Provider contract, result model, engine, matcher, ranker, calculator, unit converter |
| `:providers` | Android library | Apps, contacts, device settings, files/media and web providers |
| `:data` | Android library | Room history (launches, pins) and DataStore preferences |
| `:benchmark` | Android test | Macrobenchmark startup/keystroke benchmarks, baseline profile generator, release smoke tests |

`:core:search` has no Android dependencies on purpose. Most logic is tested there in milliseconds.

## Build flavors

| Flavor | For | Updates |
| --- | --- | --- |
| `store` | App stores that update apps themselves | Through the store. No update feed and no install permission. |
| `direct` | GitHub Releases / sideloading | Checks `ekquickfind.updateFeedUrl` daily and installs newer versions itself. See [updates.md](updates.md). |

## Requirements

- JDK 21, with `JAVA_HOME` pointing at it
- Android SDK with `platforms;android-37.0` and `build-tools;37.0.0`
- An Android 17 device or emulator for instrumented tests
- For signed release builds: `keystore.properties` in the project root (not committed):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=ekquickfind
keyPassword=...
```

Without it, release builds fall back to the debug key so they still install locally.

## Common commands

```bash
./gradlew :app:assembleDirectDebug            # build
./gradlew :app:installDirectDebug             # install on the connected device
./gradlew :app:bundleStoreRelease             # app bundle (AAB) for a store
./gradlew :core:search:test                   # engine unit tests (JUnit Jupiter)
./gradlew testDebugUnitTest :app:testStoreDebugUnitTest :app:testDirectDebugUnitTest
# Instrumented tests: install and run by hand. Gradle's connected tasks uninstall the app afterwards.
./gradlew :app:installDirectDebug :app:installDirectDebugAndroidTest
adb shell am instrument -w com.ekquickfind.app.test/com.ekquickfind.app.HiltTestRunner
./gradlew lintDebug :app:lintStoreDebug :app:lintDirectDebug ktlintCheck
./gradlew ktlintFormat                        # auto-fix formatting
scripts/release.sh 0.2.0 -n "What's new"      # build, sign and publish a GitHub release (see updates.md)
```

Landing-page screenshots of every settings page (the app's own window, no status bar or other apps' overlays):

```bash
adb shell am instrument -w -e class com.ekquickfind.app.screenshots.SettingsScreenshotsTest -e screenshots true \
  com.ekquickfind.app.test/com.ekquickfind.app.HiltTestRunner
adb pull /sdcard/Android/data/com.ekquickfind.app/files/screenshots
```

See [performance.md](performance.md) for benchmarks and [acceptance.md](acceptance.md) for the spec checklist.
