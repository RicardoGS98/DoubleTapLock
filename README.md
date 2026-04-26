# Double Tap Lock

Android live wallpaper that locks the device when you double-tap an empty area of the home screen.

The app does not collect, transmit, or persist anything beyond the single image you pick as the wallpaper. No analytics, no network calls, no preferences storage.

## Features

- **Double tap to lock** — two taps within 300 ms on empty home-screen area lock the device.
- **Custom wallpaper** — pick any photo from your gallery; updates live (no need to re-set the wallpaper).
- **Scrolling background** — wide images pan smoothly across the launcher's home pages, driven by `Choreographer` at display refresh rate and rendered through a hardware canvas.
- **Localized** — UI follows the device language (English default, Spanish when the system locale is `es`).
- **Privacy by design** — the bundled accessibility service has `canRetrieveWindowContent="false"` and listens to no events; it only exposes a handle for `GLOBAL_ACTION_LOCK_SCREEN`.

## Requirements

- Android 12 (API 31) or higher.
- A launcher that forwards taps to the wallpaper engine via `WallpaperManager.COMMAND_TAP`. Pixel Launcher does this; some third-party launchers may not.

## Setup

After installing the app, complete three one-time steps from the main screen:

1. **Enable the lock service** — grants the accessibility service that performs the lock action.
2. **Pick a background image** — any photo; panoramic images get the smooth horizontal pan.
3. **Activate the live wallpaper** — sets Double Tap Lock as your wallpaper.

The double-tap-to-lock gesture is active as soon as all three steps are green.

## Build

```bash
./gradlew assembleDebug      # debug APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # release APK signed with the local debug keystore
./gradlew installDebug       # install to a connected device/emulator
./gradlew lint               # static analysis
```

The release config is signed with `~/.android/debug.keystore` (`releaseDebugSigned`) — convenient for sideloading, **not** suitable for Play Store distribution.

Toolchain: Android Gradle Plugin via version catalog, Kotlin + Compose, Java 17, `compileSdk` 35, `minSdk` 31.

## Architecture

Single-activity app. Three pieces cooperate:

| Component | Responsibility |
|---|---|
| `DoubleTapWallpaperService` (`service/`) | `WallpaperService` engine. Listens for `COMMAND_TAP`, draws the wallpaper, handles offset panning. Glue only — logic lives in helpers. |
| `LockAccessibilityService` (`service/`) | Empty accessibility service whose only job is to expose a live handle for `GLOBAL_ACTION_LOCK_SCREEN`. |
| `MainScreen` (`ui/`) | 3-step onboarding Compose UI; status is queried from system state on every `ON_RESUME`, never persisted locally. |

Wallpaper helpers (`service/wallpaper/`):

| File | Responsibility |
|---|---|
| `BitmapLoader.kt` | Off-thread bitmap decoding with `inSampleSize` + `RGB_565`. |
| `DoubleTapDetector.kt` | Stateful `onTap(now): Boolean` gesture detector. |
| `OffsetAnimator.kt` | `Choreographer`-driven exponential follower for smooth panning between sparse `onOffsetsChanged` callbacks. |
| `WallpaperRenderer.kt` | Cover-fit + horizontal pan composition. |

There is no DI, no DataStore/Room, no `Application` class. State lives in: (a) `filesDir/wallpaper.jpg`, (b) Android system settings, (c) the bound accessibility service instance.

### Why the wallpaper service?

`WallpaperManager.COMMAND_TAP` is the cheapest way to receive tap signals on empty home-screen area: the launcher forwards them, we don't enable raw touch events. The trade-off is launcher dependence — if your launcher doesn't forward taps, the gesture won't fire.

## Versioning

Semantic-ish: `versionName` increments tracked in `app/build.gradle.kts`. `versionCode` is monotonic.

| Version | Changes |
|---|---|
| 1.1.1 | Smoother lock transition: paint a solid black frame and suppress redraws between the double tap and the system lock animation, eliminating the brief launcher flash. |
| 1.1.0 | Scrolling/panning wallpaper, hardware canvas rendering, modular `service/wallpaper/` package, i18n (en/es). |
| 1.0   | Initial release: double-tap-to-lock, custom static wallpaper. |

## License

No license declared. Personal project.
