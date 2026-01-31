# Sleep8 — Testing Plan

## Overview

This document defines the comprehensive testing strategy for Sleep8, covering unit tests, integration tests, manual test cases, and automated UI tests.

---

## 1. Testing Strategy

### 1.1 Testing Pyramid

```
                    ┌─────────────┐
                    │   Manual    │  ← 10% (Critical paths, OEM testing)
                    │   E2E       │
                    ├─────────────┤
                    │ Integration │  ← 30% (Service + DB + Schedulers)
                    │   Tests     │
                    ├─────────────┤
                    │    Unit     │  ← 60% (Business logic, utilities)
                    │   Tests     │
                    └─────────────┘
```

### 1.2 Test Categories

| Category | Framework | Location | Purpose |
|----------|-----------|----------|---------|
| Unit Tests | JUnit 5 + MockK | `test/` | Logic validation |
| Integration Tests | JUnit 5 + Robolectric | `test/` | Component interaction |
| Android Tests | JUnit 4 + Espresso | `androidTest/` | UI and system integration |
| Manual Tests | - | This document | Real device validation |

---

## 2. Unit Tests

### 2.1 NightWindowValidator Tests

```kotlin
@Test
fun `same day window - time within bounds returns true`() {
    val validator = NightWindowValidator(mockSettings("09:00", "17:00"))
    val result = validator.isInWindow(LocalTime.of(12, 0))
    assertTrue(result)
}

@Test
fun `same day window - time before start returns false`() {
    val validator = NightWindowValidator(mockSettings("09:00", "17:00"))
    val result = validator.isInWindow(LocalTime.of(8, 30))
    assertFalse(result)
}

@Test
fun `same day window - time after end returns false`() {
    val validator = NightWindowValidator(mockSettings("09:00", "17:00"))
    val result = validator.isInWindow(LocalTime.of(17, 30))
    assertFalse(result)
}

@Test
fun `midnight crossing window - time after start returns true`() {
    val validator = NightWindowValidator(mockSettings("22:00", "08:00"))
    val result = validator.isInWindow(LocalTime.of(23, 0))
    assertTrue(result)
}

@Test
fun `midnight crossing window - time before end returns true`() {
    val validator = NightWindowValidator(mockSettings("22:00", "08:00"))
    val result = validator.isInWindow(LocalTime.of(6, 0))
    assertTrue(result)
}

@Test
fun `midnight crossing window - time between end and start returns false`() {
    val validator = NightWindowValidator(mockSettings("22:00", "08:00"))
    val result = validator.isInWindow(LocalTime.of(12, 0))
    assertFalse(result)
}

@Test
fun `exactly at start time returns true`() {
    val validator = NightWindowValidator(mockSettings("22:00", "08:00"))
    val result = validator.isInWindow(LocalTime.of(22, 0))
    assertTrue(result)
}

@Test
fun `exactly at end time returns true`() {
    val validator = NightWindowValidator(mockSettings("22:00", "08:00"))
    val result = validator.isInWindow(LocalTime.of(8, 0))
    assertTrue(result)
}

@Test
fun `midnight exactly - in midnight crossing window returns true`() {
    val validator = NightWindowValidator(mockSettings("22:00", "08:00"))
    val result = validator.isInWindow(LocalTime.of(0, 0))
    assertTrue(result)
}
```

**Test Count: 9**

---

### 2.2 TimeUtils Tests

```kotlin
@Test
fun `calculate alarm time adds 8 hours`() {
    val screenOff = Instant.parse("2024-01-15T23:00:00Z")
    val alarm = TimeUtils.calculateAlarmTime(screenOff)
    assertEquals(Instant.parse("2024-01-16T07:00:00Z"), alarm)
}

@Test
fun `calculate alarm time handles day rollover`() {
    val screenOff = Instant.parse("2024-01-15T20:00:00Z")
    val alarm = TimeUtils.calculateAlarmTime(screenOff)
    assertEquals(Instant.parse("2024-01-16T04:00:00Z"), alarm)
}

@Test
fun `calculate remaining confirmation time - partial elapsed`() {
    val screenOff = Instant.now().minusSeconds(300) // 5 minutes ago
    val remaining = TimeUtils.calculateRemainingConfirmTime(screenOff, 10)
    assertTrue(remaining.toMinutes() in 4..5)
}

@Test
fun `calculate remaining confirmation time - fully elapsed`() {
    val screenOff = Instant.now().minusSeconds(700) // 11+ minutes ago
    val remaining = TimeUtils.calculateRemainingConfirmTime(screenOff, 10)
    assertTrue(remaining.isNegative || remaining.isZero)
}

@Test
fun `format alarm time for display`() {
    val time = LocalTime.of(7, 30)
    val formatted = TimeUtils.formatAlarmTime(time)
    assertEquals("7:30 AM", formatted)
}

@Test
fun `format countdown timer`() {
    val remaining = Duration.ofSeconds(325) // 5:25
    val formatted = TimeUtils.formatCountdown(remaining)
    assertEquals("5:25", formatted)
}
```

**Test Count: 6**

---

### 2.3 StateMachineManager Tests

```kotlin
@Test
fun `disarmed state - arm transitions to armed idle`() {
    val manager = createStateMachineManager(initialState = DISARMED)
    manager.arm()
    assertEquals(ARMED_IDLE, manager.currentState)
}

@Test
fun `armed idle - screen off in window transitions to pending confirm`() {
    val manager = createStateMachineManager(initialState = ARMED_IDLE)
    manager.onScreenOff(inWindowTime)
    assertEquals(ARMED_PENDING_CONFIRM, manager.currentState)
}

@Test
fun `armed idle - screen off outside window stays in armed idle`() {
    val manager = createStateMachineManager(initialState = ARMED_IDLE)
    manager.onScreenOff(outsideWindowTime)
    assertEquals(ARMED_IDLE, manager.currentState)
}

@Test
fun `pending confirm - screen on transitions to armed idle`() {
    val manager = createStateMachineManager(initialState = ARMED_PENDING_CONFIRM)
    manager.onScreenOn()
    assertEquals(ARMED_IDLE, manager.currentState)
}

@Test
fun `pending confirm - timer expired transitions to alarm set`() {
    val manager = createStateMachineManager(initialState = ARMED_PENDING_CONFIRM)
    manager.onConfirmationTimerExpired(screenStillOff = true)
    assertEquals(ARMED_ALARM_SET, manager.currentState)
}

@Test
fun `pending confirm - timer expired but screen on transitions to armed idle`() {
    val manager = createStateMachineManager(initialState = ARMED_PENDING_CONFIRM)
    manager.onConfirmationTimerExpired(screenStillOff = false)
    assertEquals(ARMED_IDLE, manager.currentState)
}

@Test
fun `pending confirm - new screen off updates candidate and restarts timer`() {
    val manager = createStateMachineManager(initialState = ARMED_PENDING_CONFIRM)
    val firstOffTime = Instant.now()
    val secondOffTime = Instant.now().plusSeconds(120)
    
    manager.onScreenOff(firstOffTime)
    manager.onScreenOff(secondOffTime)
    
    assertEquals(secondOffTime, manager.pendingCandidateTime)
    verify(exactly = 2) { confirmScheduler.scheduleConfirmation(any()) }
}

@Test
fun `alarm set - new screen off transitions back to pending confirm`() {
    val manager = createStateMachineManager(initialState = ARMED_ALARM_SET)
    manager.onScreenOff(inWindowTime)
    assertEquals(ARMED_PENDING_CONFIRM, manager.currentState)
}

@Test
fun `any armed state - disarm transitions to disarmed`() {
    listOf(ARMED_IDLE, ARMED_PENDING_CONFIRM, ARMED_ALARM_SET).forEach { state ->
        val manager = createStateMachineManager(initialState = state)
        manager.disarm()
        assertEquals(DISARMED, manager.currentState)
    }
}

@Test
fun `disarm cancels confirmation timer`() {
    val manager = createStateMachineManager(initialState = ARMED_PENDING_CONFIRM)
    manager.disarm()
    verify { confirmScheduler.cancelConfirmation() }
}
```

**Test Count: 10**

---

### 2.4 ArmManager Tests

```kotlin
@Test
fun `arm creates session with correct source`() {
    val manager = createArmManager()
    val result = manager.arm(ArmSource.APP_BUTTON)
    
    verify { sessionRepository.createSession(match { it.source == ArmSource.APP_BUTTON }) }
    assertTrue(result.isSuccess)
}

@Test
fun `arm starts foreground service`() {
    val manager = createArmManager()
    manager.arm(ArmSource.QUICK_TILE)
    
    verify { serviceStarter.startNightMonitorService() }
}

@Test
fun `disarm ends session`() {
    val manager = createArmManager()
    manager.disarm()
    
    verify { sessionRepository.endSession(any()) }
}

@Test
fun `disarm stops foreground service`() {
    val manager = createArmManager()
    manager.disarm()
    
    verify { serviceStopper.stopNightMonitorService() }
}

@Test
fun `arm when already armed is idempotent`() {
    val manager = createArmManager(isArmed = true)
    manager.arm(ArmSource.APP_BUTTON)
    
    verify(exactly = 0) { sessionRepository.createSession(any()) }
}
```

**Test Count: 5**

---

### 2.5 OsAlarmCreator Tests

```kotlin
@Test
fun `create alarm sets correct hour and minute`() {
    val creator = createOsAlarmCreator()
    val screenOffTime = createLocalDateTime(hour = 23, minute = 30)
    
    creator.createAlarm(screenOffTime.toEpochMilli())
    
    verify { 
        context.startActivity(match { intent ->
            intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1) == 7 &&
            intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1) == 30
        })
    }
}

@Test
fun `create alarm includes snooze when configured`() {
    val creator = createOsAlarmCreator(snoozeMinutes = 10)
    
    creator.createAlarm(anyTime)
    
    verify {
        context.startActivity(match { intent ->
            intent.getIntExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, -1) == 10
        })
    }
}

@Test
fun `create alarm records success in database`() {
    val creator = createOsAlarmCreator()
    
    val result = creator.createAlarm(anyTime)
    
    assertTrue(result is AlarmCreationResult.Success)
    verify { alarmRepository.insertRecord(any()) }
}

@Test
fun `create alarm handles non-resolving intent`() {
    val creator = createOsAlarmCreator(intentResolves = false)
    
    val result = creator.createAlarm(anyTime)
    
    assertTrue((result as AlarmCreationResult.Success).record.osAlarmIntentResolved == false)
}

@Test
fun `create alarm schedules backstop`() {
    val creator = createOsAlarmCreator()
    
    creator.createAlarm(anyTime)
    
    verify { backstopScheduler.scheduleBackstop(any(), any()) }
}
```

**Test Count: 5**

---

### 2.6 ConfirmOffScheduler Tests

```kotlin
@Test
fun `schedule confirmation sets exact alarm at correct time`() {
    val scheduler = createConfirmScheduler()
    val screenOffTime = Instant.now()
    
    scheduler.scheduleConfirmation(screenOffTime)
    
    val expectedTrigger = screenOffTime.plusSeconds(600) // 10 minutes
    verify {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            range(expectedTrigger.toEpochMilli() - 1000, expectedTrigger.toEpochMilli() + 1000),
            any()
        )
    }
}

@Test
fun `reschedule confirmation cancels previous and sets new`() {
    val scheduler = createConfirmScheduler()
    
    scheduler.scheduleConfirmation(Instant.now())
    scheduler.scheduleConfirmation(Instant.now().plusSeconds(120))
    
    // Should use UPDATE_CURRENT flag, so single pending intent
    verify(exactly = 2) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
}

@Test
fun `cancel confirmation removes pending alarm`() {
    val scheduler = createConfirmScheduler()
    
    scheduler.cancelConfirmation()
    
    verify { alarmManager.cancel(any<PendingIntent>()) }
}

@Test
fun `schedule with custom confirm minutes uses setting`() {
    val scheduler = createConfirmScheduler(confirmMinutes = 15)
    val screenOffTime = Instant.now()
    
    scheduler.scheduleConfirmation(screenOffTime)
    
    val expectedTrigger = screenOffTime.plusSeconds(900) // 15 minutes
    verify {
        alarmManager.setExactAndAllowWhileIdle(
            any(),
            range(expectedTrigger.toEpochMilli() - 1000, expectedTrigger.toEpochMilli() + 1000),
            any()
        )
    }
}
```

**Test Count: 4**

---

### 2.7 Repository Tests

```kotlin
// SettingsRepository
@Test
fun `get settings returns default when empty`() {
    val repo = createSettingsRepository(empty = true)
    val settings = repo.getSettings()
    assertEquals("22:00", settings.nightStart)
    assertEquals("08:00", settings.nightEnd)
}

@Test
fun `update settings persists changes`() {
    val repo = createSettingsRepository()
    repo.updateNightWindow("23:00", "07:00")
    val settings = repo.getSettings()
    assertEquals("23:00", settings.nightStart)
}

// SessionRepository
@Test
fun `create session generates unique id`() {
    val repo = createSessionRepository()
    val session1 = repo.createSession(ArmSource.APP_BUTTON)
    val session2 = repo.createSession(ArmSource.QUICK_TILE)
    assertNotEquals(session1.id, session2.id)
}

@Test
fun `get active session returns non-ended session`() {
    val repo = createSessionRepository()
    val session = repo.createSession(ArmSource.APP_BUTTON)
    val active = repo.getActiveSession()
    assertEquals(session.id, active?.id)
}

@Test
fun `end session sets disarmed timestamp`() {
    val repo = createSessionRepository()
    val session = repo.createSession(ArmSource.APP_BUTTON)
    repo.endSession(session.id)
    val ended = repo.getSession(session.id)
    assertNotNull(ended?.disarmedAt)
}

// AlarmRepository
@Test
fun `insert alarm record persists all fields`() {
    val repo = createAlarmRepository()
    val record = AlarmRecord(
        screenOffTs = 1000L,
        confirmedAt = 2000L,
        scheduledAlarmTs = 3000L,
        osAlarmIntentResolved = true
    )
    val id = repo.insertRecord(record)
    val retrieved = repo.getRecord(id)
    assertEquals(record.screenOffTs, retrieved?.screenOffTs)
}

@Test
fun `get alarms for session returns correct records`() {
    val repo = createAlarmRepository()
    val sessionId = 1L
    repo.insertRecord(createRecord(sessionId = sessionId))
    repo.insertRecord(createRecord(sessionId = sessionId))
    repo.insertRecord(createRecord(sessionId = 2L))
    
    val alarms = repo.getAlarmsForSession(sessionId)
    assertEquals(2, alarms.size)
}
```

**Test Count: 7**

---

## 3. Integration Tests

### 3.1 Database Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    
    private lateinit var db: Sleep8Database
    
    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Sleep8Database::class.java
        ).build()
    }
    
    @After
    fun teardown() {
        db.close()
    }
    
    @Test
    fun `screen events linked to session`() {
        val sessionId = db.armSessionDao().insert(createSessionEntity())
        val eventId = db.screenEventDao().insert(
            createScreenEventEntity(sessionId = sessionId)
        )
        
        val events = db.screenEventDao().getEventsForSession(sessionId)
        assertEquals(1, events.size)
        assertEquals(eventId, events[0].eventId)
    }
    
    @Test
    fun `alarm records linked to session`() {
        val sessionId = db.armSessionDao().insert(createSessionEntity())
        db.alarmRecordDao().insert(createAlarmRecordEntity(sessionId = sessionId))
        
        val records = db.alarmRecordDao().getRecordsForSession(sessionId)
        assertEquals(1, records.size)
    }
    
    @Test
    fun `cascade delete removes related records`() {
        val sessionId = db.armSessionDao().insert(createSessionEntity())
        db.screenEventDao().insert(createScreenEventEntity(sessionId = sessionId))
        db.alarmRecordDao().insert(createAlarmRecordEntity(sessionId = sessionId))
        
        db.armSessionDao().delete(sessionId)
        
        assertEquals(0, db.screenEventDao().getEventsForSession(sessionId).size)
        assertEquals(0, db.alarmRecordDao().getRecordsForSession(sessionId).size)
    }
}
```

**Test Count: 3**

---

### 3.2 Service Integration Tests

```kotlin
@RunWith(RobolectricTestRunner::class)
class ServiceIntegrationTest {
    
    @Test
    fun `service starts in foreground when armed`() {
        val scenario = ServiceScenario.launch(NightMonitorService::class.java)
        scenario.onService { service ->
            service.handleArm()
            
            val nm = shadowOf(service.getSystemService(NotificationManager::class.java))
            assertTrue(nm.notificationChannels.any { it.id == NOTIFICATION_CHANNEL_ID })
        }
    }
    
    @Test
    fun `service registers screen receiver on start`() {
        val scenario = ServiceScenario.launch(NightMonitorService::class.java)
        scenario.onService { service ->
            service.handleArm()
            
            val shadowApp = shadowOf(service.application)
            val receivers = shadowApp.registeredReceivers
            assertTrue(receivers.any { it.intentFilter.hasAction(Intent.ACTION_SCREEN_OFF) })
        }
    }
    
    @Test
    fun `service unregisters receiver on stop`() {
        val scenario = ServiceScenario.launch(NightMonitorService::class.java)
        scenario.onService { service ->
            service.handleArm()
            service.handleDisarm()
            
            val shadowApp = shadowOf(service.application)
            val receivers = shadowApp.registeredReceivers
            assertFalse(receivers.any { it.intentFilter.hasAction(Intent.ACTION_SCREEN_OFF) })
        }
    }
}
```

**Test Count: 3**

---

### 3.3 Full Flow Integration Tests

```kotlin
@RunWith(RobolectricTestRunner::class)
class FullFlowIntegrationTest {
    
    @Inject lateinit var armManager: ArmManager
    @Inject lateinit var stateMachineManager: StateMachineManager
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var alarmRepository: AlarmRepository
    
    @Test
    fun `complete flow - arm to alarm creation`() = runTest {
        // 1. Arm the app
        armManager.arm(ArmSource.APP_BUTTON)
        assertEquals(ARMED_IDLE, stateMachineManager.currentState)
        
        // 2. Simulate screen off
        stateMachineManager.onScreenOff(Instant.now())
        assertEquals(ARMED_PENDING_CONFIRM, stateMachineManager.currentState)
        
        // 3. Advance time by 10 minutes
        advanceTimeBy(Duration.ofMinutes(10))
        
        // 4. Simulate confirmation timer firing (screen still off)
        stateMachineManager.onConfirmationTimerExpired(screenStillOff = true)
        assertEquals(ARMED_ALARM_SET, stateMachineManager.currentState)
        
        // 5. Verify alarm was created
        val session = sessionRepository.getActiveSession()!!
        val alarms = alarmRepository.getAlarmsForSession(session.id)
        assertEquals(1, alarms.size)
        assertTrue(alarms[0].osAlarmIntentResolved)
    }
    
    @Test
    fun `cancel flow - screen on before confirmation`() = runTest {
        armManager.arm(ArmSource.APP_BUTTON)
        stateMachineManager.onScreenOff(Instant.now())
        
        advanceTimeBy(Duration.ofMinutes(5))
        
        stateMachineManager.onScreenOn()
        assertEquals(ARMED_IDLE, stateMachineManager.currentState)
        
        val session = sessionRepository.getActiveSession()!!
        val alarms = alarmRepository.getAlarmsForSession(session.id)
        assertEquals(0, alarms.size)
    }
    
    @Test
    fun `latest wins - multiple screen off events`() = runTest {
        armManager.arm(ArmSource.APP_BUTTON)
        
        val firstOff = Instant.now()
        stateMachineManager.onScreenOff(firstOff)
        
        advanceTimeBy(Duration.ofMinutes(3))
        
        val secondOff = Instant.now()
        stateMachineManager.onScreenOff(secondOff)
        
        advanceTimeBy(Duration.ofMinutes(10))
        stateMachineManager.onConfirmationTimerExpired(screenStillOff = true)
        
        val session = sessionRepository.getActiveSession()!!
        val alarms = alarmRepository.getAlarmsForSession(session.id)
        assertEquals(1, alarms.size)
        
        // Alarm should be based on second screen off
        val expectedAlarmTime = secondOff.plus(Duration.ofHours(8))
        assertEquals(expectedAlarmTime.toEpochMilli(), alarms[0].scheduledAlarmTs)
    }
}
```

**Test Count: 3**

---

## 4. Manual Test Cases

### 4.1 Test Case Format

Each test case includes:
- **ID**: Unique identifier
- **Category**: Functional area
- **Priority**: P0 (Critical) / P1 (High) / P2 (Medium)
- **Preconditions**: Required state before test
- **Steps**: Detailed test steps
- **Expected Result**: What should happen
- **Actual Result**: (Filled during testing)
- **Status**: Pass / Fail / Blocked

---

### 4.2 Arming & Disarming

#### TC-001: Arm via App Button
| Field | Value |
|-------|-------|
| ID | TC-001 |
| Category | Arming |
| Priority | P0 |
| Preconditions | App installed, permissions granted, within night window |
| Steps | 1. Open app<br>2. Tap "Arm Tonight" button |
| Expected Result | - Button changes to "Disarm"<br>- Status shows "Armed"<br>- Notification appears: "Sleep8 armed"<br>- Quick Settings tile shows active state |

#### TC-002: Arm via Quick Settings Tile
| Field | Value |
|-------|-------|
| ID | TC-002 |
| Category | Arming |
| Priority | P0 |
| Preconditions | App installed, permissions granted, tile added to Quick Settings |
| Steps | 1. Pull down notification shade<br>2. Tap Sleep8 tile |
| Expected Result | - Tile becomes active (highlighted)<br>- Notification appears<br>- App UI (if open) shows armed state |

#### TC-003: Disarm via App Button
| Field | Value |
|-------|-------|
| ID | TC-003 |
| Category | Arming |
| Priority | P0 |
| Preconditions | App is armed |
| Steps | 1. Open app<br>2. Tap "Disarm" button |
| Expected Result | - Button changes to "Arm Tonight"<br>- Notification disappears<br>- Quick Settings tile shows inactive |

#### TC-004: Disarm via Quick Settings Tile
| Field | Value |
|-------|-------|
| ID | TC-004 |
| Category | Arming |
| Priority | P0 |
| Preconditions | App is armed |
| Steps | 1. Pull down notification shade<br>2. Tap Sleep8 tile |
| Expected Result | - Tile becomes inactive<br>- Notification disappears |

#### TC-005: Arm Outside Night Window
| Field | Value |
|-------|-------|
| ID | TC-005 |
| Category | Arming |
| Priority | P1 |
| Preconditions | Current time is outside configured night window |
| Steps | 1. Open app<br>2. Attempt to tap "Arm Tonight" |
| Expected Result | - Button may be disabled OR<br>- Warning shown that arming outside window<br>- Service behavior documented |

---

### 4.3 Screen Detection & Confirmation

#### TC-010: Basic Screen Off Detection
| Field | Value |
|-------|-------|
| ID | TC-010 |
| Category | Detection |
| Priority | P0 |
| Preconditions | App armed, within night window |
| Steps | 1. Turn off screen (power button) |
| Expected Result | - Notification updates: "Screen off detected"<br>- Confirmation timer starts (10 min) |

#### TC-011: Screen On Cancels Confirmation
| Field | Value |
|-------|-------|
| ID | TC-011 |
| Category | Detection |
| Priority | P0 |
| Preconditions | App armed, screen off, confirmation pending (< 10 min) |
| Steps | 1. Turn on screen before 10 minutes |
| Expected Result | - Notification returns to "Armed"<br>- No alarm created<br>- Timer cancelled |

#### TC-012: Screen Stays Off - Alarm Created
| Field | Value |
|-------|-------|
| ID | TC-012 |
| Category | Alarm Creation |
| Priority | P0 |
| Preconditions | App armed, within night window |
| Steps | 1. Turn off screen<br>2. Wait 10+ minutes with screen off |
| Expected Result | - OS alarm created for screen_off_time + 8 hours<br>- Notification shows: "Alarm set for HH:MM"<br>- Alarm visible in Clock app |

#### TC-013: Multiple Screen Off - Latest Wins
| Field | Value |
|-------|-------|
| ID | TC-013 |
| Category | Detection |
| Priority | P0 |
| Preconditions | App armed, within night window |
| Steps | 1. Turn off screen at 22:00<br>2. Turn on screen at 22:03<br>3. Turn off screen at 22:05<br>4. Wait 10+ minutes |
| Expected Result | - Alarm set for 06:05 (22:05 + 8h), NOT 06:00 |

#### TC-014: Multiple Screen Off During Confirmation
| Field | Value |
|-------|-------|
| ID | TC-014 |
| Category | Detection |
| Priority | P1 |
| Preconditions | App armed, confirmation pending |
| Steps | 1. Screen off at 22:00<br>2. Screen on at 22:02<br>3. Screen off at 22:04 (new candidate)<br>4. Wait 10+ minutes |
| Expected Result | - Confirmation timer resets at 22:04<br>- Alarm set for 06:04 (22:04 + 8h) |

#### TC-015: Screen Off Outside Night Window Ignored
| Field | Value |
|-------|-------|
| ID | TC-015 |
| Category | Detection |
| Priority | P1 |
| Preconditions | App armed, current time outside night window |
| Steps | 1. Turn off screen |
| Expected Result | - No confirmation timer starts<br>- Event logged but not acted upon |

---

### 4.4 Alarm Creation

#### TC-020: Verify OS Alarm Appears in Clock App
| Field | Value |
|-------|-------|
| ID | TC-020 |
| Category | Alarm |
| Priority | P0 |
| Preconditions | Alarm creation flow completed |
| Steps | 1. Open system Clock app<br>2. Navigate to Alarms tab |
| Expected Result | - New alarm visible at scheduled time<br>- Alarm label shows "Sleep8 Alarm" |

#### TC-021: Multiple Alarms in One Session
| Field | Value |
|-------|-------|
| ID | TC-021 |
| Category | Alarm |
| Priority | P1 |
| Preconditions | App armed |
| Steps | 1. Complete first alarm creation flow<br>2. Turn screen on<br>3. Turn screen off again<br>4. Wait 10+ minutes |
| Expected Result | - Second alarm created<br>- Both alarms visible in Clock app<br>- Both recorded in DB |

#### TC-022: Alarm Time Calculation Accuracy
| Field | Value |
|-------|-------|
| ID | TC-022 |
| Category | Alarm |
| Priority | P0 |
| Preconditions | App armed |
| Steps | 1. Note exact time<br>2. Turn off screen at exactly 23:15:00<br>3. Wait for alarm creation |
| Expected Result | - Alarm set for exactly 07:15 |

#### TC-023: Alarm with Snooze Enabled
| Field | Value |
|-------|-------|
| ID | TC-023 |
| Category | Alarm |
| Priority | P2 |
| Preconditions | Snooze enabled in settings (e.g., 10 min) |
| Steps | 1. Complete alarm creation flow<br>2. Check created alarm in Clock app |
| Expected Result | - Alarm has snooze configured (if supported by Clock app) |

#### TC-024: Disarm Does Not Delete Created Alarms
| Field | Value |
|-------|-------|
| ID | TC-024 |
| Category | Alarm |
| Priority | P1 |
| Preconditions | Alarm already created |
| Steps | 1. Disarm the app<br>2. Check Clock app |
| Expected Result | - Previously created alarm still exists<br>- User must manually delete if unwanted |

---

### 4.5 Reboot Recovery

#### TC-030: Reboot While Armed (Idle)
| Field | Value |
|-------|-------|
| ID | TC-030 |
| Category | Reboot |
| Priority | P0 |
| Preconditions | App armed, within night window, no pending confirmation |
| Steps | 1. Reboot device<br>2. Wait for boot complete |
| Expected Result | - App restores armed state<br>- Notification reappears<br>- Monitoring resumes |

#### TC-031: Reboot During Confirmation (Before Deadline)
| Field | Value |
|-------|-------|
| ID | TC-031 |
| Category | Reboot |
| Priority | P0 |
| Preconditions | App armed, confirmation pending (5 min into 10 min timer) |
| Steps | 1. Reboot device<br>2. Wait for boot complete |
| Expected Result | - Armed state restored<br>- Remaining timer (5 min) resumes<br>- If screen still off, alarm created after remaining time |

#### TC-032: Reboot After Deadline Passed
| Field | Value |
|-------|-------|
| ID | TC-032 |
| Category | Reboot |
| Priority | P0 |
| Preconditions | App armed, pending screen off timestamp exists |
| Steps | 1. Set screen off time to 15+ minutes ago (simulate)<br>2. Reboot device while screen is OFF |
| Expected Result | - On boot, alarm created immediately<br>- (elapsed time > 10 min confirms screen was off) |

#### TC-033: Reboot Outside Night Window
| Field | Value |
|-------|-------|
| ID | TC-033 |
| Category | Reboot |
| Priority | P1 |
| Preconditions | Was armed, device reboots after night window ends |
| Steps | 1. Arm at 22:00<br>2. Reboot at 09:00 (after window end) |
| Expected Result | - Session marked as ended<br>- App not armed after reboot |

---

### 4.6 Night Window Edge Cases

#### TC-040: Window Crossing Midnight - Before Midnight
| Field | Value |
|-------|-------|
| ID | TC-040 |
| Category | Window |
| Priority | P0 |
| Preconditions | Night window: 22:00-08:00, current time: 23:30 |
| Steps | 1. Arm app<br>2. Turn off screen |
| Expected Result | - Correctly detects as within window<br>- Confirmation starts |

#### TC-041: Window Crossing Midnight - After Midnight
| Field | Value |
|-------|-------|
| ID | TC-041 |
| Category | Window |
| Priority | P0 |
| Preconditions | Night window: 22:00-08:00, current time: 02:00 |
| Steps | 1. Arm app<br>2. Turn off screen |
| Expected Result | - Correctly detects as within window<br>- Confirmation starts |

#### TC-042: Exactly at Window Start
| Field | Value |
|-------|-------|
| ID | TC-042 |
| Category | Window |
| Priority | P2 |
| Preconditions | Night window: 22:00-08:00, current time: 22:00:00 |
| Steps | 1. Turn off screen exactly at 22:00 |
| Expected Result | - Within window (boundary inclusive) |

#### TC-043: Exactly at Window End
| Field | Value |
|-------|-------|
| ID | TC-043 |
| Category | Window |
| Priority | P2 |
| Preconditions | Night window: 22:00-08:00, current time: 08:00:00 |
| Steps | 1. Turn off screen exactly at 08:00 |
| Expected Result | - Within window (boundary inclusive) |

#### TC-044: Auto-Disarm at Window End
| Field | Value |
|-------|-------|
| ID | TC-044 |
| Category | Window |
| Priority | P1 |
| Preconditions | App armed |
| Steps | 1. Wait until night window ends |
| Expected Result | - App auto-disarms<br>- Notification removed<br>- Session marked ended |

---

### 4.7 Settings

#### TC-050: Change Night Window Start
| Field | Value |
|-------|-------|
| ID | TC-050 |
| Category | Settings |
| Priority | P1 |
| Preconditions | Default settings |
| Steps | 1. Open Settings<br>2. Change start time to 21:00<br>3. Save |
| Expected Result | - New window effective immediately<br>- Persists across app restart |

#### TC-051: Change Night Window End
| Field | Value |
|-------|-------|
| ID | TC-051 |
| Category | Settings |
| Priority | P1 |
| Preconditions | Default settings |
| Steps | 1. Open Settings<br>2. Change end time to 09:00<br>3. Save |
| Expected Result | - New window effective immediately<br>- Auto-disarm time updated |

#### TC-052: Enable/Disable Snooze
| Field | Value |
|-------|-------|
| ID | TC-052 |
| Category | Settings |
| Priority | P2 |
| Preconditions | Snooze is OFF |
| Steps | 1. Open Settings<br>2. Enable snooze<br>3. Set to 10 minutes |
| Expected Result | - Setting persists<br>- Next alarm includes snooze (if supported) |

---

### 4.8 Permissions & Reliability

#### TC-060: Exact Alarm Permission Check
| Field | Value |
|-------|-------|
| ID | TC-060 |
| Category | Permissions |
| Priority | P0 |
| Preconditions | Fresh install |
| Steps | 1. Open app<br>2. Check reliability checklist |
| Expected Result | - Shows status of SCHEDULE_EXACT_ALARM<br>- Provides action to grant if needed |

#### TC-061: Battery Optimization Warning
| Field | Value |
|-------|-------|
| ID | TC-061 |
| Category | Permissions |
| Priority | P1 |
| Preconditions | Battery optimization enabled for app |
| Steps | 1. Open app<br>2. Check reliability checklist |
| Expected Result | - Warning shown<br>- Link to battery settings |

#### TC-062: Grant Battery Optimization Exclusion
| Field | Value |
|-------|-------|
| ID | TC-062 |
| Category | Permissions |
| Priority | P1 |
| Preconditions | Battery optimization warning shown |
| Steps | 1. Tap "Request exclusion"<br>2. Grant in system dialog |
| Expected Result | - Warning disappears<br>- Checklist shows green status |

---

### 4.9 Edge Cases & Error Handling

#### TC-070: App Force Stopped While Armed
| Field | Value |
|-------|-------|
| ID | TC-070 |
| Category | Resilience |
| Priority | P1 |
| Preconditions | App armed |
| Steps | 1. Settings > Apps > Sleep8 > Force Stop |
| Expected Result | - On next boot/interaction, state may be lost<br>- Document expected behavior |

#### TC-071: Low Memory Service Kill
| Field | Value |
|-------|-------|
| ID | TC-071 |
| Category | Resilience |
| Priority | P1 |
| Preconditions | App armed, many apps running |
| Steps | 1. Open many memory-intensive apps<br>2. Observe Sleep8 behavior |
| Expected Result | - START_STICKY should restart service<br>- State restored from DB |

#### TC-072: Timezone Change
| Field | Value |
|-------|-------|
| ID | TC-072 |
| Category | Edge Case |
| Priority | P2 |
| Preconditions | App armed, pending confirmation |
| Steps | 1. Change device timezone<br>2. Observe behavior |
| Expected Result | - Alarm time adjusts to local time<br>- Night window interpreted in new timezone |

#### TC-073: Clock App Not Available
| Field | Value |
|-------|-------|
| ID | TC-073 |
| Category | Error |
| Priority | P1 |
| Preconditions | Device without compatible Clock app |
| Steps | 1. Complete alarm creation flow |
| Expected Result | - Error handled gracefully<br>- Warning shown to user<br>- Backstop alarm still scheduled |

#### TC-074: Network Offline Verification
| Field | Value |
|-------|-------|
| ID | TC-074 |
| Category | Privacy |
| Priority | P0 |
| Preconditions | Device with network monitoring |
| Steps | 1. Use app in all flows<br>2. Monitor network traffic |
| Expected Result | - ZERO network requests made |

---

## 5. OEM-Specific Test Matrix

### 5.1 Devices to Test

| OEM | Model | Android Version | Priority |
|-----|-------|-----------------|----------|
| Google | Pixel 7/8 | 14 | P0 |
| Samsung | Galaxy S23/S24 | 14 | P0 |
| Samsung | Galaxy A series | 13/14 | P1 |
| OnePlus | 11/12 | 14 | P1 |
| Xiaomi | 13/14 | 14 | P2 |
| Oppo | Find X6 | 14 | P2 |

### 5.2 OEM-Specific Tests

#### TC-OEM-001: Samsung Clock App Integration
| Field | Value |
|-------|-------|
| Steps | Complete alarm creation on Samsung device |
| Expected | Alarm appears in Samsung Clock app |
| Known Issues | May require UI confirmation |

#### TC-OEM-002: Xiaomi Battery Aggressive Kill
| Field | Value |
|-------|-------|
| Steps | Arm app, lock device for extended period |
| Expected | Service survives aggressive battery management |
| Notes | May require MIUI-specific battery exemption |

#### TC-OEM-003: OnePlus Doze Mode
| Field | Value |
|-------|-------|
| Steps | Arm app, let device enter deep doze |
| Expected | Exact alarms still fire |
| Notes | Test with OxygenOS battery optimization |

---

## 6. Test Data Requirements

### 6.1 Time Configurations

| Scenario | Night Start | Night End | Current Time | Expected |
|----------|-------------|-----------|--------------|----------|
| Default | 22:00 | 08:00 | 23:00 | In window |
| Default | 22:00 | 08:00 | 15:00 | Out of window |
| Same day | 06:00 | 14:00 | 10:00 | In window |
| Same day | 06:00 | 14:00 | 16:00 | Out of window |
| Midnight | 22:00 | 08:00 | 00:00 | In window |
| Midnight | 22:00 | 08:00 | 08:01 | Out of window |

### 6.2 Timing Scenarios

| Scenario | Screen Off | Screen On | Confirm Time | Alarm Time |
|----------|------------|-----------|--------------|------------|
| Normal | 22:30 | - | 22:40 | 06:30 |
| Cancel | 22:30 | 22:35 | - | - |
| Latest wins | 22:30, 22:45 | 22:35 | 22:55 | 06:45 |
| Multiple | 22:30, 23:00 | 22:40 | 22:40, 23:10 | 06:30, 07:00 |

---

## 7. Automated Test Execution

### 7.1 CI Configuration

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
      - name: Upload test results
        uses: actions/upload-artifact@v3
        with:
          name: unit-test-results
          path: app/build/reports/tests/

  instrumented-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 31
          target: google_apis
          script: ./gradlew connectedDebugAndroidTest
```

### 7.2 Test Coverage Targets

| Module | Target Coverage |
|--------|-----------------|
| domain/validator | 95% |
| domain/manager | 90% |
| domain/scheduler | 85% |
| data/repository | 80% |
| Overall | 80% |

---

## 8. Test Sign-Off Checklist

### 8.1 Pre-Release Checklist

| Category | Tests | Status |
|----------|-------|--------|
| Unit Tests | All passing | □ |
| Integration Tests | All passing | □ |
| P0 Manual Tests | All passing | □ |
| P1 Manual Tests | All passing | □ |
| OEM Tests (Pixel) | All passing | □ |
| OEM Tests (Samsung) | All passing | □ |
| Performance Tests | Battery < 2% | □ |
| Accessibility | Scanner passing | □ |

### 8.2 Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| QA Lead | | | |
| Product Owner | | | |

---

## 9. Known Issues & Workarounds

| Issue ID | Description | Severity | Workaround |
|----------|-------------|----------|------------|
| KI-001 | Samsung Clock may show UI | Medium | User confirms once |
| KI-002 | Xiaomi kills service | High | Add to battery whitelist |
| KI-003 | Some OEMs ignore SKIP_UI | Low | Document in FAQ |

---

## 10. Test Environment Setup

### 10.1 Required Tools

- Android Studio Hedgehog+
- ADB (for manual testing)
- Charles Proxy (for network verification)
- Android Emulators (API 31-35)
- Physical test devices

### 10.2 Test Device Setup

```bash
# Enable USB debugging
adb shell settings put global development_settings_enabled 1

# Grant exact alarm permission (API 31+)
adb shell appops set com.sleep8 SCHEDULE_EXACT_ALARM allow

# Check screen state
adb shell dumpsys power | grep "Display Power"

# Simulate screen off (requires root or use physical button)
adb shell input keyevent KEYCODE_POWER
```

### 10.3 Time Manipulation (for testing)

```bash
# Set device time (requires root or test build)
adb shell date MMDDhhmmYYYY

# Alternative: Use debug menu in app to override time for testing
```
