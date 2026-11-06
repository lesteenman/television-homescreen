## Context

`AppRepository` lists every activity that resolves MAIN + LEANBACK_LAUNCHER. The Play Store (`com.android.vending`) isn't in that list on either TV, so on Google TV it apparently has no such entry. The stock launcher opens it from its Apps tab instead, and our accessibility redirect hides that screen.

Not yet checked on a TV: which activities `com.android.vending` exports there. To check:
`adb shell cmd package query-activities -a android.intent.action.MAIN --components com.android.vending`.

## Goals / Non-Goals

**Goals:**
- One obvious way to open the store and install an app.

**Non-Goals:**
- Showing the store as a tile in All apps, or listing store recommendations.

## Decisions

### D1: Fallback chain
`AppStore.intent` returns the first of these that resolves:
1. The store's MAIN + LEANBACK_LAUNCHER activity, as an explicit component.
2. Its MAIN + LAUNCHER activity, as an explicit component.
3. `Intent.makeMainSelectorActivity(MAIN, APP_MARKET)`, the documented way to open "the app store".
4. `market://search?q=` sent to the Play Store, which opens its search.

1 and 2 are resolved to explicit components because launcher activities usually lack CATEGORY_DEFAULT, so an implicit intent with only the package set would not start.

### D2: Package visibility
The manifest's `<queries>` only covered LEANBACK_LAUNCHER intents and the sleep action, so the Play Store's other activities were invisible to us. Added `<package android:name="com.android.vending"/>` and an intent for MAIN + APP_MARKET.

### D3: Header button
Same as the Screensaver button (screensaver-shortcut D2): a third header button, labelled "Play Store", hidden when `AppStore.intent` is null. Resolved once in `onCreate`.

## Risks / Trade-offs

- [The Play Store's TV activity might hand off to the stock launcher's Apps tab, which our redirect would then bounce back home] → Check on the kitchen TV first. If it does, the redirect only matches `launcherx.home.*`, so it would need an exception.
- [Step 4 opens an empty search] → Only used when 1–3 fail; the user can search from there.

## Open Questions

- Which step resolves on the TVs (see Context).
