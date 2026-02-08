package com.sleep8.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleep8.domain.manager.MonitoringReliabilityManager
import com.sleep8.domain.model.MonitoringTriggerSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringHealthCheckReceiver : BroadcastReceiver() {

    @Inject lateinit var monitoringReliabilityManager: MonitoringReliabilityManager

    @androidx.annotation.VisibleForTesting
    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        scope.launch {
            monitoringReliabilityManager.onTrigger(context, MonitoringTriggerSource.PERIODIC_HEALTH_CHECK)
            pending?.finish()
        }
    }
}
