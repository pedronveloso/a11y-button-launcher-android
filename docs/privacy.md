---
layout: default
title: Privacy Policy
permalink: /privacy/
---

# Privacy Policy

**A11Y Button Launcher** is an open-source Android app developed by Pedro Veloso and licensed under the [Apache License 2.0](https://github.com/pedronveloso/a11y-button-launcher-android/blob/main/LICENSE).

**Effective date:** October 5, 2026

---

## Summary

A11Y Button Launcher does not collect, transmit, or share any personal data. Everything the app stores stays on your device.

---

## What the app does

A11Y Button Launcher lets you assign any installed app, or one of an app's shortcuts (for example, composing a new message), to the Android accessibility button shortcut. When you press the accessibility button, the app or app shortcut you selected is launched.

---

## Data collected

**None.** The app does not collect, transmit, or share any personal information.

### Data stored locally on your device

The app saves a small set of preferences using Android's DataStore, which lives entirely on your device. This includes the app or app shortcut you picked: for a shortcut, its package name, identifier, display name, and the launch intent.

None of this data ever leaves your device. There are no servers, no cloud sync, and no third-party SDKs that collect data.

### In-app logs (optional)

You can turn on in-app logging in the app's settings, for troubleshooting. It is off by default in release builds. While on, the app saves its own log messages on your device, including across crashes, with sensitive values such as URLs, email addresses and credentials redacted. Turning logging off deletes the saved logs. Logs leave your device only if you choose to copy or share them from the log viewer.
---

## Permissions

### Accessibility service (`BIND_ACCESSIBILITY_SERVICE`)

This permission is required to register a callback for the Android accessibility button. The service is configured to receive **no accessibility events**, it does not observe, read, or interact with the content of any app or screen. It only responds to button presses.

### Notifications (`POST_NOTIFICATIONS`)

Used to send an optional reminder notification when the accessibility service is not running. You can opt out at any time through the in-app settings or your device's notification settings.

### Battery optimization exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)

Used only to ask Android to stop restricting the app's battery use, so the system does not put the accessibility service to sleep and stop the button from working. The request is shown by Android itself and you can decline it. The app does not read your battery state or any other battery data.

### Installed apps and app shortcuts query

Android requires apps to declare `<queries>` entries to see other apps. A11Y Button Launcher declares two:

- **Launchable apps** (`MAIN` / `LAUNCHER`). The list is used only to let you pick an app to assign to the accessibility button.
- **Create-shortcut screens** (`CREATE_SHORTCUT`). These are used to find apps that can create a shortcut for you to pick.

To list an app's shortcuts, the app reads the static shortcut definitions other apps publish (their `shortcuts.xml` metadata) and the display names stored in those apps' resources. It can also open an app's own create-shortcut screen when you ask it to. It does not read pinned or dynamic shortcuts, and it does not read any content inside other apps.

None of this information is transmitted anywhere. Only the single app or shortcut you pick is saved, on your device.

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
