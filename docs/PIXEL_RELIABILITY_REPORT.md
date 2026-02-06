# Pixel 8 Pro Monitoring Reliability Report

Date: 2026-02-06

## 1) Root-cause analysis of observed behavior

### Observed symptom
- If app is unopened for long time, monitoring sometimes does not start at night window start.
- Launching app manually while still in-window makes monitoring start.

### Code-level root cause before fix
- `NightWindowStartReceiver` was the single boundary path for start-monitoring and directly called `startNightMonitorService`.
- `NightWindowScheduler` swallowed `SecurityException` from exact alarms and had no persisted boundary/start telemetry.
- No boundary backstop alarms, no periodic in-window health checks, and no time/timezone/package-replaced reschedule handling.
- Result: if boundary delivery/start was delayed/blocked (Doze bucket restrictions, alarm permission issues, process lifecycle quirks), there was no automatic late-start recovery except app foreground paths.

### What changed in this fix
- Added persisted boundary/start observability table: `monitoring_start_events`.
- Added backstop boundary triggers (+2 min, +10 min) and periodic in-window health checks (5 min).
- Added boot/time/timezone/package-replaced reconcile receiver coverage.
- Added app-launch reconcile (`MainViewModel` startup) for late-start self-heal and classification.
- Added human-readable reason buckets for failures and in-app Pixel guidance.

## 2) Pixel/Android state diagnosis matrix

| State | Can this prevent night window start from firing? | Can it prevent monitoring start even if boundary fired? | Evidence that proves this cause |
|---|---|---|---|
| App battery mode `Restricted` | Yes, can heavily defer/limit alarms/background execution in practice | Yes, service start can be deferred/blocked | `monitoring_start_events`: missing boundary execution + late app/backstop recovery, plus reason bucket `process not started` / `start attempt blocked` |
| Battery Saver ON | Sometimes (defer execution timing) | Sometimes (background start latency/denial) | Boundary observed much later than scheduled (`boundary_observed_at_ts - expected_boundary_ts` large) |
| Extreme Battery Saver ON (Pixel) | Yes, can suppress background for non-allowed apps | Yes | Consistent missed boundary + backstop + periodic checks until app launch/settings change; reason `app restricted / force-stopped suspected` |
| Doze / App Standby bucket | Yes (delivery delays) | Yes (start latency) | Scheduled boundary exists, no boundary execution record at expected time, later backstop/health record starts monitoring |
| Force-stop | Yes, effectively freezes background starts until user opens app | Yes | No boundary/backstop/health execution while stopped; immediate reconcile on app open; reason `app restricted / force-stopped suspected` |
| Exact alarm access not effectively available | Yes (boundary alarm may not be scheduled/fired as expected) | Indirectly yes | Missing boundary/backstop records around expected boundary; settings checklist indicates exact alarm disabled |
| Foreground service start restrictions (newer Android) | Boundary may fire | Yes (`startForegroundService` path can be blocked) | Boundary record with `boundary_trigger_executed=true` but `monitoring_active=false`, reason `start attempt blocked` |

## 3) Durability and observability implemented

Persisted records now include:
- Scheduled boundary time (`expected_boundary_ts`) and schedule timestamp (`scheduled_at_ts`)
- Actual observed boundary/trigger time (`boundary_observed_at_ts`)
- Gate snapshot at trigger time (`armed_at_boundary`, `in_night_window_at_boundary`, `gate_open`)
- Whether boundary trigger executed (`boundary_trigger_executed`)
- Whether monitoring became active and when (`monitoring_active`, `monitoring_activated_at_ts`)
- Human-readable reason bucket (`reason_bucket`)

Reason buckets:
- `boundary event did not run`
- `process not started`
- `start attempt blocked`
- `app restricted / force-stopped suspected`
- `unknown`

## 4) Never-miss contract summary (implemented)

- Contract trigger: start-monitoring event is required when `armed && inNightWindow` becomes true.
- Primary trigger: night window exact boundary alarm.
- Backstops: +2 minute and +10 minute alarms.
- Self-healing: periodic 5-minute in-window health checks.
- Reconcile hooks: app launch and boot/time/timezone/package update.
- Recovery target: when OS permits background execution, recovery starts within 5 minutes of the next allowed trigger.

Known unguaranteeable cases are explicitly documented:
- Force-stop
- Severe system restrictions (including Pixel Extreme Battery Saver configurations)

## 5) User-facing guidance added

In-app guidance now explicitly instructs Pixel users to:
- Set Sleep8 battery usage to **Unrestricted**
- Disable/avoid **Extreme Battery Saver** restrictions for Sleep8
- Re-open Sleep8 after force-stop to restore background execution

## 6) Verification delivered

Automated:
- Deterministic gating and late-start tests in `MonitoringReliabilityManagerTest`
- Updated receiver and manager tests for boundary/reconcile behavior

Manual:
- Pixel-focused checklist added to `docs/MANUAL_TESTS.md` for:
  - Optimized vs Restricted
  - Battery Saver ON/OFF
  - Extreme Battery Saver ON/OFF
  - App unopened 24h+
  - Reboot
  - Timezone/time change
  - Force-stop limitation

## 7) Reference docs used

- [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby#adapt-doze)
- [Exact alarms behavior and permissions](https://developer.android.com/develop/background-work/services/alarms)
- [Request `SCHEDULE_EXACT_ALARM`](https://developer.android.com/reference/android/provider/Settings#ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
- [Foreground service launch restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Stopped packages (`FLAG_EXCLUDE_STOPPED_PACKAGES` / `FLAG_INCLUDE_STOPPED_PACKAGES`)](https://developer.android.com/reference/android/content/Intent#FLAG_EXCLUDE_STOPPED_PACKAGES)
