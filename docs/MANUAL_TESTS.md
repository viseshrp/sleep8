# Sleep8 — Manual Test Cases

These cases are manual because they require physical device interaction, OEM clock/battery behavior, screen on/off, reboot flows, or system settings dialogs that cannot be reliably automated in instrumentation tests.

## Arming & Disarming

### TC-001: Arm via App Button (P0)
- Preconditions: App installed, permissions granted, within night window
- Steps: Open app → Tap "Arm Tonight"
- Expected: Button shows "Disarm"; status shows Armed; notification appears; Quick Settings tile active

### TC-002: Arm via Quick Settings Tile (P0)
- Preconditions: App installed, permissions granted, tile added
- Steps: Pull down Quick Settings → Tap Sleep8 tile
- Expected: Tile active; notification appears; app UI shows armed state

### TC-003: Disarm via App Button (P0)
- Preconditions: App is armed
- Steps: Open app → Tap "Disarm"
- Expected: Button shows "Arm Tonight"; notification removed; tile inactive

### TC-004: Disarm via Quick Settings Tile (P0)
- Preconditions: App is armed
- Steps: Pull down Quick Settings → Tap Sleep8 tile
- Expected: Tile inactive; notification removed

### TC-005: Arm Outside Night Window (P1)
- Preconditions: Current time outside night window
- Steps: Open app → Tap "Arm Tonight"
- Expected: Warning/behavior documented; service behavior matches app guidance

## Screen Detection & Confirmation

### TC-010: Basic Screen Off Detection (P0)
- Preconditions: Armed, within night window
- Steps: Turn off screen
- Expected: Notification shows screen off detected; 10-min confirmation timer starts

### TC-011: Screen On Cancels Confirmation (P0)
- Preconditions: Armed, screen off, confirmation pending
- Steps: Turn on screen before 10 minutes
- Expected: Notification returns to Armed; no alarm created; timer cancelled

### TC-012: Screen Stays Off → Alarm Created (P0)
- Preconditions: Armed, within night window
- Steps: Turn off screen → wait 10+ minutes
- Expected: OS alarm created for screen_off + 8h; notification shows alarm time; alarm visible in Clock app

### TC-013: Multiple Screen Off — Latest Wins (P0)
- Preconditions: Armed, within night window
- Steps: Screen off at 22:00 → on at 22:03 → off at 22:05 → wait 10+ minutes
- Expected: Alarm set for 06:05 (latest off + 8h)

### TC-014: Multiple Screen Off During Confirmation (P1)
- Preconditions: Armed, confirmation pending
- Steps: Screen off at 22:00 → on at 22:02 → off at 22:04 → wait 10+ minutes
- Expected: Timer resets at 22:04; alarm set for 06:04

### TC-015: Screen Off Outside Night Window Ignored (P1)
- Preconditions: Armed, outside night window
- Steps: Turn off screen
- Expected: No confirmation timer; event logged but not acted upon

## Alarm Creation

### TC-020: Verify OS Alarm Appears (P0)
- Preconditions: Alarm creation completed
- Steps: Open Clock app → Alarms tab
- Expected: New alarm at scheduled time; label "Sleep8 Alarm"

### TC-021: Multiple Alarms in One Session (P1)
- Preconditions: Armed
- Steps: Complete first alarm flow → screen on → screen off → wait 10+ minutes
- Expected: Second alarm created; both visible; both recorded in DB

### TC-022: Alarm Time Accuracy (P0)
- Preconditions: Armed
- Steps: Screen off at exactly 23:15:00 → wait confirmation
- Expected: Alarm set for exactly 07:15

### TC-023: Alarm with Snooze Enabled (P2)
- Preconditions: Snooze enabled
- Steps: Complete alarm creation → check Clock app
- Expected: Snooze configured (if supported)

### TC-024: Disarm Does Not Delete Created Alarms (P1)
- Preconditions: Alarm already created
- Steps: Disarm → check Clock app
- Expected: Existing alarm still present

## Reboot Recovery

### TC-030: Reboot While Armed (Idle) (P0)
- Preconditions: Armed, within night window, no pending confirmation
- Steps: Reboot → wait for boot complete
- Expected: Armed restored; notification reappears; monitoring resumes

### TC-031: Reboot During Confirmation (P0)
- Preconditions: Armed, confirmation pending
- Steps: Reboot → wait for boot complete
- Expected: Armed restored; remaining timer resumes; alarm created after remaining time if screen off

### TC-032: Reboot After Deadline Passed (P0)
- Preconditions: Pending screen off timestamp exists
- Steps: Simulate screen off 15+ minutes ago → reboot while screen off
- Expected: Alarm created immediately on boot

### TC-033: Reboot Outside Night Window (P1)
- Preconditions: Armed, reboot after window ends
- Steps: Reboot at 09:00
- Expected: Session ended; app not armed

## Night Window Edge Cases

### TC-040: Window Crossing Midnight — Before Midnight (P0)
- Preconditions: Window 22:00–08:00, time 23:30
- Steps: Arm → screen off
- Expected: Within window; confirmation starts

### TC-041: Window Crossing Midnight — After Midnight (P0)
- Preconditions: Window 22:00–08:00, time 02:00
- Steps: Arm → screen off
- Expected: Within window; confirmation starts

### TC-042: Exactly at Window Start (P2)
- Preconditions: Window 22:00–08:00, time 22:00:00
- Steps: Screen off at 22:00
- Expected: Within window

### TC-043: Exactly at Window End (P2)
- Preconditions: Window 22:00–08:00, time 08:00:00
- Steps: Screen off at 08:00
- Expected: Within window

### TC-044: Auto-Disarm at Window End (P1)
- Preconditions: Armed
- Steps: Wait until window ends
- Expected: Auto-disarm; notification removed; session ended

### TC-045: Armed Outside Window → Enter Window Starts Monitoring (P1)
- Preconditions: App armed manually, current time outside night window
- Steps: Wait until night window starts
- Expected: Monitoring starts (foreground notification appears); no monitoring started before window begins

### TC-046: Armed In Window → Exit Window Stops Monitoring (P1)
- Preconditions: App armed manually, current time within night window
- Steps: Wait until night window ends
- Expected: Monitoring stops (notification removed); app remains armed

## Settings

### TC-050: Change Night Window Start (P1)
- Preconditions: Default settings
- Steps: Open Settings → change start to 21:00 → save
- Expected: New window effective; persists across restart

### TC-051: Change Night Window End (P1)
- Preconditions: Default settings
- Steps: Open Settings → change end to 09:00 → save
- Expected: New window effective; auto-disarm time updated

### TC-052: Enable/Disable Snooze (P2)
- Preconditions: Snooze off
- Steps: Open Settings → enable snooze → set 10 minutes
- Expected: Setting persists; next alarm includes snooze (if supported)

## Permissions & Reliability

### TC-060: Exact Alarm Permission Check (P0)
- Preconditions: Fresh install
- Steps: Open app → check reliability checklist
- Expected: Shows exact alarm status and grant action

### TC-061: Battery Optimization Warning (P1)
- Preconditions: Battery optimization enabled
- Steps: Open app → check reliability checklist
- Expected: Warning shown; link to battery settings

### TC-062: Grant Battery Optimization Exclusion (P1)
- Preconditions: Warning shown
- Steps: Tap request exclusion → grant
- Expected: Warning disappears; checklist green

## Edge Cases & Error Handling

### TC-070: App Force Stopped While Armed (P1)
- Preconditions: Armed
- Steps: Force stop app
- Expected: State may be lost; documented behavior

### TC-071: Low Memory Service Kill (P1)
- Preconditions: Armed
- Steps: Create memory pressure
- Expected: Service restarts (START_STICKY); state restored

### TC-072: Timezone Change (P2)
- Preconditions: Armed, confirmation pending
- Steps: Change device timezone
- Expected: Alarm adjusts to local time; window interpreted in new timezone

### TC-073: Clock App Not Available (P1)
- Preconditions: Device without compatible Clock app
- Steps: Complete alarm creation flow
- Expected: Error handled gracefully; warning shown; backstop scheduled

### TC-074: Network Offline Verification (P0)
- Preconditions: Device with network monitoring
- Steps: Use app across flows → monitor traffic
- Expected: Zero network requests

## OEM-Specific Tests

### TC-OEM-001: Samsung Clock App Integration
- Steps: Complete alarm creation on Samsung device
- Expected: Alarm appears in Samsung Clock app
- Notes: May require UI confirmation

### TC-OEM-002: Xiaomi Battery Aggressive Kill
- Steps: Arm app → lock device for extended period
- Expected: Service survives aggressive battery management

### TC-OEM-003: OnePlus Doze Mode
- Steps: Arm app → let device enter deep doze
- Expected: Exact alarms still fire

## Auto-Arming & Scheduling

### TC-020: Auto-arm at Auto-arm Start (P0)
- Preconditions: Auto-arm enabled in settings, current time before auto-arm start
- Steps: Wait until auto-arm start
- Expected: App arms automatically; notification appears; tile active

### TC-021: Auto-disarm at Auto-arm End (P0)
- Preconditions: Auto-arm enabled, currently armed, current time before auto-arm end
- Steps: Wait until auto-arm end
- Expected: App disarms automatically; notification removed; tile inactive

### TC-022: Enable Auto-arm During Auto-arm Window (P0)
- Preconditions: Auto-arm disabled, current time within auto-arm window
- Steps: Enable auto-arm in settings
- Expected: App arms immediately; notification appears; tile active

### TC-023: Manual Override (P0)
- Preconditions: Auto-arm enabled, currently armed by schedule
- Steps: Manually disarm via app button or tile
- Expected: App remains disarmed until next scheduled event (night start); manual override resets after event
