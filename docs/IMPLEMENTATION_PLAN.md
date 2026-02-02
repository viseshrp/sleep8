# Sleep8 — Implementation Plan

## Overview

This document outlines the phased implementation plan for Sleep8, broken into 5 milestones with detailed tasks, dependencies, and acceptance criteria for each phase.

---

## Timeline Summary

| Milestone | Name | Duration | Dependencies |
|-----------|------|----------|--------------|
| A | Project Skeleton | 3-4 days | None |
| B | Monitoring & Confirmation | 4-5 days | Milestone A |
| C | Alarm Creation | 3-4 days | Milestone B |
| D | Reboot & Resilience | 2-3 days | Milestone C |
| E | QA & Polish | 3-4 days | Milestone D |

**Total Estimated Duration: 15-20 days**

---

## Milestone A — Project Skeleton

### Objective
Create the foundational Android project structure with database, settings UI, and Quick Settings tile.

### A.1 Project Setup

#### A.1.1 Create Android Project
```
Tasks:
□ Create new Android project with Kotlin DSL
□ Configure minSdk: 31, targetSdk: 35
□ Set up package structure: com.sleep8
□ Configure Gradle with required dependencies
□ Set up .gitignore for Android
```

**Dependencies (build.gradle.kts):**
```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.0")
    
    // Compose (or Material for XML)
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
```

#### A.1.2 Configure AndroidManifest.xml
```xml
<manifest>
    <!-- Permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="com.android.alarm.permission.SET_ALARM" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    
    <!-- No network - enforce offline only -->
    <!-- Intentionally NOT including INTERNET permission -->
    
    <application android:usesCleartextTraffic="false">
        <!-- Components declared in later tasks -->
    </application>
</manifest>
```

**Acceptance Criteria:**
- [ ] Project builds successfully
- [ ] All dependencies resolve
- [ ] App runs on emulator (empty activity)

---

### A.2 Database Setup

#### A.2.1 Create Room Entities
```
Files to create:
□ data/db/entity/SettingsEntity.kt
□ data/db/entity/ArmSessionEntity.kt
□ data/db/entity/ScreenEventEntity.kt
□ data/db/entity/AlarmRecordEntity.kt
```

**SettingsEntity.kt:**
```kotlin
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "night_start") val nightStart: String = "22:00",
    @ColumnInfo(name = "night_end") val nightEnd: String = "08:00",
    @ColumnInfo(name = "confirm_off_minutes") val confirmOffMinutes: Int = 10,
    @ColumnInfo(name = "snooze_minutes") val snoozeMinutes: Int? = null,
    @ColumnInfo(name = "armed_default") val armedDefault: Boolean = false,
    @ColumnInfo(name = "offline_only") val offlineOnly: Boolean = true
)
```

#### A.2.2 Create DAOs
```
Files to create:
□ data/db/dao/SettingsDao.kt
□ data/db/dao/ArmSessionDao.kt
□ data/db/dao/ScreenEventDao.kt
□ data/db/dao/AlarmRecordDao.kt
```

#### A.2.3 Create Database Class
```
Files to create:
□ data/db/Sleep8Database.kt
□ data/db/DatabaseModule.kt (Hilt)
```

#### A.2.4 Create Repositories
```
Files to create:
□ data/repository/SettingsRepository.kt
□ data/repository/SessionRepository.kt
□ data/repository/AlarmRepository.kt
```

**Acceptance Criteria:**
- [ ] Database schema matches spec
- [ ] Unit tests pass for all DAOs
- [ ] Repositories correctly wrap DAO operations
- [ ] Migration tests (if needed later)

---

### A.3 Domain Models & State

#### A.3.1 Create Domain Models
```
Files to create:
□ domain/model/AppState.kt (enum)
□ domain/model/ArmSession.kt
□ domain/model/ScreenEvent.kt
□ domain/model/AlarmRecord.kt
□ domain/model/NightWindow.kt
```

**AppState.kt:**
```kotlin
enum class AppState {
    DISARMED,
    ARMED_IDLE,
    ARMED_PENDING_CONFIRM,
    ARMED_ALARM_SET
}
```

#### A.3.2 Create StateHolder
```
Files to create:
□ domain/state/StateHolder.kt
```

```kotlin
@Singleton
class StateHolder @Inject constructor() {
    private val _state = MutableStateFlow(AppState.DISARMED)
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    private val _activeSession = MutableStateFlow<ArmSession?>(null)
    val activeSession: StateFlow<ArmSession?> = _activeSession.asStateFlow()
    
    // ... other state properties
}
```

**Acceptance Criteria:**
- [ ] State enums defined
- [ ] StateHolder provides reactive state access
- [ ] State persistence via repositories

---

### A.4 Settings UI

#### A.4.1 Create Settings Screen
```
Files to create:
□ ui/settings/SettingsActivity.kt (or SettingsScreen.kt for Compose)
□ ui/settings/SettingsViewModel.kt
□ ui/settings/SettingsUiState.kt
```

**Settings Screen Content:**
- Night Window start time picker
- Night Window end time picker
- Auto-Arm Schedule start time picker
- Auto-Arm Schedule end time picker
- Auto-Arm Schedule enable toggle
- Snooze toggle + duration picker
- Reliability checklist section

#### A.4.2 Create Reliability Checklist Component
```
Checklist items:
□ Exact alarm permission status
□ Battery optimization status
□ Foreground service status
□ Action buttons to fix each issue
```

**Acceptance Criteria:**
- [ ] User can set night window times
- [ ] User can enable/disable snooze
- [ ] Settings persist across app restarts
- [ ] Reliability checklist shows correct status

---

### A.5 Main UI

#### A.5.1 Create Main Screen
```
Files to create:
□ ui/main/MainActivity.kt
□ ui/main/MainViewModel.kt
□ ui/main/MainUiState.kt
```

**Main Screen Content:**
- Large ARM/DISARM button
- Current status display
- Last screen-off time (if any)
- Pending confirmation timer
- Link to Settings

#### A.5.2 Create UI Components
```
Files to create:
□ ui/components/ArmButton.kt
□ ui/components/StatusCard.kt
□ ui/components/CountdownTimer.kt
```

**Acceptance Criteria:**
- [ ] Arm button toggles state
- [ ] Status updates reactively
- [ ] Navigation to Settings works

---

### A.6 Quick Settings Tile

#### A.6.1 Create Tile Service
```
Files to create:
□ ui/tile/Sleep8TileService.kt
```

```kotlin
class Sleep8TileService : TileService() {
    
    override fun onStartListening() {
        // Update tile state based on armed status
    }
    
    override fun onClick() {
        // Toggle armed state
    }
    
    override fun onTileAdded() {
        // Initial setup
    }
}
```

#### A.6.2 Register in Manifest
```xml
<service
    android:name=".ui.tile.Sleep8TileService"
    android:label="@string/tile_label"
    android:icon="@drawable/ic_tile"
    android:exported="true"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
    <intent-filter>
        <action android:name="android.service.quicksettings.action.QS_TILE" />
    </intent-filter>
</service>
```

**Acceptance Criteria:**
- [ ] Tile appears in Quick Settings
- [ ] Tile reflects current armed state
- [ ] Tapping toggles arm/disarm
- [ ] Tile updates immediately on state change

---

### A.7 Utilities

#### A.7.1 Create Utility Classes
```
Files to create:
□ util/TimeUtils.kt
□ util/PermissionUtils.kt
□ util/Constants.kt
```

**Constants.kt:**
```kotlin
object Constants {
    const val ALARM_OFFSET_HOURS = 8
    const val DEFAULT_CONFIRM_MINUTES = 10
    const val DEFAULT_NIGHT_START = "22:00"
    const val DEFAULT_NIGHT_END = "08:00"
    
    const val NOTIFICATION_CHANNEL_ID = "sleep8_monitoring"
    const val NOTIFICATION_ID = 1001
}
```

**Acceptance Criteria:**
- [ ] Time calculations handle midnight crossing
- [ ] Permission helpers work correctly
- [ ] Constants are properly defined

---

### Milestone A Deliverables

| Item | Status |
|------|--------|
| Android project with all dependencies | □ |
| Room database with 4 entities and DAOs | □ |
| Settings screen with time pickers | □ |
| Main screen with arm button | □ |
| Quick Settings tile | □ |
| Unit tests for database layer | □ |

---

## Milestone B — Monitoring & Confirmation

### Objective
Implement the foreground service, screen event detection, and 10-minute confirmation logic.

### B.1 ArmManager

#### B.1.1 Create ArmManager
```
Files to create:
□ domain/manager/ArmManager.kt
```

```kotlin
@Singleton
class ArmManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val stateHolder: StateHolder,
    private val context: Context
) {
    suspend fun arm(source: ArmSource): Result<ArmSession>
    suspend fun disarm(): Result<Unit>
    suspend fun isArmed(): Boolean
    fun observeArmedState(): Flow<Boolean>
}
```

**Acceptance Criteria:**
- [ ] arm() creates session and starts service
- [ ] disarm() ends session and stops service
- [ ] State persists to database
- [ ] UI updates via Flow

---

### B.2 NightMonitorService

#### B.2.1 Create Foreground Service
```
Files to create:
□ service/NightMonitorService.kt
□ service/notification/NotificationHelper.kt
```

```kotlin
class NightMonitorService : Service() {
    
    private lateinit var screenReceiver: ScreenStateReceiver
    
    override fun onCreate() {
        createNotificationChannel()
        registerScreenReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ARM -> handleArm()
            ACTION_DISARM -> handleDisarm()
        }
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    override fun onDestroy() {
        unregisterScreenReceiver()
    }
}
```

#### B.2.2 Register in Manifest
```xml
<service
    android:name=".service.NightMonitorService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="sleep_tracking" />
</service>
```

**Acceptance Criteria:**
- [ ] Service starts when armed
- [ ] Service stops when disarmed
- [ ] Notification shows correct state
- [ ] Service survives app being closed

---

### B.3 Screen State Receiver

#### B.3.1 Create Receiver
```
Files to create:
□ service/receiver/ScreenStateReceiver.kt
```

```kotlin
class ScreenStateReceiver(
    private val onScreenOff: () -> Unit,
    private val onScreenOn: () -> Unit
) : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
            Intent.ACTION_SCREEN_ON -> onScreenOn()
        }
    }
    
    companion object {
        fun register(context: Context, receiver: ScreenStateReceiver) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            context.registerReceiver(receiver, filter)
        }
    }
}
```

**Acceptance Criteria:**
- [ ] Receives SCREEN_OFF events
- [ ] Receives SCREEN_ON events
- [ ] Only active when service is running
- [ ] Events logged to database

---

### B.4 Night Window Validator

#### B.4.1 Create Validator
```
Files to create:
□ domain/validator/NightWindowValidator.kt
```

```kotlin
class NightWindowValidator @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    fun isInWindow(timestamp: LocalDateTime): Boolean {
        val settings = settingsRepository.getSettings()
        val start = LocalTime.parse(settings.nightStart)
        val end = LocalTime.parse(settings.nightEnd)
        val time = timestamp.toLocalTime()
        
        return if (start <= end) {
            // Same day window (e.g., 06:00-12:00)
            time in start..end
        } else {
            // Crosses midnight (e.g., 22:00-08:00)
            time >= start || time <= end
        }
    }
    
    fun getCurrentWindowBounds(): Pair<LocalDateTime, LocalDateTime>
}
```

**Acceptance Criteria:**
- [ ] Correctly handles same-day windows
- [ ] Correctly handles midnight-crossing windows
- [ ] Returns correct window bounds
- [ ] Unit tests cover edge cases

---

### B.5 State Machine Manager

#### B.5.1 Create State Machine
```
Files to create:
□ domain/manager/StateMachineManager.kt
```

```kotlin
@Singleton
class StateMachineManager @Inject constructor(
    private val stateHolder: StateHolder,
    private val screenEventRepository: ScreenEventRepository,
    private val confirmScheduler: ConfirmOffScheduler
) {
    suspend fun onScreenOff(timestamp: Instant) {
        when (stateHolder.state.value) {
            ARMED_IDLE, ARMED_ALARM_SET -> {
                screenEventRepository.recordEvent(SCREEN_OFF, timestamp)
                stateHolder.setPendingCandidate(timestamp)
                confirmScheduler.scheduleConfirmation(timestamp)
                stateHolder.transitionTo(ARMED_PENDING_CONFIRM)
            }
            ARMED_PENDING_CONFIRM -> {
                // Latest wins - restart timer
                screenEventRepository.recordEvent(SCREEN_OFF, timestamp)
                stateHolder.setPendingCandidate(timestamp)
                confirmScheduler.rescheduleConfirmation(timestamp)
            }
            else -> { /* Ignore */ }
        }
    }
    
    suspend fun onScreenOn(timestamp: Instant) {
        if (stateHolder.state.value == ARMED_PENDING_CONFIRM) {
            confirmScheduler.cancelConfirmation()
            stateHolder.transitionTo(ARMED_IDLE)
        }
        screenEventRepository.recordEvent(SCREEN_ON, timestamp)
    }
}
```

**Acceptance Criteria:**
- [ ] State transitions follow spec
- [ ] Latest screen-off wins
- [ ] Events persisted correctly
- [ ] Timer managed correctly

---

### B.6 Confirmation Scheduler

#### B.6.1 Create ConfirmOffScheduler
```
Files to create:
□ domain/scheduler/ConfirmOffScheduler.kt
□ service/receiver/ConfirmationAlarmReceiver.kt
```

```kotlin
@Singleton
class ConfirmOffScheduler @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val settingsRepository: SettingsRepository
) {
    fun scheduleConfirmation(screenOffTime: Instant) {
        val confirmMinutes = settingsRepository.getSettings().confirmOffMinutes
        val triggerTime = screenOffTime.plusSeconds(confirmMinutes * 60L)
        
        val intent = Intent(context, ConfirmationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCREEN_OFF_TIME, screenOffTime.toEpochMilli())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CONFIRM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime.toEpochMilli(),
            pendingIntent
        )
    }
    
    fun cancelConfirmation() {
        val intent = Intent(context, ConfirmationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(...)
        alarmManager.cancel(pendingIntent)
    }
}
```

#### B.6.2 Create Confirmation Receiver
```kotlin
class ConfirmationAlarmReceiver : BroadcastReceiver() {
    
    @Inject lateinit var stateMachineManager: StateMachineManager
    @Inject lateinit var osAlarmCreator: OsAlarmCreator
    
    override fun onReceive(context: Context, intent: Intent) {
        val screenOffTime = intent.getLongExtra(EXTRA_SCREEN_OFF_TIME, 0)
        
        // Check if screen is still off
        val powerManager = context.getSystemService<PowerManager>()
        if (!powerManager.isInteractive) {
            // Screen still off - create alarm
            osAlarmCreator.createAlarm(screenOffTime)
        } else {
            // Screen turned on - cancel
            stateMachineManager.onConfirmationFailed()
        }
    }
}
```

**Acceptance Criteria:**
- [ ] 10-minute timer fires exactly
- [ ] Timer can be rescheduled
- [ ] Timer can be cancelled
- [ ] Confirmation checks screen state

---

### B.7 Persistence Updates

#### B.7.1 Update Database Operations
```
Tasks:
□ Add methods to persist pending candidate timestamp
□ Add methods to query last screen-off time
□ Add SharedPreferences for quick state access
```

**Acceptance Criteria:**
- [ ] Pending state survives process death
- [ ] Quick tile can read state without DB query
- [ ] All screen events logged

---

### Milestone B Deliverables

| Item | Status |
|------|--------|
| ArmManager with full lifecycle | □ |
| NightMonitorService foreground service | □ |
| ScreenStateReceiver runtime receiver | □ |
| NightWindowValidator with midnight handling | □ |
| StateMachineManager with all transitions | □ |
| ConfirmOffScheduler with exact alarms | □ |
| Integration tests for monitoring flow | □ |

---

## Milestone C — Alarm Creation

### Objective
Implement OS alarm creation via Clock app intent and internal backstop alarm.

### C.1 OsAlarmCreator

#### C.1.1 Create Alarm Creator
```
Files to create:
□ domain/scheduler/OsAlarmCreator.kt
```

```kotlin
@Singleton
class OsAlarmCreator @Inject constructor(
    private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository
) {
    fun createAlarm(screenOffTimeMillis: Long): AlarmCreationResult {
        val alarmTime = Instant.ofEpochMilli(screenOffTimeMillis)
            .plus(Duration.ofHours(8))
        
        val localDateTime = LocalDateTime.ofInstant(alarmTime, ZoneId.systemDefault())
        
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, localDateTime.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, localDateTime.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sleep8 Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            
            settingsRepository.getSettings().snoozeMinutes?.let {
                putExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, it)
            }
            
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        // Check if intent resolves
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        val canResolve = resolveInfo != null
        
        // Attempt to fire intent
        return try {
            if (canResolve) {
                context.startActivity(intent)
            }
            
            val record = AlarmRecord(
                screenOffTs = screenOffTimeMillis,
                confirmedAt = System.currentTimeMillis(),
                scheduledAlarmTs = alarmTime.toEpochMilli(),
                osAlarmIntentResolved = canResolve,
                osAlarmUiRequired = null // Detect if possible
            )
            
            alarmRepository.insertRecord(record)
            
            AlarmCreationResult.Success(record)
        } catch (e: Exception) {
            AlarmCreationResult.Failure(e)
        }
    }
    
    fun checkUiRequirement(): Boolean {
        // Heuristic to detect if OEM requires UI confirmation
        // This varies by device - document limitation
    }
}

sealed class AlarmCreationResult {
    data class Success(val record: AlarmRecord) : AlarmCreationResult()
    data class Failure(val error: Throwable) : AlarmCreationResult()
}
```

**Acceptance Criteria:**
- [ ] Creates OS alarm with correct time
- [ ] Handles non-resolving intent gracefully
- [ ] Persists alarm record
- [ ] Shows warning if UI required

---

### C.2 BackstopAlarmScheduler

#### C.2.1 Create Backstop Scheduler
```
Files to create:
□ domain/scheduler/BackstopAlarmScheduler.kt
□ service/receiver/BackstopAlarmReceiver.kt
```

```kotlin
@Singleton
class BackstopAlarmScheduler @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun scheduleBackstop(alarmRecordId: Long, alarmTimeMillis: Long) {
        val intent = Intent(context, BackstopAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_RECORD_ID, alarmRecordId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRecordId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTimeMillis,
            pendingIntent
        )
    }
}

class BackstopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recordId = intent.getLongExtra(EXTRA_ALARM_RECORD_ID, -1)
        
        // Log for audit purposes
        Log.i("Sleep8", "Backstop alarm fired for record: $recordId")
        
        // Optionally verify OS alarm exists or show notification
        // This is a resilience/audit mechanism, not the actual alarm
    }
}
```

**Acceptance Criteria:**
- [ ] Backstop scheduled for every alarm
- [ ] Backstop fires at correct time
- [ ] Logging for audit trail

---

### C.3 Integration with State Machine

#### C.3.1 Update Confirmation Flow
```
Update ConfirmationAlarmReceiver to:
□ Call OsAlarmCreator.createAlarm()
□ Call BackstopAlarmScheduler.scheduleBackstop()
□ Update state to ARMED_ALARM_SET
□ Update notification
```

**Acceptance Criteria:**
- [ ] Full flow from screen-off to alarm creation works
- [ ] State transitions correctly
- [ ] Multiple alarms can be created in one session

---

### C.4 UI Updates

#### C.4.1 Update Main Screen
```
Add to MainUiState:
□ List of created alarms this session
□ Next scheduled alarm time
□ Warning if OS clock UI required
```

#### C.4.2 Update Notification
```
Notification states:
□ ARMED_IDLE: "Sleep8 armed • Waiting for sleep"
□ ARMED_PENDING_CONFIRM: "Screen off detected • Confirming..."
□ ARMED_ALARM_SET: "Alarm set for HH:MM"
```

**Acceptance Criteria:**
- [ ] UI shows created alarms
- [ ] Notification updates correctly
- [ ] Warning shown if needed

---

### C.5 OEM Compatibility

#### C.5.1 Document Known Issues
```
Files to create:
□ docs/oem-compatibility.md
```

**Content:**
- List of tested OEM clock apps
- Known limitations per OEM
- Workarounds and user guidance

**Acceptance Criteria:**
- [ ] Major OEMs tested (Samsung, Pixel, OnePlus)
- [ ] Fallback behavior documented
- [ ] User guidance for problematic devices

---

### Milestone C Deliverables

| Item | Status |
|------|--------|
| OsAlarmCreator with intent handling | □ |
| BackstopAlarmScheduler for resilience | □ |
| Full confirmation → alarm flow | □ |
| Updated UI with alarm info | □ |
| OEM compatibility documentation | □ |
| Integration tests for alarm creation | □ |

---

## 2026 Update: Auto-Arming & Scheduling
- Add WindowScheduler to handle both night start/end triggers.
- Refactor ArmManager to support ArmSource (SCHEDULED, APP_BUTTON, QUICK_TILE).
- Implement Auto-Arm Schedule: arms/disarms at auto-arm schedule boundaries if enabled.
- Add separate Auto-Arm start/end settings (independent of Night Window).
- Auto-Arm schedules are authoritative; manual actions are immediate but temporary.
- Monitoring service starts/stops based on `armed && inNightWindow` (Night Window is a filter only).

---

## 2026 Update: Alarm Observability UI
- Add Alarm History screen (read-only list from `alarm_records`).
- Add “latest scheduled alarm” section on Home (latest record from `alarm_records`).
- Add navigation drawer with Home, Alarm History, Settings.
- Remove Settings entry from Home UI; access via drawer only.
- Do not query system Clock alarms; local DB is the source of truth.

## Milestone D — Reboot & Resilience

### Objective
Implement state restoration after device reboot and handle edge cases.

### D.1 BootReceiver

#### D.1.1 Create Boot Receiver
```
Files to create:
□ service/receiver/BootReceiver.kt
```

```kotlin
class BootReceiver : BroadcastReceiver() {
    
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var stateHolder: StateHolder
    @Inject lateinit var nightWindowValidator: NightWindowValidator
    @Inject lateinit var confirmScheduler: ConfirmOffScheduler
    @Inject lateinit var osAlarmCreator: OsAlarmCreator
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        goAsync {
            restoreState(context)
        }
    }
    
    private suspend fun restoreState(context: Context) {
        // 1. Check for active session
        val activeSession = sessionRepository.getActiveSession() ?: return
        
        // 2. Check if within night window
        val now = LocalDateTime.now()
        if (!nightWindowValidator.isInWindow(now)) {
            sessionRepository.endSession(activeSession.id)
            return
        }
        
        // 3. Restore state
        stateHolder.restore(activeSession)
        
        // 4. Check pending confirmation
        val pendingScreenOffTs = activeSession.pendingCandidateTs
        if (pendingScreenOffTs != null) {
            val elapsed = Duration.between(
                Instant.ofEpochMilli(pendingScreenOffTs),
                Instant.now()
            )
            val confirmMinutes = settingsRepository.getSettings().confirmOffMinutes
            
            val powerManager = context.getSystemService<PowerManager>()
            val screenOff = !powerManager.isInteractive
            
            if (elapsed.toMinutes() >= confirmMinutes && screenOff) {
                // Deadline passed while screen was off - create alarm now
                osAlarmCreator.createAlarm(pendingScreenOffTs)
            } else if (screenOff) {
                // Resume timer for remaining duration
                val remaining = Duration.ofMinutes(confirmMinutes.toLong()) - elapsed
                confirmScheduler.scheduleConfirmation(
                    pendingScreenOffTs,
                    remaining.toMillis()
                )
            }
            // If screen is on, just restore to ARMED_IDLE
        }
        
        // 5. Start monitoring service
        NightMonitorService.start(context)
    }
}
```

#### D.1.2 Register in Manifest
```xml
<receiver
    android:name=".service.receiver.BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

**Acceptance Criteria:**
- [ ] Auto-Arm boundaries restored after reboot
- [ ] Armed state restored per Auto-Arm schedule
- [ ] Timer resumes with correct remaining time when `armed && inNightWindow`
- [ ] Alarm created if deadline already passed and `armed && inNightWindow`
- [ ] Service starts/stops based on `armed && inNightWindow`

---

### D.2 Process Death Recovery

#### D.2.1 Update StateHolder Persistence
```
Tasks:
□ Persist all state to SharedPreferences for fast recovery
□ Mirror critical state to database
□ Add recovery logic to Application.onCreate()
```

**Acceptance Criteria:**
- [ ] State survives process death
- [ ] Quick recovery on app relaunch
- [ ] No duplicate alarms created

---

### D.3 Battery Optimization Handling

#### D.3.1 Create Battery Optimization Helper
```
Files to create:
□ util/BatteryOptimizationHelper.kt
```

```kotlin
object BatteryOptimizationHelper {
    
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService<PowerManager>()
        return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }
    
    fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    }
}
```

**Acceptance Criteria:**
- [ ] Can detect battery optimization status
- [ ] Can request exclusion
- [ ] Guidance shown to user

---

### D.4 Edge Case Handling

#### D.4.1 Window Transition
```
Tasks:
□ Auto-disarm when night window ends
□ Handle timezone changes
□ Handle daylight saving time transitions
```

#### D.4.2 Permission Revocation
```
Tasks:
□ Detect when exact alarm permission revoked
□ Show warning and guidance
□ Graceful degradation if permission lost
```

**Acceptance Criteria:**
- [ ] Auto-disarm at window end
- [ ] Timezone changes handled
- [ ] Permission loss handled gracefully

---

### D.5 Logging & Debugging

#### D.5.1 Add Comprehensive Logging
```
Tasks:
□ Add structured logging for all state transitions
□ Add logging for all alarm operations
□ Create debug screen (dev builds only)
```

**Acceptance Criteria:**
- [ ] All significant events logged
- [ ] Debug build has inspection tools
- [ ] No sensitive data logged

---

### Milestone D Deliverables

| Item | Status |
|------|--------|
| BootReceiver with full state restoration | □ |
| Process death recovery | □ |
| Battery optimization handling | □ |
| Edge case handling (window, timezone) | □ |
| Comprehensive logging | □ |
| Resilience integration tests | □ |

---

## Milestone E — QA & Polish

### Objective
Comprehensive testing, performance optimization, and release preparation.

### E.1 Manual Test Execution

#### E.1.1 Execute All Test Cases
```
See testing-plan.md for complete test cases
```

### E.2 Performance Optimization

#### E.2.1 Battery Impact Analysis
```
Tasks:
□ Profile battery usage during armed state
□ Optimize receiver registration
□ Minimize wake locks
```

#### E.2.2 Memory Optimization
```
Tasks:
□ Check for memory leaks
□ Optimize database queries
□ Review object allocations
```

**Acceptance Criteria:**
- [ ] Battery usage < 2% during night
- [ ] No memory leaks detected
- [ ] Smooth UI performance

---

### E.3 Accessibility

#### E.3.1 Add Accessibility Support
```
Tasks:
□ Content descriptions for all UI elements
□ Support for TalkBack
□ Sufficient color contrast
□ Touch target sizes >= 48dp
```

**Acceptance Criteria:**
- [ ] App passes accessibility scanner
- [ ] Usable with TalkBack
- [ ] WCAG AA compliant

---

### E.4 Localization

#### E.4.1 Prepare for Localization
```
Tasks:
□ Extract all strings to resources
□ Use plurals correctly
□ Handle RTL layouts
□ Date/time formatting locale-aware
```

**Acceptance Criteria:**
- [ ] All user-visible strings in resources
- [ ] Works in RTL languages
- [ ] Date/time formats correctly

---

### E.5 Release Preparation

#### E.5.1 Build Configuration
```
Tasks:
□ Configure ProGuard/R8 rules
□ Set up signing configuration
□ Create release build type
□ Version code/name management
```

#### E.5.2 Store Listing Preparation
```
Tasks:
□ App icon (all densities)
□ Feature graphic
□ Screenshots
□ Store description
□ Privacy policy
```

**Acceptance Criteria:**
- [ ] Release build works correctly
- [ ] APK size optimized
- [ ] Store assets ready

---

### Milestone E Deliverables

| Item | Status |
|------|--------|
| All manual tests passed | □ |
| Battery optimized | □ |
| Accessibility compliant | □ |
| Localization ready | □ |
| Release build configured | □ |
| Store listing prepared | □ |

---

## Dependency Graph

```
    ┌─────────────────────────────────────────────────────────────┐
    │                      MILESTONE A                             │
    │              Project Skeleton & Database                     │
    └──────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                      MILESTONE B                             │
    │              Monitoring & Confirmation                       │
    └──────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                      MILESTONE C                             │
    │                   Alarm Creation                             │
    └──────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                      MILESTONE D                             │
    │                 Reboot & Resilience                          │
    └──────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                      MILESTONE E                             │
    │                    QA & Polish                               │
    └─────────────────────────────────────────────────────────────┘
```

---

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OEM Clock app requires UI confirmation | High | Medium | Always show Clock UI |
| Battery optimization kills service | Medium | High | Guidance UI, backstop alarms |
| Exact alarm permission denied | Low | High | Graceful degradation, user guidance |
| Room migration issues | Low | Medium | Comprehensive testing, backup |
| State desync after crash | Medium | Medium | Idempotent operations, validation |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Alarm creation success rate | > 95% |
| State restoration after reboot | 100% |
| Battery usage during armed night | < 2% |
| Crash-free sessions | > 99.5% |
| Time from screen-off to alarm creation | < 10m 30s |
