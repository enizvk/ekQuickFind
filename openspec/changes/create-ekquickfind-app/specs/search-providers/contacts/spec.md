# Spec Delta

## Purpose

Finds people in the device contacts and offers direct communication actions, only when the user has granted contacts access.

## ADDED Requirements

### Requirement: Contacts are searched only with permission
The provider SHALL stay disabled until the contacts permission is granted, and SHALL show a single inline "Enable contacts search" result instead of requesting permission at launch.

#### Scenario: Permission not granted
- **WHEN** the user types a query and contacts permission is not granted
- **THEN** the Contacts section shows one tappable row offering to enable contacts search, and tapping it shows the system permission dialog

#### Scenario: Permission denied permanently
- **WHEN** the user has denied the permission twice
- **THEN** the row opens the app's system settings page instead of the permission dialog

### Requirement: Contacts match by name, nickname, phone and email
The provider SHALL match on display name, nickname, organisation, phone numbers (digits only, ignoring formatting) and email addresses.

#### Scenario: Partial phone number
- **WHEN** the user types "0871"
- **THEN** contacts whose numbers contain "0871" appear, with the matching number shown

#### Scenario: Organisation match
- **WHEN** the user types "acme"
- **THEN** contacts whose organisation contains "Acme" appear

### Requirement: Contact results offer direct actions
Each contact result SHALL show call and message actions inline when a phone number exists, and open the contact's detail screen on tap.

#### Scenario: Call from result
- **WHEN** the user taps the call action on a contact with one number
- **THEN** the dialler opens with that number ready to call

#### Scenario: Multiple numbers
- **WHEN** the contact has several numbers and the user taps call
- **THEN** a chooser lists the numbers with their labels

### Requirement: Starred contacts rank higher
The provider SHALL rank starred contacts above others at equal text match quality. Contacts the user often opens from search rank higher through search history, because Android 10 and later no longer expose call or message frequency to apps.

#### Scenario: Starred tie-break
- **WHEN** two contacts named "Alex" match equally and one is starred
- **THEN** the starred contact is listed first

#### Scenario: Often opened from search
- **WHEN** two contacts named "Sam" match equally and the user has opened one of them from search several times
- **THEN** that contact is listed first
