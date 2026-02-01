# Sleep8 — System Architecture

## 1. Overview

Sleep8 is a native Android application that automatically schedules OS alarms based on screen-off detection during a user-defined night window. The app follows a layered architecture with clear separation of concerns, leveraging Android's foreground services, broadcast receivers, and Room database for persistence.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                          │
├─────────────────────────────────────────────────────────────────────┤
│  MainActivity    │  SettingsActivity   │  QuickSettingsTile         │
│  (Arm/Disarm UI) │  (Configuration)    │  (System Tile)             │
└────────┬────────────────────┬──────────────────────┬────────────────┘
         │                    │                      │
         ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DOMAIN LAYER                               │
├─────────────────────────────────────────────────────────────────────┤
│  ArmManager  │  StateMachine  │  NightWindowValidator               │
│              │                │                                     │
│  ConfirmOffScheduler  │  OsAlarmCreator  │  BackstopAlarmScheduler  │
└────────┬────────────────────┬──────────────────────┬────────────────┘
         │                    │                      │
         ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER                               │
├─────────────────────────────────────────────────────────────────────┤
│  NightMonitorService (Foreground)  │  BootReceiver                  │
│  ScreenStateReceiver (Runtime)     │  ConfirmationAlarmReceiver     │
└────────┬────────────────────┬──────────────────────┬────────────────┘
         │                    │                      │
         ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Room Database  │  SharedPreferences  │  In-Memory StateHolder      │
│  (Persistent)   │  (Quick Access)     │  (Runtime Cache)            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. High-Level Component Diagram

```
                                    ┌──────────────────┐
                                    │   User Device    │
                                    └────────┬─────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              │                              │                              │
              ▼                              ▼                              ▼
    ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐
    │   MainActivity  │          │  Quick Settings │          │   OS Clock App  │
    │   (Arm Button)  │          │     Tile        │          │  (Alarm Target) │
    └────────┬────────┘          └────────┬────────┘          └────────▲────────┘
             │                            │                            │
             └────────────┬───────────────┘                            │
                          ▼                                            │
                ┌─────────────────┐                                    │
                │   ArmManager    │────────────────────────────────────┤
                │                 │                                    │
                │ • start/stop    │                                    │
                │ • session mgmt  │                                    │
                └────────┬────────┘                                    │
                         │                                             │
                         ▼                                             │
           ┌─────────────────────────────┐                             │
           │   NightMonitorService       │                             │
           │   (Foreground Service)      │                             │
           │                             │                             │
           │  ┌───────────────────────┐  │                             │
           │  │ ScreenStateReceiver   │  │                             │
           │  │ (Runtime Registered)  │  │                             │
           │  └───────────┬───────────┘  │                             │
           └──────────────┼──────────────┘                             │
                          │                                            │
                          ▼                                            │
           ┌─────────────────────────────┐                             │
           │  ConfirmOffScheduler        │                             │
           │                             │                             │
           │  • 10-min timer management  │                             │
           │  • Exact alarm scheduling   │                             │
           └──────────────┬──────────────┘                             │
                          │                                            │
                          ▼                                            │
           ┌─────────────────────────────┐      ┌──────────────────────┴───┐
           │   OsAlarmCreator            │──────│  ACTION_SET_ALARM Intent │
           │                             │      └──────────────────────────┘
           │  • Intent construction      │
           │  • Resolvability check      │
           └─────────────────────────────┘
```

---

## 3. Module Structure

```
com.sleep8/
├── app/                              # Application entry point
│   ├── Sleep8Application.kt          # App initialization, DI setup
│   └── di/
│       └── AppModule.kt              # Hilt/Koin module definitions
│
├── ui/                               # Presentation Layer
│   ├── main/
│   │   ├── MainActivity.kt           # Main screen with arm/disarm
│   │   ├── MainViewModel.kt          # UI state management
│   │   └── MainUiState.kt            # UI state data class
│   ├── settings/
│   │   ├── SettingsActivity.kt       # Night window & snooze config
│   │   ├── SettingsViewModel.kt
│   │   └── ReliabilityChecklistFragment.kt
│   ├── components/                   # Reusable UI components
│   │   ├── ArmButton.kt
│   │   └── StatusCard.kt
│   └── tile/
│       └── Sleep8TileService.kt      # Quick Settings Tile
│
├── domain/                           # Business Logic Layer
│   ├── model/
│   │   ├── AppState.kt               # State machine enum
│   │   ├── ArmSession.kt             # Domain model
│   │   ├── ScreenEvent.kt
│   │   └── AlarmRecord.kt
│   ├── manager/
│   │   ├── ArmManager.kt             # Arm/disarm orchestration
│   │   └── StateMachineManager.kt    # State transitions
│   ├── scheduler/
│   │   ├── ConfirmOffScheduler.kt    # 10-min confirmation logic
│   │   ├── BackstopAlarmScheduler.kt # Internal exact alarm
│   │   └── OsAlarmCreator.kt         # OS Clock alarm creation
│   └── validator/
│       └── NightWindowValidator.kt   # Time window checks
│
├── service/                          # Android Services & Receivers
│   ├── NightMonitorService.kt        # Foreground service
│   ├── receiver/
│   │   ├── ScreenStateReceiver.kt    # SCREEN_ON/OFF events
│   │   ├── BootReceiver.kt           # BOOT_COMPLETED handler
│   │   └── ConfirmationAlarmReceiver.kt # Exact alarm callback
│   └── notification/
│       └── NotificationHelper.kt     # Service notification
│
├── data/                             # Data Layer
│   ├── db/
│   │   ├── Sleep8Database.kt         # Room database
│   │   ├── dao/
│   │   │   ├── SettingsDao.kt
│   │   │   ├── ArmSessionDao.kt
│   │   │   ├── ScreenEventDao.kt
│   │   │   └── AlarmRecordDao.kt
│   │   └── entity/
│   │       ├── SettingsEntity.kt
│   │       ├── ArmSessionEntity.kt
│   │       ├── ScreenEventEntity.kt
│   │       └── AlarmRecordEntity.kt
│   ├── repository/
│   │   ├── SettingsRepository.kt
│   │   ├── SessionRepository.kt
│   │   └── AlarmRepository.kt
│   └── preferences/
│       └── AppPreferences.kt         # SharedPreferences wrapper
│
└── util/                             # Utilities
    ├── TimeUtils.kt                  # Time calculations
    ├── PermissionUtils.kt            # Permission helpers
    └── Constants.kt                  # App-wide constants
```

---

## 4. State Machine

### 4.1 State Diagram

```
                              ┌─────────────┐
                              │  DISARMED   │◄────────────────────────┐
                              └──────┬──────┘                         │
                                     │                                │
                                     │ arm()                          │ disarm()
                                     ▼                                │
                              ┌─────────────┐                         │
                    ┌────────►│ ARMED_IDLE  │─────────────────────────┤
                    │         └──────┬──────┘                         │
                    │                │                                │
                    │                │ SCREEN_OFF (in window)         │
                    │                ▼                                │
                    │         ┌──────────────────────┐                │
          SCREEN_ON │         │ ARMED_PENDING_CONFIRM│────────────────┤
       (before 10m) │         └──────────┬───────────┘                │
                    │                    │                            │
                    │                    │ 10 min elapsed             │
                    │                    │ (screen still OFF)         │
                    │                    ▼                            │
                    │         ┌──────────────────────┐                │
                    └─────────│  ARMED_ALARM_SET     │────────────────┘
                   new        └──────────────────────┘
                SCREEN_OFF
```

### 4.2 State Definitions

| State | Description | Entry Actions | Exit Actions |
|-------|-------------|---------------|--------------|
| `DISARMED` | App is not monitoring | Cancel all timers, stop service | - |
| `ARMED_IDLE` | Armed, waiting for screen-off | Start foreground service | - |
| `ARMED_PENDING_CONFIRM` | Screen off detected, 10-min timer running | Start confirmation timer, persist candidate | Cancel timer if transitioning back |
| `ARMED_ALARM_SET` | At least one alarm created this session | Create OS alarm, persist record | - |

### 4.3 State Transition Table

| Current State | Event | Condition | Next State | Action |
|---------------|-------|-----------|------------|--------|
| DISARMED | arm() | - | ARMED_IDLE | Create session, start service |
| ARMED_IDLE | SCREEN_OFF | in night window | ARMED_PENDING_CONFIRM | Store candidate, start 10m timer |
| ARMED_IDLE | SCREEN_OFF | outside window | ARMED_IDLE | Log, ignore |
| ARMED_PENDING_CONFIRM | SCREEN_ON | - | ARMED_IDLE | Cancel timer, keep candidate in DB |
| ARMED_PENDING_CONFIRM | SCREEN_OFF | - | ARMED_PENDING_CONFIRM | Update candidate, restart timer |
| ARMED_PENDING_CONFIRM | TIMER_EXPIRED | screen still off | ARMED_ALARM_SET | Create OS alarm |
| ARMED_ALARM_SET | SCREEN_OFF | in night window | ARMED_PENDING_CONFIRM | New candidate, start timer |
| ANY_ARMED | disarm() | - | DISARMED | End session, stop service |
| ANY_ARMED | window_end | - | DISARMED | Auto-disarm |

---

## 5. Data Flow

### 5.1 Arm Flow
```
User Action (Button/Tile)
         │
         ▼
    ArmManager.arm()
         │
         ├─► Create ArmSession in DB
         │
         ├─► Update StateHolder (armed = true)
         │
         ├─► Start NightMonitorService
         │
         └─► Update UI via LiveData/Flow
```

### 5.2 Screen-Off Detection Flow
```
System Broadcast: ACTION_SCREEN_OFF
         │
         ▼
ScreenStateReceiver.onReceive()
         │
         ▼
NightWindowValidator.isInWindow(now)
         │
         ├─► FALSE: Log & ignore
         │
         └─► TRUE:
              │
              ▼
         StateMachineManager.onScreenOff()
              │
              ├─► Persist ScreenEvent to DB
              │
              ├─► Update pending_candidate_screen_off_ts
              │
              └─► ConfirmOffScheduler.scheduleConfirmation(10min)
                       │
                       ▼
                  AlarmManager.setExactAndAllowWhileIdle()
```

### 5.3 Confirmation & Alarm Creation Flow
```
AlarmManager fires: CONFIRMATION_TIMER_EXPIRED
         │
         ▼
ConfirmationAlarmReceiver.onReceive()
         │
         ▼
Check: Is screen currently OFF?
         │
         ├─► FALSE (screen on): Cancel, return to ARMED_IDLE
         │
         └─► TRUE:
              │
              ▼
         Calculate: alarm_time = screen_off_ts + 8 hours
              │
              ▼
         OsAlarmCreator.createAlarm(alarm_time)
              │
              ├─► Check if ACTION_SET_ALARM resolves
              │
              ├─► Fire intent with hour/minute/message
              │
              └─► Persist AlarmRecord to DB
                       │
                       ▼
              BackstopAlarmScheduler.scheduleBackstop(alarm_time)
```

### 5.4 Reboot Recovery Flow
```
System Broadcast: BOOT_COMPLETED
         │
         ▼
BootReceiver.onReceive()
         │
         ▼
Query DB: Active session with armed_at != null, disarmed_at == null
         │
         ├─► NONE: Do nothing
         │
         └─► EXISTS:
              │
              ▼
         Restore StateHolder from DB
              │
              ▼
         NightWindowValidator.isInWindow(now)
              │
              ├─► FALSE: Mark session ended, return
              │
              └─► TRUE:
                   │
                   ▼
              Check pending_candidate_screen_off_ts
                   │
                   ├─► NULL: Start service in ARMED_IDLE
                   │
                   └─► EXISTS:
                        │
                        ▼
                   Calculate: elapsed = now - pending_ts
                        │
                        ├─► elapsed >= 10min & screen OFF:
                        │        Create alarm immediately
                        │
                        └─► elapsed < 10min:
                                 Resume timer (10min - elapsed)
```

---

## 6. Database Schema

### 6.1 Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│    settings     │       │    arm_sessions     │       │   screen_events     │
├─────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ id (PK)         │       │ session_id (PK)     │◄──┐   │ event_id (PK)       │
│ night_start     │       │ armed_at            │   │   │ session_id (FK)     │──┐
│ night_end       │       │ disarmed_at         │   │   │ type                │  │
│ auto_arm_start  │       │ window_start_ts     │   │   │ ts                  │  │
│ auto_arm_end    │       │ window_end_ts       │   │   └─────────────────────┘  │
│ auto_arm_enabled│       │ source              │   │                            │
│ confirm_minutes │       └─────────────────────┘   │   ┌─────────────────────┐  │
│ snooze_minutes  │                                 │   │   alarm_records     │  │
│ armed_default   │                                 │   ├─────────────────────┤  │
│ offline_only    │                                 └───│ session_id (FK)     │◄─┘
└─────────────────┘                                     │ alarm_id (PK)       │
                                                        │ screen_off_ts       │
                                                        │ confirmed_at        │
                                                        │ scheduled_alarm_ts  │
                                                        │ os_alarm_resolved   │
                                                        │ os_alarm_ui_required│
                                                        │ backstop_scheduled  │
                                                        └─────────────────────┘
```


### 6.2 Table Definitions

```sql
-- Settings (singleton row)
CREATE TABLE settings (
    id INTEGER PRIMARY KEY DEFAULT 1,
    night_start TEXT NOT NULL DEFAULT '22:00',      -- HH:mm
    night_end TEXT NOT NULL DEFAULT '08:00',        -- HH:mm
    auto_arm_start TEXT NOT NULL DEFAULT '22:00',   -- HH:mm
    auto_arm_end TEXT NOT NULL DEFAULT '08:00',     -- HH:mm
    auto_arm_enabled INTEGER NOT NULL DEFAULT 0,    -- boolean
    confirm_off_minutes INTEGER NOT NULL DEFAULT 10,
    snooze_minutes INTEGER,                         -- nullable = disabled
    armed_default INTEGER NOT NULL DEFAULT 0,       -- boolean
    offline_only INTEGER NOT NULL DEFAULT 1         -- always true
);

-- Arm Sessions
CREATE TABLE arm_sessions (
    session_id INTEGER PRIMARY KEY AUTOINCREMENT,
    armed_at INTEGER NOT NULL,                      -- epoch millis
    disarmed_at INTEGER,                            -- nullable
    window_start_ts INTEGER NOT NULL,
    window_end_ts INTEGER NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('APP_BUTTON', 'QUICK_TILE'))
);

-- Screen Events
CREATE TABLE screen_events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('SCREEN_OFF', 'SCREEN_ON')),
    ts INTEGER NOT NULL,
    FOREIGN KEY (session_id) REFERENCES arm_sessions(session_id)
);

CREATE INDEX idx_screen_events_session ON screen_events(session_id);

-- Alarm Records
CREATE TABLE alarm_records (
    alarm_id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    screen_off_ts INTEGER NOT NULL,
    confirmed_at INTEGER NOT NULL,
    scheduled_alarm_ts INTEGER NOT NULL,
    os_alarm_intent_resolved INTEGER NOT NULL,      -- boolean
    os_alarm_ui_required INTEGER,                   -- nullable boolean
    internal_backstop_scheduled INTEGER NOT NULL,   -- boolean
    FOREIGN KEY (session_id) REFERENCES arm_sessions(session_id)
);

CREATE INDEX idx_alarm_records_session ON alarm_records(session_id);
```

---

## 7. Service Architecture

### 7.1 NightMonitorService (Foreground)

```kotlin
class NightMonitorService : Service() {
    
    // Lifecycle
    override fun onCreate()           // Initialize receivers, inject deps
    override fun onStartCommand()     // Handle arm/disarm commands
    override fun onDestroy()          // Unregister receivers, cleanup
    
    // Internal
    private fun registerScreenReceiver()
    private fun unregisterScreenReceiver()
    private fun updateNotification(state: AppState)
    
    // Commands (via Intent action)
    companion object {
        const val ACTION_ARM = "com.sleep8.ACTION_ARM"
        const val ACTION_DISARM = "com.sleep8.ACTION_DISARM"
        const val ACTION_CHECK_STATE = "com.sleep8.ACTION_CHECK_STATE"
    }
}
```

### 7.2 Notification Channel

```
Channel ID: sleep8_monitoring
Channel Name: "Sleep Monitoring"
Importance: LOW (no sound, visible in status bar)

Notification Content (by state):
- ARMED_IDLE: "Sleep8 armed • Waiting for sleep"
- ARMED_PENDING_CONFIRM: "Screen off detected • Confirming in X:XX"
- ARMED_ALARM_SET: "Alarm set for HH:MM • Still monitoring"
```

---

## 8. Quick Settings Tile

```
┌─────────────────────────────────────┐
│  ┌───────────────────────────────┐  │
│  │         💤 Sleep8            │  │
│  │                               │  │
│  │    State: Armed / Disarmed   │  │
│  │                               │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘

Tile States:
- STATE_INACTIVE (grey): Disarmed
- STATE_ACTIVE (accent color): Armed
- STATE_UNAVAILABLE: Outside night window (optional)
```

---

## 9. Alarm Creation Strategy

### 9.1 Primary: OS Clock Intent
```kotlin
val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
    putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour)
    putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
    putExtra(AlarmClock.EXTRA_MESSAGE, "Sleep8 Alarm")
    putExtra(AlarmClock.EXTRA_SKIP_UI, true)  // May be ignored by OEM
    settings.snoozeMinutes?.let {
        putExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, it)
    }
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
```

### 9.2 Fallback Handling
```
1. Check if intent resolves: packageManager.resolveActivity(intent)
2. If resolves:
   - Fire intent
   - Record os_alarm_intent_resolved = true
3. If not resolves:
   - Show user warning
   - Record os_alarm_intent_resolved = false
   - Still schedule backstop alarm
```

### 9.3 Internal Backstop
```kotlin
// For resilience and audit logging
val backstopIntent = Intent(context, BackstopAlarmReceiver::class.java).apply {
    putExtra(EXTRA_ALARM_RECORD_ID, alarmRecordId)
}
val pendingIntent = PendingIntent.getBroadcast(
    context, 
    requestCode, 
    backstopIntent,
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)
alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    alarmTimeMillis,
    pendingIntent
)
```

---

## 10. Permission Model

| Permission | Type | Purpose | When Requested |
|------------|------|---------|----------------|
| `FOREGROUND_SERVICE` | Normal | Run monitoring service | Auto-granted |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Normal | Android 14+ service type | Auto-granted |
| `RECEIVE_BOOT_COMPLETED` | Normal | Restore state on reboot | Auto-granted |
| `SCHEDULE_EXACT_ALARM` | Special | 10-min confirmation timer | First arm |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Special | Reliability | Settings guidance |
| `SET_ALARM` | Normal | Create OS alarms | Auto-granted |

---

## 11. Threading Model

```
┌─────────────────────────────────────────────────────────────────┐
│                         Main Thread                              │
│  - UI updates                                                    │
│  - BroadcastReceiver callbacks                                   │
│  - Service lifecycle                                             │
└─────────────────────────────────────────────────────────────────┘
         │
         │ Dispatchers.IO (via coroutines)
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Background Thread                          │
│  - Room database operations                                      │
│  - Alarm scheduling                                              │
│  - State persistence                                             │
└─────────────────────────────────────────────────────────────────┘
         │
         │ Dispatchers.Default
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Computation Thread                          │
│  - Time calculations                                             │
│  - Window validation                                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 12. Error Handling Strategy

### 12.1 Error Categories

| Category | Example | Handling |
|----------|---------|----------|
| Recoverable | Service killed by OS | Restore from DB on restart |
| User Action Required | Battery optimization on | Show guidance UI |
| Silent Failure | OS alarm UI required | Log, proceed, show one-time warning |
| Fatal | Database corruption | Show error, offer reset |

### 12.2 Resilience Patterns

1. **Idempotent Operations**: All state changes can be safely retried
2. **Persist Before Action**: Write to DB before scheduling alarms
3. **Verify After Action**: Confirm alarm was created after intent fired
4. **Graceful Degradation**: If OS alarm fails, backstop still works

---

## 13. Security Considerations

- **No Network**: `android:usesCleartextTraffic="false"` + no INTERNET permission
- **Local Storage Only**: All data in app-private database
- **No Exported Components**: Receivers/services not exported (except boot receiver)
- **Secure Intents**: All PendingIntents use FLAG_IMMUTABLE

---

## 14. Technology Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 1.9+ |
| Min SDK | 31 (Android 12) |
| Target SDK | 35 (latest stable) |
| Build | Gradle 8.x, AGP 8.x |
| DI | Hilt |
| Database | Room 2.6+ |
| Async | Kotlin Coroutines + Flow |
| UI | Jetpack Compose (or XML) |
| Architecture | MVVM + Clean Architecture |
| Testing | JUnit 5, Mockk, Turbine |

---

## Auto-Arming Schedule & Manual Override

- The app uses a WindowScheduler to automatically arm at the Auto-arm start time and disarm at the Auto-arm end time if the "Auto-arm" setting is enabled.
- Auto-arm schedule uses its own start/end times (separate from the night window).
- Manual arming/disarming (via app button or tile) acts as an override until the next scheduled event.
- ArmManager now supports multiple ArmSource types (SCHEDULED, APP_BUTTON, QUICK_TILE, etc.) and tracks manual overrides.
- WindowScheduler replaces WindowEndScheduler and handles both start and end triggers for the auto-arm schedule.
