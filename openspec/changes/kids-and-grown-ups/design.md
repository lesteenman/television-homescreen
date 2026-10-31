## Context

Each TV reads its own list from a gist (`remote-show-list`). The parser ignores unknown fields, so new optional fields don't break older builds. Rows already disappear when they have no shows, and an app whose row disappears shows up in Favorites again.

## Goals / Non-Goals

**Goals:**
- Kids' shows by day, grown-up shows in the evening, set per TV in its gist file.
- A visible, one-press override that can't get stuck.

**Non-Goals:**
- Protecting grown-up content from the kids: the apps' own profiles and PINs do that.
- Hiding apps or favorites by mode.
- Different schedules per weekday.

## Decisions

### D1. Format
```json
{
  "version": 1,
  "schedule": { "kids": "05:00", "grown-ups": "19:00" },
  "rows": [ { "app": "…", "shows": [ { "title": "…", "link": "…", "for": "kids" } ] } ]
}
```
`for` is `kids` or `grown-ups`; missing means both. An unknown `for` value makes the file invalid, so a typo is noticed (the TV keeps its previous list and logs why). `schedule` needs both times as `HH:mm`; if it's missing, the launcher has no modes. A broken `schedule` makes the file invalid.

### D2. Which mode applies
Kids' mode runs from `kids` up to `grown-ups`, grown-up mode from `grown-ups` up to the next `kids`, wrapping past midnight. Local device time.

### D3. Override until the next switch
The header button shows the mode in effect and switches to the other one. The override stores the instant of the next scheduled switch and ends there; pressing the button again ends it early. It's kept in memory only: a restart of the launcher goes back to the schedule, which is the safe default.

### D4. Switching on time
While the home screen is resumed, a coroutine waits until the next switch (or the end of the override) and recomputes the mode, so an open home screen changes at 19:00 without input. Coming back to the home screen also recomputes it.

### D5. Button placement
Next to Settings and Screensaver, labelled with the current mode. It's only there when the list has a schedule.

## Risks / Trade-offs

- [Kids can press the button] → Accepted; grown-up apps ask for a profile anyway, and the override ends at the next switch.
- [Clock changes, e.g. daylight saving] → Mode is recomputed from local time on every resume and every switch, so it corrects itself.
