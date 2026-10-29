# Performance

Measured on a recent physical Android 17 phone, 2026-10-28, with the
minified `benchmarkRelease` build and Macrobenchmark 1.5.0 (`:benchmark` module). Clocks are not
locked on this retail device, so treat numbers as indicative.

| Journey | Metric | Min | Median | Max |
| --- | --- | --- | --- | --- |
| Cold start, no profile (first run) | time to initial display | 343 ms | 364 ms | 445 ms |
| Cold start, no profile (re-run) | time to initial display | 303 ms | 311 ms | 357 ms |
| Cold start, baseline profile | time to initial display | 239 ms | 256 ms | 296 ms |
| Cold start, no profile (third run, same session as next row) | time to initial display | | 341 ms | |
| Cold start, baseline profile (third run) | time to initial display | | 300 ms | |
| Typing "camera", baseline profile | keystroke to first results (`ek.keystrokeToResults`) | 2.2 ms | 2.8 ms | 3.9 ms |
| Typing "camera", baseline profile | frame overrun p50 | | -3.0 ms | |

The baseline profile cuts cold start by 12 to 18% against the same-session no-profile run; run-to-run spread on this unlocked device is about 10 to 20%. The spec target
(search visible and accepting input within 400 ms) is met with and without the profile.

## Reproduce

Gradle's connected tasks uninstall the app afterwards, so install and run by hand:

```bash
./gradlew :app:installStoreBenchmarkRelease :benchmark:installBenchmarkRelease
adb shell am instrument -w \
  -e class com.ekquickfind.benchmark.StartupBenchmark \
  -e androidx.benchmark.suppressErrors UNLOCKED,LOW-BATTERY \
  com.ekquickfind.benchmark/androidx.test.runner.AndroidJUnitRunner
```

Regenerate the baseline profile:

```bash
./gradlew :app:installStoreNonMinifiedRelease :benchmark:installNonMinifiedRelease
adb shell am instrument -w -e class com.ekquickfind.benchmark.BaselineProfileGenerator \
  -e additionalTestOutputDir /sdcard/Android/media/com.ekquickfind.benchmark/additional_test_output \
  com.ekquickfind.benchmark/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/media/com.ekquickfind.benchmark/additional_test_output/BaselineProfileGenerator_generate-startup-prof.txt \
  app/src/release/generated/baselineProfiles/baseline-prof.txt
```

## CI regression gate

`.github/workflows/ci.yml` runs the startup benchmark on an Android 17 emulator and fails when the median regresses by more than 15% against `benchmark/startup-baseline-ci.json` (recorded on the first CI run, since emulator timings differ from phones). `scripts/check_startup_regression.py` does the comparison; `benchmark/startup-baseline.json` holds the phone numbers above.
