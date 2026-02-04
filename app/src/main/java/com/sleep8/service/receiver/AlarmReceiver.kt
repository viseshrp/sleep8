package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.data.repository.AlarmRepository
import com.sleep8.domain.model.AlarmStatus
import com.sleep8.service.AlarmRingingService
import com.sleep8.ui.ringing.AlarmRingingActivity
import com.sleep8.util.Constants
import com.sleep8.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmRepository: AlarmRepository

    @androidx.annotation.VisibleForTesting
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.launch {
            handleAlarm(context, intent)
            pendingResult?.finish()
        }
    }

    @androidx.annotation.VisibleForTesting
    internal suspend fun handleAlarm(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_ALARM_RING) return

        val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        val alarmInstanceId = intent.getLongExtra(Constants.EXTRA_ALARM_INSTANCE_ID, -1L)

        val record = if (alarmId > 0) {
            alarmRepository.getRecord(alarmId)
        } else {
            null
        }
        if (record != null && (record.status != AlarmStatus.SCHEDULED || record.alarmInstanceId != alarmInstanceId)) {
            return
        }

        val notificationsAllowed = !(PermissionUtils.needsPostNotifications(context) && !PermissionUtils.canPostNotifications(context))
        if (notificationsAllowed) {
            AlarmRingingService.start(context, alarmId)
            AlarmRingingActivity.launch(context, alarmId)
        } else {
            AlarmRingingActivity.launch(context, alarmId, ringInActivity = true)
        }

        if (alarmId > 0) {
            alarmRepository.markFired(alarmId, System.currentTimeMillis())
            alarmRepository.markActivityPresented(alarmId)
        }
    }
}
