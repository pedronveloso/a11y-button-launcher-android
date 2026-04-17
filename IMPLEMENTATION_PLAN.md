# A11Y Button Implementation Plan

## 1. Product Summary

Build an Android-only app that lets the user assign one installed launchable app to the system Accessibility button / shortcut. When the service is triggered, it launches the selected app. If no valid target exists, the app opens its own host UI and shows a clear recovery state.

This should remain narrow in scope: one app selection, one accessibility service, one primary screen, minimal permissions, no automation beyond the user-triggered shortcut behavior.

## 2. Key Technical Decisions

- Kotlin, Jetpack Compose, Material 3, coroutines/Flow, ViewModel, DataStore.
- Single-activity app with one main route and one app-picker route.
- `AccessibilityService` declared with minimal metadata and `flagRequestAccessibilityButton`.
- Use `LauncherApps.getActivityList()` for the picker because it returns launchable activities and gives label/icon/component metadata directly.
- Persist both package name and flattened `ComponentName`. Component-level persistence is the reliable key.
- Use explicit component launch intents with `FLAG_ACTIVITY_NEW_TASK`.
- Derive setup status from live service state + persisted selection + current app availability.

## 3. Proposed Architecture

### UI layer

- `MainActivity`
- `MainViewModel`
- `MainScreen`
- `AppPickerScreen`
- Reusable composables for setup card, selected app row, troubleshooting items, and status chips

### Domain layer

- `ObserveSetupStateUseCase`
- `GetSelectedAppUseCase`
- `SelectTargetAppUseCase`
- `ValidateSelectionUseCase`
- `LaunchSelectedAppUseCase`

### Data layer

- `SettingsRepository` backed by DataStore
- `InstalledAppsRepository` backed by `LauncherApps` and `PackageManager`
- `AccessibilityStatusRepository` for service-enabled state helpers

### Service layer

- `ShortcutLaunchAccessibilityService`
- `AccessibilityTriggerHandler`
- `AppLaunchExecutor`

## 4. Manifest and Service Configuration Plan

### Manifest

- Add the accessibility service with `android.permission.BIND_ACCESSIBILITY_SERVICE`.
- Add service metadata pointing to `res/xml/accessibility_service_config.xml`.
- Add a narrow `<queries>` block for launcher activities so app enumeration works on API 30+.
- Keep the app exported surface minimal: launcher activity exported, service not externally bindable except by system.

### Accessibility service XML

- `android:accessibilityFlags="flagRequestAccessibilityButton"`
- Minimal event types and feedback type
- `android:description`, `android:summary`, optional `android:settingsActivity`
- Do not request window-content retrieval, gestures, screenshots, key filtering, overlays, or touch exploration in v1.

### Service callback model

- Register the accessibility button callback in `onServiceConnected()`.
- Handle accessibility button clicks via `AccessibilityButtonController.AccessibilityButtonCallback.onClicked()`.
- Treat shortcut enablement as lifecycle-driven; there is no distinct “shortcut pressed” callback to depend on.
- Keep launch handling idempotent and fast.

## 5. Screen and State Model

### Primary screen sections

- Header: app name + one-sentence explanation
- Setup card: service status, selected app status, overall readiness
- Actions: open Accessibility settings, choose/change app
- Selected app block: icon, label, package name
- Help block: test/help copy
- Troubleshooting block: 3 high-value items only

### Persisted state

- `selectedPackageName: String?`
- `selectedComponentName: String?` as flattened `ComponentName`
- `disclosureAccepted: Boolean`

### Derived UI state

- `serviceEnabled: Boolean`
- `selectedApp: SelectedAppUiModel?`
- `selectionValidity: Valid | Missing | Disabled | NotLaunchable | None`
- `readiness: NotSetUp | PartiallySetUp | Ready`
- `transientMessage: String?`

### Readiness rules

- `NotSetUp`: service disabled and no valid selection
- `PartiallySetUp`: exactly one prerequisite missing
- `Ready`: service enabled and valid selection present

## 6. App Picker Plan

- Implement as a full-screen Compose route, not a dialog.
- Source entries from `LauncherApps.getActivityList(null, Process.myUserHandle())`.
- Map each launcher activity into `AppEntry(componentName, packageName, label, iconLoader)`.
- Support search by label and package name.
- Exclude this app from the list unless explicitly wanted for debugging.
- On selection, persist immediately and return to the main screen.
- On resume and app launch, validate the stored component with `PackageManager.getActivityInfo()` and by confirming it still appears in the current launcher list.

## 7. Launch Flow Design

### Trigger path

1. Accessibility trigger arrives in the service.
2. Service reads the latest stored selection.
3. Validate component/package.
4. Build explicit launch intent for the selected component with `FLAG_ACTIVITY_NEW_TASK`.
5. Launch target app.
6. If selection is absent or invalid, open host app instead and surface a recovery message.

### Failure handling

- No selection: open host app with “Choose an app first.”
- App uninstalled/disabled: clear invalid persisted selection and open host app with reselection prompt.
- Activity no longer launchable: treat as stale selection and require reselection.
- Launch exception: open host app and display a clear error state.
- Target already foregrounded: treat as success and do nothing special.

## 8. Execution Phases

### Phase 1: Foundation

- Add app package structure for `ui`, `domain`, `data`, `service`, and `model`.
- Add DataStore and repository scaffolding.
- Add service declaration, metadata XML, and package visibility queries.
- Add status helpers for “is accessibility service enabled”.

Exit criteria:
- App builds.
- Service appears in Accessibility settings.
- Main screen can reflect service-enabled state.

### Phase 2: Main Screen

- Replace placeholder UI with Material 3 main screen.
- Implement readiness card and action buttons.
- Add disclosure copy and acceptance state.
- Refresh status on lifecycle resume after returning from settings.

Exit criteria:
- User can understand setup in one screen.
- State changes reflect without app restart.

### Phase 3: App Picker and Persistence

- Build searchable picker from `LauncherApps`.
- Persist component/package selection in DataStore.
- Show selected app row on the main screen.
- Implement invalid-selection detection.

Exit criteria:
- User can choose/change one app.
- Selection survives process death and restart.

### Phase 4: Accessibility Trigger Launching

- Implement service trigger handler and launch executor.
- Launch selected app from accessibility button callback.
- Fall back to host app on missing/invalid selection.
- Surface recovery messages in app state.

Exit criteria:
- Pressing the accessibility button launches the configured app.
- Invalid configuration paths recover cleanly.

### Phase 5: Polish and Hardening

- Improve semantics, TalkBack labels, and touch targets.
- Add light/dark verification and refine states.
- Keep troubleshooting copy concrete and short.
- Tighten error messages and empty states.

Exit criteria:
- App is accessible, minimal, and understandable.
- No unnecessary capabilities or noisy UI remain.

### Phase 6: Testing and Release Readiness

- Add unit tests for DataStore mapping, readiness calculation, and selection validation.
- Add targeted tests for launch-intent resolution logic.
- Run manual QA across API 30+ emulator/device coverage.
- Prepare README updates and open-source notes.

Exit criteria:
- Core unit coverage exists for state and persistence.
- Manual setup and recovery flows pass on representative devices.

## 9. Edge Cases to Explicitly Handle

- Service disabled after previous setup
- Selected app uninstalled
- Selected app disabled
- Saved component removed or replaced by alias change
- Service callback with no valid saved selection
- User returns from settings and state is stale
- Accessibility button unavailable on current device/navigation mode
- Multiple launcher activities in one package

## 10. Testing Plan

### Unit tests

- DataStore serialization/deserialization for selected package/component and disclosure flag
- Readiness-state derivation
- Selection validation for valid, missing, disabled, and stale components
- Launch-intent builder behavior

### Manual QA

- First run and disclosure flow
- Open settings and return with automatic refresh
- Pick app, change app, and persist after process death
- Trigger launch from accessibility button
- Missing app recovery after uninstall/disable
- Light theme and dark theme
- API 30, 31+, and at least one gesture-navigation device/emulator

## 11. Open Questions to Resolve Before Coding

- Whether to include a tiny dedicated onboarding panel or keep disclosure inline on the main screen. Recommendation: inline for v1.
- Whether to include this app itself in the picker. Recommendation: no.
- Whether to support work-profile launcher apps in v1. Recommendation: ignore unless naturally returned by `LauncherApps`, but do not build special profile UX yet.

## 12. Recommended Implementation Order

1. Service declaration + settings/status detection
2. Main screen state model
3. DataStore repository
4. App picker
5. Trigger launch path
6. Validation/recovery handling
7. Tests and polish

## 13. Research Notes

- Android requires accessibility services to be declared via manifest service + metadata XML.
- For targetSdk above 29, the accessibility-button request flag must be declared in service metadata XML, not added later dynamically.
- `AccessibilityService` is one of the documented service types allowed to start an activity from the background.
- Package visibility filtering on API 30+ means launcher enumeration needs either `LauncherApps` or explicit manifest queries.

Official references used:

- https://developer.android.com/guide/topics/ui/accessibility/service
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityButtonController
- https://developer.android.com/reference/android/content/pm/LauncherApps
- https://developer.android.com/reference/android/content/pm/LauncherActivityInfo
- https://developer.android.com/training/package-visibility
- https://developer.android.com/guide/components/activities/background-starts
