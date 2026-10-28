## ADDED Requirements

### Requirement: Weather strip content
When forecast data is available, the home screen header SHALL show a weather strip between the Settings button and the clock. The strip SHALL show today's condition icon, current temperature, today's high and low, and today's maximum chance of rain. It SHALL also show, for each of the next 4 days, the day name, condition icon, and high and low temperature. Temperatures SHALL be whole degrees Celsius, and day names SHALL follow the device locale.

#### Scenario: Forecast available
- **WHEN** a forecast has been fetched
- **THEN** the strip shows today's details and 4 following days

#### Scenario: Not focusable
- **WHEN** the user navigates the home screen with the D-pad
- **THEN** focus never lands on the weather strip

### Requirement: Forecast from Open-Meteo for a fixed location
The launcher SHALL fetch the forecast from Open-Meteo for the latitude and longitude configured at build time, without an API key.

#### Scenario: Location configured
- **WHEN** the launcher is built with a weather location
- **THEN** forecasts are requested for that location

### Requirement: Refresh and offline behaviour
The launcher SHALL fetch the forecast at start and then every 30 minutes while it is in the foreground. It SHALL keep the last successful forecast on the device and show it immediately on start and when offline. Data older than 3 hours SHALL be shown with the time it was fetched. If no forecast has ever been fetched, the strip SHALL be hidden.

#### Scenario: Offline with cache
- **WHEN** the device is offline and a forecast was fetched earlier
- **THEN** the strip shows that forecast

#### Scenario: Stale data
- **WHEN** the newest forecast is more than 3 hours old
- **THEN** the strip shows "updated" with the fetch time

#### Scenario: Never fetched
- **WHEN** no forecast has ever been fetched successfully
- **THEN** no weather strip is shown and the header looks as before

#### Scenario: Returning home
- **WHEN** the user returns home less than 30 minutes after the last fetch
- **THEN** no new request is made
