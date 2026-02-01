# Verification Report

Date: 2026-02-01

Scope: Re-audit against updated docs/SPEC.md (authoritative), docs/ARCHITECTURE.md, docs/IMPLEMENTATION_PLAN.md, docs/TEST_PLAN.md. Auto-Arm and Night Window treated as separate schedules; monitoring gate is `armed && inNightWindow`. Offline-only preserved.

## Compliance Matrix (SPEC.md Acceptance Criteria)

| Requirement | Evidence | Status |
|---|---|---|
| 1. Armed + in night window: SCREEN_OFF starts 10-min confirmation | `app/src/main/java/com/sleep8/domain/manager/StateMachineManager.kt` `onScreenOff()` checks window and schedules `ConfirmOffScheduler.scheduleConfirmation()` | PASS |
| 2. Screen ON before 10 min: no OS alarm created | `StateMachineManager.onScreenOn()` cancels confirmation; `ConfirmationAlarmReceiver` checks screen state | PASS |
| 3. Screen remains OFF 10 min: create OS alarm at screen_off + 8h | `ConfirmationAlarmReceiver` → `StateMachineManager.onConfirmationTimerExpired()` → `OsAlarmCreator.createAlarm()` | PASS |
| 4. Multiple SCREEN_OFF before confirm: latest wins | `StateMachineManager.onScreenOff()` overwrites pending and reschedules confirmation | PASS |
| 5. Multiple OS alarms allowed (no deletion) | `OsAlarmCreator.createAlarm()` inserts new record; no deletion logic | PASS |
| 6. Reboot restore: armed state + pending confirm timer + immediate alarm if deadline passed & screen OFF | `app/src/main/java/com/sleep8/service/receiver/BootReceiver.kt` restores auto-arm boundaries, armed state, and pending confirmation gated by `armed && inNightWindow` | PARTIAL (see notes) |
| 7. Persist all events and alarm records in DB | `SessionRepository.insertScreenEvent()`, `AlarmRepository.insertRecord()` | PASS |
| 8. No network traffic | No INTERNET permission in `app/src/main/AndroidManifest.xml`; no networking code | PASS |

## Auto-Arm + Night Window Semantics (SPEC)

- Auto-Arm toggles `armed`: `app/src/main/java/com/sleep8/domain/manager/ArmManager.kt` + `WindowScheduler`/`WindowStartReceiver`/`WindowEndReceiver` → PASS
- Night Window is a filter only: `StateMachineManager.onScreenOff()` uses `TimeUtils.isInWindow()` → PASS
- Monitoring runs only when `armed && inNightWindow`:
  - Start/stop monitoring on Night Window boundaries while armed: `NightWindowStartReceiver`, `NightWindowEndReceiver` → PASS
  - Auto-Arm does not start monitoring unless in Night Window: `ArmManager.scheduleNightWindowBoundaries()` gate → PASS

## Architecture Alignment

| Doc Expectation | Code Evidence | Status |
|---|---|---|
| Auto-Arm boundary scheduling separate from Night Window filtering | `WindowScheduler` (auto-arm) vs `NightWindowScheduler` + `NightWindowStart/EndReceiver` (monitoring gate) | PASS |
| Armed state owned by ArmManager; persisted in StateHolder | `ArmManager`, `StateHolder` | PASS |
| Monitoring AND-gate (`armed && inNightWindow`) | `NightWindowStart/EndReceiver` + `StateMachineManager` window checks | PASS |

## Permissions & Platform Constraints

- minSdk 31 / targetSdk 35: `app/build.gradle.kts` → PASS
- Required permissions present: `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `SET_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → PASS
- Forbidden permissions: no INTERNET permission → PASS

## Boot Restore (Re-evaluation)

- Auto-Arm boundary restore: `BootReceiver` schedules `WindowScheduler` boundaries when auto-arm enabled → PASS
- Armed state restore from auto-arm schedule: `BootReceiver` computes `shouldBeArmedNow` and arms/disarms accordingly → PASS
- Monitoring starts only when `armed && inNightWindow`: `BootReceiver` checks Night Window before starting service; `NightWindowStart/EndReceiver` enforce gate → PASS
- Pending confirmation restore gated by `armed && inNightWindow`: `BootReceiver` only resumes confirmation when `inNightWindow` and screen off; otherwise preserved → PARTIAL (see notes)

## Tests (Re-evaluation)

- Unit tests exist for window logic, state machine, schedulers: `app/src/test/java/...` → PASS
- Auto-Arm schedule tests exist but do not explicitly cover Night Window gate transitions → PARTIAL
- Boot restore tests for auto-arm boundaries/armed state/pending confirmation gating are missing → PARTIAL
- Manual tests include Night Window entry/exit monitoring behavior (`docs/MANUAL_TESTS.md`) → PASS

## Resolved Findings (from prior report)

- Previous concern about “Night Window start scheduling restore” is resolved by clarified semantics; no longer required.
- Auto-Arm boundaries are now treated as the only arming triggers; Night Window is filter-only.

## Punch List (Prioritized)

### P0 — Spec Violations
1. Pending confirmation restore when **armed but outside Night Window**: `BootReceiver` currently ignores pending confirmation outside the window, but does not explicitly preserve/avoid advancing confirmation for later. Spec requires preservation and resumption when both conditions become true.
   - Minimal fix: ensure pending candidate/deadline are preserved and confirmation timer is **not** scheduled when `!inNightWindow`, and resume via `NightWindowStartReceiver` (e.g., call a resume method when entering window) without clearing pending state.

### P1 — Reliability Gaps
1. Monitoring gate on Night Window transitions relies on scheduled receivers; if alarms are delayed, monitoring might start/stop late.
   - Minimal fix: verify `NightWindowScheduler` is always scheduled when armed; consider rescheduling on app resume if needed (no new features, just ensure schedule is set).

### P2 — Test Coverage Gaps
1. Add tests for two-schedule model:
   - Auto-Arm cross-midnight independent of Night Window
   - Monitoring gate cases (`armed && inNightWindow` vs other combinations)
   - Boot restore gating of pending confirmation
   - Suggested locations: `app/src/test/java/com/sleep8/domain/manager/ArmManagerTest.kt`, `app/src/test/java/com/sleep8/integration/FullFlowIntegrationTest.kt`

---

End of report.
