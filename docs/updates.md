# Publishing a direct-download update

The `direct` flavor updates itself from GitHub Releases. The `store` flavor never does; a store that ships it updates it.

## How it works

1. Once a day, on Wi-Fi with enough battery, the app downloads `update.json` from the latest release of the repository set in `gradle.properties` (`ekquickfind.githubRepo`), at `https://github.com/<owner>/<repo>/releases/latest/download/update.json`. `ekquickfind.updateFeedUrl` can point elsewhere, or be `off`.
2. If its `versionCode` is higher than the installed one, it downloads the APK named in the feed and checks its SHA-256.
3. It installs the APK with Android's package installer. The first time, Android asks to allow ekQuickFind to install apps. After that, updates usually install without a prompt; when Android does want a confirmation, a notification (or the dialog, if the app is open) asks for one tap.

Android only accepts the update if it's signed with the same key as the installed app, so a tampered APK can't be installed even if the feed were changed.

## Releasing a new version

One command builds, checks and publishes:

```bash
scripts/release.sh 0.2.0 -n "What's new in this version"
```

It:
1. Checks that `keystore.properties` exists (release key), `gh` is logged in, the repo is visible and the tag is new.
2. Builds the signed direct APK and its `update.json`, with the build number one above the one in `gradle.properties`.
3. Copies them to `dist/v0.2.0/`, shows who signed the APK, and asks before publishing.
4. Creates the GitHub release `v0.2.0` with the APK and `update.json` attached.
5. Only after publishing, writes the new version and build number into `gradle.properties`.

Options: `--dry-run` builds without publishing or changing anything, `--draft` creates a draft release (phones don't see it until you publish it on GitHub), `--code N` picks the build number, `-y` skips the question.

The first release, 0.1.0 (build 1), is already built in `dist/v0.1.0/`. Publish it with `scripts/release.sh 0.1.0 --code 1`, which rebuilds it from the current code.

The newest release is what `releases/latest/download/...` points to, so phones pick it up on their next daily check. The repository (or at least its releases) must be public; private release files need a login the app doesn't have. The `gh` account must be able to see the repository (`gh auth login` to switch).

## Keep the signing key safe

The key in `~/.ekquickfind/release.jks` and the passwords in `keystore.properties` are the only way to ship updates to existing installs. Back up both somewhere safe (a password manager works). If the key is lost, installed copies can never be updated again; users would have to uninstall and reinstall.

## Testing an update locally

```bash
# 1. Install version 1, pointed at a feed on your computer
./gradlew :app:installDirectDebug -Pekquickfind.updateFeedUrl=http://localhost:8765/update.json
# 2. Build version 2 and its feed
./gradlew :app:updateFeedDirectDebug -Pekquickfind.updateFeedUrl=http://localhost:8765/update.json -Pekquickfind.versionCode=2 -Pekquickfind.versionName=0.2.0
# 3. Serve it and let the phone reach it over USB
(cd app/build/outputs/update/directDebug && python3 -m http.server 8765 --bind 127.0.0.1) &
adb reverse tcp:8765 tcp:8765
```

Then open ekQuickFind settings, *Updates > Check now*. Debug builds allow plain `http` to `localhost` only; release builds require https.
