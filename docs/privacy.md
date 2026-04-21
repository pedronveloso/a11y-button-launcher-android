---
layout: default
title: Privacy Policy
permalink: /privacy/
---

# Privacy Policy

**A11Y Button Launcher** is an open-source Android app developed by Pedro Veloso and licensed under the [Apache License 2.0](https://github.com/pedronveloso/a11y-button-launcher-android/blob/main/LICENSE).

**Effective date:** April 21, 2026

---

## Summary

A11Y Button Launcher does not collect, transmit, or share any personal data. Everything the app stores stays on your device.

---

## What the app does

A11Y Button Launcher lets you assign any installed app to the Android accessibility button shortcut. When you press the accessibility button, the app you selected is launched.

---

## Data collected

**None.** The app does not collect, transmit, or share any personal information.

### Data stored locally on your device

The app saves a small set of preferences using Android's DataStore, which lives entirely on your device.

None of this data ever leaves your device. There are no servers, no cloud sync, and no third-party SDKs that collect data.
---

## Permissions

### Accessibility service (`BIND_ACCESSIBILITY_SERVICE`)

This permission is required to register a callback for the Android accessibility button. The service is configured to receive **no accessibility events**, it does not observe, read, or interact with the content of any app or screen. It only responds to button presses.

### Notifications (`POST_NOTIFICATIONS`)

Used to send an optional reminder notification when the accessibility service is not running. You can opt out at any time through the in-app settings or your device's notification settings.

### Installed apps query

Android requires apps to declare a `<queries>` intent filter to retrieve the list of launchable apps. This list is used only to let you pick an app to assign to the accessibility button. It is never transmitted anywhere.

---

## Third-party services

None. The app has no network calls, no analytics, no crash reporting, and no advertising SDKs.

---

## Open source

The full source code is publicly available at [github.com/pedronveloso/a11y-button-launcher-android](https://github.com/pedronveloso/a11y-button-launcher-android). You can inspect exactly what the app does.

---

## Changes to this policy

If this policy is updated, the new version will be committed to the repository and the effective date above will be updated.

---

## Contact

If you have questions about this privacy policy, please open an issue on the [GitHub repository](https://github.com/pedronveloso/a11y-button-launcher-android/issues).
