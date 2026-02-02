# Sleep8 — System Architecture

## 1. Overview

Sleep8 is a native Android application that schedules **app-owned exact alarms** based on screen-off detection during a user-defined night window. The app uses a layered architecture with foreground services for monitoring, `AlarmManager` for exact alarms, and Room for persistence.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                          │
├─────────────────────────────────────────────────────────────────────┤
│  MainActivity  │  SettingsActivity  │  AlarmActivity  │  QS Tile     │
└────────┬───────────────┬────────────┬────────────┬──────────────────┘
         │               │            │            │
         ▼               ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DOMAIN LAYER                               │
├─────────────────────────────────────────────────────────────────────┤
│  ArmManager  │  StateMachine  │  AlarmScheduler  │  WindowSchedulers│
└────────┬───────────────┬────────────┬────────────┬──────────────────┘
         │               │            │            │
         ▼               ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER                               │
├─────────────────────────────────────────────────────────────────────┤
│  NightMonitorService │ AlarmReceiver │ AlarmRingingService │ BootRx │
└────────┬───────────────┬────────────┬───────────────┬───────────────┘
         │               │            │               │
         ▼               ▼            ▼               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Room Database  │  SharedPreferences  │  In-Memory StateHolder      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. High-Level Component Diagram

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
                                         │
                                         ▼
                              AlarmManager (exact)
                                         │
                                         ▼
                                 AlarmReceiver
                                         │
                                         ├──► AlarmRingingService (FGS)
                                         └──► AlarmActivity (full-screen)
```

---

## 3. Module Structure (key parts)

```
com.sleep8/
├── ui/
│   ├── main/...
│   ├── history/...
│   ├── settings/...
│   └── alarm/
│       ├── AlarmActivity.kt
│       ├── AlarmViewModel.kt
│       └── AlarmUiState.kt
├── domain/
│   ├── manager/
│   │   ├── ArmManager.kt
│   │   └── StateMachineManager.kt
│   ├── scheduler/
│   │   ├── AlarmScheduler.kt
│   │   ├── ConfirmOffScheduler.kt
│   │   └── NightWindowScheduler.kt
│   └── model/AlarmRecord.kt
├── service/
│   ├── NightMonitorService.kt
│   ├── AlarmRingingService.kt
│   └── receiver/
│       ├── AlarmReceiver.kt
│       ├── BootReceiver.kt
│       └── ConfirmationAlarmReceiver.kt
└── data/
    ├── db/...
    ├── repository/...
    └── preferences/...
```

---

## 4. Alarm Flow (owned)

1. Screen goes OFF during night window.
2. Confirm timer expires with screen still OFF.
3. `AlarmScheduler` schedules an exact alarm for `screen_off + 8 hours` and persists a `SCHEDULED` record.
4. `AlarmManager` fires → `AlarmReceiver`.
5. `AlarmReceiver` marks record `FIRED`, starts `AlarmRingingService`, and launches `AlarmActivity`.
6. User dismisses (or snoozes). DB is updated and the foreground service stops.

---

## 5. Reboot Restore

- `BootReceiver` restores armed state and pending confirmation timers.
- If a `SCHEDULED` alarm exists in DB, it is rescheduled.
- If `trigger_at` is already in the past, the alarm is scheduled to fire immediately.

---

## 6. Alarm Observability Data Flow (Read-Only UI)

- Alarm creation → `AlarmRepository` → `alarm_records` (Room) is the **only** source of truth.
- Home screen reads the **latest** alarm record to show “most recently scheduled alarm.”
- Alarm History screen reads **all** alarm records (newest → oldest).
- No dependency on the OS Clock app.
