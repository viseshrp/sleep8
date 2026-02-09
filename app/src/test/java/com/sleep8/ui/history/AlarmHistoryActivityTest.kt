package com.sleep8.ui.history

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.provider.AlarmClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.db.Sleep8Database
import com.sleep8.data.db.entity.AlarmRecordEntity
import com.sleep8.data.db.entity.ArmSessionEntity
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AlarmHistoryActivityTest {

    @Test
    fun `ACTION_SHOW_ALARMS launches history activity`() {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val activity = Robolectric.buildActivity(AlarmHistoryActivity::class.java, intent).setup().get()
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `activity refreshes cleanly on resume lifecycle`() {
        val sessionId = runWithDatabase { db ->
            db.alarmRecordDao().deleteAll()
            val sessionId = ensureSessionId(db)
            db.alarmRecordDao().insert(alarmRecord(sessionId = sessionId, scheduledAt = 1_000L))
            sessionId
        }

        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val controller = Robolectric.buildActivity(AlarmHistoryActivity::class.java, intent).setup()
        val activity = controller.get()
        val viewModel = activity.viewModelForTest()
        val repository = viewModel.repositoryForTest()

        waitForCondition { viewModel.uiState.value.alarms.size == 1 }
        assertEquals(1, viewModel.uiState.value.alarms.size)

        controller.pause()
        runBlocking {
            repository.insertRecord(
                domainAlarmRecord(
                    sessionId = sessionId,
                    scheduledAt = 2_000L
                )
            )
        }

        controller.resume()

        waitForCondition { viewModel.uiState.value.alarms.size == 2 }
        assertEquals(2_000L, viewModel.uiState.value.alarms.first().scheduledAt)

        runWithDatabase { it.alarmRecordDao().deleteAll() }
    }

    private fun domainAlarmRecord(sessionId: Long, scheduledAt: Long): AlarmRecord {
        return AlarmRecord(
            id = 0L,
            sessionId = sessionId,
            screenOffTs = 900L,
            confirmedAt = 950L,
            scheduledAt = scheduledAt,
            triggerAt = scheduledAt + 600L,
            durationUsedMinutes = 480,
            alarmInstanceId = 100L + scheduledAt,
            requestCode = (100 + scheduledAt).toInt(),
            source = AlarmSource.SLEEP_AUTOMATION,
            status = AlarmStatus.SCHEDULED,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }

    private fun alarmRecord(sessionId: Long, scheduledAt: Long): AlarmRecordEntity {
        return AlarmRecordEntity(
            sessionId = sessionId,
            screenOffTs = 900L,
            confirmedAt = 950L,
            scheduledAt = scheduledAt,
            triggerAt = scheduledAt + 600L,
            durationUsedMinutes = 480,
            alarmInstanceId = 100L + scheduledAt,
            requestCode = (100 + scheduledAt).toInt(),
            source = AlarmSource.SLEEP_AUTOMATION.name,
            status = "SCHEDULED",
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }

    private fun AlarmHistoryActivity.viewModelForTest(): AlarmHistoryViewModel {
        val field = AlarmHistoryActivity::class.java.getDeclaredField("viewModel\$delegate")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val delegate = field.get(this) as Lazy<AlarmHistoryViewModel>
        return delegate.value
    }

    private fun AlarmHistoryViewModel.repositoryForTest(): AlarmRepository {
        val field = AlarmHistoryViewModel::class.java.getDeclaredField("alarmRepository")
        field.isAccessible = true
        return field.get(this) as AlarmRepository
    }

    private fun waitForCondition(timeoutMs: Long = 2_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        assertTrue("Condition not met within $timeoutMs ms", condition())
    }

    private fun <T> runWithDatabase(block: suspend (Sleep8Database) -> T): T {
        return runBlocking {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val db = Room.databaseBuilder(app, Sleep8Database::class.java, "sleep8.db")
                .fallbackToDestructiveMigration()
                .build()
            try {
                block(db)
            } finally {
                db.close()
            }
        }
    }

    private suspend fun ensureSessionId(db: Sleep8Database): Long {
        return db.armSessionDao().getActiveSession()?.sessionId
            ?: db.armSessionDao().insert(
                ArmSessionEntity(
                    armedAt = 500L,
                    disarmedAt = null,
                    windowStartTs = 500L,
                    windowEndTs = 3_500L,
                    source = "USER_MANUAL"
                )
            )
    }
}
