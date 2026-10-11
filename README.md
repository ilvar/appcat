# AppCat

A small Android app organiser inspired by Glextor. Android 8.0+; package `pw.rkd.appcat`.

- Lists installed apps with launchable activities in the current Android profile (including launchable system apps). Background-only packages and separate work profiles are not included.
- Reads Android's `ApplicationInfo.category` first. Apps without a category can be assigned through OpenRouter **System One**, not chat completions.
- Edit any app's category, create custom categories, and rename categories. Manual assignments and completed AI results persist across rescans and restarts.
- Once all apps have a category, each category's **Add to home** button requests a pinned shortcut. Android asks for launcher confirmation; tapping it opens the current category app list.
- Optionally enable a draggable floating side circle. Tap or long press to choose a category, then an app. The picker itself is an overlay. The ongoing notification has a Stop action.

## Use

1. Open AppCat: Android categories are imported automatically.
2. Settings: enter an OpenRouter API key. Defaults: `https://openrouter.ai/api/v1/systemone`, model `typesafe/jev-1.13`. The URL is the **full endpoint**, and custom endpoints must support the same System One request/response format over HTTPS. Model must support System One typed choices.
3. Tap **Categorise missing apps**. Only app names/package IDs for uncategorised apps and category names are sent. Requests incur your provider's normal charges. Each result is saved immediately; retry resumes missing apps. Stop interrupts the job. Rotation/navigation does not restart it; process termination requires pressing Categorise again.
4. **All apps → Edit** to change a category, or create a category there and assign it immediately. Tap an app to launch it; long press also opens category editing.
5. Add category shortcuts or enable the floating button in Settings and grant **Display over other apps**. Dragging snaps the circle to the nearest side; its position is saved. Enable/disable the button in Settings, or stop it from the notification. After reboot, open AppCat to restore an enabled bubble.

The API key is encrypted using Android Keystore. Backup is disabled so keys and app inventory are not copied into cloud backups. No developer API key, analytics or app server is included. Failed requests leave apps uncategorised and show an error. HTTP 429/5xx receive up to three attempts with bounded backoff; redirects are rejected to avoid forwarding the key.

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
- Grant/revoke overlay permission; drag, long press, pick and launch an app; stop via settings/notification. Check Android 8, 13, 14 and 15 behavior.

API references: [System One](https://openrouter.ai/docs/guides/community/typesafe-sdk), [Android categories](https://developer.android.com/reference/android/content/pm/ApplicationInfo#category), [Pinned shortcuts](https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts), [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types).
