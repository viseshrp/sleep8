# Sleep8 — Testing Plan (Owned Alarm Model)

## Overview
This plan covers unit tests, integration tests, and manual validation for the app-owned exact alarm flow. **Physical device testing is required**; emulators are not authoritative for exact alarm delivery or lock-screen behavior.

---

## 1. Unit Tests (required)

### 1.1 Alarm trigger time calculation
- Verify `screen_off + 8 hours` is used for scheduling.

### 1.2 Snooze calculation
- Verify `now + snooze_minutes` for the next trigger.

### 1.3 Persistence logic
- `AlarmRecord` inserts include `scheduled_at`, `trigger_at`, `source`, `status`.
- Status updates set `fired_at` / `dismissed_at` / `snoozed_until`.

---

## 2. Integration Tests (Robolectric)
- Full flow: arm → screen off → confirm → alarm scheduled.
- Boot restore: scheduled record is rescheduled on boot.

---

## 3. Manual Tests (physical device)
- Screen off → confirmation → alarm fires ~8 hours later.
- Alarm fires while device locked.
- Alarm fires in Doze (device idle overnight).
- Alarm fires after app process death.
- Alarm fires after reboot.
- Dismiss stops sound immediately.

**Note:** Emulator behavior is not authoritative for alarm timing or lock-screen UI.
