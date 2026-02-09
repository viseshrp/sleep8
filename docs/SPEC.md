# spec.md — Sleep8: Owned Alarm (Configurable Duration)

## 0. One-liner
When the user **arms** the app (button or Quick Settings tile), the app watches for **screen-off** events during a **fixed night window**; once the screen has stayed off for **20 minutes**, it schedules an **app-owned exact alarm** for **screen-off + configured duration** (default **8h 0m**, range **0-720 minutes**) using `AlarmManager.setAlarmClock` (so the system “next alarm” UI reflects it), and always uses the **latest** screen-off event. Duration **0** rings immediately at confirmation time.

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
- No scheduled arming/disarming; armed state changes only from manual actions.
- Night window: **fixed** start/end time configured by user.
- Rescheduling: **latest screen-off wins** (keep updating the scheduled time until confirmed).
- Confirm rule: only commit when **screen remains OFF for 20 minutes** after an OFF event.
- Alarm ownership: **app-owned** exact alarm via `AlarmManager.setAlarmClock` → receiver → foreground ringing service → full-screen activity (optional overlay).
- **Single active alarm**: at most one scheduled (not fired) alarm exists at any time.
- Duration: **configurable**, default **8h 0m** (480 minutes).
- Duration UI is **always hours + minutes inputs**. Never minutes-only; never hours-only.
- Invalid duration values are rejected and not persisted until corrected.
- Minutes >= 60 are normalized into hours; total is clamped to 0-720 minutes.
- **No snooze**: snooze is not supported anywhere in the app.
- **Theme default**: dark mode is default on fresh install.
- Theme control: Settings (hamburger → Settings) includes a global **Dark mode** toggle (On/Off) that applies app-wide and persists.
- Reboot: **restore state** and reschedule alarms from DB.
- Storage: persist `duration_used_minutes`, `alarm_instance_id`, `request_code`, `overlay_used`, `activity_presented`.
- Privacy: **strictly offline**.

---

## 4. Platform Recommendation (low maintenance)
### Recommended:
- **minSdk: 31 (Android 12)**, **targetSdk: latest stable**

---

## 5. UX / User Flows

### 5.1 First-run setup
- App launches with Android splash screen (logo + app name) and transitions into Home after startup initialization.
- User sets:
  - Night window start/end (default **22:30–04:00**).
  - **Alarm duration** in hours + minutes (0-720 total, default 8h 0m).
  - Dark mode preference in Settings (defaults to On).
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
- Pending confirmation timer (20 min) or confirmed alarm schedule time
- Material card/list styling with consistent top app bars and spacing.
- Last screen-off stays visible for the active armed session even if midnight passes.

Navigation:
- Hamburger menu includes **Alarm History** and **Settings**.
- Alarm management stays on Home via an **Alarm list** section (toggle-only).


### 5.3 During the night window
- Foreground service runs (persistent notification: “Sleep8 armed”).
- On `SCREEN_OFF`, store event and start/refresh a **20-minute confirmation timer**.
- If another `SCREEN_OFF` occurs before confirmation, **replace** the pending candidate (latest wins) and restart confirmation timer.
- If screen turns on before confirmation, cancel confirmation timer; keep armed.

### 5.4 Confirmation → alarm creation
When the screen has remained OFF for 20 minutes since the latest OFF event:
- Schedule an app-owned **exact alarm** for:  
  `alarm_time = latest_screen_off_time + duration` (if duration = 0, ring immediately at confirmation time)
- Persist the alarm record in DB with status `SCHEDULED` and `duration_used_minutes` snapshot.
- Optionally show a low-importance “alarm scheduled” notification.

### 5.5 Alarm firing
- `AlarmManager` delivers to `AlarmReceiver`.
- Receiver starts `AlarmRingingService` (foreground) and launches `AlarmRingingActivity`.
- Alarm UI shows over lock screen, turns screen on, and rings until dismissed.
- Ringing UI is AOSP Clock-like: full-screen, large current time, subtle label, alarm info line, one sticky bottom **Dismiss** action, no app bar or nav chrome.
- Overlay page is shown only when both conditions are true: `overlay_enabled == true` and `SYSTEM_ALERT_WINDOW` permission is granted. Otherwise full-screen activity is used.
- When an alarm is accepted as fired, `last_screen_off_ts` is cleared immediately.

### 5.6 Dismiss
- **Dismiss** stops audio/vibration, stops the foreground service, records `dismissed_at` in DB.
- Dismiss behavior is identical for overlay and full-screen activity presentations.
- Dismiss also clears `last_screen_off_ts` (idempotent with fire-path clearing).

### 5.10 App Icon
- Launcher icon uses a new adaptive icon set (foreground/background + monochrome).
- Round icon and legacy fallbacks are updated.
- Notification alarm icon is monochrome and separate from launcher icon.

### 5.7 Disarming
- User can disarm anytime (button/tile).
- Disarm stops monitoring service and cancels pending confirmation timer.
- Manual disarm does not cancel existing alarms; it only prevents new alarms and clears pending confirmation.
- Disarm does not retroactively alter already-fired alarms.
- Manual disarm clears `last_screen_off_ts`.

### 5.8 Alarm Observability (Local Only)
- The app maintains a **local alarm log** in its DB (`alarm_records`).
- Home screen shows the **most recently scheduled alarm**.
- Alarm History screen shows the full alarm log (newest → oldest).
- Alarm History includes a **Clear** action with a confirmation dialog to delete all history records.

### 5.9 Alarm List (Home section)
- Home includes an **Alarm list** section showing **current alarms** (not history).
- Each row shows time, a subtitle, and a toggle.
- Users can **enable/disable** alarms; **no edits** to time/label.
- Past alarms appear **disabled** and cannot be toggled on.
- **Single active alarm** policy applies: enabling one alarm disables other enabled alarms automatically.

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
  - start/restart 20-minute confirmation countdown.
- On `SCREEN_ON`:
  - cancel countdown; candidate remains in DB as last observed but not confirmed.

### 6.3 Alarm creation
- Create a **new app-owned exact alarm** each time confirmation succeeds.
- Cancel/replace any previously scheduled app-owned alarm and mark them `CANCELED` with reason `REPLACED_BY_NEW_ALARM`.

### 6.4 Single active alarm invariant
- Only one `SCHEDULED` alarm may exist at a time.

### 6.5 Reboot handling
If armed at reboot or there was a pending confirmation:
- Restore armed state and continue monitoring if still within night window.
- If there was a pending candidate and the screen is currently OFF, re-evaluate confirmation using stored timestamps:
  - If `now - pending_candidate_screen_off_time >= 20 minutes` then schedule alarm immediately.
  - Else resume timer for the remaining duration.
- If multiple scheduled alarms exist in DB, keep only the newest and cancel others with reason `REBOOT_CLEANUP`.
- If a scheduled alarm exists in DB and its `trigger_at` is in the past, schedule it to fire immediately.

### 6.6 Last screen-off lifecycle
- `last_screen_off_ts` is shown whenever it exists for the active session, including across midnight.
- `last_screen_off_ts` is cleared on:
  - accepted alarm fire (`AlarmReceiver`)
  - ringing dismiss action
  - manual disarm
  - start of a new arm session
- It is **not** cleared just because the local date changed.

---

## 7. Alarm Ownership (App)

### 7.1 Target mechanism
Use `AlarmManager.setAlarmClock(AlarmClockInfo(triggerAt, showIntent), operation)` with an app-owned `BroadcastReceiver`, so the system lockscreen “next alarm” reflects the app’s alarm.

### 7.2 Alarm UI
- `AlarmRingingActivity` is full-screen, shows over lock screen, and turns screen on.
- UI is AOSP-like: large time, subtle label, alarm info, sticky red **Dismiss** action.
- Alarm uses `AudioManager.STREAM_ALARM` semantics with looping sound and repeating vibration.
- Foreground service runs **only while ringing**.
- Optional overlay (only if user-enabled + permission granted) shows the same ringing UI while ringing.

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
- `night_start` (HH:MM, default 22:30)
- `night_end` (HH:MM, default 04:00)
- `confirm_off_minutes` (default 20)
- `alarm_duration_minutes` (0-720, default 480)
- `overlay_enabled` (bool, default false)
- `armed_default` (bool, default false)

#### `alarm_records`
- `alarm_id` (pk)
- `session_id` (fk)
- `screen_off_ts` (timestamp)         # triggering OFF
- `confirmed_at` (timestamp)          # after 20 min off
- `scheduled_at` (timestamp)          # record creation time
- `trigger_at` (timestamp)            # off + duration
- `duration_used_minutes` (int)
- `alarm_instance_id` (long)
- `request_code` (int)
- `source` (enum: SLEEP_AUTOMATION)
- `status` (enum: SCHEDULED | FIRED | DISMISSED | CANCELED)
- `canceled_reason` (enum: REPLACED_BY_NEW_ALARM | USER_DISARM | USER_TOGGLE_OFF | REBOOT_CLEANUP)
- `fired_at` (timestamp nullable)
- `dismissed_at` (timestamp nullable)
- `overlay_used` (bool)
- `activity_presented` (bool)

#### `monitoring_start_events`
- `id` (pk)
- `expected_boundary_ts` (timestamp) # scheduled night window start boundary
- `scheduled_at_ts` (timestamp) # when the boundary trigger was scheduled
- `boundary_observed_at_ts` (timestamp nullable) # actual observed time of trigger execution
- `armed_at_boundary` (bool)
- `in_night_window_at_boundary` (bool)
- `gate_open` (bool) # `armed && inNightWindow`
- `boundary_trigger_executed` (bool)
- `monitoring_active` (bool)
- `monitoring_activated_at_ts` (timestamp nullable)
- `reason_bucket` (string) # human-readable reason when monitoring is not active
- `trigger_source` (string) # schedule/boundary/backstop/health/app/boot
- `created_at_ts` (timestamp)

---

## 10. Reliability Contract: Monitoring Start Guarantees

### 10.1 Guaranteed behavior under normal OS conditions
- If `armed && inNightWindow` becomes true at night window start, monitoring will become active automatically without user interaction.
- “Monitoring active” is defined as: `NightMonitorService` is running and screen events (`SCREEN_OFF` / `SCREEN_ON`) are being observed by the state machine.

### 10.2 Reliability strategy implemented
- Primary boundary trigger: exact night-window start alarm.
- Backstops: additional exact alarms at +2 minutes and +10 minutes after boundary.
- Self-healing health checks: periodic (15-minute interval) checks while `armed && inNightWindow`.
- Reconcile triggers: app launch and boot/time/timezone/package-replaced events reconcile monitoring start and re-schedule boundaries.
- Late start behavior: if the boundary was missed but current state is `armed && inNightWindow`, monitoring is started as soon as any backstop/health/reconcile trigger executes.

### 10.3 Recovery SLO
- Under normal OS background execution conditions (not force-stopped, not severely restricted), missed boundary start recovers within 15 minutes of the next allowed trigger.
- With backstop and periodic checks, expected practical recovery is usually within 2-10 minutes after boundary.

### 10.4 Known non-guaranteeable cases
- Force-stop: Android places apps in stopped state; alarms/jobs/implicit background delivery are effectively frozen until user launches the app again.
- Severe background restrictions (including Pixel Extreme Battery Saver modes) can suppress/defer alarm delivery and service starts.
- If exact alarms are not granted, boundary precision and reliability degrade.

### 10.5 Detection and reporting of misses
- Every scheduled boundary and trigger attempt is persisted to `monitoring_start_events`.
- Records include scheduled vs observed boundary time, gate state, trigger execution, monitoring activation result, and reason bucket.
- Reason buckets:
  - `boundary event did not run`
  - `process not started`
  - `start attempt blocked`
  - `app restricted / force-stopped suspected`
  - `unknown`

### 10.6 User-visible health status
- Home shows “Monitoring health” with one of:
  - `Healthy (not required now)`
  - `Healthy (monitoring active)`
  - `Degraded (monitoring should be active)`
- When degraded, the app surfaces actionable Pixel guidance (Unrestricted battery, disable Extreme Battery Saver for Sleep8, reopen app after force-stop).

### 10.7 Acceptance criteria (contract-aligned)
- Monitoring starts at night window start when `armed && inNightWindow` without user interaction.
- If boundary start is missed but background execution later becomes available, self-healing starts monitoring within recovery SLO.
- Monitoring never starts when disarmed or outside the night window.
- Persisted records prove after-the-fact whether contract was met for each boundary.
- Known non-guaranteeable cases are detected, classified, and communicated in-app and in docs.
