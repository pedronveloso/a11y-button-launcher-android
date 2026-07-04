# PRODUCT.md — Accessibility Button Launcher

Accessibility Button Launcher is a small open-source Android app that maps Android's built-in accessibility button/shortcut to one app of the user's choosing. Press the system trigger, the chosen app opens. No overlays, no custom launcher, no automation engine.

- Source: <https://github.com/pedronveloso/a11y-button-launcher-android>
- F-Droid: <https://f-droid.org/en/packages/com.pedronveloso.a11ybutton/>

## Problem

Android exposes an accessibility button and accessibility shortcut, but almost all apps use that trigger only for their own accessibility services. This app repurposes the trigger as a lightweight, system-native launcher for one app the user cares about.

## Who it's for

- Anyone who wants one important app reachable from anywhere via a native system trigger (communication, notes, translation, reading, or assistive apps).
- Users on OEM skins with restricted triggers. Example: Xiaomi/MIUI users on custom launchers often can't invoke Google Assistant via the usual gestures — this app gives them a low-friction system shortcut that launches Assistant (or any app) reliably.
- Family members who need a single, simple shortcut to one app.
- People who'd rather reuse Android's existing trigger than install gesture apps, floating buttons, or launcher replacements.

## What it does

- Select one installed launchable app.
- Enable an accessibility service that listens only for the accessibility button/shortcut event.
- Launch the selected app when triggered.
- Recover gracefully if the selected app is uninstalled or disabled, with clear guidance.
- Show guidance when the service is disabled or interrupted, including device-specific background-protection tips for aggressive OEMs.

## What it does not do

No analytics, ads, cloud sync, accounts, crash-reporting SDKs, or network data collection. No overlays, launcher replacement, multi-app automation, screen reading, or gesture injection. The accessibility service exists solely to receive the shortcut event and launch the selected app.

## Product principles

1. **Focused** — one trigger, one app. Reject features that turn it into a general launcher, macro engine, or overlay system.
2. **Native-first** — rely on Android's built-in shortcut behavior; no duplicated system UI.
3. **Transparent** — clearly explain why accessibility permission is needed and exactly what the service does.
4. **Private by default** — local settings only; no telemetry of any kind.
5. **Fail clearly** — if the selected app can't launch, say what happened and how to fix it. No silent failures.

## Setup flow

1. Open the app and accept the accessibility disclosure.
2. Enable the app's accessibility service in Android Accessibility settings.
3. Pick one installed app from the picker.
4. Use the accessibility button/shortcut to launch it.

After setup the app should feel invisible: trigger → app opens. No intermediate screens unless something needs fixing.

## Scope

**Current:** one selected app, one shortcut action, local-only configuration, clear setup and recovery states.

**Possible future:** better per-Android-version onboarding, more OEM-specific guidance (Xiaomi, Samsung, etc.), improved copy for why Android disables accessibility services after updates, optional local config import/export.

**Out of scope:** multi-app menus, automation chains, Tasker-style actions, screen scraping, floating buttons, launcher replacement, remote config, cloud accounts, analytics.

## Distribution & go-to-market

Distributed via F-Droid and GitHub Releases; stays friendly to reproducible builds and privacy-focused users.

GTM notes:

- Lead with the concrete pain: "launch any app (including Google Assistant on Xiaomi) from Android's accessibility button." Specific OEM pain points convert better than generic pitches.
- Natural channels: F-Droid discovery, r/fossdroid, r/Xiaomi, r/MIUI, XDA forums, and privacy-focused Android communities.
- The privacy stance (no telemetry, minimal permissions, tiny APK) is the differentiator against gesture/automation apps — state it up front in listings.
- Keep the F-Droid description and screenshots aligned with the "one trigger, one app" positioning; avoid feature-list bloat.

## Positioning

A minimal utility that turns Android's accessibility button/shortcut into a direct launcher for one chosen app. Not a launcher, not an automation tool, not an accessibility inspector — a focused shortcut bridge.
