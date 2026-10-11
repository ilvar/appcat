# AppCat

A small Android app organiser inspired by Glextor. Android 8.0+; package `pw.rkd.appcat`.

- Material 3 app bar, Categories/Apps tabs, compact category cards with overflow actions, theme-aware light/dark colors, and icon close controls.
- 30 bundled Material Icons Rounded choices; tap a category’s overflow menu → Change icon. Default categories have matching icons and pinned shortcuts update when their icon changes.
- Optional **Show only on the home screen** mode requests Android Usage Access, then tracks activity transitions locally to show the bubble only over the default launcher. Unknown foreground state, revoked access, locked screen and other apps hide it. The foreground service remains running; visibility updates about every 750 ms and device event reporting may add delay. Usage events are never sent to the classifier.
- Category app views use responsive launcher-style icon grids in both pinned shortcuts and the floating picker.
- Lists installed apps with launchable activities in the current Android profile (including launchable system apps). Background-only packages and separate work profiles are not included.
- Reads Android's `ApplicationInfo.category` first. Apps without a category can be assigned through OpenRouter **System One**, not chat completions. “Other” is excluded from AI choices but remains available manually. Category purpose descriptions distinguish utilities and communication from office productivity.
- Edit any app's category, create custom categories, and rename categories. Manual assignments and completed AI results persist across rescans and restarts.
- Once all apps have a category, each category's **Add to home** button requests a pinned shortcut. Android asks for launcher confirmation; tapping it opens the current category app list.
- Optionally enable a draggable floating side circle. Tap or long press to choose a category, then an app. The picker itself is an overlay. The ongoing notification has a Stop action.

## Use

1. Open AppCat: Android categories are imported automatically.
2. Settings: enter an OpenRouter API key. Defaults: `https://openrouter.ai/api/v1/systemone`, model `typesafe/jev-1.13`. The URL is the **full endpoint**, and custom endpoints must support the same System One request/response format over HTTPS. Model must support System One typed choices.
3. Tap **Categorise missing apps**. App names/package IDs, category definitions, available Android descriptions and Google Play context are sent. Google Play lookup is enabled by default and can be turned off in Settings. It uses the exact package listing, validates package identity, and extracts a bounded full description and store category. Context is cached for 7 days; failures for 1 hour. Missing/blocked listings fall back to local app information. Requests incur your provider's normal charges. Each result is saved immediately; retry resumes missing apps. Stop interrupts the job. Rotation/navigation does not restart it; process termination requires pressing Categorise again.
4. **Apps → app overflow icon** to change a category, or create a category there and assign it immediately. Tap an app to launch it; long press also opens category editing.
5. **Category overflow menu → Reclassify category** reruns AI for every current member, including Android/manual assignments. It allows the original category as an answer. Failed requests keep existing assignments; edits made during a job win over its results. **App bar overflow → Reset all assignments** (also available in Settings) clears every assignment, including manual ones, while keeping custom categories/shortcuts/settings. Apps stay uncategorised until manually assigned or classified; Android categories are not reimported.
6. Add category shortcuts or enable the floating button in Settings and grant **Display over other apps**. Dragging snaps the circle to the nearest side; its position is saved. Enable/disable the button in Settings, or stop it from the notification. After reboot, open AppCat to restore an enabled bubble.

The API key is encrypted using Android Keystore. Backup is disabled so keys and app inventory are not copied into cloud backups. No developer API key, analytics or app server is included. Google Play lookup is a best-effort public HTML lookup; website layout changes or anti-bot blocks may prevent enrichment. It sends package IDs to Google Play, never the classifier API key. Failed requests leave apps uncategorised and show an error. HTTP 429/5xx receive up to three attempts with bounded backoff; redirects are rejected to avoid forwarding the key.

## Build

JDK 17, Android SDK 35:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions builds and uploads `appcat-debug` on pushes and pull requests. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Release signing is not configured yet. GitHub-hosted debug APKs are for initial testing; stable signing is needed before relying on upgrades across builds.

## Device checks

- Verify launchable system/user apps, Android categories, unknown apps and installing/uninstalling apps (reopen AppCat to refresh).
- Categorise using a real key; rotate, stop, retry; test invalid key/model and network loss. Completed assignments must remain saved.
- Change an AI/Android category manually; rerun AI; manual changes must remain. Rename a pinned category and confirm its shortcut still opens it.
- Pin a category in your launcher; verify its contents track manual app changes and newly installed apps.
- Reset all assignments, rescan/reopen, and confirm they stay uncategorised. Reclassify Productivity: apps may stay or move. Manually edit an app while its request is running; the result must not overwrite that edit.
- Open categories from shortcuts and floating picker: confirm icons form a grid, labels stay readable, scrolling works, and tapping launches the correct app.
- Grant/revoke overlay permission; drag, long press, pick and launch an app; stop via settings/notification. Check Android 8, 13, 14 and 15 behavior.

API references: [System One](https://openrouter.ai/docs/guides/community/typesafe-sdk), [Android categories](https://developer.android.com/reference/android/content/pm/ApplicationInfo#category), [Pinned shortcuts](https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts), [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types).

## Icon credits

Bundled vectors are Google Material Icons Rounded, converted from the official [material-design-icons](https://github.com/google/material-design-icons) source. Apache 2.0 license: `licenses/material-icons-LICENSE.txt`. No external font or runtime icon download is required.

### JSON backups

Settings → Export JSON backup writes a portable, versioned JSON file through Android’s document picker. It includes the endpoint/model, API key, lookup and floating-button preferences/position, category names/icons and all saved package mappings (including explicit unassigned/reset records and apps no longer installed). Save pending settings edits before exporting.

**The exported API key is plaintext. Keep the file private.** Import re-encrypts the key with Android Keystore on the destination device. Settings → Import JSON backup validates the complete file and asks before replacing the saved settings, categories and mappings. Stop categorisation before either operation. Backups are limited to 5 MB. Android permissions, description caches and launcher shortcut placement are not transferred; existing shortcuts are refreshed or disabled if their category was removed.
