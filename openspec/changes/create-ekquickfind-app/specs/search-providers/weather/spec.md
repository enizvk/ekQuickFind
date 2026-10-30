## ADDED Requirements

### Requirement: Weather card
The system SHALL show a live weather card at the top for "weather sofia", "sofia weather", "forecast sofia" or the weather keyword ("w sofia"), with the current temperature and conditions, the place, and today's high and low, from Open-Meteo (credited on the card). "weather" with no place uses the phone's approximate location; without the location permission a row asks for it and suggests typing a place. Units follow the region (°F in the US) unless set to °C or °F. Each place's answer is kept for 15 minutes; nothing is asked for until weather is searched, and the card can be turned off. Tapping it searches the web for that place's weather.

#### Scenario: Weather for a city
- **WHEN** the user types "weather sofia"
- **THEN** a card like "14° Overcast · Sofia, High 17° · Low 9° · Open-Meteo" is the best match
