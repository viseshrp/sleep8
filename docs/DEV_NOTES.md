# Sleep8 — Developer Notes

## Build
- Open the project in Android Studio (Giraffe+).
- Let Gradle sync.
- Build from Android Studio: Build > Make Project.

## Run (Debug)
- Select app configuration.
- Run on an Android 12+ device or emulator (minSdk 31).

## Unit Tests (JVM)
- From Android Studio: Run > Run 'All Tests' or run specific tests under app/src/test.
- Or from terminal: ./gradlew testDebugUnitTest

## Instrumentation Tests (Device/Emulator)
- From Android Studio: Run > Run 'All Tests in androidTest'.
- Or from terminal: ./gradlew connectedDebugAndroidTest

## Manual Tests (Physical Device)
- Follow the test cases in docs/MANUAL_TESTS.md.
- Focus on P0/P1 cases first, especially screen on/off flows, reboot recovery, and OEM clock app behavior.

## New Features (2026)
- Auto-arm schedule: Enable in settings to automatically arm/disarm at night window boundaries.
- WindowScheduler: Replaces WindowEndScheduler, handles both start and end triggers.
- Manual arming/disarming acts as an override until the next scheduled event.
- ArmManager now supports multiple ArmSource types (SCHEDULED, APP_BUTTON, QUICK_TILE).
