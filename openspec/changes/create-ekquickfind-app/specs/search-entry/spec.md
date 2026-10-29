# Spec Delta

## Purpose

Defines every way a user opens and closes ekQuickFind and how the search surface behaves as an overlay on Android 17+ devices.

## ADDED Requirements

### Requirement: Search opens from the app icon
The system SHALL open the search surface with the keyboard shown and the query field focused when the user taps the app icon.

#### Scenario: Cold launch from icon
- **WHEN** the user taps the ekQuickFind launcher icon on a device where the app is not running
- **THEN** the search surface is visible and accepting input within 400 ms on a mid-range Android 17 device, with the software keyboard open

#### Scenario: Warm launch keeps nothing stale
- **WHEN** the user reopens ekQuickFind after dismissing it
- **THEN** the query field is empty and the zero-query state is shown

### Requirement: Search opens from a home-screen widget
The system SHALL provide a resizable home-screen search-bar widget that opens the search surface when tapped.

#### Scenario: Widget tap
- **WHEN** the user taps the search-bar widget
- **THEN** the search surface opens with the keyboard shown, animating from the widget's position

#### Scenario: Widget follows system theme
- **WHEN** the device switches between light and dark theme or changes its dynamic colour palette
- **THEN** the widget updates its colours to match without user action

### Requirement: Search opens from a Quick Settings tile
The system SHALL offer a Quick Settings tile that opens the search surface over the current app.

#### Scenario: Tile tap over another app
- **WHEN** the user expands Quick Settings while in any app and taps the ekQuickFind tile
- **THEN** the shade collapses and the search surface opens with the keyboard shown

#### Scenario: Tile tap on locked device
- **WHEN** the user taps the tile while the device is locked
- **THEN** the system prompts to unlock first and opens search only after a successful unlock

### Requirement: Search can be the default digital assistant
The system SHALL be selectable as the device's default digital assistant so the assistant gesture (power-button long press or corner swipe) opens the search surface.

#### Scenario: Assistant gesture
- **WHEN** ekQuickFind is the default assistant and the user performs the assistant gesture
- **THEN** the search surface opens over the current screen with the keyboard shown

#### Scenario: Not the default assistant
- **WHEN** ekQuickFind is not the default assistant
- **THEN** the in-app settings show a one-tap action that opens the system screen for choosing the default assistant

### Requirement: Search handles system search intents
The system SHALL respond to the standard Android search and web-search intents with the incoming query pre-filled.

#### Scenario: Incoming web search intent
- **WHEN** another app sends a web-search intent with a query
- **THEN** the search surface opens with the query filled and results already loading

### Requirement: Overlay dismisses predictably
The search surface SHALL close with predictive back, swipe-down on the result list at scroll top, or a tap on the dimmed background, returning to whatever was underneath.

#### Scenario: Predictive back preview
- **WHEN** the user begins a back gesture
- **THEN** the surface previews shrinking toward the edge and the underlying screen becomes visible before the gesture commits

#### Scenario: Tap outside
- **WHEN** the user taps the dimmed area outside the search surface
- **THEN** the surface closes and the keyboard hides

### Requirement: Keyboard and surface stay in sync
The search surface SHALL anchor the query field directly above the software keyboard and animate with keyboard show and hide.

#### Scenario: Keyboard dismissed by scrolling
- **WHEN** the user scrolls the result list upward
- **THEN** the keyboard hides and the result list expands to use the freed space in a single continuous animation

### Requirement: Motion is quick and continuous
Every visible change on the search surface (opening, closing, results arriving or reordering, sections expanding, icons loading, menus and dialogs) SHALL animate smoothly and finish within about 250 ms, and SHALL fall back to short fades when reduce motion is on.

#### Scenario: Closing animates
- **WHEN** the user dismisses the surface by any means
- **THEN** the surface and scrim animate out before the window disappears

#### Scenario: Icons fade in
- **WHEN** a result icon or thumbnail finishes loading
- **THEN** it fades in instead of popping

### Requirement: Launcher shortcuts
Long-pressing the ekQuickFind icon SHALL offer static shortcuts for a new search and for ekQuickFind settings, plus dynamic shortcuts for up to three pinned results that open the pinned item directly. Dynamic shortcuts SHALL follow pins as they are added and removed.

#### Scenario: Settings shortcut
- **WHEN** the user long-presses the ekQuickFind icon and taps "Settings"
- **THEN** ekQuickFind opens on its settings screen

#### Scenario: Pinned result shortcut
- **WHEN** the user has pinned Calculator and taps its shortcut from the icon's long-press menu
- **THEN** Calculator opens without going through the search surface

#### Scenario: Unpinning removes the shortcut
- **WHEN** the user unpins Calculator
- **THEN** its shortcut disappears from the icon's long-press menu
