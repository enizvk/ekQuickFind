<p align="center">
  <img src="docs/icon.png" width="128" alt="ekQuickFind icon">
</p>

<h1 align="center">ekQuickFind</h1>

<p align="center">
  <b>Find anything on your phone in a keystroke.</b><br>
  Apps, contacts, settings, files, quick answers and the web, in one fast, private search bar for Android.
</p>

<p align="center">
  <img alt="Android 17+" src="https://img.shields.io/badge/Android-17%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Private" src="https://img.shields.io/badge/data-stays%20on%20your%20phone-2E7D32">
  <img alt="License: Apache 2.0" src="https://img.shields.io/badge/license-Apache%202.0-blue">
</p>

<p align="center">
  <img src="docs/screenshots/search.png" width="200" alt="Results grouped into apps and settings, with a live switch">
  &nbsp;
  <img src="docs/screenshots/toggles.png" width="200" alt="Switch Bluetooth on right from the results">
  &nbsp;
  <img src="docs/screenshots/answers.png" width="200" alt="Unit conversion answer as you type">
  &nbsp;
  <img src="docs/screenshots/settings.png" width="200" alt="Frosted glass background with a live preview">
</p>

---

## Why ekQuickFind

Spotlight made "just type it" the fastest way around a Mac. ekQuickFind brings that to Android: open it, type two or three letters and the thing you want is already selected. Press Enter and it opens.

- **One bar for everything.** Apps (work profile too), contacts with call and message buttons, device settings, photos, videos, music and documents, and web suggestions from your search engine.
- **Flip switches without leaving search.** Bluetooth, Wi-Fi, Do Not Disturb, auto-rotate and more show their live state and toggle right in the results.
- **Find photos by what's in them.** Turn on Photo tags and search "dog", "beach" or "pizza". Photos are tagged on your phone, and keywords saved in your photos count too.
- **Ask AI in one tap.** Hand what you typed to ChatGPT, Claude or Gemini straight from the results.
- **Answers as you type.** Calculator and unit conversion (`12*7+5`, `5 km in miles`) appear before you finish typing.
- **Gets smarter the more you use it.** Results you open rank higher, typos are forgiven, and matched letters are highlighted. Pin favourites and hide what you never want to see.
- **Opens from anywhere.** App icon, home screen search bar widget, Quick Settings tile, launcher shortcuts, the system search key, or as your digital assistant.
- **Made to fit your hand.** Search bar at the bottom or top, the best match next to the bar, "More below" hints when results continue, and haptics.
- **Make it yours.** Frosted glass, blur or solid backgrounds with adjustable dim. Pick the icon size, density, section order (drag to reorder) and layout per section: rows, a compact grid or chips. Light, dark or Material You colours.

## Features

**Search everything**
- **Apps**, including work profile apps, with app shortcuts and uninstall/info actions
- **Contacts** with call and message buttons
- **Device settings**: about 80 settings screens, plus live switches for Wi-Fi, Bluetooth, Do Not Disturb, auto-rotate, battery saver and the flashlight that flip right in the results
- **Files and media**: photos, videos, music, and documents and downloads with All files access
- **Photo tags**: find photos by what's in them (dog, beach, food) and by keywords saved in them, tagged on the phone in the background
- **Quick answers**: a calculator and unit conversion for length, weight, volume, temperature, time, data and speed
- **Web**: live suggestions from DuckDuckGo, Google, Bing, Brave or your own engine, plus a "Search the web" row; web search can be turned off entirely
- **Ask AI**: hand what you typed to ChatGPT, Claude or Gemini in one tap

**Smart results**
- Typo-tolerant matching that highlights the letters that matched, including initials: "gm" finds Google Maps
- The best match is selected, so Enter opens it; arrow keys move the selection on a hardware keyboard
- Results you open rank higher over time
- Pin favourites, hide results and exclude apps
- Recently opened items, as a list or a strip of chips, and suggested apps when you haven't typed yet
- "More below" names the sections that are still out of view

**Make it yours**
- Results grouped into sections you can reorder by dragging, each shown as rows, a compact grid or chips
- Icon size, density and results per section
- Search bar at the bottom or top, with the selected result kept right above it for one-handed use
- Frosted glass, blur or solid background with adjustable dim and blur strength, including Samsung's own blur on One UI
- Light, dark or system theme, wallpaper colours and ten accent colours
- Haptics, reduced motion and smooth Material 3 Expressive animations

**Open it from anywhere**
- A floating button over every app: drag it anywhere, it settles on the nearest edge; size and transparency are adjustable
- App icon, home screen search bar widget and Quick Settings tile
- Launcher shortcuts for pinned results
- Set as your digital assistant, or as the system search for other apps' search buttons

**Private and up to date**
- Everything stays on the phone: no accounts, analytics or ads, and history is left out of backups
- One tap clears history
- The direct-download version checks GitHub once a day and updates itself

## Settings

Everything above is configurable, grouped into pages like the system Settings app.

<p align="center">
  <img src="docs/screenshots/settings/00-menu.png" width="280" alt="Settings menu: Search sources, Web and AI, Results, Look and feel, Keyboard and feedback, Ways to open, Privacy, Updates, About">
</p>

## Private by design

- History, pins and settings stay on your phone. No accounts, no analytics, no ads.
- The only things that touch the network are web suggestions and the daily update check (direct download version). Both can be turned off, and web search can be switched off entirely.
- Photo tags are worked out on the phone; no photo ever leaves it.
- History is left out of cloud backups unless you turn that on.

## Get it

| | |
| --- | --- |
| **Direct download** | Grab the latest APK from [Releases](https://github.com/enizvk/ekQuickFind/releases/latest). This version keeps itself up to date: it checks once a day on Wi-Fi and installs new versions after you allow it once. |

Requires Android 17 or newer. ekQuickFind is free and open source.

## Tips

- **Faster than the icon:** add the Quick Settings tile or the home screen search bar from *Settings > Ways to open*.
- **One-handed:** turn on *Selected result by the search bar* so the best match sits right above the keyboard.
- **Samsung:** set the side button double press to open ekQuickFind (*Settings > Advanced features > Side button*).

## For developers

ekQuickFind is written in Kotlin with Jetpack Compose, Hilt, Room, DataStore and WorkManager, split into a pure-Kotlin search engine and Android modules. See [docs/development.md](docs/development.md) for building and testing, and [docs/updates.md](docs/updates.md) for publishing a direct-download release.

## License

Copyright 2026 ekQuickFind contributors.

Licensed under the [Apache License, Version 2.0](LICENSE). You may use, change and share the code, including in other projects, as long as you keep the license and notices.

Photo tags use [Google ML Kit](https://developers.google.com/ml-kit) image labelling (and the Google support libraries it brings with it). It runs on the phone, but Google distributes it under its own [terms](https://developers.google.com/ml-kit/terms) rather than an open-source license. ekQuickFind's other libraries are open source.

