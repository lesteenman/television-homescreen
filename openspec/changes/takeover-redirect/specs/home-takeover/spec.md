## MODIFIED Requirements

### Requirement: Approaches tried in order E, B, C
The takeover SHALL be attempted on the kitchen device in this order: E (assign the HOME role), then B (disable the stock launcher, plus setupwraith if needed), then C (accessibility watchdog). Work SHALL stop at the first approach that meets the stickiness criteria. If an approach breaks features of other system apps that depend on the stock packages, the next approach SHALL replace it, with the stock packages enabled again. The result of each attempt SHALL be recorded in `design.md`.

#### Scenario: E succeeds
- **WHEN** approach E meets the stickiness criteria
- **THEN** approaches B and C are not applied to the device

#### Scenario: E fails
- **WHEN** after approach E, HOME still opens the stock launcher or the role reverts
- **THEN** the device is restored to stock and approach B is tried

#### Scenario: B breaks other apps
- **WHEN** with approach B applied, system features that depend on launcherx stop working (such as Google Photos in Ambient, or the device in Google Home)
- **THEN** launcherx and setupwraith are enabled again and approach C is applied instead
