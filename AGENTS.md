# Repository Guidelines

## Project Structure & Module Organization
This repository is a single-module Android app built with Kotlin, Gradle Kotlin DSL, and Jetpack Compose. App code lives under `app/src/main/java/com/pedronveloso/a11ybutton`, with shared theme code in `app/src/main/java/com/pedronveloso/a11ybutton/ui/theme`. Resources are in `app/src/main/res`. Local JVM tests live in `app/src/test`, and device/emulator instrumentation tests live in `app/src/androidTest`. Build logic is defined in `build.gradle.kts`, `app/build.gradle.kts`, and the version catalog at `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repository root:

- `./gradlew assembleDebug` builds the debug APK.
- `./gradlew testDebugUnitTest` runs JVM unit tests in `app/src/test`.
- `./gradlew lintDebug` runs Android lint with warnings treated as errors.
- `./gradlew spotlessCheck` verifies formatting for Kotlin, Gradle Kotlin DSL, and Markdown.
- `./gradlew spotlessApply` fixes formatting issues automatically.

CI runs `spotlessCheck`, `testDebugUnitTest`, `lintDebug`, and `assembleDebug` on pushes and pull requests to `main`.

## Coding Style & Naming Conventions
Follow Kotlin conventions with 4-space indentation and keep code formatted by Spotless using `ktfmt`. Do not edit generated files under `build/`. Keep package names lowercase, classes and composables in `UpperCamelCase`, functions and properties in `lowerCamelCase`, and Android resource names in `snake_case` such as `ic_launcher_background`. Prefer small composables and keep theme-related code in `ui/theme`.

## Testing Guidelines
Add fast logic tests to `app/src/test/java/...` and Android-dependent tests to `app/src/androidTest/java/...`. Name test files after the subject under test, for example `MainActivityTest.kt`, and use descriptive test names such as `button_isAnnouncedToAccessibilityServices`. Run `./gradlew testDebugUnitTest lintDebug` before opening a PR; add instrumentation coverage when UI or platform behavior changes.

## Commit & Pull Request Guidelines
Current history uses short, imperative commit subjects such as `Initial project setup with CI tooling`. Keep commits focused and under about 72 characters when practical. Pull requests should include a concise summary, linked issue if applicable, test notes, and screenshots or recordings for UI changes. Call out any accessibility impact explicitly.

## Configuration Tips
Use JDK 17 to match CI. Treat `local.properties` as machine-specific and avoid committing secrets or environment-specific changes.
