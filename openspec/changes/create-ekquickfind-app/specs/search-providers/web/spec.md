# Spec Delta

## Purpose

Extends search beyond the device with web suggestions and a web-search fallback, using a search engine the user controls and that can be turned off entirely.

## ADDED Requirements

### Requirement: Web fallback is always available
The system SHALL always offer a "Search the web for <query>" result that opens the query in the user's chosen search engine in the default browser.

#### Scenario: Open web search
- **WHEN** the user taps "Search the web for cats"
- **THEN** the default browser opens the chosen engine's results page for "cats" and the search surface closes

### Requirement: Web suggestions are fetched from the chosen engine
When enabled, the provider SHALL fetch up to five suggestions from the chosen search engine after the user pauses typing, and SHALL cancel in-flight requests when the query changes.

#### Scenario: Suggestions appear after a pause
- **WHEN** the user types "weather in" and pauses for 250 ms
- **THEN** up to five suggestions appear in a Web section

#### Scenario: Offline
- **WHEN** the device has no connectivity
- **THEN** the Web section is hidden and only the web fallback result remains, with no error shown

### Requirement: Search engine is selectable
The user SHALL be able to choose among DuckDuckGo (default), Google, Bing, Brave and a custom URL template.

#### Scenario: Custom engine
- **WHEN** the user enters a custom search template containing "%s"
- **THEN** web search uses that template with the query substituted

#### Scenario: Custom suggestions
- **WHEN** the user also enters a custom suggestion template containing "%s" that returns OpenSearch suggestion JSON
- **THEN** suggestions are fetched from it; without one, the custom engine shows no suggestions

### Requirement: Web suggestions can be disabled
When web suggestions are disabled, the system SHALL make no network requests at all.

#### Scenario: Suggestions off
- **WHEN** web suggestions are disabled and the user types any query
- **THEN** no network request is made and only the web fallback result is offered
