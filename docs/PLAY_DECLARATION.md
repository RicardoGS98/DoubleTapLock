# Play Console — AccessibilityService Permission Declaration (Draft)

This document contains draft answers for the Google Play Console **Permission Declaration Form**
that must be completed because Double Tap Lock uses `BIND_ACCESSIBILITY_SERVICE` without setting
`android:isAccessibilityTool="true"`. Copy each section into the corresponding field in Play
Console.

---

## App identity

- **App name:** Double Tap Lock
- **Package:** `com.doubletaplock.app`
- **AccessibilityService class:** `com.doubletaplock.app.service.LockAccessibilityService`
- **`isAccessibilityTool` flag:** `false` (intentionally — this app is not advertised as an
  accessibility tool; it uses the API only as the system-provided way to invoke the lock action).

---

## 1. What is the core functionality of your app?

A live wallpaper that locks the device when the user double-taps an empty area of the home screen.
The double-tap gesture is detected by the wallpaper engine via `WallpaperManager.COMMAND_TAP`. The
lock action is performed via `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`.

## 2. Why does your app require the AccessibilityService API?

`GLOBAL_ACTION_LOCK_SCREEN` is the only public Android API that lets a third-party app trigger the
system's lock action. There is no equivalent in `WindowManager`, `KeyguardManager`, or any other
publicly available API.

The only alternative — `DevicePolicyManager.lockNow()` — has two disqualifying problems for this
app:

1. It is restricted to apps registered as **device administrators** (or fully managed device
   owners), which is a far more invasive permission, with stronger UX warnings, intended for MDM
   scenarios.
2. It transitions the device to a stronger lock state that **disables biometric unlock** on
   subsequent unlock attempts. This breaks the fundamental UX of a one-tap convenience locker.

`AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` is therefore the correct and
only viable public API for this feature.

## 3. What functionality does the AccessibilityService provide?

Exactly one action: when invoked from the wallpaper service after a double-tap is detected, the
AccessibilityService calls `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` to lock the screen.
Nothing else.

## 4. What data does the AccessibilityService collect or transmit?

**None.** The service is configured with:

| Attribute                          | Value        | Effect                                            |
|------------------------------------|--------------|---------------------------------------------------|
| `android:canRetrieveWindowContent` | `false`      | Cannot read on-screen text or UI structure        |
| `android:accessibilityEventTypes`  | `""` (empty) | Does not subscribe to any accessibility event     |
| `android:canPerformGestures`       | `false`      | Cannot inject touch gestures                      |
| `android:notificationTimeout`      | `100`        | Required field; no events are received regardless |

The `LockAccessibilityService` class does **not** override `onAccessibilityEvent` to process events;
the only override is `onInterrupt` (a no-op required by the abstract class). It exposes a single
static method, `lockNow()`, that the wallpaper service calls on a double tap. The codebase contains
no networking layer, no analytics, no crash reporter, and no third-party SDK.

## 5. How does the app obtain user consent for AccessibilityService access?

A full-screen prominent disclosure (`AccessibilityDisclosureScreen`) is displayed in-app **before**
the user is redirected to system Accessibility Settings. It enumerates five points clearly, in the
user's language (English or Spanish):

1. The app uses the Android Accessibility Service.
2. Why this API: `GLOBAL_ACTION_LOCK_SCREEN` is the only public API that can lock the device, and
   the alternative `DevicePolicyManager.lockNow()` is restricted to device-admin scenarios and
   breaks biometric unlock.
3. What the service does: invokes the system lock action when the user double-taps the home screen.
4. What the service does NOT do: does not read screen content, does not listen to accessibility
   events, does not track input, does not access other apps, does not use the network, and does not
   collect or share any data.
5. The user can revoke accessibility permission at any time in system Settings.

The disclosure screen has two actions: **Continue to Settings** (proceeds to
`Settings.ACTION_ACCESSIBILITY_SETTINGS`) and **Cancel** (dismisses with no side effects).

The disclosure is shown every time the user attempts to enable accessibility while the permission is
not granted; it is not bypassed by any persisted "consent" flag.

## 6. Where in the app is the AccessibilityService usage visible?

The first onboarding step on the main screen is labeled **"Enable lock service (Accessibility)"**,
making the use of the Accessibility Service explicit on the very first screen the user sees. Tapping
that step opens the prominent-disclosure screen described above.

## 7. Is the app distributed only to users who explicitly need accessibility tools?

No — the app is for general consumers. This is exactly why `isAccessibilityTool="true"` is **not**
set: this app is not an accessibility tool. It uses the Accessibility API solely because Android
offers no other public API for the desired functionality.

---

## Reviewer video — checklist for the developer

Record a short screen capture (around 60–90 seconds) demonstrating, in order:

1. Open the app on a fresh installation.
2. Show the main screen with three onboarding steps; pause briefly on **"Enable lock service (
   Accessibility)"**.
3. Tap that step; the prominent disclosure screen appears full-screen. **Slowly scroll through all
   five points so each is fully readable.**
4. Tap **Continue to Settings** — the system Accessibility Settings open.
5. Enable Double Tap Lock; return to the app; show that step 1 is now green.
6. Show one more run-through of the disclosure flow ending with **Cancel**, demonstrating the
   rejection path: nothing changes, no permission is granted.

Upload the video to YouTube as **unlisted** and paste the URL in the corresponding Play Console
field.

---

## Data Safety form — short version

See `DATA_SAFETY.md` for the full Data Safety form draft. Summary: every category is answered as *
*No data collected / No data shared**.
