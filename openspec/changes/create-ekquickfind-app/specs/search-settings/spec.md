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
The user SHALL be able to choose result icon size (Small, Medium, Large), list density (Compact, Comfortable), and how many results each section shows before "More" (3, 4, 6 or 8), on the Search sources page below the sources and the result order. A Text panel on Look and feel SHALL offer a text size slider (small A to big A, 80 % to 160 % in steps of 10, default 100 %; it scales text on the search screen on top of Android's font size), a font list for result text where each name is shown in its own font (Default, Serif · Lora, Mono · JetBrains Mono, Condensed · Roboto Condensed, Rounded · Nunito; bundled with the app so a system font style can't replace them; the search bar keeps the theme font) and a weight (Light, Regular, Medium, Bold; Regular keeps the theme's mix), with sample results showing all three.

#### Scenario: Larger text
- **WHEN** the user picks Text size Larger
- **THEN** result titles, subtitles, section headings and the search bar text on the search screen get bigger immediately; the settings screen keeps its size

#### Scenario: Large icons
- **WHEN** the user picks Large icons
- **THEN** result rows and the top result render with larger icons immediately, without restarting the app

#### Scenario: Results per section
- **WHEN** the user sets results per section to 6
- **THEN** sections show up to 6 rows before the "More" control

### Requirement: Active item and search bar effects
Look and feel SHALL group these options in two panels, each with a live preview under its own choices. Every preview SHALL look unlike a control: a "Preview" label with an eye icon (and Play where there's an animation) over a coloured backdrop, taking no taps and read by screen readers as one "Preview".

- **Search bar**: position, recent items strip, "Recent items shown" (a number field, 1 to 20, default 5; it limits recent items before anything is typed, in the list and the strip; other values show as an error and aren't saved), "Exact match button" (on by default; hiding it also turns exact match off, so searches can't stay strict with no way back), and "Animation when search opens" (on by default) with its type: Light sweep (a point of light runs along the border about one and a half times, about 3 seconds), Colour pulse (a soft multicolour glow breathes twice, about 3.6 seconds), Shimmer ring (a multicolour outline turns slowly with a faint halo), Breathe (one slow accent-colour breath) or Twin lights (default; two lights run round the border half a turn apart). It plays once each time search opens and never while typing. The preview bar under the choices plays it when the choice changes or on Play.
- **Active item**: Top result (In its section, default: it stays first in its own section's rows, grid or chips, selected and marked; a best match alone in its section brings that section first; or Featured: the best match as a big card above the sections), Selected result by the search bar, Style (Tinted card default with a soft accent gradient fill, soft accent edge and soft glow; Soft glow with an even thin accent line and halo; Gradient with a thin accent-to-pink line), and "Animation when it becomes active" (on by default) with its type: Settle (default; the mark fades and settles in), Lift (the item rises a touch and settles back) or a border glow matching a search bar type (Light sweep, Colour pulse, Shimmer ring, Breathe, Twin lights). The preview is a small results panel laid out by Top result and Selected result by the search bar, and replays the animation when any of them changes or on Play.

The mark SHALL be equally thick all the way round on rows, tiles, chips and cards; on the top card and the selected slot the card itself is the mark, so its rounded clip never trims it. The two animations turn off separately; Reduce motion turns both off, and the mark then just moves without motion.

#### Scenario: Light sweep on open
- **WHEN** the search bar animation is on and the user opens search
- **THEN** a light runs along the search bar's border for about 3 seconds and fades, then the bar looks as usual

#### Scenario: Bar animation off, item animation on
- **WHEN** the user turns "Animation when search opens" off
- **THEN** search opens without the bar glow, its type choices and preview are hidden, and the active item still animates

#### Scenario: Preview follows the layout
- **WHEN** the user turns on "Selected result by the search bar"
- **THEN** the active item preview shows the best-match card just above the search bar instead of on top

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
The user SHALL be able to choose, per section, whether results show as rows, a grid of small icons, or compact chips. Defaults: Apps as grid, Settings and Recent as chips, everything else as rows. Web results always show as a compact link list with small icons, so the Web section has no layout choice.

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
Settings SHALL open on a menu (like the system Settings app) of pages, each with an icon, title, short summary and an arrow: Search sources, Web and AI, Look and feel, Keyboard and feedback, Ways to open, Privacy and App (version, GitHub page, the updates panel in the direct version, and reset). Each page groups its options into rounded panels in a container colour a clear step from the page background, under icon headings; a panel named like its page shows no heading. Back returns from a page to the menu, and from the menu to search.

#### Scenario: Open a page
- **WHEN** the user opens settings and taps "Look and feel"
- **THEN** a page titled "Look and feel" shows the Background, Appearance and Search bar panels, and Back returns to the menu

#### Scenario: Font with a custom system font style
- **WHEN** the phone uses a system font style (for example Samsung's) and the user picks Mono
- **THEN** results and the sample switch to JetBrains Mono anyway, because the font ships inside the app

### Requirement: Settings can be reset to defaults
The App page SHALL end with a Reset panel whose "Reset all settings" button, after a confirmation dialog, puts every setting back to its install default. History, pins, hidden results and excluded apps SHALL stay. Photo tags go back to off, so tagging stops and the tag index is cleared, as when the user turns them off; the floating button goes away as its setting returns to off.

#### Scenario: Reset keeps the user's data
- **WHEN** the user has a dark theme, larger text and a hidden result, taps "Reset all settings" and confirms
- **THEN** the theme follows the system again, text is 100 %, and the hidden result is still hidden

### Requirement: Floating button is on by default
The floating button SHALL be on in a fresh install. Until "Display over other apps" is granted, no button appears and its settings row shows "Needs Display over other apps" with an Allow button.

### Requirement: Before you type
A "Before you type" panel on Search sources SHALL offer three switches, all on by default:

- **Smart suggestions**: the Suggested row first lists what the user usually opens at this time of day (and kind of day), or with the headphones or car that are connected now, learned on the phone from their own launch history of the last 30 days; usage-based apps fill the rest. Each launch keeps the name of the connected audio device (read without any permission); one-off launches don't count.
- **Next up**: the calendar event running now or starting in the next 12 hours (all-day events left out) shows first, when calendar search is on and allowed.
- **Clipboard**: a Clipboard chip appears when text (not marked sensitive) was copied, checked again as soon as the search window has focus (Android refuses clipboard access before that), or when clips are remembered. Only tapping it reads the clipboard, so Android's "pasted from your clipboard" message never appears on open. The clip read is remembered (the last 10, for 7 days, in the app's no-backup folder so never in a cloud backup; sensitive clips never), and the remembered clips show as pages to swipe through, newest first, with dots below marking the current one. Each page has Search, Open link (for a web address), Call (for a phone number), Share and Forget. Clearing history also clears remembered clips.

#### Scenario: The car playlist
- **WHEN** the user has opened a music app a few times with "My Car" connected, and opens search with the car connected again
- **THEN** that music app is the first suggestion, at any time of day

#### Scenario: Swiping through clips
- **WHEN** the user has opened three different clips with the chip this week and taps it again
- **THEN** the newest clip shows first, swiping shows the other two, and three dots below mark which one is shown
