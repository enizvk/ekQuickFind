package com.ekquickfind.app.tile

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ekquickfind.app.search.SearchActivity
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.userPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile that opens search over whatever is on screen. */
class SearchTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { PreferencesRepository(applicationContext.userPreferencesStore) }

    override fun onStartListening() {
        // Listening only happens while the tile is in Quick Settings, so this also repairs a missed onTileAdded.
        scope.launch { prefs.update { if (it.tileAdded) it else it.copy(tileAdded = true) } }
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Search"
            subtitle = "ekQuickFind"
            updateTile()
        }
    }

    override fun onClick() {
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        // On the lock screen, ask to unlock first; search opens only after a successful unlock.
        if (isLocked) unlockAndRun { startActivityAndCollapse(pending) } else startActivityAndCollapse(pending)
    }

    override fun onTileAdded() {
        scope.launch { prefs.update { it.copy(tileAdded = true) } }
    }

    override fun onTileRemoved() {
        scope.launch { prefs.update { it.copy(tileAdded = false) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
