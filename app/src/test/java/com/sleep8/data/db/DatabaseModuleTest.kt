package com.sleep8.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sleep8.data.db.dao.AlarmRecordDao
import com.sleep8.data.db.dao.ArmSessionDao
import com.sleep8.data.db.dao.MonitoringStartEventDao
import com.sleep8.data.db.dao.ScreenEventDao
import com.sleep8.data.db.dao.SettingsDao
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DatabaseModuleTest {

    @Test
    fun `provideDatabase creates working room database`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val db = DatabaseModule.provideDatabase(context)

        assertNotNull(db.settingsDao())
        assertNotNull(db.armSessionDao())
        assertNotNull(db.screenEventDao())
        assertNotNull(db.alarmRecordDao())
        assertNotNull(db.monitoringStartEventDao())
        db.close()
    }

    @Test
    fun `dao providers delegate to database`() {
        val db = mockk<Sleep8Database>()
        val settingsDao = mockk<SettingsDao>()
        val armSessionDao = mockk<ArmSessionDao>()
        val screenEventDao = mockk<ScreenEventDao>()
        val alarmRecordDao = mockk<AlarmRecordDao>()
        val monitoringStartEventDao = mockk<MonitoringStartEventDao>()

        every { db.settingsDao() } returns settingsDao
        every { db.armSessionDao() } returns armSessionDao
        every { db.screenEventDao() } returns screenEventDao
        every { db.alarmRecordDao() } returns alarmRecordDao
        every { db.monitoringStartEventDao() } returns monitoringStartEventDao

        assertSame(settingsDao, DatabaseModule.provideSettingsDao(db))
        assertSame(armSessionDao, DatabaseModule.provideArmSessionDao(db))
        assertSame(screenEventDao, DatabaseModule.provideScreenEventDao(db))
        assertSame(alarmRecordDao, DatabaseModule.provideAlarmRecordDao(db))
        assertSame(monitoringStartEventDao, DatabaseModule.provideMonitoringStartEventDao(db))
    }
}
