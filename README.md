# Accessibility Button Launcher

**Accessibility Button Launcher** is a minimal Android app that lets you assign one installed app to the system Accessibility button / shortcut. When the accessibility trigger fires, the app launches the selected target app. If no valid target exists, it opens the host app and explains what needs to be fixed.

## Requirements

- Android 11+ (API 30+)
- JDK 17

## Local development

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew spotlessCheck
```

The main app module lives in `app/`. UI and screen state are Compose-driven, app settings are stored in DataStore, and the trigger entry point is `ShortcutLaunchAccessibilityService`.

## Setup flow

1. Open the app.
2. Read and accept the disclosure.
3. Open Accessibility settings and enable `A11Y Button Shortcut Service`.
4. Choose one launchable app from the picker.
5. Use the system Accessibility button or shortcut to launch the selected app.

## Notes

- The service requests only the minimum accessibility configuration needed for the shortcut/button behavior.
- No telemetry, analytics, overlays, or multi-app automation are included in v1.
