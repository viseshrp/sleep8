package com.sleep8.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.sleep8.R
import com.sleep8.data.preferences.AppPreferences
import com.sleep8.domain.manager.StateMachineManager
import com.sleep8.domain.model.AppState
import com.sleep8.domain.state.StateHolder
import com.sleep8.service.notification.NotificationHelper
import com.sleep8.util.Constants
import com.sleep8.util.TimeUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
class NightMonitorService : Service() {

    private lateinit var stateMachineManager: StateMachineManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var stateHolder: StateHolder
    private lateinit var appPreferences: AppPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    serviceScope.launch {
                        stateMachineManager.onScreenOff(Instant.now())
                        updateNotificationForState(AppState.ARMED_PENDING_CONFIRM)
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    serviceScope.launch {
                        stateMachineManager.onScreenOn()
                        updateNotificationForState(AppState.ARMED_IDLE)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
        stateMachineManager = entryPoint.stateMachineManager()
        notificationHelper = entryPoint.notificationHelper()
        stateHolder = entryPoint.stateHolder()
        appPreferences = entryPoint.appPreferences()
        registerScreenReceiver()
        updateNotificationForState(stateHolder.state.value)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotificationForState(stateHolder.state.value)
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterScreenReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun handleArm() {
        updateNotificationForState(AppState.ARMED_IDLE)
    }

    fun handleDisarm() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun unregisterScreenReceiver() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already unregistered
        }
    }

    private fun updateNotificationForState(state: AppState) {
        val text = when (state) {
            AppState.ARMED_PENDING_CONFIRM -> getString(R.string.notification_text_pending)
            AppState.ARMED_ALARM_SET -> {
                val lastOff = stateHolder.lastScreenOffTs.value
                if (lastOff > 0) {
                    val alarmTime = TimeUtils.toLocalTime(lastOff + appPreferences.alarmOffsetHours * 3600_000L)
                    getString(R.string.notification_text_alarm_set, TimeUtils.formatAlarmTime(alarmTime))
                } else {
                    getString(R.string.notification_text_idle)
                }
            }
            else -> getString(R.string.notification_text_idle)
        }
        val notification = notificationHelper.buildNotification(text)
        startForeground(Constants.NOTIFICATION_ID, notification)
        NotificationManagerCompat.from(this).notify(Constants.NOTIFICATION_ID, notification)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceEntryPoint {
    fun stateMachineManager(): StateMachineManager
    fun notificationHelper(): NotificationHelper
    fun stateHolder(): StateHolder
    fun appPreferences(): AppPreferences
}
