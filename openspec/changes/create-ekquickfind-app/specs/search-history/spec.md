# Spec Delta

## Purpose

Remembers what the user searched for and opened so that search gets faster over time, while keeping all of that data on the device and under the user's control.

## ADDED Requirements

### Requirement: Opened results are remembered
The system SHALL record each result opened from search with a timestamp and list the most recently opened ones under Recent on the empty search screen, each with its own icon (an app's icon, or the icon of the app that handles a setting, file or contact).

#### Scenario: Recent after opening
- **WHEN** the user opens "Spotify" from search and later reopens ekQuickFind with an empty query
- **THEN** Spotify appears under Recent with the Spotify icon, and typing "s" ranks Spotify higher

### Requirement: Typed queries are not stored
The system SHALL not keep a list of typed search text. Only opened results, launch counts and pins are remembered.

#### Scenario: No recent searches
- **WHEN** the user types "weather", opens a result and later reopens ekQuickFind with an empty query
- **THEN** no "Recent searches" list is shown and "weather" is not stored; the opened result appears under Recent

### Requirement: Results can be pinned
The user SHALL be able to pin any result so it appears at the top of the zero-query state and receives a ranking boost.

#### Scenario: Pinned results are marked
- **WHEN** the user pins a result
- **THEN** that result shows a pin mark wherever it appears (search results, Pinned, Recent, strip) until it is unpinned

#### Scenario: Pin and unpin
- **WHEN** the user pins "Camera" and later unpins it
- **THEN** "Camera" appears in Pinned until unpinned, then disappears from Pinned

### Requirement: History can be cleared
The user SHALL be able to remove individual history items and clear all history and launch counts in one action.

#### Scenario: Clear all
- **WHEN** the user taps "Clear search history" and confirms
- **THEN** Recent is empty and ranking boosts are reset; pinned items are kept

### Requirement: History stays on device
History SHALL be stored only in app-private storage and SHALL never be transmitted or included in backups unless the user turns on backup explicitly.

#### Scenario: Default backup
- **WHEN** the device performs a cloud backup with default ekQuickFind settings
- **THEN** history data is excluded from the backup
