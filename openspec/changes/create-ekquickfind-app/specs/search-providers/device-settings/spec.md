# Spec Delta

## Purpose

Lets users jump straight to a device Settings screen by typing what they want to change, using plain-language keywords rather than exact menu names.

## ADDED Requirements

### Requirement: Settings screens are matched by keywords
The provider SHALL maintain a curated index of Settings screens, each with a title, a path breadcrumb and multiple plain-language keywords, and match queries against all of them.

#### Scenario: Plain-language keyword
- **WHEN** the user types "hotspot"
- **THEN** "Hotspot & tethering · Network & internet" appears in the Settings section

#### Scenario: Synonym keyword
- **WHEN** the user types "dark mode"
- **THEN** the Display settings screen for theme appears even though the system calls it "Dark theme"

### Requirement: Settings results open the exact screen
Tapping a settings result SHALL open the specific Settings screen, and SHALL fall back to the nearest parent screen if the device does not expose that screen.

#### Scenario: Screen unavailable on device
- **WHEN** the device lacks the targeted screen (for example no SIM, no mobile data screen)
- **THEN** the parent "Network & internet" screen opens instead of a crash or no-op

### Requirement: Common toggles can be read inline
For a curated set of toggles (Wi‑Fi, Bluetooth, Flashlight, Do Not Disturb, Rotation lock, Battery saver) the result SHALL show the current state and offer the relevant system panel or toggle action directly.

#### Scenario: Bluetooth quick toggle
- **WHEN** the user types "bluetooth"
- **THEN** the result shows whether Bluetooth is on and tapping its action opens the system Bluetooth panel

#### Scenario: Flashlight
- **WHEN** the user types "torch" or "flashlight" on a device with a camera flash
- **THEN** the result's action toggles the flashlight without leaving search

### Requirement: Single toggles can be switched from the result
Results for single on/off settings SHALL show a switch reflecting the current state. Tapping the switch SHALL change the setting in place when Android allows apps to (flashlight; Do Not Disturb and auto-rotate once the user grants the needed special access). For Bluetooth it SHALL ask the system to switch it through the system's small confirmation dialog over the search surface (after the Nearby devices permission is granted once), falling back to Bluetooth settings only when the system refuses. Wi‑Fi opens the system Wi‑Fi panel and battery saver its settings page, because Android reserves those changes for the system.

#### Scenario: Flip in place
- **WHEN** the user types "torch" and taps the switch on the Flashlight result
- **THEN** the flashlight turns on, the switch shows on, and the search surface stays open

#### Scenario: Access needed first
- **WHEN** the user taps the Do Not Disturb switch and ekQuickFind has no Do Not Disturb access yet
- **THEN** the system page for granting that access opens; after granting, the next tap changes Do Not Disturb in place

#### Scenario: Bluetooth through the system dialog
- **WHEN** Bluetooth is off and the user taps its switch
- **THEN** a system "ekQuickFind wants to turn on Bluetooth" dialog appears over search; after Allow, Bluetooth turns on and the switch shows on without leaving search

#### Scenario: System refuses the dialog
- **WHEN** the device doesn't let apps ask to turn Bluetooth off
- **THEN** tapping the switch while Bluetooth is on opens Bluetooth settings instead

#### Scenario: System-only toggle
- **WHEN** the user taps the Wi‑Fi switch
- **THEN** the system Wi‑Fi panel opens over the search surface, where the user can turn Wi‑Fi on or off
