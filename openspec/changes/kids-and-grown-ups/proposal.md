## Why

The show rows mix the kids' shows with the parents'. The kids are in bed before 19:00, so the home screen can show only kids' shows during the day and only grown-up shows in the evening, with a button for the exceptions.

## What Changes

- A show in the list can be marked `"for": "kids"` or `"for": "grown-ups"`. Unmarked shows appear in both modes.
- Each TV's list can have a `schedule` with the time kids' mode starts and the time grown-up mode starts, e.g. 05:00 and 19:00. Without a schedule nothing changes: every show is shown and there is no mode button.
- The header gets a button showing the current mode ("Kids" / "Grown-ups"). Pressing it switches to the other mode until the next scheduled switch, when the schedule takes over again.
- A row that has no shows in the current mode is hidden, and its app shows as a normal favorite again.
- The format stays version 1: older builds ignore the new fields and show everything.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `show-rows`: shows can be marked for an audience, and rows follow the TV's schedule and the override.

## Impact

- **Depends on** `show-rows` and `remote-show-list` being archived first.
- Code: `ShowConfig` (parse `for` and `schedule`), a small pure schedule model, `HomeViewModel` (mode, override and a timer for the switch), `HomeScreen` header button.
- Data: both gist files get marks and a schedule.
