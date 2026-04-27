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
./gradlew installDebug       # install to a connected device/emulator
./gradlew lint               # static analysis
./gradlew bundleRelease      # signed AAB at app/build/outputs/bundle/release/app-release.aab
```

If `keystore.properties` is **not** present at the repo root, the release build falls back to `~/.android/debug.keystore` (the `releaseDebugSigned` config) so contributors can still build and sideload locally. That fallback build is **not** suitable for Play Store upload — see the [Publishing](#publishing) section below for the production signing flow.

Toolchain: Android Gradle Plugin via version catalog, Kotlin + Compose, Java 17, `compileSdk` 36, `targetSdk` 35, `minSdk` 31.

## Architecture

Single-activity app. Three pieces cooperate:

| Component                                | Responsibility                                                                                                                         |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `DoubleTapWallpaperService` (`service/`) | `WallpaperService` engine. Listens for `COMMAND_TAP`, draws the wallpaper, handles offset panning. Glue only — logic lives in helpers. |
| `LockAccessibilityService` (`service/`)  | Empty accessibility service whose only job is to expose a live handle for `GLOBAL_ACTION_LOCK_SCREEN`.                                 |
| `MainScreen` (`ui/`)                     | 3-step onboarding Compose UI; status is queried from system state on every `ON_RESUME`, never persisted locally.                       |

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

## Publishing

This section is for the human shipping the app to Google Play.

### 1. Generate a release keystore (once, locally)

The keystore is **never** committed. Run on your machine:

```bash
keytool -genkeypair -v \
    -keystore release.keystore \
    -alias doubletaplock \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -storetype PKCS12
```

Place `release.keystore` at the repo root (the same directory as `keystore.properties.example`), or anywhere else and update the path below.

### 2. Create `keystore.properties`

Copy the template and fill in the values:

```bash
cp keystore.properties.example keystore.properties
```

```properties
storeFile=../release.keystore
storePassword=<your keystore password>
keyAlias=doubletaplock
keyPassword=<your key password>
```

`keystore.properties` and any `*.keystore` are `.gitignore`'d. Verify with `git status` before committing.

### 3. Build the release AAB

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`. Resource shrinking and R8 are enabled for the release build type.

### 4. Upload to Play Console

- Upload the AAB. **Enrol in Play App Signing on first upload** so Google holds the upload key on your behalf.
- Fill the [Permission Declaration Form](docs/PLAY_DECLARATION.md) — Double Tap Lock uses `BIND_ACCESSIBILITY_SERVICE` without `isAccessibilityTool="true"`, so a declaration plus a video of the prominent-disclosure flow is required. Reference: <https://support.google.com/googleplay/android-developer/answer/10964491>.
- Fill the [Data Safety form](docs/DATA_SAFETY.md) — every category answered as **No data collected / No data shared**.
- Set the Privacy Policy URL to your hosted copy of [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md). Enable GitHub Pages on this repo with `docs/` as the source; the policy will be served at `https://ricardogs98.github.io/DoubleTapLock/PRIVACY_POLICY` (paste this URL in the Play Console privacy-policy field).

## Versioning

Semantic-ish: `versionName` increments tracked in `app/build.gradle.kts`. `versionCode` is monotonic.

| Version | Changes                                                                                                                                                                                                                                                                            |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.2.0   | Prominent-disclosure flow before granting accessibility permission, Play-suitable signing config (`keystore.properties`), `compileSdk` bumped to 36 (`targetSdk` stays at 35 per current Play policy), Play submission docs (privacy policy, permission declaration, data safety). |
| 1.1.1   | Smoother lock transition: paint a solid black frame and suppress redraws between the double tap and the system lock animation, eliminating the brief launcher flash.                                                                                                               |
| 1.1.0   | Scrolling/panning wallpaper, hardware canvas rendering, modular `service/wallpaper/` package, i18n (en/es).                                                                                                                                                                        |
| 1.0     | Initial release: double-tap-to-lock, custom static wallpaper.                                                                                                                                                                                                                      |

## License

No license declared. Personal project.
