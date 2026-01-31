# spec.md — Sleep8: Armed Screen-Off → OS Alarm (+8h)

## 0. One-liner
When the user **arms** the app (button or Quick Settings tile), the app watches for **screen-off** events during a **fixed night window**; once the screen has stayed off for **10 minutes**, it schedules a **real OS alarm** for **8 hours after the original screen-off time**, and re-schedules from the **latest** screen-off event.

---

## 1. Goals
- **Automation**: user arms once; no further interaction needed.
- **Accuracy**: detect screen-off and set an **actual system alarm** (Clock app) for `screen_off_time + 8 hours`.
- **Reliability mode**: foreground service at night + exact alarms + battery optimization guidance.
- **Offline-only**: no network calls; local storage only.
- **Auditability**: persist triggers/alarms in a local DB.

## 2. Non-goals
- Being an alarm app, custom alarm UI, or custom alarm sounds.
- Sleep stage tracking, health integrations, cloud sync.
- “Power button pressed” detection (we use screen state only).

---

## 3. Key Decisions (locked)
- Arming: **only when armed** (in-app button + Quick Settings tile).
- Night window: **fixed** start/end time configured by user.
- Rescheduling: **latest screen-off wins** (keep updating the scheduled time until confirmed).
- Confirm rule: only commit when **screen remains OFF for 10 minutes** after an OFF event.
- Alarm integration: create an **OS/Clock app alarm**, not a custom alarm UI.
- Snooze: configurable option in settings (applies to OS alarm if supported).
- Multiple alarms: **allowed** (create a new one; keep both).
- Reboot: **restore state** from DB and continue/restore scheduled alarms.
- Storage: persist “set time” and all relevant state.
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
  - “Alarm lead time” is fixed: **+8 hours** (not user-editable in v1 unless explicitly desired later).
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
- Create an OS alarm for:  
  `alarm_time = latest_screen_off_time + 8 hours`
- Persist alarm creation record in DB.

### 5.5 Disarming
- User can disarm anytime (button/tile).
- Disarm stops monitoring service and cancels pending confirmation timer.
- Does **not** delete already created OS alarms (we’re not the alarm app).

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
- Create a **new OS alarm** each time confirmation succeeds.
- Don’t attempt to dedupe/overwrite prior OS alarms.

### 6.4 Reboot handling
If armed at reboot or there was a pending confirmation:
- Restore armed state and continue monitoring if still within night window.
- If there was a pending candidate and the screen is currently OFF, re-evaluate confirmation using stored timestamps:
  - If `now - pending_candidate_screen_off_time >= 10 minutes` then schedule alarm immediately.
  - Else resume timer for the remaining duration.

---

## 7. OS Alarm Integration (Clock app)

### 7.1 Target mechanism
Use a best-effort strategy to create an alarm in the system’s clock/alarm app:
- Primary: `AlarmClock.ACTION_SET_ALARM` intent with extras:
  - hour/minute derived from alarm_time (local)
  - optional extras for message/skip UI/snooze (where supported)

**Important constraint**: Some OEM clock apps may ignore “skip UI” or require UI confirmation. The app should:
- Detect if the intent can resolve.
- If UI is required by the OS clock app, show a one-time warning: “Your clock app requires confirmation”.

### 7.2 Exact timing
In addition to creating the OS alarm entry, schedule an internal exact wake trigger (for resilience and telemetry) using `AlarmManager.setExactAndAllowWhileIdle`.  
This internal trigger is NOT the user’s alarm UI; it’s a backstop/logging hook.

---

## 8. Permissions & System Settings

### 8.1 Required / conditional
- Foreground service:
  - `FOREGROUND_SERVICE`
  - (Add foreground service type if needed by targetSdk)
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
- `scheduled_alarm_ts` (timestamp)    # off + 8h
- `os_alarm_intent_resolved` (bool)
- `os_alarm_ui_required` (bool nullable)
- `internal_backstop_scheduled` (bool)

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
- `OsAlarmCreator`
  - constructs and fires `ACTION_SET_ALARM` intents
  - determines resolvability and UI behavior when possible
- `BackstopAlarmScheduler`
  - uses AlarmManager exact to log/verify and to re-run flows after Doze/reboot
- `BootReceiver`
  - restores active session / pending confirmations

---

## 11. State Machine

States:
- `DISARMED`
- `ARMED_IDLE` (armed, waiting for screen-off)
- `ARMED_PENDING_CONFIRM` (have candidate OFF + deadline)
- `ARMED_ALARM_SET` (alarm created; still armed and can create more if new OFF+confirm occurs)

Transitions:
- DISARMED → ARMED_IDLE (arm)
- ARMED_IDLE → ARMED_PENDING_CONFIRM (SCREEN_OFF in window)
- ARMED_PENDING_CONFIRM → ARMED_IDLE (SCREEN_ON before deadline)
- ARMED_PENDING_CONFIRM → ARMED_ALARM_SET (deadline reached while still OFF)
- ARMED_ALARM_SET → ARMED_PENDING_CONFIRM (new SCREEN_OFF occurs later)
- Any ARMED_* → DISARMED (disarm)

---

## 12. Acceptance Criteria (must pass)
1. When armed and within night window, a SCREEN_OFF starts a 10-minute confirmation.
2. If the screen turns ON before 10 minutes, no OS alarm is created.
3. If the screen remains OFF for 10 minutes, the app creates an OS alarm for `screen_off + 8 hours`.
4. If multiple SCREEN_OFF events occur before confirmation, the latest OFF is used (latest wins).
5. The app allows multiple OS alarms to exist; it does not delete prior alarms.
6. After reboot, the app restores:
   - armed state/session
   - pending confirmation timer (remaining time)
   - and schedules alarm if deadline already passed and screen is OFF.
7. All events and alarm records are persisted in DB.
8. No network traffic is performed (verified by build config + runtime checks).

---

## 13. Known Constraints / Risks
- Some OEM Clock apps may:
  - ignore “skip UI”, requiring manual confirmation
  - limit setting alarms programmatically
- Battery optimization settings vary by OEM; provide guidance but can’t guarantee.
- “Screen off” is not a perfect proxy for “sleep”, but confirmation delay reduces false positives.

---

## 14. Agentic AI Implementation Plan (what the coding agent should do)

### Milestone A — Project skeleton
- Create Android app module + Room DB + settings screen.
- Add Quick Settings Tile with arm/disarm.

### Milestone B — Monitoring & confirmation
- Foreground service with runtime receiver for screen events.
- Implement confirmation deadline persistence + restoration.

### Milestone C — Alarm creation
- Implement OsAlarmCreator with best-effort `ACTION_SET_ALARM`.
- Add internal backstop exact AlarmManager scheduling for confirmations.

### Milestone D — Reboot + resilience
- BootReceiver restores session and pending confirmations.

### Milestone E — QA checklist
- Manual test cases:
  - normal flow, cancel flow, multiple OFF events, reboot mid-confirm, window crossing midnight, battery optimization enabled/disabled.

---

## 15. Open Questions (none blocking, but document)
- Default night window times (propose: 22:00–08:00).
- Default snooze value when enabled (propose: 10 minutes).
- Whether +8h should ever be configurable in settings (currently fixed).