## ADDED Requirements

### Requirement: Calendar search
The system SHALL find calendar events (in calendars shown in the user's calendar app) by title or place, from 30 days ago to a year ahead, once the calendar permission is granted; until then a single "Enable calendar search" row asks for it. Upcoming events in the next week rank first, then later ones, then past ones. Each row shows the day, time (or "All day") and place, for example "Tue 3 Nov · 14:00 · Clinic", and opens the event in the calendar app.

#### Scenario: Find the dentist
- **WHEN** the user types "dentist" with an appointment tomorrow and one last week
- **THEN** tomorrow's appointment is listed above last week's, with its day, time and place

### Requirement: Next up
The provider SHALL offer the timed event running now or starting in the next 12 hours as "Next up", described as "Now · until 10:00", "Today · 14:00" or "Tomorrow · 08:00".
