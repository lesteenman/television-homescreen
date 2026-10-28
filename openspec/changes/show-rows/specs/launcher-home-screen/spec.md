## MODIFIED Requirements

### Requirement: Gradient background with no ads or recommendations
The home screen SHALL use a static gradient background drawn locally. It SHALL NOT show content recommendations, promoted apps or ads. Network access SHALL be limited to user-configured content: the show configuration file and the artwork it references.

#### Scenario: Offline home screen
- **WHEN** the device has no network connection
- **THEN** the home screen renders fully, with the gradient, clock, favorites, cached show rows and grid

#### Scenario: No recommendations
- **WHEN** the home screen is shown
- **THEN** every show tile on it comes from the user's show configuration, and none come from Google or the apps' own recommendations
