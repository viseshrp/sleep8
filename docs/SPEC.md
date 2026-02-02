# spec.md — Sleep8: Owned Alarm (+8h)

## 0. One-liner
When the user **arms** the app (button or Quick Settings tile), the app watches for **screen-off** events during a **fixed night window**; once the screen has stayed off for **10 minutes**, it schedules an **app-owned exact alarm** for **8 hours after the original screen-off time**, and re-schedules from the **latest** screen-off event.

---

## 1. Goals
- **Automation**: user arms once; no further interaction needed.
- **Accuracy**: detect screen-off and schedule the app’s own **exact alarm** for `screen_off_time + 8 hours`.
- **Reliability mode**: foreground service at night + exact alarms + battery optimization guidance.
- **Offline-only**: no network calls; local storage only.
- **Auditability**: persist triggers/alarms in a local DB.

## 2. Non-goals
- Delegating alarms to the OS Clock app.
- Sleep stage tracking, health integrations, cloud sync.
- “Power button pressed” detection (we use screen state only).

---

## 3. Key Decisions (locked)
- Arming: **only when armed** (in-app button + Quick Settings tile).
- Night window: **fixed** start/end time configured by user.
- Rescheduling: **latest screen-off wins** (keep updating the scheduled time until confirmed).
- Confirm rule: only commit when **screen remains OFF for 10 minutes** after an OFF event.
- Alarm ownership: **app-owned** exact alarm via `AlarmManager` + receiver + full-screen activity.
- Snooze: configurable option in settings, uses app-owned exact alarms.
- Multiple alarms: **replace prior scheduled alarm** (one active at a time).
- Reboot: **restore state** and reschedule alarms from DB.
- Storage: persist “scheduled_at / trigger_at / source / status” and relevant timestamps.
- Privacy: **strictly offline**.

---

## 4. Platform Recommendation (low maintenance)
### Recommended:
- **minSdk: 31 (Android 12)**, **targetSdk: latest stable**
Why:
- Android 12+ has the “exact alarm” regime and consistent modern behavior around scheduling and restrictions; supporting older versions increases edge-case handling without real product value for this use case.

---

## 5. UX / User Flows

### 5.1 First-run setup
- User sets:
  - Night window start/end (e.g., 22:00–08:00).
  - Auto-arm schedule start/end (separate from night window; defaults to night window times).
  - Alarm duration is fixed: **+8 hours**.
  - Snooze option (default OFF or a chosen minutes value).
- App shows a “Reliability checklist”:
  - Exact alarm capability (Android 12+)
  - Battery optimization exclusion
  - Foreground service enabled during night window when armed

### 5.2 Arming
Two entry points:
1) In-app button: **Arm Tonight**
2) Quick Settings Tile: **Arm/Disarm**

Armed state shows:
- “Armed until: end of night window”
- Last screen-off detected time (if any)
- Pending confirmation timer (10 min) or confirmed alarm schedule time

### 5.3 During the night window
- Foreground service runs (persistent notification: “Sleep8 armed”).
- On `SCREEN_OFF`, store event and start/refresh a **10-minute confirmation timer**.
- If another `SCREEN_OFF` occurs before confirmation, **replace** the pending candidate (latest wins) and restart confirmation timer.
- If screen turns on before confirmation, cancel confirmation timer; keep armed.

### 5.4 Confirmation → alarm creation
When the screen has remained OFF for 10 minutes since the latest OFF event:
- Schedule an app-owned exact alarm for:  
  `alarm_time = latest_screen_off_time + 8 hours`
- Persist the alarm record in DB with status `SCHEDULED`.

### 5.5 Alarm firing
- `AlarmManager` delivers to `AlarmReceiver`.
- Receiver starts `AlarmRingingService` (foreground) and launches `AlarmActivity`.
- Alarm UI shows over lock screen, turns screen on, and rings until dismissed.

### 5.6 Dismiss / Snooze
- **Dismiss** stops audio/vibration, stops the foreground service, records `dismissed_at` in DB.
- **Snooze** schedules a new exact alarm (e.g., +10 minutes) and marks the original record as `SNOOZED`.

### 5.7 Disarming
- User can disarm anytime (button/tile).
- Disarm stops monitoring service and cancels pending confirmation timer.
- Disarm does not retroactively alter already-fired alarms.

### 5.8 Alarm Observability (Local Only)
- The app maintains a **local alarm log** in its DB (`alarm_records`).
- Home screen shows the **most recently scheduled alarm**.
- Alarm History screen shows the full alarm log (newest → oldest).

---

## 6. Behavior Rules (exact)

### 6.1 Night window enforcement
- Only react to screen events if:
  - `armed == true`
  - current local time is within night window
  - (If window crosses midnight, handle correctly.)

### 6.2 “Latest wins” reschedule policy
- Maintain a `pending_candidate_screen_off_time`.
- On every `SCREEN_OFF` within window:
  - set `pending_candidate_screen_off_time = now`
  - start/restart 10-minute confirmation countdown.
- On `SCREEN_ON`:
  - cancel countdown; candidate remains in DB as last observed but not confirmed.

### 6.3 Alarm creation
- Create a **new app-owned exact alarm** each time confirmation succeeds.
- Cancel/replace any previously scheduled app-owned alarm.

### 6.4 Reboot handling
If armed at reboot or there was a pending confirmation:
- Restore armed state and continue monitoring if still within night window.
- If there was a pending candidate and the screen is currently OFF, re-evaluate confirmation using stored timestamps:
  - If `now - pending_candidate_screen_off_time >= 10 minutes` then schedule alarm immediately.
  - Else resume timer for the remaining duration.
- If a scheduled alarm exists in DB and its `trigger_at` is in the past, schedule it to fire immediately.

---

## 7. Alarm Ownership (App)

### 7.1 Target mechanism
Use `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP)` with an app-owned `BroadcastReceiver`.

### 7.2 Alarm UI
- `AlarmActivity` is full-screen, shows over lock screen, and turns screen on.
- Alarm uses `AudioManager.STREAM_ALARM` semantics with looping sound and repeating vibration.
- Foreground service runs **only while ringing**.

---

## 8. Permissions & System Settings

### 8.1 Required / conditional
- Foreground service:
  - `FOREGROUND_SERVICE`
- Reboot:
  - `RECEIVE_BOOT_COMPLETED`
- Exact alarm (Android 12+):
  - `SCHEDULE_EXACT_ALARM` (or request capability flow as required)

### 8.2 Battery optimization
- Provide guided UI to request the user to exclude the app from battery optimizations.
- Store `battery_opt_out_ack` in settings.

---

## 9. Data & Storage

### 9.1 Database (Room recommended)
Tables:

#### `settings`
- `id` (singleton)
- `night_start` (HH:MM)
- `night_end` (HH:MM)
- `auto_arm_start` (HH:MM)
- `auto_arm_end` (HH:MM)
- `auto_arm_enabled` (bool, default false)
- `confirm_off_minutes` (default 10)
- `snooze_minutes` (nullable / default null)
- `armed_default` (bool, default false)
- `offline_only` (bool, always true)

#### `arm_sessions`
- `session_id` (pk)
- `armed_at` (timestamp)
- `disarmed_at` (timestamp nullable)
- `window_start_ts` (timestamp)
- `window_end_ts` (timestamp)
- `source` (enum: APP_BUTTON | QUICK_TILE)

#### `screen_events`
- `event_id` (pk)
- `session_id` (fk)
- `type` (enum: SCREEN_OFF | SCREEN_ON)
- `ts` (timestamp)

#### `alarm_records`
- `alarm_id` (pk)
- `session_id` (fk)
- `screen_off_ts` (timestamp)         # triggering OFF
- `confirmed_at` (timestamp)          # after 10 min off
- `scheduled_at` (timestamp)          # record creation time
- `trigger_at` (timestamp)            # off + 8h or snooze time
- `source` (enum: SLEEP_AUTOMATION | SNOOZE)
- `status` (enum: SCHEDULED | FIRED | DISMISSED | SNOOZED)
- `fired_at` (timestamp nullable)
- `dismissed_at` (timestamp nullable)
- `snoozed_until` (timestamp nullable)

### 9.2 In-memory state (single source of truth mirrored from DB)
- `armed: bool`
- `active_session_id`
- `pending_candidate_screen_off_ts`
- `pending_confirm_deadline_ts`
- `night_window_start_ts`, `night_window_end_ts`

---

## 10. Components (Implementation Design)

### 10.1 Core modules
- `ArmManager`
  - start/stop session, persist session, manage service lifecycle
- `NightMonitorService` (foreground)
  - registers runtime receivers
  - maintains timers / deadlines
- `ScreenStateReceiver` (runtime registered)
  - handles SCREEN_OFF / SCREEN_ON events
- `ConfirmOffScheduler`
  - manages the 10-minute confirmation logic (via exact alarm or handler + persistence)
- `AlarmScheduler`
  - schedules exact alarm using `AlarmManager`
  - cancels/replaces prior alarm
  - persists alarm record status
- `AlarmReceiver`
  - entrypoint for alarm firing
- `AlarmRingingService`
  - foreground service for active alarm sound/vibration
- `AlarmActivity`
  - full-screen UI for dismissal/snooze
