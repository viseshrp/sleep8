# Sleep8
[![CI](https://github.com/viseshrp/sleep8/actions/workflows/android-ci-cd.yml/badge.svg?branch=main)](https://github.com/viseshrp/sleep8/actions/workflows/android-ci-cd.yml)
[![codecov](https://codecov.io/gh/viseshrp/sleep8/branch/main/graph/badge.svg)](https://codecov.io/gh/viseshrp/sleep8)

Sleep8 is an offline Android alarm app that schedules a wake alarm based on when you actually fall asleep.

When armed (from the app or Quick Settings tile), Sleep8 monitors screen on/off events only during the configured night window. If the screen remains off for the confirmation period (default 20 minutes), it schedules an app-owned exact alarm for:

`trigger_at = screen_off_time + configured_duration`

Default duration is 8h 0m, and valid duration range is 0-720 minutes.

## What the app does
- Arms/disarms manually from Home and Quick Settings tile.
- Runs `NightMonitorService` only while armed and inside the night window.
- Tracks screen-off confirmation with an exact timer (`setExactAndAllowWhileIdle`).
- Schedules alarms with `AlarmManager.setAlarmClock` (app-owned alarms, no OS clock delegation).
- Enforces a single active scheduled alarm at a time.
- Provides:
  - Home alarm list section (toggle enabled/disabled for existing records).
  - Alarm history (full local audit trail, with clear-all action).
  - Full-screen ringing UI and optional overlay ringing UI.
- Restores state and reconciles alarms after reboot.
- Uses boundary backstops and periodic health checks to self-heal monitoring start.
- Persists monitoring start telemetry for postmortem diagnosis.
- Works offline only (no `INTERNET` permission).

## Product behavior
1. User arms Sleep8.
2. If current local time is inside night window, monitoring is active.
3. On each `SCREEN_OFF` event in-window:
   - candidate screen-off timestamp is updated
   - 20-minute confirmation timer is (re)started
4. If `SCREEN_ON` occurs before confirmation, timer is cancelled.
5. If confirmation expires while still off:
   - new alarm record is created (`SCHEDULED`)
   - previous scheduled alarms are cancelled (`REPLACED_BY_NEW_ALARM`)
   - exact alarm is scheduled with `setAlarmClock`
6. At trigger time, receiver launches ringing flow (service + full-screen activity).
7. User dismisses alarm; record moves to `DISMISSED`.

Notes:
- "Latest screen-off wins" before confirmation.
- Duration `0` means ring immediately at confirmation time.
- Manual disarm stops monitoring and pending confirmation, but does not cancel already scheduled alarms.

## Architecture summary
Code is split into `ui`, `domain`, `service`, `data`, and `util`.

- UI layer:
  - `MainActivity`, `SettingsActivity`, `AlarmHistoryActivity`, `AlarmRingingActivity`
  - Compose screens and Quick Settings tile (`Sleep8TileService`)
- Domain layer:
  - `ArmManager` (manual arming lifecycle and night-window boundary scheduling)
  - `StateMachineManager` (screen events/confirmation transitions)
  - `AlarmScheduler`, `ConfirmOffScheduler`, `NightWindowScheduler`
- Service/receiver layer:
  - `NightMonitorService`, `AlarmRingingService`
  - receivers for alarm firing, boot restore, confirmation, and window boundaries
- Data layer:
  - Room DB + repositories for settings, sessions, and alarm records
  - SharedPreferences (`AppPreferences`) for runtime state and IDs

State machine:
- `DISARMED`
- `ARMED_IDLE`
- `ARMED_PENDING_CONFIRM`
- `ARMED_ALARM_SET`

## Data model
Room database: `Sleep8Database` (version 12)

- `settings`
  - night window, confirmation minutes
  - alarm duration, overlay enabled, armed default
- `arm_sessions`
  - arm/disarm lifecycle and source (`APP_BUTTON`, `QUICK_TILE`)
- `screen_events`
  - `SCREEN_OFF` / `SCREEN_ON` audit trail per session
- `alarm_records`
  - trigger and lifecycle fields, including:
    - `duration_used_minutes`
    - `alarm_instance_id`
    - `request_code`
    - `status` (`SCHEDULED` / `FIRED` / `DISMISSED` / `CANCELED`)
    - `canceled_reason`
    - `overlay_used`, `activity_presented`
- `monitoring_start_events`
  - expected/scheduled/observed night window boundary timestamps
  - armed/window gate snapshot at trigger time
  - whether monitoring became active
  - human-readable reason bucket when start failed

## Permissions and reliability checklist
Manifest permissions include:
- `SCHEDULE_EXACT_ALARM`
- `POST_NOTIFICATIONS`
- `USE_FULL_SCREEN_INTENT`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `SYSTEM_ALERT_WINDOW` (optional overlay)
- `RECEIVE_BOOT_COMPLETED`
- Foreground service permissions

Settings includes a reliability section to verify/request:
- exact alarms
- notifications
- full-screen alarm UI capability
- battery optimization exclusion
- overlay permission (optional)
- Pixel guidance: set Sleep8 battery usage to Unrestricted and avoid Extreme Battery Saver restrictions for Sleep8.

## Monitoring reliability contract (summary)
- Guarantee under normal OS conditions: if `armed && inNightWindow` at night window start, monitoring auto-starts.
- Reliability strategy: boundary exact alarm + +2/+10 minute boundary backstops + periodic 15-minute in-window health checks + boot/time/app-launch reconcile.
- Known limitations: force-stop and severe OS background restrictions can block any app-level guarantee.

## Tech stack
- Kotlin, Jetpack Compose, Material 3
- Hilt dependency injection
- Room persistence
- AlarmManager (`setExactAndAllowWhileIdle`, `setAlarmClock`)
- JUnit5 + MockK + Robolectric + Compose UI tests

SDK/toolchain:
- `minSdk = 31`
- `targetSdk = 35`
- `compileSdk = 35`
- Java 17
- Kotlin 2.0.21

## Build and run
Prerequisites:
- Android Studio (or Gradle CLI)
- Android SDK for API 31+
- Java 17

Commands:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Test and coverage
Local commands:
```bash
make check
make test-unit
make test-integration
make test-ui
make coverage
```

Equivalent Gradle examples:
```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

Current test footprint:
- 30 JVM test classes under `app/src/test`
- 6 instrumentation/Compose test classes under `app/src/androidTest`

Coverage artifacts:
- `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`

Coverage verification enforces >=90% on production-critical automation packages
(`domain.manager`, `domain.state`, `domain.overlay`, `domain.validator`, and core time/duration utils).

## CI/CD
Workflow: `.github/workflows/android-ci-cd.yml`

Runs:
- quality/lint
- unit + integration + coverage
- UI tests on emulator
- release build (non-PR events)
- optional Play deployment (workflow dispatch)

Codecov config: `codecov.yml`

## Project structure
- `app/` Android app module
- `docs/` architecture/spec/testing/design docs
- `app/src/main/java/com/sleep8/...` production source
- `app/src/test/...` unit/integration tests
- `app/src/androidTest/...` instrumentation/Compose tests

## Documentation map
- `docs/SPEC.md` authoritative product spec
- `docs/ARCHITECTURE.md` architecture and runtime flow
- `docs/TEST_PLAN.md` what to verify across unit/integration/UI/manual
- `docs/MANUAL_TESTS.md` device-first validation checklist
- `docs/DEV_NOTES.md` developer workflow notes
- `docs/VERIFICATION_REPORT.md` spec compliance snapshot

## License
See `LICENSE`.
