# Spec Delta

## Purpose

Gives the user control over which providers run, how results look, which search engine is used, and what the app may access.

## ADDED Requirements

### Requirement: Each provider can be toggled
The settings screen SHALL list every provider with a switch, and a disabled provider SHALL not run and SHALL not be shown in results.

#### Scenario: Disable files
- **WHEN** the user turns off the Files provider and types "pdf"
- **THEN** no Files section appears and no media query runs

### Requirement: Web search can be turned off
Settings SHALL offer a "Web search" switch. When off, no web suggestions are fetched, no Web section or "Search the web" row is shown, and the web options are hidden.

#### Scenario: Web search off
- **WHEN** the user turns off Web search and types "zzqxv"
- **THEN** no Web section and no "Search the web" row appear, and ekQuickFind makes no network requests

### Requirement: Permission status is visible per provider
The settings screen SHALL show, for each provider that needs a permission, whether it is granted and offer a way to grant or revoke it.

#### Scenario: Permission revoked externally
- **WHEN** the user revokes contacts access in system settings and returns to ekQuickFind
- **THEN** the Contacts provider shows "Permission needed" and search no longer queries contacts

### Requirement: Accent colour is selectable
The user SHALL be able to pick an accent colour (System, Cyan, Blue, Teal, Green, Amber, Orange, Red, Pink, Purple). It SHALL re-tint matched-text highlights, the selected result's outline and background, section headings, links, switches and the "More below" line, in light and dark themes, keeping text on accent surfaces readable. System keeps the wallpaper (or default) accent.

#### Scenario: Red accent
- **WHEN** the user picks Red
- **THEN** switches, headings, highlights and the selected result turn red immediately; the System swatch still shows the wallpaper accent

### Requirement: Appearance follows the system by default
The app SHALL use the system light/dark setting and dynamic colours by default and SHALL allow overriding theme and disabling dynamic colour.

#### Scenario: Force dark
- **WHEN** the user picks Dark theme while the system is in light mode
- **THEN** the search surface, widget and settings all render dark

### Requirement: Entry points are configurable from settings
Settings SHALL provide shortcuts to add the widget, add the Quick Settings tile and set ekQuickFind as default assistant, each showing whether it is already set up.

#### Scenario: Floating button
- **WHEN** the user turns on "Show floating button" and allows "Display over other apps"
- **THEN** a round button appears on the right edge of the screen over every app; tapping it opens search, dragging moves it and letting go snaps it to the nearer edge; its size (36–80 dp) and opacity (10–100%) follow the sliders live, it hides while ekQuickFind is open (except on its own settings page), and it comes back after a restart

#### Scenario: Add widget prompt
- **WHEN** the user taps "Add to home screen" on a launcher that supports pinning
- **THEN** the system widget-pin dialog appears

### Requirement: Privacy summary is one screen
Settings SHALL include a privacy section stating which data is stored, that nothing is sent except web suggestions, and offering the clear-history action.

#### Scenario: Network use stated
- **WHEN** the user opens the privacy section
- **THEN** it states that the only network use is web suggestions and shows whether they are on

### Requirement: Surface background style is selectable
The user SHALL be able to choose how the screen behind the search surface looks: Blur, Frosted glass or Solid colour, with an adjustable blur strength for Blur and Frosted glass, a chosen colour for Solid colour, and an adjustable dim amount (background opacity) for every style. Defaults: Frosted glass, blur 29, dim 45%, Medium icons, Comfortable density.

#### Scenario: Blur
- **WHEN** the background style is Blur and the device supports cross-window blur
- **THEN** the app underneath is blurred at the chosen strength behind a light dim, and the result panels stay opaque

#### Scenario: Frosted glass
- **WHEN** the background style is Frosted glass
- **THEN** the app underneath is blurred and the result panels and query field are translucent tinted glass, so the blurred content shows through them

#### Scenario: Solid colour
- **WHEN** the background style is Solid colour with a chosen colour and opacity
- **THEN** the screen behind the surface is filled with that colour at that opacity and nothing is blurred

#### Scenario: Stronger dim
- **WHEN** the user raises the dim amount to 70%
- **THEN** the area behind the surface darkens accordingly on the next open and in the live preview, making it obvious the search surface is open

#### Scenario: One UI blur
- **WHEN** Blur or Frosted glass is selected on a Samsung phone, where Android's cross-window blur isn't supported
- **THEN** the app underneath is blurred with One UI's own window blur at the chosen strength

#### Scenario: Blur unavailable
- **WHEN** Blur or Frosted glass is selected but the device has cross-window blur turned off (for example in power saving)
- **THEN** the surface falls back to a dimmed solid background and returns to blur automatically once blur is available again

### Requirement: Result appearance is configurable
The user SHALL be able to choose result icon size (Small, Medium, Large), list density (Compact, Comfortable) and how many results each section shows before "More" (3, 4, 6 or 8).

#### Scenario: Large icons
- **WHEN** the user picks Large icons
- **THEN** result rows and the top result render with larger icons immediately, without restarting the app

#### Scenario: Results per section
- **WHEN** the user sets results per section to 6
- **THEN** sections show up to 6 rows before the "More" control

### Requirement: Surface position is configurable
The user SHALL be able to place the search surface at the bottom (above the keyboard, default) or at the top of the screen.

#### Scenario: Top position
- **WHEN** the user picks Top
- **THEN** the query field sits under the status bar and results flow downward from it

### Requirement: Keyboard behaviour is configurable
The user SHALL be able to choose whether the keyboard opens automatically and whether Enter runs the selected result. The selected result is the top result by default and can be moved with hardware arrow keys.

#### Scenario: Enter runs the selected result
- **WHEN** "Enter runs selected result" is on and the user presses Enter
- **THEN** the selected result's primary action runs

#### Scenario: Enter only hides the keyboard
- **WHEN** "Enter runs selected result" is off and the user presses Enter
- **THEN** the keyboard hides and nothing is launched

#### Scenario: Arrow keys move the selection
- **WHEN** a hardware keyboard is attached and the user presses Down twice
- **THEN** the third result is highlighted as selected and Enter runs it

#### Scenario: Keyboard does not auto-open
- **WHEN** "Open keyboard automatically" is off
- **THEN** the surface opens showing the zero-query state with the keyboard hidden until the user taps the query field

### Requirement: Feedback and motion are configurable
The user SHALL be able to turn haptic feedback off and to reduce motion, which replaces spring and slide animations with short fades.

#### Scenario: Haptics off
- **WHEN** haptics are off and the user launches a result or long-presses a row
- **THEN** no vibration is produced

#### Scenario: Reduced motion
- **WHEN** reduce motion is on
- **THEN** the surface appears and results update with fades only, without slides or item movement animations

### Requirement: Compact grid layout is available and on by default
The surface SHALL offer a compact grid layout, enabled by default for the Apps section, in which results are shown as a grid of small icons with short labels so more results fit on one screen. Any other section (for example Files, where tiles show thumbnails) can be switched to the grid in settings, and the user SHALL be able to switch Apps back to list rows.

#### Scenario: Default grid
- **WHEN** the user types "s" on a fresh install
- **THEN** matching apps appear as a grid of small icons with labels, several per row, under the Apps header

#### Scenario: List layout
- **WHEN** the user turns grid layout off
- **THEN** every section, including Apps, shows one result per row with title and subtitle

#### Scenario: Grid long-press
- **WHEN** the user long-presses an app tile in the grid
- **THEN** the same action menu as in list layout appears

### Requirement: Section order is configurable
The user SHALL be able to reorder result sections (Answer, Apps, Contacts, Settings, Files, Web) in settings by dragging a row's handle; screen readers get "Move up" and "Move down" actions instead. Results SHALL appear in that order, the change SHALL apply to the next keystroke without restarting, and the top result SHALL still be the single best match across all sections.

#### Scenario: Settings above apps
- **WHEN** the user moves Settings above Apps and types "display"
- **THEN** the Settings section is shown before the Apps section

#### Scenario: Drag to reorder
- **WHEN** the user drags the Settings row's handle above Apps and lets go
- **THEN** the rows follow the finger, and the saved order puts Settings before Apps

#### Scenario: Reset order
- **WHEN** the user taps "Reset order"
- **THEN** sections return to the default order Answer, Apps, Contacts, Settings, Files, Web

### Requirement: Section layout is selectable per section
The user SHALL be able to choose, per section, whether results show as rows, a grid of small icons, or compact chips. Defaults: Apps as grid, Recent as chips, everything else as rows. Web results always show as a compact link list with small icons, so the Web section has no layout choice.

#### Scenario: Settings as chips
- **WHEN** the user sets the Settings section layout to Chips and types "display"
- **THEN** matching settings screens appear as tappable chips that wrap across lines

#### Scenario: Chip long-press
- **WHEN** the user long-presses a chip
- **THEN** the same action menu as the row layout appears

### Requirement: Recent items can show as a strip
The user SHALL be able to show recently opened items as a one-line, horizontally scrollable strip of small chips, visible before and while typing, placed above the results (Top, default) or below them (Bottom). When on, the strip replaces the Recent list on the empty screen and doesn't repeat pinned items. Off by default.

#### Scenario: Strip on
- **WHEN** the option is on and the user types "cal"
- **THEN** recently opened items stay visible as small chips above the results, and no Recent list appears in the panel

#### Scenario: Strip at the bottom
- **WHEN** the user sets the strip position to Bottom with the search bar at the bottom
- **THEN** the strip sits between the results and the search bar

#### Scenario: Nothing else to show
- **WHEN** the strip is on, the query is empty and there are no pinned or suggested items
- **THEN** only the strip and the search bar are shown, with no empty panel above them

### Requirement: Selected result can sit by the search bar
The user SHALL be able to show the selected result (the top result unless arrow keys moved it) at the bottom of the result panel, fixed just above the search bar, instead of first in the list. It SHALL stay visible while the rest of the results scroll. Off by default.

#### Scenario: Selected result at the bottom
- **WHEN** the option is on and the user types "cam"
- **THEN** the Camera top result sits at the bottom of the panel next to the search bar, and the sections scroll above it

#### Scenario: More results below
- **WHEN** the results are longer than the panel
- **THEN** the last visible rows fade out and a line above the selected result names what's still below (for example "More below: Contacts · Settings · Web"); tapping it scrolls down, and it disappears once the end is reached

#### Scenario: Swiping on the selected result
- **WHEN** the option is on and the user swipes up starting on the selected result
- **THEN** the keyboard hides, the same as swiping the list; pulling down from it closes the surface

### Requirement: About is shown in settings
Settings SHALL end with an About panel showing the project's GitHub page as a link and the installed version name.

#### Scenario: About panel
- **WHEN** the user scrolls to the end of settings
- **THEN** the About panel shows the project's GitHub page (`github.com/<owner>/<repo>` from `ekquickfind.githubRepo`), which opens in the browser when tapped, and the version name (for example "0.1.0") without the build number

### Requirement: Settings open on a menu of pages
Settings SHALL open on a menu (like the system Settings app) of pages, each with an icon, title, short summary and an arrow: Search sources, Web and AI, Results, Look and feel, Keyboard and feedback, Ways to open, Privacy, Updates (direct version only) and About. Each page groups its options into rounded panels in a container colour a clear step from the page background, under icon headings; a panel named like its page shows no heading. Back returns from a page to the menu, and from the menu to search.

#### Scenario: Open a page
- **WHEN** the user opens settings and taps "Look and feel"
- **THEN** a page titled "Look and feel" shows the Background, Appearance and Search bar panels, and Back returns to the menu
