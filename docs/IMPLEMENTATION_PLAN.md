# Sleep8 — Implementation Plan (Owned Alarm Model)

## Overview
This plan reflects the **app-owned alarm** architecture (AlarmManager → Receiver → Activity) and replaces all OS Clock delegation.

---

## Phase 1 — Core Alarm Ownership
- Add `AlarmScheduler` to schedule exact alarms with `setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP)`.
- Persist alarm metadata in Room: `scheduled_at`, `trigger_at`, `source`, `status`.
- Remove all Clock intents (`ACTION_SET_ALARM`) and backstop hooks.

**Acceptance:** An alarm record is created and an exact alarm is scheduled when screen-off confirmation succeeds.

---

## Phase 2 — Alarm Trigger Flow
- Implement `AlarmReceiver` (BroadcastReceiver) to start the alarm flow.
- Implement `AlarmActivity` (full-screen, show over lock screen, turn screen on).
- Implement `AlarmRingingService` (foreground service only while ringing).
- Loop alarm sound (STREAM_ALARM semantics) and repeating vibration until dismissed.

**Acceptance:** Alarm fires in Doze/lock/process death and launches the full-screen UI.

---

## Phase 3 — Dismiss/Snooze + Persistence
- Dismiss updates DB (`status = DISMISSED`, `dismissed_at`), stops audio + vibration, stops service.
- Snooze schedules a new exact alarm (e.g., +10 minutes) and marks original as `SNOOZED`.

**Acceptance:** Dismiss stops immediately; snooze re-arms using owned alarm flow.

---

## Phase 4 — Reboot Restore
- Restore armed state + pending confirmation timers.
- Reschedule any `SCHEDULED` alarms from DB on boot.
- If `trigger_at` already passed, fire immediately (schedule for now + 1s).

**Acceptance:** Alarm still fires after reboot with no user interaction.

---

## Phase 5 — Permissions + UI Guidance
- Declare exact alarm + foreground service permissions.
- Detect missing exact alarm permission and guide to Settings.

**Acceptance:** Missing permission yields a clear warning and logs; scheduling only occurs when allowed.

---

## Phase 6 — Tests + Documentation
- Unit tests: alarm trigger time (+8h), snooze time, persistence.
- Manual tests documented in `MANUAL_TESTS.md`.
- Update `SPEC.md`, `ARCHITECTURE.md`, `TEST_PLAN.md`, `VERIFICATION_REPORT.md`.

**Acceptance:** All docs reflect owned-alarm model and test coverage is updated.
