package com.sleep8.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.db.Sleep8Database
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmCancelReason
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.domain.model.ArmSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RepositoryTest {

    private lateinit var db: Sleep8Database
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var alarmRepository: AlarmRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, Sleep8Database::class.java).build()
        settingsRepository = SettingsRepository(db.settingsDao())
        sessionRepository = SessionRepository(db.armSessionDao(), db.screenEventDao())
        alarmRepository = AlarmRepository(db.alarmRecordDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `get settings returns default when empty`() {
        val settings = kotlinx.coroutines.runBlocking { settingsRepository.getSettings() }
        assertEquals("22:30", settings.nightStart)
        assertEquals("04:00", settings.nightEnd)
        assertEquals(20, settings.confirmOffMinutes)
        assertEquals(com.sleep8.util.Constants.ALARM_DEFAULT_DURATION_MINUTES, settings.alarmDurationMinutes)
    }

    @Test
    fun `existing settings are not overridden by defaults`() {
        val custom = com.sleep8.data.db.entity.SettingsEntity(
            id = 1,
            nightStart = "23:30",
            nightEnd = "05:30",
            confirmOffMinutes = 15,
            alarmDurationMinutes = 300,
            overlayEnabled = true,
            armedDefault = true)
        kotlinx.coroutines.runBlocking { db.settingsDao().upsert(custom) }

        val settings = kotlinx.coroutines.runBlocking { settingsRepository.getSettings() }

        assertEquals("23:30", settings.nightStart)
        assertEquals("05:30", settings.nightEnd)
        assertEquals(300, settings.alarmDurationMinutes)
    }

    @Test
    fun `update settings persists changes`() {
        kotlinx.coroutines.runBlocking { settingsRepository.updateNightWindow("23:00", "07:00") }
        val settings = kotlinx.coroutines.runBlocking { settingsRepository.getSettings() }
        assertEquals("23:00", settings.nightStart)
    }

    @Test
    fun `update settings persists alarm duration minutes`() {
        val updated = com.sleep8.domain.model.Settings(
            nightStart = "22:00",
            nightEnd = "08:00",
            confirmOffMinutes = 10,
            alarmDurationMinutes = 0,
            overlayEnabled = false,
            armedDefault = false)
        kotlinx.coroutines.runBlocking { settingsRepository.updateSettings(updated) }
        val settings = kotlinx.coroutines.runBlocking { settingsRepository.getSettings() }
        assertEquals(0, settings.alarmDurationMinutes)
    }

    @Test
    fun `create session generates unique id`() {
        val session1 = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val session2 = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.QUICK_TILE) }
        assertNotEquals(session1.id, session2.id)
    }

    @Test
    fun `get active session returns non-ended session`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val active = kotlinx.coroutines.runBlocking { sessionRepository.getActiveSession() }
        assertEquals(session.id, active?.id)
    }

    @Test
    fun `end session sets disarmed timestamp`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        kotlinx.coroutines.runBlocking { sessionRepository.endSession(session.id, System.currentTimeMillis()) }
        val ended = kotlinx.coroutines.runBlocking { sessionRepository.getSession(session.id) }
        assertNotNull(ended?.disarmedAt)
    }

    @Test
    fun `insert alarm record persists all fields`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val record = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 111L,
            requestCode = 111,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        val id = kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(record) }
        val retrieved = kotlinx.coroutines.runBlocking { alarmRepository.getRecord(id) }
        assertEquals(record.screenOffTs, retrieved?.screenOffTs)
    }

    @Test
    fun `latest alarm record returns most recent confirmed time`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val older = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 222L,
            requestCode = 222,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        val newer = older.copy(
            screenOffTs = 4000L,
            confirmedAt = 5000L,
            scheduledAt = 5500L,
            triggerAt = 6000L,
            alarmInstanceId = 333L,
            requestCode = 333
        )
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(older) }
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(newer) }

        val latest = kotlinx.coroutines.runBlocking { alarmRepository.getLatestRecord() }
        assertEquals(5500L, latest?.scheduledAt)
    }

    @Test
    fun `alarm history orders newest to oldest`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val first = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 444L,
            requestCode = 444,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        val second = first.copy(
            screenOffTs = 4000L,
            confirmedAt = 5000L,
            scheduledAt = 5500L,
            triggerAt = 6000L,
            alarmInstanceId = 555L,
            requestCode = 555
        )
        val third = first.copy(
            screenOffTs = 7000L,
            confirmedAt = 8000L,
            scheduledAt = 8500L,
            triggerAt = 9000L,
            alarmInstanceId = 666L,
            requestCode = 666
        )
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(first) }
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(third) }
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(second) }

        val history = kotlinx.coroutines.runBlocking { alarmRepository.getAllRecordsNewestFirst() }
        assertEquals(listOf(8500L, 5500L, 2500L), history.map { it.scheduledAt })
    }

    @Test
    fun `alarm history paging returns expected slices`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        repeat(25) { index ->
            val scheduledAt = 10_000L + index
            val record = AlarmRecord(
                id = 0,
                sessionId = session.id,
                screenOffTs = 1_000L + index,
                confirmedAt = 2_000L + index,
                scheduledAt = scheduledAt,
                triggerAt = scheduledAt + 500L,
                durationUsedMinutes = 480,
                alarmInstanceId = 800L + index,
                requestCode = 800 + index,
                source = AlarmSource.SLEEP_AUTOMATION,
                status = AlarmStatus.SCHEDULED,
                canceledReason = null,
                firedAt = null,
                dismissedAt = null,
                overlayUsed = false,
                activityPresented = false
            )
            kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(record) }
        }

        val firstPage = kotlinx.coroutines.runBlocking {
            alarmRepository.getRecordsNewestFirstPaged(limit = 10, offset = 0)
        }
        val secondPage = kotlinx.coroutines.runBlocking {
            alarmRepository.getRecordsNewestFirstPaged(limit = 10, offset = 10)
        }
        val thirdPage = kotlinx.coroutines.runBlocking {
            alarmRepository.getRecordsNewestFirstPaged(limit = 10, offset = 20)
        }

        assertEquals(10, firstPage.size)
        assertEquals(10, secondPage.size)
        assertEquals(5, thirdPage.size)
        assertEquals(10_024L, firstPage.first().scheduledAt)
        assertEquals(10_014L, secondPage.first().scheduledAt)
        assertEquals(10_004L, thirdPage.first().scheduledAt)
    }

    @Test
    fun `alarm record lifecycle updates status fields`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val base = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 888L,
            requestCode = 888,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        val id = kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(base) }

        kotlinx.coroutines.runBlocking { alarmRepository.markFired(id, 3333L) }
        var updated = kotlinx.coroutines.runBlocking { alarmRepository.getRecord(id) }
        assertEquals(AlarmStatus.FIRED, updated?.status)
        assertEquals(3333L, updated?.firedAt)

        kotlinx.coroutines.runBlocking { alarmRepository.markDismissed(id, 4444L) }
        updated = kotlinx.coroutines.runBlocking { alarmRepository.getRecord(id) }
        assertEquals(AlarmStatus.DISMISSED, updated?.status)
        assertEquals(4444L, updated?.dismissedAt)

        kotlinx.coroutines.runBlocking { alarmRepository.markCanceled(id, AlarmCancelReason.USER_TOGGLE_OFF) }
        updated = kotlinx.coroutines.runBlocking { alarmRepository.getRecord(id) }
        assertEquals(AlarmStatus.CANCELED, updated?.status)
        assertEquals(AlarmCancelReason.USER_TOGGLE_OFF, updated?.canceledReason)
    }

    @Test
    fun `mark scheduled and flags update alarm fields`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val record = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 0L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 0L,
            requestCode = 0,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.CANCELED,
            canceledReason = AlarmCancelReason.USER_TOGGLE_OFF,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        val id = kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(record) }

        kotlinx.coroutines.runBlocking { alarmRepository.markScheduled(id, 5555L, 123L, 321) }
        kotlinx.coroutines.runBlocking { alarmRepository.markOverlayUsed(id) }
        kotlinx.coroutines.runBlocking { alarmRepository.markActivityPresented(id) }
        val updated = kotlinx.coroutines.runBlocking { alarmRepository.getRecord(id) }

        assertEquals(AlarmStatus.SCHEDULED, updated?.status)
        assertEquals(5555L, updated?.scheduledAt)
        assertEquals(123L, updated?.alarmInstanceId)
        assertEquals(321, updated?.requestCode)
        assertEquals(true, updated?.overlayUsed)
        assertEquals(true, updated?.activityPresented)
    }

    @Test
    fun `scheduled queries return only scheduled alarms`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val scheduled = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 5000L,
            triggerAt = 6000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 999L,
            requestCode = 999,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        val canceled = scheduled.copy(status = AlarmStatus.CANCELED, scheduledAt = 7000L, alarmInstanceId = 1000L)
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(scheduled) }
        kotlinx.coroutines.runBlocking { alarmRepository.insertRecord(canceled) }

        val scheduledOnly = kotlinx.coroutines.runBlocking { alarmRepository.getScheduledRecords() }
        val latestScheduled = kotlinx.coroutines.runBlocking { alarmRepository.getLatestScheduledRecord() }

        assertEquals(1, scheduledOnly.size)
        assertEquals(AlarmStatus.SCHEDULED, scheduledOnly.first().status)
        assertEquals(AlarmStatus.SCHEDULED, latestScheduled?.status)
    }

    @Test
    fun `clear all records removes alarm history`() {
        val session = kotlinx.coroutines.runBlocking { sessionRepository.createSession(ArmSource.APP_BUTTON) }
        val record = AlarmRecord(
            id = 0,
            sessionId = session.id,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 777L,
            requestCode = 777,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
        kotlinx.coroutines.runBlocking {
            alarmRepository.insertRecord(record)
            alarmRepository.clearAllRecords()
        }

        val history = kotlinx.coroutines.runBlocking { alarmRepository.getAllRecordsNewestFirst() }
        assertEquals(emptyList<AlarmRecord>(), history)
    }
}
