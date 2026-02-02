# Sleep8 — Manual Test Cases (Owned Alarm)

These cases require a **physical device** because exact alarms, lock-screen UI, and OEM power management are not reliable on emulators.

## Core Flow

### TC-001: Screen Off → Confirmation → Alarm Fires (P0)
- Preconditions: Armed, within night window
- Steps: Turn screen off → wait 10+ minutes → wait until +8 hours
- Expected: Alarm fires at ~8 hours after confirmed screen-off

### TC-002: Alarm Fires While Locked (P0)
- Preconditions: Alarm scheduled
- Steps: Lock device before trigger
- Expected: Full-screen AlarmActivity shows over lock screen and rings

### TC-003: Alarm Fires in Doze (P0)
- Preconditions: Alarm scheduled
- Steps: Leave device idle overnight
- Expected: Alarm still fires at scheduled time

### TC-004: Alarm Fires After Process Death (P0)
- Preconditions: Alarm scheduled
- Steps: Force stop app
- Expected: Alarm still fires at scheduled time

### TC-005: Alarm Fires After Reboot (P0)
- Preconditions: Alarm scheduled
- Steps: Reboot device
- Expected: Alarm rescheduled and fires; if trigger time passed, fires immediately after boot

### TC-006: Dismiss Stops Sound Immediately (P0)
- Preconditions: Alarm ringing
- Steps: Tap Dismiss
- Expected: Sound/vibration stop immediately; alarm UI closes

## Snooze (optional)

### TC-010: Snooze Schedules New Alarm (P1)
- Preconditions: Snooze enabled
- Steps: Let alarm ring → tap Snooze
- Expected: Current alarm stops; new alarm scheduled for +snooze minutes

## Confirmation Logic

### TC-020: Screen On Cancels Confirmation (P0)
- Preconditions: Armed, confirmation pending
- Steps: Turn screen on before confirmation timer ends
- Expected: Confirmation cancels; no alarm scheduled

### TC-021: Latest Screen Off Wins (P0)
- Preconditions: Armed, within night window
- Steps: Screen off → on → off again → wait confirmation
- Expected: Alarm scheduled based on latest screen-off time

## Reliability / Permissions

### TC-030: Exact Alarm Permission Missing (P0)
- Preconditions: Exact alarm permission revoked
- Steps: Trigger confirmation
- Expected: Warning shown guiding user to settings; alarm scheduling blocked and logged
