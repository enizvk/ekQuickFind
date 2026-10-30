package com.ekquickfind.app.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
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
        val intent = Intent(this, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // On the lock screen, ask to unlock first; search opens only after a successful unlock.
        if (isLocked) unlockAndRun { open(intent) } else open(intent)
    }

    private fun open(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            // Android 13 only has the Intent version; the "throws when targeting 34" rule starts on Android 14.
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
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
