## 1. Model

- [x] 1.1 Parse `for` per show and `schedule` per list (D1), with unit tests
- [x] 1.2 Pure schedule logic: mode at a time and the next switch, across midnight (D2), with unit tests
- [x] 1.3 `HomeViewModel`: mode state, override until the next switch, timer while resumed (D3, D4); filter rows by mode

## 2. UI

- [x] 2.1 Header mode button next to Screensaver, only with a schedule (D5)
- [x] 2.2 On-device test: the button switches the rows and its label

## 3. Data

- [x] 3.1 Gist files: schedule 05:00 / 19:00, mark the shows, add Woezel & Pip (kids, Videoland) and Malcolm in the Middle (grown-ups, Disney+)
- [x] 3.2 Bundled `shows.json` gets the same marks, so the fallback behaves alike
- [x] 3.3 README: the new fields

## 4. Verify

- [x] 4.1 Emulator: rows by mode and the button both ways (the automatic switch at a set time is covered by unit tests only)
- [x] 4.2 Living room TV: rows by mode, button, Woezel & Pip and Malcolm links
- [x] 4.3 Kitchen TV: install without interrupting playback (2026-10-31, during Videoland playback; home screen not seen yet)

## Notes

- 2026-10-31: the longer "Grown-ups" label pushed the weather into the clock. The weather now fills the space between the buttons and the clock and drops forecast days that don't fit (`FittingRow` in `WeatherStrip.kt`): four days in Kids mode, three in Grown-ups on 1080p.
- Living room TV: Malcolm opened straight on its page (Disney+ already had a profile); Woezel & Pip opened after Videoland's profile picker.
