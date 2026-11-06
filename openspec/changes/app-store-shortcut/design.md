## Context

`AppRepository` lists every activity that resolves MAIN + LEANBACK_LAUNCHER. The Play Store (`com.android.vending`) isn't in that list on either TV, so on Google TV it apparently has no such entry. The stock launcher opens it from its Apps tab instead, and our accessibility redirect hides that screen.

Not yet checked on a TV: which activities `com.android.vending` exports there. To check:
`adb shell cmd package query-activities -a android.intent.action.MAIN --components com.android.vending`.

## Goals / Non-Goals

**Goals:**
- One obvious way to open the store and install an app.

**Non-Goals:**
- Making the store a favorite, or listing store recommendations.

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

### D3: Its own tile at the end of All apps
- Decided 2026-11-06 (Erik): not in the header; a tile below all apps.
- The tile uses the banner, icon and label of the activity the store intent resolves to, like any app, or the label "Play Store" when that's unknown (a selector the system can't resolve to one activity).
- Resolved on every resume with the app list, so it appears after a store is installed or enabled. Cached while it resolves to the same activity.
- Long press does nothing: the store isn't in the app list, so it couldn't be shown as a favorite.
- Left out if the store does appear in All apps on some device, so it isn't shown twice.

## Risks / Trade-offs

- [The Play Store's TV activity might hand off to the stock launcher's Apps tab, which our redirect would then bounce back home] → Check on the kitchen TV first. If it does, the redirect only matches `launcherx.home.*`, so it would need an exception.
- [Step 4 opens an empty search] → Only used when 1–3 fail; the user can search from there.

## Open Questions

- Which step resolves on the TVs (see Context).
