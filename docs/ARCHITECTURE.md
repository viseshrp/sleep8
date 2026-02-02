# Sleep8 — System Architecture

## 1. Overview

Sleep8 schedules **app-owned exact alarms** using `AlarmManager.setExactAndAllowWhileIdle` (`RTC_WAKEUP`), then drives a full-screen alarm UI with a foreground ringing service. The app never delegates to the OS Clock app.

```
User
  │
  ▼
MainActivity / QS Tile ──► ArmManager ──► NightMonitorService
                                         │
                                         ▼
                                Screen OFF/ON events
                                         │
                                         ▼
                               ConfirmOffScheduler
                                         │ (screen off confirmed)
                                         ▼
                                 AlarmScheduler
                                         │  setExactAndAllowWhileIdle(RTC_WAKEUP)
                                         ▼
                                AlarmManager
                                         │
                                         ▼
                                 AlarmReceiver
                                         │
                                         ├──► AlarmRingingService (FGS)
                                         ├──► AlarmActivity (full-screen)
                                         └──► Optional Overlay (WindowManager)
```

---

## 2. Key Components

- **AlarmScheduler**
  - Calculates `triggerAt` using configured duration.
  - Persists metadata (`duration_used_minutes`, `alarm_instance_id`, `request_code`, `overlay_used`, `activity_presented`).
  - Schedules `AlarmManager.setExactAndAllowWhileIdle` (`RTC_WAKEUP`).

- **AlarmReceiver**
  - Receives the alarm clock operation.
  - Dedupes by `alarm_instance_id` and record status.
  - Starts ringing service and full-screen activity.

- **AlarmRingingService**
  - Foreground service only while ringing.
  - Uses ALARM-category notification with Dismiss/Snooze actions.
  - Shows optional overlay when user-enabled + permission granted.

- **AlarmActivity**
  - Full-screen, shows over lock screen, turns screen on.

---

## 3. Best-effort OS Integration

- **ACTION_SHOW_ALARMS** opens Alarm History.
- **Deep links**:
  - `sleep8://alarms`
  - `sleep8://alarm/<id>`

Android does not guarantee a third-party app can be the system default alarm app; this is best-effort.

---

## 4. Data Flow

- Screen-off confirmed → DB record written (`SCHEDULED`).
- Alarm fires → record set `FIRED`.
- Dismiss → record set `DISMISSED`.
- Snooze → original record set `SNOOZED`, new record scheduled.

---

## 5. Reboot Restore

- Restore armed state and pending confirmation timers.
- Reschedule latest `SCHEDULED` record; if overdue, fire immediately.
