## MODIFIED Requirements

### Requirement: Gradient background with no ads or recommendations
The home screen SHALL use a static gradient background drawn locally. It SHALL NOT show content recommendations, promoted apps or ads. Network access SHALL be limited to the weather forecast for the configured location.

#### Scenario: Offline home screen
- **WHEN** the device has no network connection
- **THEN** the home screen renders fully, with the gradient, clock, cached weather (if any), favorites and grid

#### Scenario: No recommendations
- **WHEN** the home screen is shown
- **THEN** nothing on it is a recommendation or ad
