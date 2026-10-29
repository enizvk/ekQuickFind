# Spec Delta

## Purpose

Defines how a query is turned into a ranked, sectioned, progressively updated set of results from all enabled providers, and what the user can do with those results.

## ADDED Requirements

### Requirement: Results update on every keystroke
The system SHALL start searching on each query change without a submit action and show results as each provider finishes, rather than waiting for all providers.

#### Scenario: Fast provider shows first
- **WHEN** the user types a character and the apps provider finishes before the files provider
- **THEN** app results appear immediately and file results are inserted later without the list jumping under the user's finger

#### Scenario: Stale results are discarded
- **WHEN** the user types "ma" then "map" before results for "ma" are complete
- **THEN** only results for "map" are shown; no result for "ma" appears after "map" results begin

### Requirement: Results are grouped by section
The system SHALL group results into labelled sections (for example Apps, Contacts, Settings, Files, Web) with a configurable per-section cap and a "More" affordance when the cap is exceeded.

#### Scenario: Section cap
- **WHEN** a provider returns 12 matches and the section cap is 4
- **THEN** the section shows 4 results and a "More" control that expands the section inline

#### Scenario: Mixed query shows every type together
- **WHEN** the user types a query that matches apps, settings and files
- **THEN** the Apps, Settings and Files sections appear together on the same scrollable screen, each with its own header

#### Scenario: Empty sections are hidden
- **WHEN** a provider returns no matches
- **THEN** its section header is not shown

### Requirement: Best match is promoted
The system SHALL show the single highest-scoring result across all providers as a prominent top result, and pressing the keyboard action key SHALL activate it.

#### Scenario: Keyboard action opens top result
- **WHEN** the user types "cam" and the Camera app is the top result and presses the keyboard's action key
- **THEN** the Camera app launches and the search surface closes

#### Scenario: No top result falls back to web
- **WHEN** no provider returns a match
- **THEN** the top result is "Search the web for <query>"

### Requirement: Matching tolerates typos and abbreviations
The system SHALL match results by prefix, word-initial letters, substring and small edit-distance typos, with prefix matches ranked above the others. A one-character query SHALL match only by prefix or word prefix. Two characters SHALL also match the initials of a multi-word name. All-words and substring matching start at three characters and typo matching at four.

#### Scenario: Short queries match only starts of words
- **WHEN** the user types "s"
- **THEN** "Slack" and "My Settings" appear, but "Pulse" (which only contains an "s") does not

#### Scenario: Initials match
- **WHEN** the user types "vsc"
- **THEN** "Visual Studio Code" appears

#### Scenario: Exact match
- **WHEN** the user turns on the exact-match toggle (a quote-mark button in the search bar) and types "eren"
- **THEN** only names with "eren" as a whole word appear ("Eren Yılmaz", "Ali Eren", "eren_beach.jpg"), not longer words that contain it ("Erenay", "Reference") or initials and typo matches ("gm" no longer finds Google Maps or Gmail); case and accents are still ignored, and the toggle stays as set for the next search

#### Scenario: Two-letter initials
- **WHEN** the user types "gm"
- **THEN** "Google Maps" appears (with G and M highlighted) alongside "Gmail", and "Pigment" does not

#### Scenario: Typo match
- **WHEN** the user types "whatsap"
- **THEN** "WhatsApp" appears in the Apps section

### Requirement: Ranking learns from use
The system SHALL boost results the user has opened before from search, proportional to how often and how recently they were opened.

#### Scenario: Previously launched app ranks higher
- **WHEN** the user has opened "Spotify" from search five times and types "s"
- **THEN** "Spotify" ranks above apps starting with "s" that were never opened from search

### Requirement: Results expose secondary actions
Each result SHALL offer a long-press menu of provider-specific actions (for example App info, Uninstall, Call, Share, Pin).

#### Scenario: Long-press on an app
- **WHEN** the user long-presses an app result
- **THEN** a menu appears with the app's shortcuts, App info, Uninstall (if allowed) and Pin

### Requirement: Zero-query state is useful
With an empty query the system SHALL show pinned results, recently opened results and suggested apps, each item dismissible.

#### Scenario: Fresh install
- **WHEN** the query is empty and there is no history
- **THEN** the surface shows a short hint about what can be searched and suggested apps from the device's most used apps, if usage access is granted

### Requirement: Slow providers do not block the UI
The system SHALL cap each provider's response time per query and show whatever has arrived when the cap expires.

#### Scenario: Provider timeout
- **WHEN** a provider has not responded 700 ms after the latest keystroke
- **THEN** results from other providers are shown as final and the slow provider's late results are appended only if the query has not changed

### Requirement: Every result shows the app that opens it
Every result SHALL show the icon of the app that will handle it. Rows that have no app icon of their own (settings, contacts, files, web) use the handling app's icon. Rows that show a thumbnail or avatar carry the handling app's icon as a small corner badge.

#### Scenario: Settings result
- **WHEN** a Settings screen matches
- **THEN** its row shows the device Settings app icon

#### Scenario: File with a thumbnail
- **WHEN** an image file matches
- **THEN** its row shows the image thumbnail with the default gallery app's icon as a corner badge

#### Scenario: Web result
- **WHEN** a web suggestion or the web fallback is shown
- **THEN** its row shows the default browser's icon

### Requirement: Matched characters are highlighted
Each result title SHALL highlight the characters that matched the query, using the same matching rules as ranking (prefix, word prefix, all words, initials, substring and one-edit typos), mapped back to the original text so accents and extra spaces don't shift the highlight.

#### Scenario: Prefix and word prefix
- **WHEN** the user types "notif"
- **THEN** "Notif" is highlighted in "Notification" and in "My Notification"

#### Scenario: Initials
- **WHEN** the user types "vsc" and "Visual Studio Code" matches by initials
- **THEN** the "V", "S" and "C" are highlighted

#### Scenario: Web suggestions emphasise the completion
- **WHEN** the user types "sound" and the suggestions "soundcloud" and "soundtrap" appear
- **THEN** "cloud" and "trap" are emphasised and the typed "sound" is plain, so suggestions don't read as repeated "sound"

#### Scenario: Accents
- **WHEN** the user types "cafe" and "Café Finder" matches
- **THEN** "Café" is highlighted including the accented letter
