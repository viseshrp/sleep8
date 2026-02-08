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

## CI/CD
- GitHub Actions runs lint, unit, integration, UI tests, and coverage via Makefile targets.
- Local coverage: `make coverage` (HTML report in `app/build/reports/jacoco/jacocoTestReport/html/index.html`).

## Manual Tests (Physical Device)
- Follow the test cases in docs/MANUAL_TESTS.md.
- Focus on P0/P1 cases first, especially screen on/off flows, reboot recovery, and lockscreen/full-screen alarm behavior.

## New Features (2026)
- Armed state is manual-only (`APP_BUTTON`, `QUICK_TILE`); no scheduled arming/disarming exists.
- Night window boundaries and backstops gate monitoring service activation.
- Boot/time/timezone/package-replaced flows reconcile monitoring reliability without changing armed state.
