# Spec Delta

## Purpose

Finds installed applications and their shortcuts, including work-profile apps, and keeps that index current as packages change.

## ADDED Requirements

### Requirement: All launchable apps are searchable
The provider SHALL index every app that has a launcher activity in the personal profile and any managed (work) profile, by label and package name.

#### Scenario: Work profile app
- **WHEN** a work profile is present and the user types the name of a work-only app
- **THEN** the app appears with a work badge and launches into the work profile

#### Scenario: Search by package name
- **WHEN** the user types "com.android.chrome"
- **THEN** Chrome appears as a match

### Requirement: Index stays current
The provider SHALL reflect installs, uninstalls, updates and label changes without the user restarting the app.

#### Scenario: App installed while search is open
- **WHEN** an app finishes installing while the search surface is in the background and the user returns and types its name
- **THEN** the new app appears in results

### Requirement: App shortcuts are searchable
The provider SHALL match static and dynamic app shortcuts by their labels and launch them directly.

#### Scenario: Shortcut match
- **WHEN** the user types "new tab" and Chrome publishes a "New tab" shortcut
- **THEN** a result "New tab · Chrome" appears and tapping it opens that shortcut

### Requirement: Index is immediate on first keystroke
The provider SHALL answer from an in-memory index so that the first keystroke after launch produces app results within one frame once the index is loaded, and SHALL load the index in the background at launch.

#### Scenario: Index not yet loaded
- **WHEN** the user types before the index finishes loading on cold start
- **THEN** the Apps section shows a loading indicator and fills in as soon as the index is ready, within 300 ms of launch

### Requirement: Hidden apps are respected
The provider SHALL let the user hide individual apps from results, and hidden apps SHALL never appear except in the settings list used to unhide them.

#### Scenario: Hide an app
- **WHEN** the user chooses "Hide" from an app result's long-press menu
- **THEN** the app disappears from current results and future searches until unhidden in settings

### Requirement: Apps can be excluded from settings
Settings SHALL offer an "Excluded apps" list of every launchable app (personal and work profile) with a switch per app and a filter field. Excluded apps SHALL not appear in search results, and the list SHALL stay in sync with apps hidden from a result's long-press menu.

#### Scenario: Exclude from the list
- **WHEN** the user turns on exclusion for Calculator in Excluded apps and then searches "calculator"
- **THEN** no Calculator app result appears

#### Scenario: In sync with Hide
- **WHEN** the user hides an app from its long-press menu
- **THEN** it shows as excluded in the Excluded apps list, and turning the switch off brings it back
