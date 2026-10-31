## ADDED Requirements

### Requirement: Shows for kids or grown-ups by time of day
A show MAY be marked for kids or grown-ups, and a TV's list MAY have a schedule with the times kids' mode and grown-up mode start (design.md D1, D2). With a schedule, the launcher SHALL show only the shows for the current mode plus unmarked shows. Without a schedule it SHALL show every show and no mode button.

#### Scenario: Daytime
- **WHEN** the schedule is kids 05:00, grown-ups 19:00 and it is 15:00
- **THEN** kids' and unmarked shows are shown, and grown-up shows are not

#### Scenario: Evening
- **WHEN** it is 21:00 or 03:00 with the same schedule
- **THEN** grown-up and unmarked shows are shown, and kids' shows are not

#### Scenario: Switch while the home screen is open
- **WHEN** the home screen is open at 18:59 and stays open
- **THEN** at 19:00 the rows change to grown-up mode without any input

#### Scenario: Row with nothing in this mode
- **WHEN** none of an app's shows are for the current mode
- **THEN** its row is hidden and the app shows in Favorites if it is a favorite

### Requirement: Override until the next switch
The header SHALL have a button that shows the current mode. Pressing it SHALL switch to the other mode until the next scheduled switch; pressing it again SHALL return to the scheduled mode (design.md D3).

#### Scenario: Grown-up shows during the day
- **WHEN** it is 15:00 and the button is pressed
- **THEN** grown-up shows are shown until 19:00, after which the schedule applies again

#### Scenario: Kids' shows in the evening
- **WHEN** it is 20:00 and the button is pressed
- **THEN** kids' shows are shown until 05:00 the next morning

#### Scenario: Launcher restarted
- **WHEN** the launcher process restarts during an override
- **THEN** the scheduled mode applies
