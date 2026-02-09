# Sleep8 — System Architecture

## 1. Overview

Sleep8 uses app-owned exact alarms (`AlarmManager.setAlarmClock`) and a local ringing flow (receiver + service + full-screen UI). It does not delegate alarm creation to the OS Clock app.

High-level flow:

```
User (Home / QS Tile)
        │
        ▼
    ArmManager
        │
        ├── schedules Night-window boundaries (NightWindowScheduler)
        └── starts/stops NightMonitorService when armed + in night window
                                     │
                                     ▼
                           SCREEN_OFF / SCREEN_ON
                                     │
                                     ▼
                            StateMachineManager
                                     │
                                     ├── ConfirmOffScheduler (20m default)
                                     └── AlarmScheduler (setAlarmClock)
                                               │
                                               ▼
                                          AlarmReceiver
                                               │
                                               ├── AlarmRingingService (FGS)
                                               ├── AlarmRingingActivity (full-screen)
                                               └── AlarmOverlayController (optional)
```

---

## 2. Core Components

- `ArmManager`
  - Handles manual arm/disarm from app button and tile.
  - Maintains active arm session and schedules window boundaries.
  - Night window only gates monitoring; it does not force disarm.
  - Clears session-scoped `lastScreenOffTs` on disarm and on new arm session start.

- `StateMachineManager`
  - Owns runtime states: `DISARMED`, `ARMED_IDLE`, `ARMED_PENDING_CONFIRM`, `ARMED_ALARM_SET`.
  - Applies "latest screen-off wins" policy and coordinates confirmation timer.
  - Triggers alarm scheduling when confirmation succeeds.

- `ConfirmOffScheduler`
  - Uses `setExactAndAllowWhileIdle` for confirmation timeout.
  - Persists pending candidate/deadline in `AppPreferences`.

- `AlarmScheduler`
  - Computes alarm trigger from `screenOffTs + duration`.
  - Uses `setAlarmClock` so system "next alarm" can surface Sleep8's alarm.
  - Enforces single-active invariant by cancelling prior scheduled records.
  - Reconciles/cleans scheduled records after boot.

- `NightMonitorService`
  - Foreground service that listens for screen on/off broadcasts.
  - Updates monitoring notification based on armed/pending state.

- `AlarmReceiver`
  - Validates alarm record status + instance ID before firing.
  - Starts ringing service and ringing activity (or activity-only fallback if notifications denied).
  - Marks record `FIRED` and `activity_presented`.
  - Clears `lastScreenOffTs` when a fire event is accepted.

- `AlarmRingingService` and `AlarmRingingActivity`
  - Ring with alarm audio + vibration.
  - Expose dismiss-only action.
  - Optional overlay path controlled by settings + overlay permission.
  - Dismiss path also clears `lastScreenOffTs` for the completed alarm session.

---

## 3. Storage Boundaries

- Room (`Sleep8Database`)
  - `settings`, `arm_sessions`, `screen_events`, `alarm_records`
  - Durable product data and alarm lifecycle history.

- SharedPreferences (`AppPreferences`)
  - Runtime continuity keys (armed flag, active IDs, pending confirmation timestamps, theme mode).
  - Instance ID generation for alarm pending intent uniqueness.

---

## 4. Navigation and Surfaces

- `MainActivity` (home)
  - Arm/disarm, current status, latest scheduled alarm summary, and toggle-only alarm list.
  - Last screen-off text is shown while available for the active session (not date-gated).

- `AlarmHistoryActivity`
  - Full local audit trail and deep-link/ACTION_SHOW_ALARMS target.

- `SettingsActivity`
  - Night window, duration, confirm window, overlay toggle, reliability checklist, theme.

---

## 5. Reboot and Recovery

`BootReceiver` restores system behavior in this order:
- Restores active session and night-window monitoring state.
- Restores pending confirmation (immediate schedule if overdue and screen still off).
- Reconciles multiple scheduled alarms (keeps newest, cancels extras with `REBOOT_CLEANUP`).
- Reschedules surviving scheduled alarm; if overdue, schedules near-immediate trigger.

---

## 6. OS Integration (Best-effort)

- Handles `AlarmClock.ACTION_SHOW_ALARMS` via `AlarmHistoryActivity`.
- Supports deep links:
  - `sleep8://alarms`
  - `sleep8://alarm/<id>`

Android does not guarantee third-party default alarm ownership; integration remains best-effort.
