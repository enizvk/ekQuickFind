package com.ekquickfind.app.search

import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.data.prefs.MAX_RECENTS
import com.ekquickfind.providers.apps.AppsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

data class ZeroQueryState(
    val pinned: List<SearchResult> = emptyList(),
    val recent: List<SearchResult> = emptyList(),
    val suggested: List<SearchResult> = emptyList(),
    /** The calendar event running now or coming up in the next hours. */
    val nextUp: SearchResult? = null,
) {
    val isEmpty: Boolean get() = nextUp == null && pinned.isEmpty() && recent.isEmpty() && suggested.isEmpty()

    /** False when the strip already shows the recents and nothing else is left, so no empty panel is drawn. */
    fun showsPanel(recentsInStrip: Boolean): Boolean =
        isEmpty || nextUp != null || pinned.isNotEmpty() || suggested.isNotEmpty() || !recentsInStrip

    fun limitRecents(count: Int): ZeroQueryState = copy(recent = recent.take(count))
}

/** Smart picks first, then usage-based apps, without repeats or anything already shown. */
internal fun suggestedRows(
    smart: List<SearchResult>,
    usage: List<SearchResult>,
    shown: Set<ResultId>,
    limit: Int,
): List<SearchResult> = (smart + usage).distinctBy { it.id }.filter { it.id !in shown }.take(limit)

/** What the surface shows before anything is typed. Rows are rebuilt through providers so they stay live. */
@OptIn(ExperimentalCoroutinesApi::class)
class ZeroQuerySource(
    history: HistoryRepository,
    private val resolvers: Map<ProviderId, ResultResolver>,
    private val apps: AppsProvider,
    private val suggestedPackages: (Int) -> List<String>,
    /** What usually gets opened right now (time of day, connected headphones or car); empty when turned off. */
    private val smartPicks: suspend (Int) -> List<Suggestion> = { emptyList() },
    private val nextUp: suspend () -> SearchResult? = { null },
) {
    val state: Flow<ZeroQueryState> =
        combine(history.pins, history.recentResults, ::Pair)
            .mapLatest { (pins, recent) ->
                val pinned = pins.mapNotNull { resolve(it.providerId, it.resultId) }
                val pinnedIds = pinned.map { it.id }.toSet()
                val recentRows = recent.mapNotNull { resolve(it.providerId, it.resultId) }.filter { it.id !in pinnedIds }
                val shown = pinnedIds + recentRows.map { it.id }
                val smart = smartPicks(SUGGESTED).mapNotNull { resolve(it.providerId, it.resultId) }
                val suggested = suggestedRows(smart, apps.forPackages(suggestedPackages(SUGGESTED)), shown, SUGGESTED)
                // The most the setting allows; the surface trims to the chosen count.
                ZeroQueryState(pinned, recentRows.take(MAX_RECENTS), suggested, runCatching { nextUp() }.getOrNull())
            }

    private suspend fun resolve(
        provider: String,
        id: String,
    ) = runCatching { resolvers[ProviderId(provider)]?.resolve(ResultId(id)) }.getOrNull()

    private companion object {
        const val SUGGESTED = 5
    }
}
