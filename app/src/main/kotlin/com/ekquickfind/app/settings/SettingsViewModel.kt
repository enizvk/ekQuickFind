package com.ekquickfind.app.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekquickfind.app.photos.PhotoTagWorker
import com.ekquickfind.app.ui.settings.PhotoTagProgress
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.data.photos.PhotoTagStore
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.apps.AppIndex
import com.ekquickfind.providers.apps.AppIndexState
import com.ekquickfind.providers.photos.PhotoLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val prefs: PreferencesRepository,
        private val history: HistoryRepository,
        index: AppIndex,
        private val photoTags: PhotoTagStore,
        private val photos: PhotoLibrary,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val photoCount = MutableStateFlow<Int?>(null)

        /** How far photo tagging has got; null until the photo count is known. */
        val photoTagProgress: StateFlow<PhotoTagProgress?> =
            combine(photoTags.taggedCount, photoCount) { done, total -> total?.let { PhotoTagProgress(minOf(done, it), it) } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        fun refreshPhotoCount() =
            viewModelScope.launch(Dispatchers.IO) {
                val total = runCatching { photos.count() }.getOrNull()
                photoCount.value = total
                // Coming back to settings with tagging unfinished (for example after a restart) picks it up again.
                val done = photoTags.taggedCount.first()
                if (prefs.preferences.first().photoTags && total != null && done < total) PhotoTagWorker.start(context)
            }

        /** Off also deletes the tags, so nothing is left behind. */
        fun setPhotoTags(on: Boolean) =
            viewModelScope.launch {
                prefs.update { it.copy(photoTags = on) }
                if (on) {
                    PhotoTagWorker.start(context, restart = true)
                } else {
                    PhotoTagWorker.stop(context)
                    photoTags.clear()
                }
            }

        val preferences: StateFlow<UserPreferences> = prefs.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

        val appFilter = MutableStateFlow("")

        /** Excluded apps page; excluded ones first so they're easy to find and restore. */
        val excludableApps: StateFlow<List<ExcludableApp>> =
            combine(index.state.filterIsInstance<AppIndexState.Ready>(), prefs.preferences, appFilter) { ready, p, filter ->
                excludableApps(ready.apps.map { it.entry }, p.hiddenResults.keys, filter, excludedFirst = filter.isBlank())
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        fun setExcluded(
            app: ExcludableApp,
            excluded: Boolean,
        ) = viewModelScope.launch { if (excluded) prefs.hide(app.id, app.label) else prefs.unhide(app.id) }

        fun update(transform: (UserPreferences) -> UserPreferences) = viewModelScope.launch { prefs.update(transform) }

        fun setProviderEnabled(
            id: String,
            enabled: Boolean,
        ) = viewModelScope.launch { prefs.setProviderEnabled(id, enabled) }

        fun move(
            section: SectionKey,
            up: Boolean,
        ) = viewModelScope.launch { prefs.moveSection(section, up) }

        fun resetOrder() = viewModelScope.launch { prefs.resetSectionOrder() }

        fun setLayout(
            section: SectionKey,
            layout: SectionLayout,
        ) = viewModelScope.launch { prefs.setLayout(section, layout) }

        fun unhide(id: String) = viewModelScope.launch { prefs.unhide(id) }

        fun clearHistory() = viewModelScope.launch { history.clear() }

        /** Photo tags are off by default, so a reset also stops tagging and clears the index, like turning them off. */
        fun resetSettings() =
            viewModelScope.launch {
                val hadPhotoTags = prefs.preferences.first().photoTags
                prefs.update { it.resetToDefaults() }
                if (hadPhotoTags) {
                    PhotoTagWorker.stop(context)
                    photoTags.clear()
                }
            }
    }
