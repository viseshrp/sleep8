# Verification Report

Date: 2026-02-01

Scope: Full audit against docs/SPEC.md (authoritative), docs/ARCHITECTURE.md, docs/IMPLEMENTATION_PLAN.md, docs/TEST_PLAN.md. Offline-only preserved (no INTERNET permission).

## Compliance Matrix (SPEC.md Acceptance Criteria)

| Requirement | Evidence | Status |
|---|---|---|
| 1. Armed + in night window: SCREEN_OFF starts 10-min confirmation | `app/src/main/java/com/sleep8/domain/manager/StateMachineManager.kt` `onScreenOff()` computes window, schedules confirmation via `ConfirmOffScheduler.scheduleConfirmation()` | PASS |
| 2. Screen ON before 10 min: no OS alarm created | `StateMachineManager.onScreenOn()` cancels confirmation + clears pending; `ConfirmationAlarmReceiver` checks screen state | PASS |
| 3. Screen remains OFF 10 min: create OS alarm at screen_off + 8h | `ConfirmationAlarmReceiver.onReceive()` calls `StateMachineManager.onConfirmationTimerExpired()`; `OsAlarmCreator.createAlarm()` uses `TimeUtils.calculateAlarmTime()` | PASS |
| 4. Multiple SCREEN_OFF before confirm: latest wins | `StateMachineManager.onScreenOff()` overwrites pending and reschedules confirmation | PASS |
| 5. Multiple OS alarms allowed (no deletion) | `OsAlarmCreator.createAlarm()` always inserts new record; no deletion logic | PASS |
| 6. Reboot restore: armed state + pending confirm timer + immediate alarm if deadline passed & screen OFF | `app/src/main/java/com/sleep8/service/receiver/BootReceiver.kt` restores session, checks window, schedules or creates alarm | PARTIAL (see notes) |
| 7. Persist all events and alarm records in DB | `SessionRepository.insertScreenEvent()`, `AlarmRepository.insertRecord()` | PASS |
| 8. No network traffic | No INTERNET permission in `app/src/main/AndroidManifest.xml`; no networking code | PASS |

## Architecture Alignment

| Doc Expectation | Code Evidence | Status |
|---|---|---|
| Layered architecture (UI → Domain → Service → Data) | Package layout under `app/src/main/java/com/sleep8/*` | PASS |
| Foreground service + runtime screen receiver | `NightMonitorService` registers receiver | PASS |
| ConfirmOffScheduler uses exact alarms | `ConfirmOffScheduler.scheduleConfirmation()` uses `AlarmManager.setExactAndAllowWhileIdle` | PASS |
| OsAlarmCreator uses ACTION_SET_ALARM | `OsAlarmCreator.createAlarm()` | PASS |
| Backstop exact alarm for resilience | `BackstopAlarmScheduler.scheduleBackstop()` | PASS |
| WindowScheduler handles start/end | `WindowScheduler` + `WindowStartReceiver` + `WindowEndReceiver` | PASS |

## Permissions & Platform Constraints

- minSdk 31 / targetSdk 35: `app/build.gradle.kts` → PASS
- Required permissions: `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `SET_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` present in manifest → PASS
- Forbidden permissions: no INTERNET permission → PASS

## Alarm Behavior

- Confirmation delay and latest-wins: `StateMachineManager` + `ConfirmOffScheduler` → PASS
- Screen-on cancels deadline: `StateMachineManager.onScreenOn()` → PASS
- OS alarm delegation via `ACTION_SET_ALARM`: `OsAlarmCreator` → PASS
- No custom alarm UI: none present → PASS

## Persistence & Reboot

- State survives process death: `StateHolder` mirrors to `AppPreferences` → PASS
- Boot restore per spec: `BootReceiver` restores session and pending confirmations → PARTIAL
  - Missing: if pending screen-off exists and deadline already passed, code creates alarm (PASS), but it does not reschedule window start for the current/next window when session is restored or when no session exists (auto-arm is handled elsewhere). Spec emphasizes restore of monitoring when armed; current boot path only schedules window end for active session. Window start scheduling for future windows is not restored here.

## Tests

- Unit tests present for validators/schedulers/managers per `app/src/test/...` → PASS
- Integration tests present (Robolectric) → PASS
- Manual tests documented in `docs/MANUAL_TESTS.md` → PASS
- Coverage of auto-arm/manual override in tests exists (after adjustments) → PASS

## Build/Test Execution

- Build/test commands not run in this environment. Expected to run:
  - `./gradlew testDebugUnitTest`
  - `./gradlew connectedDebugAndroidTest`

## Punch List (Prioritized)

### P0 — Correctness / Spec Violations
1. Boot restore should also restore scheduling for future window boundaries when an active session exists (spec: “restore state and continue/restore scheduled alarms”).
   - Minimal fix: in `app/src/main/java/com/sleep8/service/receiver/BootReceiver.kt`, after restoring active session, call `windowScheduler.scheduleWindowStart()` for the next window boundary (or re-run the same logic as `ArmManager.handleAutoArm()` when auto-arm enabled). Keep behavior limited to spec; do not add new features.

### P1 — Reliability Gaps
1. Foreground service status indicator removed from Settings UI; no visibility into service health from the checklist. (Spec mentions reliability checklist; current UI still includes exact alarms and battery optimization, but not foreground service.)
   - Minimal fix: re-add a non-interactive status row or place it in the main screen if Settings should be minimal.

### P2 — Test Coverage Gaps
1. Boot restore behavior for pending confirmation when deadline passed should be explicitly asserted in unit/integration tests.
   - Minimal fix: add a Robolectric or unit test in `app/src/test/java/com/sleep8/integration/ServiceIntegrationTest.kt` or a new test file to verify BootReceiver path with pending candidate & screen OFF.

## Ambiguities / Doc Gaps (No changes proposed)
- Spec does not define persistence of manual override across reboot. Current code resets manual override after scheduled event; it is not persisted.
- Spec does not specify whether “armed default” should exist; it is stored but not used in code path to auto-arm on app start.

---

End of report.
