package com.ekquickfind.app.search

import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.providers.apps.AppsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

data class ZeroQueryState(
    val pinned: List<SearchResult> = emptyList(),
    val recent: List<SearchResult> = emptyList(),
    val suggested: List<SearchResult> = emptyList(),
) {
    val isEmpty: Boolean get() = pinned.isEmpty() && recent.isEmpty() && suggested.isEmpty()

    /** False when the strip already shows the recents and nothing else is left, so no empty panel is drawn. */
    fun showsPanel(recentsInStrip: Boolean): Boolean = isEmpty || pinned.isNotEmpty() || suggested.isNotEmpty() || !recentsInStrip
}

/** What the surface shows before anything is typed. Rows are rebuilt through providers so they stay live. */
@OptIn(ExperimentalCoroutinesApi::class)
class ZeroQuerySource(
    history: HistoryRepository,
    private val resolvers: Map<ProviderId, ResultResolver>,
    private val apps: AppsProvider,
    private val suggestedPackages: (Int) -> List<String>,
) {
    val state: Flow<ZeroQueryState> =
        combine(history.pins, history.recentResults, ::Pair)
            .mapLatest { (pins, recent) ->
                val pinned = pins.mapNotNull { resolve(it.providerId, it.resultId) }
                val pinnedIds = pinned.map { it.id }.toSet()
                val recentRows = recent.mapNotNull { resolve(it.providerId, it.resultId) }.filter { it.id !in pinnedIds }
                val shown = pinnedIds + recentRows.map { it.id }
                val suggested = apps.forPackages(suggestedPackages(SUGGESTED)).filter { it.id !in shown }.take(SUGGESTED)
                ZeroQueryState(pinned, recentRows.take(RECENT), suggested)
            }

    private suspend fun resolve(
        provider: String,
        id: String,
    ) = runCatching { resolvers[ProviderId(provider)]?.resolve(ResultId(id)) }.getOrNull()

    private companion object {
        const val RECENT = 6
        const val SUGGESTED = 5
    }
}
