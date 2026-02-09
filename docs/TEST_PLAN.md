# Sleep8 — Testing Plan (Owned Exact Alarm + Optional Overlay)

## Overview
Physical device testing is required; emulators are not authoritative for exact alarm scheduling, lockscreen behavior, or overlay reliability.

---

## Unit Tests
- Duration config stored in `duration_used_minutes`.
- `AlarmManager.setAlarmClock` uses expected trigger time.
- PendingIntent uniqueness via `alarm_instance_id`/`request_code`.
- Notification permission logic (Android 13+).
- Overlay policy logic (enabled vs permission granted).
- Alarm ringing notification includes Dismiss-only action.
- Home alarm list toggles route schedule/cancel operations correctly.
- Single active alarm: scheduling a new confirmed alarm cancels prior scheduled alarms.
- Reboot cleanup: multiple scheduled alarms → keep newest, cancel extras.
- Disarm keeps existing scheduled alarms untouched and clears pending session runtime state.
- Last screen-off timestamp remains visible across midnight within the same active session.
- Last screen-off timestamp clears on accepted alarm fire, dismiss, disarm, and new arm session start.
- Duration boundaries: 0, 1, 480, 720 minutes.
- Invalid duration inputs: below 0 / above 720 are rejected (not persisted).
- Duration 0 schedules at confirmation timestamp.
- Alarm ringing activity label uses the ringing title.
- Duration UI has both Hours and Minutes inputs.
- Alarm list toggles schedule/cancel alarms and enforce single-active policy.
- Alarm list has no edit actions.
- `AlarmManager.getNextAlarmClock` reflects the earliest scheduled alarm (debug/log assertion).
- AlarmRingingActivity fullScreenIntent is used for ringing UI.
- Boot restore behavior with pending confirmations and overdue scheduled alarms.
- AlarmReceiver behavior for notification-permitted vs denied flows.
- Night-window schedulers (start/end + backstops).
- Monitoring reliability manager:
  - gate-open boundary start
  - gate-closed no-start
  - late-start classification (`app restricted / force-stopped suspected`)
- Night-window receivers gate monitoring without changing armed state.
- AppPreferences migration and instance id generation.

---

## Integration Tests (Robolectric)
- Arm → screen off → confirm → alarm scheduled.
- Boot restore reschedules `SCHEDULED` record.
- Night window start/end receivers gate monitoring.
- Service lifecycle and foreground notification behavior.

---

## UI Tests (Compose)
- Settings screen sections and reliability checklist.
- Theme toggle switches dark/light mode state from Settings.
- Home alarm list section content (empty state, toggles).
- Alarm history page, clear confirmation dialog, and back navigation.
- Ringing UI (alarm info visible, sticky Dismiss, no top app bar/back/menu).
- Main navigation drawer flows (hamburger/menu selections).
- Navigation remains functional across Home → Alarm History/Settings.

---

## Manual Tests (Pixel 8 / Android 14+)
- Cold start shows splash screen and transitions smoothly to Home (no blank frame/jank).
- Verify dark mode is default on fresh install.
- Toggle dark mode On/Off in Settings and verify every screen updates (Home, Home alarm list section, History, Settings, ringing UI).
- Alarm fires and displays full-screen UI on lockscreen.
- Overlay toggle on: overlay appears while ringing (permission granted).
- Overlay toggle on, permission denied: alarm still rings; overlay not shown.
- Overlay toggle off: alarm always shows full-screen ringing activity (no overlay).
- Notification permission requested on first arm; deny → alarm still rings without FGS notification.
- ACTION_SHOW_ALARMS opens Alarm History screen.
- Deep links:
  - `sleep8://alarms`
  - `sleep8://alarm/<id>`
- Lockscreen/system “next alarm” shows app alarm when earlier than other alarms.
- Home alarm list section shows time + switch; toggling off disables the alarm.
- Ringing UI shows Dismiss only (no snooze anywhere).
- Last screen-off shown at 23:59 remains visible after midnight while still in the same active session.
- Last screen-off is hidden immediately after alarm fire/dismiss, after disarm, and after starting a fresh arm session.
- Notifications permission denied still rings; UI warns about reduced lockscreen UX.
- Icon verification checklist:
  - Launcher icon (home screen/app drawer)
  - App info/settings list icon
  - Recents/task switcher icon
  - Alarm notification small icon is monochrome and legible

---

## Emulator Disclaimer
Emulators are not authoritative for Doze, exact alarms, or lockscreen alarm indicators.
