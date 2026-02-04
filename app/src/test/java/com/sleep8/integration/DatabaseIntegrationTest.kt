package com.sleep8.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.db.Sleep8Database
import com.sleep8.data.db.entity.AlarmRecordEntity
import com.sleep8.data.db.entity.ArmSessionEntity
import com.sleep8.data.db.entity.ScreenEventEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
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
    fun `screen events linked to session`() = runTest {
        val sessionId = db.armSessionDao().insert(createSessionEntity())
        val eventId = db.screenEventDao().insert(
            createScreenEventEntity(sessionId = sessionId)
        )

        val events = db.screenEventDao().getEventsForSession(sessionId)
        assertEquals(1, events.size)
        assertEquals(eventId, events[0].eventId)
    }

    @Test
    fun `alarm records linked to session`() = runTest {
        val sessionId = db.armSessionDao().insert(createSessionEntity())
        db.alarmRecordDao().insert(createAlarmRecordEntity(sessionId = sessionId))

        val records = db.alarmRecordDao().getRecordsForSession(sessionId)
        assertEquals(1, records.size)
    }

    @Test
    fun `cascade delete removes related records`() = runTest {
        val sessionId = db.armSessionDao().insert(createSessionEntity())
        db.screenEventDao().insert(createScreenEventEntity(sessionId = sessionId))
        db.alarmRecordDao().insert(createAlarmRecordEntity(sessionId = sessionId))

        db.armSessionDao().delete(sessionId)

        assertEquals(0, db.screenEventDao().getEventsForSession(sessionId).size)
        assertEquals(0, db.alarmRecordDao().getRecordsForSession(sessionId).size)
    }

    private fun createSessionEntity(): ArmSessionEntity {
        return ArmSessionEntity(
            armedAt = System.currentTimeMillis(),
            disarmedAt = null,
            windowStartTs = System.currentTimeMillis(),
            windowEndTs = System.currentTimeMillis() + 3600_000L,
            source = "APP_BUTTON"
        )
    }

    private fun createScreenEventEntity(sessionId: Long): ScreenEventEntity {
        return ScreenEventEntity(
            sessionId = sessionId,
            type = "SCREEN_OFF",
            ts = System.currentTimeMillis()
        )
    }

    private fun createAlarmRecordEntity(sessionId: Long): AlarmRecordEntity {
        return AlarmRecordEntity(
            sessionId = sessionId,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 123L,
            requestCode = 123,
            source = "SLEEP_AUTOMATION",
            status = "SCHEDULED",
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }
}
