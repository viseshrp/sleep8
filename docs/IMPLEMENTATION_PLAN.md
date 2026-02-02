# Sleep8 — Implementation Plan (Owned Exact Alarm + Optional Overlay)

## Phase 1 — Exact Alarm Scheduling
- Add `AlarmScheduler` with `AlarmManager.setAlarmClock(AlarmClockInfo(triggerAt, showIntent), operation)` so system “next alarm” reflects Sleep8.
- Persist metadata: `duration_used_minutes`, `alarm_instance_id`, `request_code`, `snoozed_at`, `snoozed_until`, `overlay_used`, `activity_presented`.

## Phase 2 — Alarm Trigger Flow
- `AlarmReceiver` → `AlarmRingingService` (FGS) → `AlarmActivity`.
- Optional overlay (WindowManager) shown while ringing when user-enabled + permission granted.
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
- Unit: duration config, setAlarmClock scheduling, snooze reschedule, overlay policy, notification actions.
- Manual: lockscreen alarm UI + next alarm indicator, overlay behavior, notification permission denial.
