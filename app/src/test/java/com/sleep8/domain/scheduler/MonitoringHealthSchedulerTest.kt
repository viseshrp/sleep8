package com.sleep8.domain.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MonitoringHealthSchedulerTest {

    @Test
    fun `schedule sets exact health-check alarm`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        val scheduler = MonitoringHealthScheduler(context, alarmManager)
        val triggerAt = System.currentTimeMillis() + 60_000L

        scheduler.schedule(triggerAt)

        verify(exactly = 1) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, any())
        }
    }

    @Test
    fun `cancel removes scheduled health-check alarm`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        val scheduler = MonitoringHealthScheduler(context, alarmManager)

        scheduler.cancel()

        verify(exactly = 1) { alarmManager.cancel(any<android.app.PendingIntent>()) }
    }

    @Test
    fun `schedule swallows security exception from exact alarm api`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        every {
            alarmManager.setExactAndAllowWhileIdle(any<Int>(), any<Long>(), any<android.app.PendingIntent>())
        } throws SecurityException("denied")
        val scheduler = MonitoringHealthScheduler(context, alarmManager)

        scheduler.schedule(System.currentTimeMillis() + 60_000L)
    }
}
