# Spec Delta

## Purpose

Keeps the direct-download version of ekQuickFind up to date without an app store, while the store version is updated only by the store that ships it.

## ADDED Requirements

### Requirement: Two distribution versions
The app SHALL be built in two versions: a store version that never updates itself and declares no install permission, and a direct-download version that updates itself from a release feed.

#### Scenario: Store version
- **WHEN** the store version is installed
- **THEN** it has no update feed, no "Install unknown apps" permission and no Updates panel in settings

### Requirement: Direct version checks for updates
The direct version SHALL read `update.json` (version code, version name, APK link, SHA-256, size, notes) from its feed URL, offer the release when its version code is higher than the installed one, and only accept APK links over https (plain http only to `localhost`, for testing).

#### Scenario: Newer version published
- **WHEN** the feed lists version code 2 and version 1 is installed
- **THEN** settings shows "Version 0.2.0 is available" with an Install button and the release notes

#### Scenario: Broken feed
- **WHEN** the feed can't be fetched or parsed
- **THEN** settings shows "Couldn't update" with the reason and a Try again button; nothing crashes

### Requirement: Updates are verified and installed by Android
The direct version SHALL download the APK, delete it if its SHA-256 doesn't match the feed, and install it with the system package installer, asking for confirmation only when Android requires it.

#### Scenario: Checksum mismatch
- **WHEN** the downloaded APK's SHA-256 differs from the feed
- **THEN** the file is deleted and the update fails with a checksum message

#### Scenario: Confirmation needed in the background
- **WHEN** a background update needs the user's confirmation and notifications are allowed
- **THEN** a notification "ekQuickFind update ready" opens the install confirmation

### Requirement: Automatic updates are optional
The direct version SHALL check once a day on an unmetered network with enough battery when "Update automatically" is on (default), and SHALL show in settings whether it may install apps and post notifications, with buttons to allow both.

#### Scenario: Auto-update off
- **WHEN** the user turns off "Update automatically"
- **THEN** the daily job checks nothing, and updates only happen from *Check now*
