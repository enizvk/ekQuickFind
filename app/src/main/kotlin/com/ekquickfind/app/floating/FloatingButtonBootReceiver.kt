package com.ekquickfind.app.floating

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ekquickfind.data.prefs.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Brings the floating button back after a restart or an app update (both stop it). */
@AndroidEntryPoint
class FloatingButtonBootReceiver : BroadcastReceiver() {
    @Inject lateinit var prefs: PreferencesRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (prefs.preferences.first().floatingButton) FloatingButtonService.sync(context.applicationContext, enabled = true)
            } finally {
                pending.finish()
            }
        }
    }
}
