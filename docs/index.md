---
title: Privacy Policy — Double Tap Lock
description: Double Tap Lock collects no personal data, makes no network calls, and contains no analytics or third-party SDKs.
---

# Privacy Policy — Double Tap Lock

**Effective date:** 2026-04-27

This document describes the privacy practices of the Android application **Double Tap Lock** (
package name `io.github.ricardogs98.doubletaplock`), distributed on Google Play.

## TL;DR

The app collects no personal data, sends no data to any server, and contains no analytics,
advertising, crash reporting, or third-party SDKs of any kind. The only data the app stores is a
single image file you choose as your wallpaper, kept locally in app-private storage.

## Data we collect

**None.** The application does not collect, transmit, store, sell, or share any personal or
sensitive user data.

This includes — but is not limited to — the following categories, all of which are explicitly **not
** collected:

- Personal identifiers (name, email, phone number, account IDs)
- Device identifiers (advertising ID, IMEI, MAC, Android ID, installation ID)
- Location (precise or approximate)
- Contacts, calendar, SMS, call logs
- Photos or videos beyond the one image you explicitly pick as wallpaper
- App activity, page views, in-app actions, or interaction analytics
- Crash logs, diagnostics, or performance metrics
- Files on the device or content from other applications
- Microphone or camera input

## Data we store locally

The app persists exactly **one** file in its private internal storage:

- **`wallpaper.jpg`** — the image you select via the Android system Photo Picker, copied into the
  app's private files directory (`filesDir/wallpaper.jpg`). It is used as the live wallpaper
  background. It is never transmitted off the device. It is automatically deleted when you uninstall
  the app.

The app does not use any preferences storage, database, or other persistence mechanism.

## Network access

The app declares **no** network permissions. It cannot access the internet. It does not contact any
remote server, content delivery network, or analytics endpoint.

## Third-party SDKs

**None.** The app does not embed any third-party analytics, advertising, attribution, crash
reporting, A/B testing, push notification, or social SDK. All dependencies are limited to the
AndroidX and Jetpack Compose libraries needed to render the UI.

## Permissions

The app uses the following Android system capabilities:

- **`BIND_WALLPAPER`** — declared on its `WallpaperService` so the system can render Double Tap Lock
  as a live wallpaper. This is not a runtime permission you grant; it is a permission the system
  uses to bind to the service.
- **`BIND_ACCESSIBILITY_SERVICE`** — declared on its `AccessibilityService` so that, with your
  explicit consent, the system can bind to it. The service exists for one purpose only: to invoke
  `GLOBAL_ACTION_LOCK_SCREEN` when you double-tap the home screen.

The accessibility service is configured with:

- `canRetrieveWindowContent="false"` — it cannot read screen contents.
- No accessibility event types — it does not receive accessibility events.
- `canPerformGestures="false"` — it cannot perform gestures.

You can revoke accessibility permission at any time from **Settings &gt; Accessibility &gt; Double
Tap Lock**.

The app uses the Android Photo Picker to let you select a wallpaper image. The Photo Picker does *
*not** require a storage permission — it returns a single user-selected image without granting the
app broad access to your media library.

## Children's privacy

The app does not knowingly collect data from anyone, including children under 13. It contains no
advertising, no profiling, and no account system.

## GDPR / data subject rights

Because the application processes no personal data, the rights granted by the GDPR (access,
rectification, erasure, portability, objection) do not apply — there is no data to access or erase.
You retain full control of the locally stored wallpaper image: replace it from the app, or uninstall
the app to remove it entirely.

## Changes to this policy

If this policy is updated, the effective date at the top of this document will change. Because no
data is collected, no notification mechanism is needed; the latest version will always be available
at the URL hosting this document.

## Contact

For questions about this policy, contact:

- **Source code:** <https://github.com/RicardoGS98/DoubleTapLock>
