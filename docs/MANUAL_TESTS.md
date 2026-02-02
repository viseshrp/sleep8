# Sleep8 — Manual Test Cases (Owned Exact Alarms + Optional Overlay)

These tests require **physical devices** (Pixel 8 / Android 14+ recommended).

## Core Alarm Flow
- Screen off → confirmation → alarm fires at `screen_off + duration`.
- Alarm fires while device locked; full-screen UI shows and rings.
- Alarm fires in Doze and after app process death.
- Alarm fires after reboot; if overdue, fires immediately.
- Re-arm or confirm twice → only the newest scheduled alarm remains active.

## Overlay (Optional)
- Enable overlay toggle → if permission granted, overlay appears while alarm is ringing.
- Enable overlay toggle without permission → permission screen opens; overlay does not show until granted.
- Disable overlay toggle → only AlarmActivity is shown.

## Notifications / Permission
- Deny POST_NOTIFICATIONS → alarm still rings; UI shows warning about limited lockscreen UX.
- Grant POST_NOTIFICATIONS → ringing notification appears with Dismiss/Snooze actions.

## OS Integration
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms` opens history
  - `sleep8://alarm/<id>` opens history with selected record
