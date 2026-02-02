# Sleep8 — Testing Plan (AlarmClock Semantics)

## Overview
Physical device testing is required; emulators are not authoritative for AlarmClock scheduling, lockscreen behavior, or system alarm indicators.

---

## Unit Tests
- Duration config stored in `duration_used_minutes`.
- `AlarmManager.setAlarmClock` uses expected `AlarmClockInfo.triggerTime`.
- PendingIntent uniqueness via `alarm_instance_id`/`request_code`.
- Snooze schedules AlarmClock and updates next alarm.
- Notification permission logic (Android 13+).

---

## Integration Tests (Robolectric)
- Arm → screen off → confirm → alarm scheduled.
- Boot restore reschedules `SCHEDULED` record.

---

## Manual Tests (Pixel 8 / Android 14+)
- Next alarm appears in system UI/lockscreen after scheduling (verify system alarm indicator).
- Snooze updates next alarm display.
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms`
  - `sleep8://alarm/<id>`
- Notifications permission denied still rings; UI warns about reduced lockscreen UX.

---

## Emulator Disclaimer
Emulators are not authoritative for Doze, exact alarms, or lockscreen alarm indicators.
