package com.sleep8.ui.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sleep8.domain.manager.ArmManager
import com.sleep8.domain.model.ArmSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Sleep8TileService : TileService() {

    @Inject lateinit var armManager: ArmManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            if (armManager.isArmed()) {
                armManager.disarm()
            } else {
                armManager.arm(ArmSource.QUICK_TILE)
            }
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (armManager.isArmed()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
