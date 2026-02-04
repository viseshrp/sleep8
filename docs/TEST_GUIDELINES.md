# Sleep8 — Test Guidelines

## Overview
This project uses a mix of unit tests (JVM + Robolectric) and Compose UI tests. Unit tests cover core business logic, schedulers, repositories, receivers, and utilities. Compose UI tests cover key screens and flows.

Target coverage threshold: **90% line coverage** (unit tests via JaCoCo).

## What Tests Exist

### Unit tests (`app/src/test/java`)
- Domain logic: `ArmManager`, `StateMachineManager`, schedulers, validators.
- Repositories and Room integration.
- Receivers: boot restore, alarm receiver, confirmation receiver, window receivers.
- Utilities: time, duration validation, intents, permissions.
- Notifications and alarm scheduling behavior.

### UI tests (`app/src/androidTest/java`)
- Settings screen sections and reliability checklist.
- Alarm list content and toggles.
- Alarm history screen, clear confirmation dialog, and back navigation.
- Main navigation drawer flows.
- Ringing UI content (Dismiss-only).

### Integration tests (`app/src/test/java/com/sleep8/integration`)
- Full automation flow (arm → screen off → confirm → schedule).
- Reboot restore scheduling.
- Service lifecycle behavior.

## Running Tests

### With Makefile
- `make check` — runs static checks (if configured) and unit tests.
- `make test-unit` — JVM unit tests.
- `make test-integration` — integration tests (Robolectric).
- `make test-ui` — instrumentation/Compose UI tests.
- `make coverage` — JaCoCo report + 90% verification.

### With Gradle (direct)
- Unit tests: `./gradlew testDebugUnitTest`
- UI tests: `./gradlew connectedDebugAndroidTest`
- Coverage: `./gradlew jacocoTestReport jacocoTestCoverageVerification`

## Coverage Reports

Coverage reports are generated at:
- `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`

The verification task enforces **>= 90%** overall line coverage.

## Expected Thresholds
- Unit test coverage: **90%+** (JaCoCo).
- UI tests: validate critical screens and navigation flows.

## Notes
- Exact alarm and lockscreen behaviors still require physical device validation.
- Some OEM behavior is not deterministic in emulators.
