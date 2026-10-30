## ADDED Requirements

### Requirement: Site shortcuts
The system SHALL search a site straight from the bar when the first word is that site's keyword, followed by a space ("yt cats"). If the user set a separator, it may come between them, touching the keyword or not, and is still optional ("yt: cats", "yt - cats", "yt -> cats"); a space after it is required, so names like "yt-dlp" stay normal searches. A matching word at the start or end also works ("youtube cats", "translate hello"). The site's row appears in a "Search with" section, placed right under answers and weighted so a keyword beats any app whose name starts the same; a keyword match is the best match.

Ready-made sites, each on by default with its own keyword (editable, unique): YouTube (yt; searches in the YouTube app when installed), Google Translate (tr; the Translate app, else translate.google.com into the phone's language), Google Maps (map; a geo: link for the preferred map app, else the Maps website), Play Store (play; market: link, else the website), Wikipedia (wiki; in the phone's language, the app takes the link) and Reddit (r). Users can add their own sites with a name, a keyword and an https address with %s for the search, and delete them.

#### Scenario: YouTube from the bar
- **WHEN** the user types "yt: lofi beats" with the YouTube app installed
- **THEN** "Search YouTube for “lofi beats”" is the best match and opens the YouTube app's search

#### Scenario: A dashed name stays a normal search
- **WHEN** the separator is "-" and the user types "yt-dlp"
- **THEN** no site shortcut is offered

### Requirement: Keyword separator
The Search sites panel SHALL have a small text box for the separator: up to 3 symbols with no letters, digits or spaces (others show an error and aren't saved); blank, the default, means just a space. Site keywords use small text boxes and checkboxes to keep the panel compact.

### Requirement: The bar shows a typed keyword
Once a site keyword is followed by a space (or the separator and a space), the bar SHALL colour the keyword (and its separator) in the accent and bold, on a softly rounded, lightly padded tint, fading in, and swap the search icon for the site's icon with a small spring; Reduce motion switches without animating. Matching words aren't coloured. A keyword only counts with something after it, so an app with the same name is found as usual when the keyword is typed alone.
