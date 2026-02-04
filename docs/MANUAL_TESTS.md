# Sleep8 — Manual Test Cases (Owned Exact Alarms + Optional Overlay)

These tests require **physical devices** (Pixel 8 / Android 14+ recommended).
Automated unit, integration, and Compose UI tests cover most logic and UI flows; manual tests focus on platform-specific behaviors (exact alarms, lockscreen/full-screen behavior, overlay reliability).

## Core Alarm Flow
- Screen off → confirmation → alarm fires at `screen_off + duration`.
- Alarm fires while device locked; full-screen UI shows and rings.
- Alarm fires in Doze and after app process death.
- Alarm fires after reboot; if overdue, fires immediately.
- Re-arm or confirm twice → only the newest scheduled alarm remains active.
- Alarm UI title displays updated label (task switcher and screen header match).

## Navigation Tap Reliability
- On Home, Alarm History, and Settings: tap the top-left icon 20x; no missed taps.
- Ensure taps do not accidentally open the notification shade.

## Visual Regression Checklist
- Home, Alarm list, History, and Settings use consistent Material top app bars and spacing rhythm (8/12/16/24dp feel).
- Home uses card-based grouping and clear hierarchy; Alarm list/History use list-row/card patterns.
- Interactive controls meet touch-target expectations and are easy to tap.
- Contrast remains readable in dark and light themes.
- Toggle dark mode in Settings and verify every screen updates immediately.
- Ringing UI presents AOSP-like layout (large centered time, subtle label, dismiss-only).
- Ringing UI shows alarm info text and a sticky red dismiss button at bottom.
- Ringing dismiss action stops audio/vibration and closes UI instantly.
- Icon checks on emulator/device:
  - launcher/home screen icon
  - app info/settings icon
  - recents/task switcher icon
  - ringing notification small icon

## Duration Settings
- Set duration to 0 minutes → alarm rings immediately at confirmation time.
- Set duration to 720 minutes → alarm scheduled exactly +720 minutes.
- Enter -1 or 721 → inline error shown; value is not saved until corrected.
- Duration inputs are always Hours + Minutes fields.

## Overlay (Optional)
- Enable overlay toggle + grant permission → overlay appears while alarm is ringing.
- Enable overlay toggle without permission → permission screen opens; overlay does not show until granted.
- Disable overlay toggle (even with permission granted) → only AlarmRingingActivity is shown.

## Notifications / Permission
- Deny POST_NOTIFICATIONS → alarm still rings; UI shows warning about limited lockscreen UX.
- Grant POST_NOTIFICATIONS → ringing notification appears with **Dismiss-only** action.

## OS Integration
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms` opens history
  - `sleep8://alarm/<id>` opens history with selected record
- Schedule an alarm earlier than Google Clock → lockscreen “next alarm” shows Sleep8 time.

## Remaining Platform-Only Behaviors
- Verify exact-alarm permission prompts on Android 12+ and that alarms still fire after Doze.
- Confirm lockscreen indicator updates on real hardware.
- Validate OEM-specific battery optimization flows.

## Alarm List (AOSP-style)
- Alarm page shows time, subtitle, and toggle switch per alarm.
- Toggling ON enables the alarm; toggling OFF disables it.
- Past alarms show disabled toggles.

## Ringing UI (AOSP-style)
- Alarm fires → ringing UI shows large time + subtle label + **Dismiss** only.
- Dismiss stops sound/vibration immediately and closes the ringing UI.
