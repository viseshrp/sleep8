# Sleep8 — Manual Test Cases (AlarmClock + Owned Alarms)

These tests require **physical devices** (Pixel 8 / Android 14+ recommended).

## Core Alarm Flow
- Screen off → confirmation → alarm fires at `screen_off + duration`.
- Alarm fires while device locked; full-screen UI shows and rings.
- Alarm fires in Doze and after app process death.
- Alarm fires after reboot; if overdue, fires immediately.

## Next Alarm Indicator
- After scheduling, verify system “next alarm” indicator shows the scheduled time.
- After snooze, verify the system “next alarm” updates to the snoozed time.

## Notifications / Permission
- Deny POST_NOTIFICATIONS → alarm still rings; UI shows warning about limited lockscreen UX.
- Grant POST_NOTIFICATIONS → ringing notification appears with Dismiss/Snooze actions.

## OS Integration
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms` opens history
  - `sleep8://alarm/<id>` opens history with selected record
