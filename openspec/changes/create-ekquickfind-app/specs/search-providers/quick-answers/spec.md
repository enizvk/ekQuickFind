# Spec Delta

## Purpose

Answers calculations and unit conversions directly in the search surface, computed on device with no network access.

## ADDED Requirements

### Requirement: Arithmetic expressions are evaluated inline
The provider SHALL evaluate arithmetic expressions with + − × ÷, parentheses, percentages, powers and decimal numbers, and show the result as the top result.

#### Scenario: Basic expression
- **WHEN** the user types "12*8+3"
- **THEN** a top result shows "99" and tapping it copies the value to the clipboard

#### Scenario: Percentage
- **WHEN** the user types "15% of 240"
- **THEN** the result shows "36"

#### Scenario: Invalid expression
- **WHEN** the user types "12*"
- **THEN** no quick answer is shown and no error is displayed

### Requirement: Unit conversions are answered inline
The provider SHALL convert between common units of length, mass, temperature, volume, speed, data size and time using natural phrasing.

#### Scenario: Natural phrasing
- **WHEN** the user types "5 km in miles"
- **THEN** the result shows "3.11 mi"

#### Scenario: Temperature
- **WHEN** the user types "72f to c"
- **THEN** the result shows "22.2 °C"

### Requirement: Quick answers never delay other results
Quick answers SHALL be computed within the same frame as the keystroke and SHALL not depend on any other provider.

#### Scenario: Quick answer with slow providers
- **WHEN** the user types "2+2" while file search is still running
- **THEN** "4" is visible immediately
