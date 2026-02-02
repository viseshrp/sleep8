# Sleep8 — Testing Plan (Owned Exact Alarm + Optional Overlay)

## Overview
Physical device testing is required; emulators are not authoritative for exact alarm scheduling, lockscreen behavior, or overlay reliability.

---

## Unit Tests
- Duration config stored in `duration_used_minutes`.
- `AlarmManager.setExactAndAllowWhileIdle` uses expected trigger time.
- PendingIntent uniqueness via `alarm_instance_id`/`request_code`.
- Snooze schedules exact alarm with expected trigger time.
- Notification permission logic (Android 13+).
- Overlay policy logic (enabled vs permission granted).
- Alarm ringing notification includes Dismiss/Snooze actions.

---

## Integration Tests (Robolectric)
- Arm → screen off → confirm → alarm scheduled.
- Boot restore reschedules `SCHEDULED` record.

---

## Manual Tests (Pixel 8 / Android 14+)
- Alarm fires and displays full-screen UI on lockscreen.
- Overlay toggle on: overlay appears while ringing (permission granted).
- Overlay toggle on, permission denied: alarm still rings; overlay not shown.
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms`
  - `sleep8://alarm/<id>`
- Notifications permission denied still rings; UI warns about reduced lockscreen UX.

---

## Emulator Disclaimer
Emulators are not authoritative for Doze, exact alarms, or lockscreen alarm indicators.
