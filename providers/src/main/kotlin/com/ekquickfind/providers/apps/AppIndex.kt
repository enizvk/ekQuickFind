package com.ekquickfind.providers.apps

import com.ekquickfind.core.search.MatchTarget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IndexedApp(
    val entry: AppEntry,
) {
    val label = MatchTarget(entry.label)
    val packageName = MatchTarget(entry.packageName)
}

class IndexedShortcut(
    val entry: ShortcutEntry,
) {
    val label = MatchTarget(entry.label)
}

sealed interface AppIndexState {
    data object Loading : AppIndexState

    class Ready(
        val apps: List<IndexedApp>,
        val shortcuts: List<IndexedShortcut>,
    ) : AppIndexState
}

/**
 * In-memory index of launchable apps, built off the main thread at startup and rebuilt on
 * every package or shortcut change, so searching never touches PackageManager.
 */
class AppIndex(
    private val source: AppsSource,
    scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val cache: AppLabelCache? = null,
) {
    private val _state = MutableStateFlow<AppIndexState>(AppIndexState.Loading)
    val state: StateFlow<AppIndexState> = _state.asStateFlow()

    init {
        scope.launch {
            // Cached labels make the first keystroke after a cold start instant; the live build replaces them.
            cache?.read()?.takeIf { it.isNotEmpty() }?.let { cached ->
                if (_state.value == AppIndexState.Loading) _state.value = AppIndexState.Ready(cached.map(::IndexedApp), emptyList())
            }
            rebuild()
            source.changes().collect { rebuild() }
        }
    }

    private suspend fun rebuild() {
        val (apps, shortcuts) = withContext(dispatcher) { source.loadApps() to source.loadShortcuts() }
        _state.value = AppIndexState.Ready(apps.map(::IndexedApp), shortcuts.map(::IndexedShortcut))
        cache?.write(apps)
    }
}
