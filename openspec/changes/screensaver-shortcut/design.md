## Context

On the kitchen Chromecast HD (Google TV, Android 14) the screensaver is Ambient (`com.google.android.apps.tv.dreamx/.service.Backdrop`), starting after 10 minutes. Android's usual "start screensaver now" activity (SystemUI's Somnambulator) doesn't exist on this device, and `DreamManager` can't be called by normal apps.

## Goals / Non-Goals

**Goals:**
- Start the normal Ambient screensaver from the home screen with the header button or BACK.
- No accidental starts.

**Non-Goals:**
- Our own screensaver (DreamService), a shorter idle timeout, or changing Ambient's settings.

## Decisions

### D1: Start it through the Settings app's sleep voice action
- `Intent("com.google.android.pano.action.SLEEP")` resolves to `com.android.tv.settings/.device.display.daydream.DaydreamVoiceAction`, which starts the configured screensaver. In the manifest of `TvSettingsTwoPanel.apk` on the TV it is `exported=true` with no permission (checked 2026-10-28), and starting it from adb started Ambient (`dumpsys dreams` showed `mCurrentDream=…dreamx…Backdrop`).
- Alternatives: `cmd dreams` needs root; Somnambulator isn't on Google TV; `DreamManager.startDream` is a system API.
- The launcher checks once whether the action resolves (needs a `<queries>` entry). If it doesn't, the button is hidden and BACK does nothing, as before.

### D2: Header button
- A second button next to Settings, same style, labelled "Screensaver". From the first row, UP then LEFT/RIGHT reaches it.

### D3: BACK, without accidental starts
- BACK starts the screensaver only for a fresh press (`repeatCount == 0`) and only when the home screen has been resumed for at least 1.5 seconds. This ignores the extra BACK presses people use to leave an app, which can land on the home screen right after it appears.
- With the long-press menu open, BACK still just closes the menu (the dialog handles it first).

### Verification (kitchen TV, 2026-10-28)
- Header button: Ambient started. BACK on the home screen after it settled: Ambient started. Three BACKs 0.3 s apart to leave Settings: back on the home screen, no screensaver. BACK used to wake Ambient: back home, no restart.
- A D-pad key sent with `adb shell input` did not wake Ambient, but BACK did; a real remote wasn't tried for this.

## Risks / Trade-offs

- [The intent is undocumented and could disappear in an update] → Both entry points hide or disable themselves (D1); voice still works.
- [BACK is pressed by habit] → The 1.5-second guard covers leaving apps. If it still starts too often, BACK can be limited to a double press.

## Open Questions

- None.
