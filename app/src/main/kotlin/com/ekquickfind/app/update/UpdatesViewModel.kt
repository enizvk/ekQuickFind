package com.ekquickfind.app.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekquickfind.data.prefs.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel
    @Inject
    constructor(
        private val updates: Updates,
        private val prefs: PreferencesRepository,
    ) : ViewModel() {
        val state: StateFlow<UpdateState> = updates.state
        val autoUpdate: StateFlow<Boolean> = prefs.preferences.map { it.autoUpdate }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

        fun canInstall(): Boolean = updates.canInstall()

        fun check() = viewModelScope.launch { updates.check() }

        fun install(release: Release) = viewModelScope.launch { updates.install(release) }

        fun setAutoUpdate(on: Boolean) = viewModelScope.launch { prefs.update { it.copy(autoUpdate = on) } }
    }
