# Sleep8 — System Architecture

## 1. Overview

Sleep8 schedules **app-owned exact alarms** using `AlarmManager.setAlarmClock`, then drives a full-screen alarm UI with a foreground ringing service. The app never delegates to the OS Clock app.

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
                                         │  setAlarmClock(AlarmClockInfo)
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
  - Schedules `AlarmManager.setAlarmClock` so system “next alarm” UI reflects the app’s alarm.
  - Enforces **single active alarm** by canceling previously scheduled alarms and marking them `CANCELED`.

- **AlarmReceiver**
  - Receives the alarm clock operation.
  - Dedupes by `alarm_instance_id` and record status.
  - Starts ringing service and full-screen activity.

- **AlarmRingingService**
  - Foreground service only while ringing.
  - Uses ALARM-category notification with Dismiss/Snooze actions.
  - Shows optional overlay when user-enabled + permission granted.

- **Navigation**
  - Hamburger menu includes **Alarm** entry.
  - Routes to active ringing UI if an alarm is ringing; otherwise opens alarm preview/history.
  - Alarm page is an AOSP-style list with toggle-only controls (no edits).

- **Alarm List Flow**
  - Alarm list screen reads from DB (`AlarmRecord`).
  - Toggle ON → `AlarmScheduler.enableExisting` → `AlarmManager.setAlarmClock` + DB status update.
  - Toggle OFF → `AlarmScheduler.cancelAlarm` → DB status update.
  - Single active invariant: enabling one alarm cancels other scheduled alarms.

- **AlarmActivity**
  - Full-screen, shows over lock screen, turns screen on.

- **Duration UI invariant**
  - Settings duration input is always **hours + minutes** (never minutes-only).

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
- Reschedule latest `SCHEDULED` record; cancel extras with reason `REBOOT_CLEANUP`; if overdue, fire immediately.
