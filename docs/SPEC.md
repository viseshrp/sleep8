# spec.md — Sleep8: Owned Alarm (Configurable Duration)

## 0. One-liner
When the user **arms** the app (button or Quick Settings tile), the app watches for **screen-off** events during a **fixed night window**; once the screen has stayed off for **10 minutes**, it schedules an **app-owned exact alarm** for **screen-off + configured duration** (default **8 hours**) using `AlarmManager.setExactAndAllowWhileIdle` (`RTC_WAKEUP`), and always uses the **latest** screen-off event.

---

## 1. Goals
- **Automation**: user arms once; no further interaction needed after screen-off.
- **Accuracy**: detect screen-off and schedule the app’s own **exact alarm** for `screen_off_time + duration`.
- **Reliability**: exact alarm semantics + reboot restore + Doze resistance.
- **Offline-only**: no network calls; local storage only.
- **Auditability**: persist alarm metadata in a local DB.

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
- Alarm ownership: **app-owned** exact alarm via `AlarmManager.setExactAndAllowWhileIdle` (`RTC_WAKEUP`) → receiver → foreground ringing service → full-screen activity (optional overlay).
- Duration: **configurable**, default **8 hours**.
- Snooze: configurable option in settings, uses app-owned alarms.
- Reboot: **restore state** and reschedule alarms from DB.
- Storage: persist `duration_used_minutes`, `alarm_instance_id`, `request_code`, `snoozed_at`, `snoozed_until`, `overlay_used`, `activity_presented`.
- Privacy: **strictly offline**.

---

## 4. Platform Recommendation (low maintenance)
### Recommended:
- **minSdk: 31 (Android 12)**, **targetSdk: latest stable**

---

## 5. UX / User Flows

### 5.1 First-run setup
- User sets:
  - Night window start/end (e.g., 22:00–08:00).
  - Auto-arm schedule start/end (separate from night window; defaults to night window times).
  - **Alarm duration** in hours/minutes (default 8h).
  - Snooze option (default OFF or a chosen minutes value).
- App shows a “Reliability checklist”:
  - Exact alarm capability (Android 12+)
  - Notifications permission (Android 13+)
  - Battery optimization exclusion
  - Optional overlay permission

### 5.2 Arming
Two entry points:
1) In-app button: **Arm Tonight**
2) Quick Settings Tile: **Arm/Disarm**

Armed state shows:
- “Armed until: end of night window”
- Last screen-off detected time (if any)
- Pending confirmation timer (10 min) or confirmed alarm schedule time

Navigation:
- Hamburger menu includes **Alarm** entry:
  - If ringing → opens active Alarm UI.
  - If not ringing → opens alarm preview (latest scheduled alarm detail or history).


### 5.3 During the night window
- Foreground service runs (persistent notification: “Sleep8 armed”).
- On `SCREEN_OFF`, store event and start/refresh a **10-minute confirmation timer**.
- If another `SCREEN_OFF` occurs before confirmation, **replace** the pending candidate (latest wins) and restart confirmation timer.
- If screen turns on before confirmation, cancel confirmation timer; keep armed.

### 5.4 Confirmation → alarm creation
When the screen has remained OFF for 10 minutes since the latest OFF event:
- Schedule an app-owned **exact alarm** for:  
  `alarm_time = latest_screen_off_time + duration`
- Persist the alarm record in DB with status `SCHEDULED` and `duration_used_minutes` snapshot.
- Optionally show a low-importance “alarm scheduled” notification.

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
Use `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)` with an app-owned `BroadcastReceiver`.

### 7.2 Alarm UI
- `AlarmActivity` is full-screen, shows over lock screen, and turns screen on.
- Alarm uses `AudioManager.STREAM_ALARM` semantics with looping sound and repeating vibration.
- Foreground service runs **only while ringing**.
- Optional overlay (if user-enabled + permission granted) shows a full-screen WindowManager UI while ringing.

### 7.3 Best-effort OS integration
- Handle `AlarmClock.ACTION_SHOW_ALARMS` to open the app’s alarm history screen.
- Support deep links:
  - `sleep8://alarms` (history)
  - `sleep8://alarm/<id>` (specific record)
- Android does not guarantee a third-party app can be the system’s default alarm app; this is best-effort.

---

## 8. Permissions & System Settings

### 8.1 Required / conditional
- Foreground service:
  - `FOREGROUND_SERVICE`
- Reboot:
  - `RECEIVE_BOOT_COMPLETED`
- Exact alarm (Android 12+):
  - `SCHEDULE_EXACT_ALARM` (or request capability flow as required)
- Notifications (Android 13+):
  - `POST_NOTIFICATIONS` (runtime request once)
- Optional overlay:
  - `SYSTEM_ALERT_WINDOW` (only if user enables overlay)

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
- `alarm_duration_minutes` (default 480)
- `overlay_enabled` (bool, default false)
- `armed_default` (bool, default false)

#### `alarm_records`
- `alarm_id` (pk)
- `session_id` (fk)
- `screen_off_ts` (timestamp)         # triggering OFF
- `confirmed_at` (timestamp)          # after 10 min off
- `scheduled_at` (timestamp)          # record creation time
- `trigger_at` (timestamp)            # off + duration or snooze time
- `duration_used_minutes` (int)
- `alarm_instance_id` (long)
- `request_code` (int)
- `source` (enum: SLEEP_AUTOMATION | SNOOZE)
- `status` (enum: SCHEDULED | FIRED | DISMISSED | SNOOZED)
- `fired_at` (timestamp nullable)
- `dismissed_at` (timestamp nullable)
- `snoozed_at` (timestamp nullable)
- `snoozed_until` (timestamp nullable)
- `overlay_used` (bool)
- `activity_presented` (bool)
