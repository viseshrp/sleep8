# Sleep8 — Testing Plan (Owned Exact Alarm + Optional Overlay)

## Overview
Physical device testing is required; emulators are not authoritative for exact alarm scheduling, lockscreen behavior, or overlay reliability.

---

## Unit Tests
- Duration config stored in `duration_used_minutes`.
- `AlarmManager.setAlarmClock` uses expected trigger time.
- PendingIntent uniqueness via `alarm_instance_id`/`request_code`.
- Snooze schedules exact alarm with expected trigger time.
- Notification permission logic (Android 13+).
- Overlay policy logic (enabled vs permission granted).
- Alarm ringing notification includes Dismiss/Snooze actions.
- Alarm menu routing chooses ringing UI vs preview/history.
- Single active alarm: scheduling a new confirmed alarm cancels prior scheduled alarms.
- Single active alarm: snooze replaces any scheduled alarm.
- Reboot cleanup: multiple scheduled alarms → keep newest, cancel extras.
- Disarm cancels the active scheduled alarm.
- Duration boundaries: 0, 1, 480, 720 minutes.
- Invalid duration inputs: below 0 / above 720 are rejected (not persisted).
- Duration 0 schedules at confirmation timestamp.
- Alarm UI title appears in AlarmActivity label and UI header.
- `AlarmManager.getNextAlarmClock` reflects the earliest scheduled alarm (debug/log assertion).

---

## Integration Tests (Robolectric)
- Arm → screen off → confirm → alarm scheduled.
- Boot restore reschedules `SCHEDULED` record.

---

## Manual Tests (Pixel 8 / Android 14+)
- Alarm fires and displays full-screen UI on lockscreen.
- Overlay toggle on: overlay appears while ringing (permission granted).
- Overlay toggle on, permission denied: alarm still rings; overlay not shown.
- Notification permission requested on first arm; deny → alarm still rings without FGS notification.
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms`
  - `sleep8://alarm/<id>`
- Lockscreen/system “next alarm” shows app alarm when earlier than other alarms.
- Notifications permission denied still rings; UI warns about reduced lockscreen UX.

---

## Emulator Disclaimer
Emulators are not authoritative for Doze, exact alarms, or lockscreen alarm indicators.
