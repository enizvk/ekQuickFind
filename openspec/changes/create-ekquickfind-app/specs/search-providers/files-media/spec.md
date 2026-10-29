# Spec Delta

## Purpose

Finds photos, videos, audio, documents and downloads stored on the device by name, only when the user has granted the relevant media permissions.

## ADDED Requirements

### Requirement: Media search is permission-gated per type
The provider SHALL search each media type only after its permission is granted and SHALL offer inline enable rows for ungranted types instead of requesting at launch.

#### Scenario: Partial media access
- **WHEN** the user has granted access to selected photos only
- **THEN** only those photos are searchable and the Files section shows a row to grant full access

#### Scenario: No permission yet
- **WHEN** the user types a query and no media permission is granted
- **THEN** the Files section shows one row offering to enable file search

### Requirement: Files match by display name and extension
The provider SHALL match on file display name and extension, and SHALL present a thumbnail for images and videos and a type icon otherwise.

#### Scenario: Extension query
- **WHEN** the user types "pdf"
- **THEN** PDF documents appear sorted by most recently modified

#### Scenario: Image thumbnail
- **WHEN** an image file matches
- **THEN** its thumbnail is shown within the result row

### Requirement: Recent files rank higher
At equal text match quality the provider SHALL rank files modified more recently above older files.

#### Scenario: Recency tie-break
- **WHEN** two files "report.pdf" match and one was modified today
- **THEN** today's file is listed first

### Requirement: Files open with their default handler
Tapping a file result SHALL open it with the system default app for its type, and long-press SHALL offer Share and Show in folder actions.

#### Scenario: No handler installed
- **WHEN** no app can open the file type
- **THEN** the system app chooser is shown rather than a silent failure

### Requirement: Photos can be found by their tags
When "Photo tags" is on, the system SHALL tag photos on the phone (ML Kit's bundled image labeller, about 400 everyday labels, confidence 0.7 or more) and read keywords saved in each photo's XMP metadata, store them in a local index, and match queries of three or more letters against the start of any tag word. A photo found by both name and tag SHALL appear once; tag matches show the tag before the folder and date. Samsung Gallery's own tags can't be read (they need a Samsung-only permission).

#### Scenario: Found by what's in it
- **WHEN** photo tags are on, tagging has run, and the user types "dog"
- **THEN** photos labelled Dog appear under Files with "Dog · Camera · 2 days ago"

#### Scenario: Tagging is resumable and incremental
- **WHEN** tagging is stopped partway, or new photos are taken
- **THEN** the next run (now, or the daily run while charging) tags only photos that are new or edited, and forgets deleted ones

#### Scenario: Keeps going after closing
- **WHEN** the user turns photo tags on and closes settings or the app
- **THEN** tagging continues as a foreground job with a "Tagging photos" progress notification until every photo is done

#### Scenario: Turning it off
- **WHEN** the user turns photo tags off
- **THEN** tagging stops and every stored tag is deleted

