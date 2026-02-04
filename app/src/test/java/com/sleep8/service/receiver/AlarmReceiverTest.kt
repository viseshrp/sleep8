package com.sleep8.service.receiver

import android.Manifest
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmRecord
import com.sleep8.domain.model.AlarmSource
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.service.AlarmRingingService
import com.sleep8.ui.ringing.AlarmRingingActivity
import com.sleep8.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AlarmReceiverTest {

    @Test
    @Config(sdk = [31])
    fun `non scheduled alarm does not launch activity or service`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(context)
        while (shadowApp.nextStartedService != null) {}
        while (shadowApp.nextStartedActivity != null) {}
        val repo = mockk<AlarmRepository>()
        val record = baseRecord(status = AlarmStatus.CANCELED)
        coEvery { repo.getRecord(any()) } returns record

        val receiver = AlarmReceiver().apply {
            this.alarmRepository = repo
            this.dispatcher = Dispatchers.Default
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, record.id)
            putExtra(Constants.EXTRA_ALARM_INSTANCE_ID, record.alarmInstanceId)
        }

        runBlocking { receiver.handleAlarm(context, intent) }
        org.junit.Assert.assertNull(shadowApp.nextStartedService)
        org.junit.Assert.assertNull(shadowApp.nextStartedActivity)
    }

    @Test
    @Config(sdk = [33])
    fun `scheduled alarm launches service and activity when notifications allowed`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(context)
        while (shadowApp.nextStartedService != null) {}
        while (shadowApp.nextStartedActivity != null) {}
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val repo = mockk<AlarmRepository>(relaxed = true)
        val record = baseRecord(status = AlarmStatus.SCHEDULED)
        coEvery { repo.getRecord(any()) } returns record

        val receiver = AlarmReceiver().apply {
            this.alarmRepository = repo
            this.dispatcher = Dispatchers.Default
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, record.id)
            putExtra(Constants.EXTRA_ALARM_INSTANCE_ID, record.alarmInstanceId)
        }

        runBlocking { receiver.handleAlarm(context, intent) }
        val startedService = shadowApp.nextStartedService
        val startedActivity = shadowApp.nextStartedActivity

        org.junit.Assert.assertEquals(AlarmRingingService::class.java.name, startedService?.component?.className)
        org.junit.Assert.assertEquals(AlarmRingingActivity::class.java.name, startedActivity?.component?.className)
        coVerify(timeout = 1000) { repo.markFired(record.id, any()) }
        coVerify(timeout = 1000) { repo.markActivityPresented(record.id) }
    }

    @Test
    @Config(sdk = [33])
    fun `scheduled alarm launches activity only when notifications denied`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(context)
        while (shadowApp.nextStartedService != null) {}
        while (shadowApp.nextStartedActivity != null) {}
        val repo = mockk<AlarmRepository>(relaxed = true)
        val record = baseRecord(status = AlarmStatus.SCHEDULED)
        coEvery { repo.getRecord(any()) } returns record

        val receiver = AlarmReceiver().apply {
            this.alarmRepository = repo
            this.dispatcher = Dispatchers.Default
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_RING
            putExtra(Constants.EXTRA_ALARM_ID, record.id)
            putExtra(Constants.EXTRA_ALARM_INSTANCE_ID, record.alarmInstanceId)
        }

        runBlocking { receiver.handleAlarm(context, intent) }
        org.junit.Assert.assertNull(shadowApp.nextStartedService)
        val startedActivity = shadowApp.nextStartedActivity
        org.junit.Assert.assertEquals(AlarmRingingActivity::class.java.name, startedActivity?.component?.className)
        org.junit.Assert.assertTrue(startedActivity?.getBooleanExtra(Constants.EXTRA_RING_IN_ACTIVITY, false) == true)
    }

    private fun baseRecord(status: AlarmStatus): AlarmRecord {
        return AlarmRecord(
            id = 11L,
            sessionId = 1L,
            screenOffTs = 1000L,
            confirmedAt = 2000L,
            scheduledAt = 2500L,
            triggerAt = 3000L,
            durationUsedMinutes = 480,
            alarmInstanceId = 222L,
            requestCode = 222,
            source = AlarmSource.SLEEP_AUTOMATION,
            status = status,
            canceledReason = null,
            firedAt = null,
            dismissedAt = null,
            overlayUsed = false,
            activityPresented = false
        )
    }
}
