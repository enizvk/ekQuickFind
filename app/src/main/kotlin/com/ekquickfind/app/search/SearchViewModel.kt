package com.ekquickfind.app.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchEngine
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.SearchState
import com.ekquickfind.data.clips.ClipRepository
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.sites.KeywordHint
import com.ekquickfind.providers.sites.SitesConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Async trace section from a keystroke to the first results for it; read by the benchmarks. */
private const val KEYSTROKE_TRACE = "ek.keystrokeToResults"

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        engine: SearchEngine,
        zeroQuery: ZeroQuerySource,
        private val prefs: PreferencesRepository,
        private val history: HistoryRepository,
        private val clipHistory: ClipRepository,
        private val resolvers: Map<ProviderId, @JvmSuppressWildcards ResultResolver>,
    ) : ViewModel() {
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        // Toggling exact match from the bar re-runs the current query.
        private val exactMatch = prefs.preferences.map { it.exactMatch }.distinctUntilChanged()

        val state: StateFlow<SearchState> =
            engine
                .results(
                    merge(
                        combine(_query, exactMatch) { text, exact -> Query(text, exact) },
                        refreshes.map { Query(_query.value, preferences.value.exactMatch) },
                    ),
                ).stateIn(viewModelScope, SharingStarted.Eagerly, SearchState.empty(Query("")))

        val zeroQueryState: StateFlow<ZeroQueryState> = zeroQuery.state.stateIn(viewModelScope, SharingStarted.Eagerly, ZeroQueryState())

        val preferences: StateFlow<UserPreferences> = prefs.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

        val pinnedIds: StateFlow<Set<ResultId>> =
            history.pins
                .map { pins ->
                    pins.map { ResultId(it.resultId) }.toSet()
                }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

        private var traceCookie = 0
        private var pendingTrace: Pair<String, Int>? = null

        fun onQueryChange(text: String) {
            // Benchmarks measure keystroke-to-results through this async trace section.
            pendingTrace?.let { (_, cookie) -> Trace.endAsyncSection(KEYSTROKE_TRACE, cookie) }
            pendingTrace = text to ++traceCookie
            Trace.beginAsyncSection(KEYSTROKE_TRACE, traceCookie)
            _query.value = text
        }

        init {
            // Hiding or unhiding (from long-press or settings) should update the results on screen.
            viewModelScope.launch {
                prefs.preferences
                    .map { it.hiddenResults }
                    .distinctUntilChanged()
                    .drop(1)
                    .collect { refreshes.tryEmit(Unit) }
            }
            viewModelScope.launch {
                state.collect { s ->
                    val pending = pendingTrace ?: return@collect
                    if (s.query.raw == pending.first && (s.top != null || s.sections.isNotEmpty() || s.query.isBlank)) {
                        Trace.endAsyncSection(KEYSTROKE_TRACE, pending.second)
                        pendingTrace = null
                    }
                }
            }
        }

        /** Null until preferences are read, so the welcome never flashes for people who finished it. */
        val onboardingNeeded: StateFlow<Boolean?> =
            prefs.preferences.map { !it.onboardingDone }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

        fun finishOnboarding() = viewModelScope.launch { prefs.update { it.copy(onboardingDone = true) } }

        /** A site keyword typed at the start of the bar, so the bar can show it's a shortcut. */
        val keywordHint: StateFlow<KeywordHint?> =
            combine(_query, prefs.preferences) { text, p -> SitesConfig.from(p).hint(text) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        /** Empties the recent list; launches still count for ranking. */
        fun clearRecents() = viewModelScope.launch { prefs.update { it.copy(recentsClearedAt = System.currentTimeMillis()) } }

        fun setExactMatch(on: Boolean) = viewModelScope.launch { prefs.update { it.copy(exactMatch = on) } }

        fun reset() {
            _query.value = ""
        }

        /** Runs an action and records what it means for history. The outcome tells the activity whether to close. */
        suspend fun perform(
            result: SearchResult?,
            action: Action,
            host: ActionHost,
        ): ActionOutcome {
            val outcome = action.perform(host)
            if (outcome == ActionOutcome.Close) {
                result?.let { history.recordLaunch(it) }
            }
            if (outcome == ActionOutcome.Refresh) refreshes.tryEmit(Unit)
            return outcome
        }

        suspend fun hide(id: ResultId) {
            prefs.hide(id.value, titleOf(id) ?: id.value)
        }

        /** Rebuilds a result from a launcher shortcut. */
        suspend fun resolve(
            providerId: String,
            resultId: String,
        ): SearchResult? = runCatching { resolvers[ProviderId(providerId)]?.resolve(ResultId(resultId)) }.getOrNull()

        suspend fun pin(result: SearchResult) = history.pin(result)

        suspend fun unpin(id: ResultId) = history.unpin(id)

        fun forgetRecent(id: ResultId) = viewModelScope.launch { history.forgetResult(id) }

        fun clearHistory() =
            viewModelScope.launch {
                history.clear()
                clipHistory.clear()
            }

        /** Clips opened with the Clipboard chip, newest first (last 10, for 7 days). */
        val clips: StateFlow<List<String>> =
            clipHistory.clips.map { list -> list.map { it.text } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        fun rememberClip(text: String) = viewModelScope.launch { clipHistory.remember(text) }

        fun forgetClip(text: String) = viewModelScope.launch { clipHistory.forget(text) }

        private fun titleOf(id: ResultId): String? {
            val current = state.value
            val zero = zeroQueryState.value
            return (
                listOfNotNull(current.top?.result) + current.sections.flatMap { s -> s.items.map { it.result } } + zero.pinned +
                    zero.recent
            ).firstOrNull { it.id == id }
                ?.title
        }
    }
