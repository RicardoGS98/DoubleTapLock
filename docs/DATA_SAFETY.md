# Play Console — Data Safety Form (Draft)

Draft answers for the Google Play **Data Safety** form. Copy each answer into the corresponding Play
Console question.

## Section 1 — Data collection and security

| Question                                                              | Answer                                                                            |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| Does your app collect or share any of the required user data types?   | **No**                                                                            |
| Is all of the user data collected by your app encrypted in transit?   | **N/A** (no data is collected or transmitted)                                     |
| Do you provide a way for users to request that their data be deleted? | **N/A** (no data is collected; uninstalling removes the only locally stored file) |

## Section 2 — Data types

For every category Google lists, the answer is **No data collected** and **No data shared**:

- Personal info (name, email, address, phone, identifiers, other) — **No**
- Financial info — **No**
- Health and fitness — **No**
- Messages — **No**
- Photos and videos — **No** (the wallpaper image is selected via the Android Photo Picker, copied
  into app-private storage, and never transmitted; per Play's definition this is not "collection"
  because no data leaves the device)
- Audio files — **No**
- Files and docs — **No**
- Calendar — **No**
- Contacts — **No**
- App activity — **No**
- Web browsing — **No**
- App info and performance (crash logs, diagnostics, other) — **No**
- Device or other identifiers — **No**

## Section 3 — Security practices

| Question                                                                      | Answer                                                                                                                                                     |
|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Is data encrypted in transit using a secure protocol?                         | **N/A — no data is transmitted.**                                                                                                                          |
| Does your app follow Google Play's Families Policy?                           | _Check with Play Console; this app is not aimed at children but contains no advertising, in-app purchases, or data collection, so it complies regardless._ |
| Has your app been independently validated against a global security standard? | **No.**                                                                                                                                                    |

## Section 4 — Why "No" everywhere is correct

- The app declares **no `INTERNET` permission**, so it physically cannot transmit data.
- The bundled AccessibilityService is configured with `canRetrieveWindowContent="false"`, no event
  types, and `canPerformGestures="false"`, so it cannot harvest screen content, input, or gestures.
- The codebase contains **no analytics SDK, no crash reporter, no advertising SDK, and no
  third-party network library**. All dependencies are AndroidX/Compose UI libraries.
- The only persisted file is `filesDir/wallpaper.jpg`, which the user explicitly selected via the
  system Photo Picker. It is never copied off the device.
- Wallpaper image selection uses the **Android Photo Picker** (
  `ActivityResultContracts.PickVisualMedia`), which does not require a storage permission.

## Section 5 — Data deletion

Uninstalling the app removes its private storage, including `wallpaper.jpg`. No server-side deletion
is needed because nothing is stored server-side.

## Section 6 — Privacy Policy URL

Host `PRIVACY_POLICY.md` (or `docs/PRIVACY_POLICY.md`) on GitHub Pages and use that public URL in
the Privacy Policy field of the listing. Suggested URL pattern once Pages is enabled:

```
https://ricardogs98.github.io/DoubleTapLock/privacy-policy.html
```
