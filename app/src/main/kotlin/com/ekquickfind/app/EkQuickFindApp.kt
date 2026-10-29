package com.ekquickfind.app

import android.app.Application
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.ekquickfind.app.di.AppScope
import com.ekquickfind.app.photos.PhotoTagWorker
import com.ekquickfind.app.search.ZeroQuerySource
import com.ekquickfind.app.shortcuts.ShortcutSync
import com.ekquickfind.app.shortcuts.ShortcutsPublisher
import com.ekquickfind.app.update.UpdateWorker
import com.ekquickfind.app.update.Updates
import com.ekquickfind.app.widget.SearchWidget
import com.ekquickfind.app.widget.SearchWidgetReceiver
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.providers.apps.AppIndex
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EkQuickFindApp : Application() {
    // Injecting the index here starts building it at process start, before the first keystroke.
    @Inject lateinit var appIndex: AppIndex

    @Inject lateinit var history: HistoryRepository

    @Inject @AppScope
    lateinit var scope: CoroutineScope

    @Inject lateinit var prefs: PreferencesRepository

    @Inject lateinit var zeroQuery: ZeroQuerySource

    @Inject lateinit var updates: Updates

    override fun onCreate() {
        super.onCreate()
        scope.launch { history.prune() }
        if (updates.isConfigured) UpdateWorker.schedule(this)
        // Picks up photos taken since the last run.
        scope.launch { if (prefs.preferences.first().photoTags) PhotoTagWorker.start(this@EkQuickFindApp) }
        scope.launch {
            if (prefs.preferences.first().floatingButton) {
                com.ekquickfind.app.floating.FloatingButtonService
                    .sync(this@EkQuickFindApp, enabled = true)
            }
        }
        // Keep home-screen widgets in step with the app's theme settings, and publish a generated preview.
        scope.launch {
            prefs.preferences
                .map { it.theme to it.dynamicColor }
                .distinctUntilChanged()
                .collect { SearchWidget().updateAll(this@EkQuickFindApp) }
        }
        // Pinned results double as launcher shortcuts (long-press the ekQuickFind icon).
        ShortcutSync(zeroQuery, ShortcutsPublisher(this)).start(scope)
        scope.launch { runCatching { GlanceAppWidgetManager(this@EkQuickFindApp).setWidgetPreviews(SearchWidgetReceiver::class) } }
    }
}
