# Sleep8 — Implementation Plan (Owned Alarm + AlarmClock Semantics)

## Phase 1 — Alarm Clock Scheduling
- Add `AlarmScheduler` with `AlarmManager.setAlarmClock(AlarmClockInfo(...), operation)`.
- Persist metadata: `duration_used_minutes`, `alarm_instance_id`, `request_code`, `scheduled_via_alarm_clock`.
- Use showIntent to open alarm history.

## Phase 2 — Alarm Trigger Flow
- `AlarmReceiver` → `AlarmRingingService` (FGS) → `AlarmActivity`.
- Deduplicate by `alarm_instance_id` and record status.

## Phase 3 — Notifications & Permissions
- High-importance channel for ringing (`alarm_ringing`).
- Low-importance channel for scheduled alarm (`alarm_scheduled`).
- POST_NOTIFICATIONS runtime request (Android 13+) with one-time prompt.
- Reliability screen shows Exact Alarms / Notifications / Overlay / Battery.

## Phase 4 — Best-effort OS Integration
- Handle `AlarmClock.ACTION_SHOW_ALARMS`.
- Deep links: `sleep8://alarms`, `sleep8://alarm/<id>`.

## Phase 5 — Reboot Restore
- Restore armed state + pending confirmation.
- Reschedule latest `SCHEDULED` record; if overdue, fire immediately.

## Phase 6 — Tests & Docs
- Unit: duration config, AlarmClockInfo construction, snooze reschedule, dedupe.
- Manual: lockscreen “next alarm” indicator on Pixel 8, notification permission denial.
