package com.ekquickfind.core.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

data class EngineConfig(
    val sectionCap: Int = 4,
    val sectionCaps: Map<Section, Int> = emptyMap(),
    val defaultBudgetMillis: Long = 700,
    /** User-chosen section order; sections not listed keep their default order after the listed ones. */
    val sectionOrder: List<Section> = Section.entries,
) {
    fun capFor(section: Section) = sectionCaps[section] ?: sectionCap

    val orderedSections: List<Section> get() = (sectionOrder + Section.entries).distinct()
}

/** Builds the always-available "Search the web" row for a query. */
fun interface FallbackFactory {
    fun create(query: Query): SearchResult?
}

/** Snapshot of history-based ranking inputs, read once per query. */
fun interface SignalsSource {
    suspend fun snapshot(): RankingSignals
}

data class SectionResults(
    val section: Section,
    val items: List<RankedResult>,
    val cap: Int,
    val isLoading: Boolean = false,
) {
    val visible: List<RankedResult> get() = items.take(cap)
    val hasMore: Boolean get() = items.size > cap
}

data class SearchState(
    val query: Query,
    val top: RankedResult?,
    val sections: List<SectionResults>,
    /** True once no provider is pending (done or past its budget). */
    val isSettled: Boolean,
) {
    companion object {
        fun empty(query: Query) = SearchState(query, top = null, sections = emptyList(), isSettled = true)
    }
}

/**
 * Fans a query out to every enabled provider and merges their answers progressively.
 * A new query (or toggle change) cancels all in-flight provider work, so stale results can't leak.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchEngine(
    private val providers: List<SearchProvider>,
    private val enabledProviders: Flow<Set<ProviderId>>,
    private val signals: SignalsSource,
    private val fallback: FallbackFactory? = null,
    private val ranker: Ranker = Ranker(),
    private val configs: Flow<EngineConfig> = flowOf(EngineConfig()),
) {
    /** Convenience for a fixed configuration. */
    constructor(
        providers: List<SearchProvider>,
        enabledProviders: Flow<Set<ProviderId>>,
        signals: SignalsSource,
        fallback: FallbackFactory? = null,
        ranker: Ranker = Ranker(),
        config: EngineConfig,
    ) : this(providers, enabledProviders, signals, fallback, ranker, flowOf(config))

    fun results(queries: Flow<Query>): Flow<SearchState> =
        // Queries aren't de-duplicated so callers can re-emit the same query to refresh (for example after hiding a result).
        combine(
            queries,
            enabledProviders.distinctUntilChanged(),
            configs.distinctUntilChanged(),
        ) { q, enabled, config -> Triple(q, enabled, config) }
            .flatMapLatest { (query, enabled, config) -> search(query, providers.filter { it.id in enabled }, config) }

    private fun search(
        query: Query,
        active: List<SearchProvider>,
        config: EngineConfig,
    ): Flow<SearchState> {
        if (query.isBlank) return flowOf(SearchState.empty(query))
        return flow {
            val snapshot = signals.snapshot()
            val initial = Progress(active.associate { it.id to ProviderProgress() })
            val states =
                updates(query, active, config)
                    .scan(initial) { progress, update -> progress.apply(update) }
                    // Skip the all-pending seed so the previous query's results stay on screen until new ones arrive.
                    .drop(1)
                    .map { build(query, active, it, snapshot, config) }
                    .distinctUntilChanged()
            emitAll(states)
        }
    }

    private fun updates(
        query: Query,
        active: List<SearchProvider>,
        config: EngineConfig,
    ): Flow<Update> =
        channelFlow {
            active.forEach { provider -> launch { run(provider, query, config) } }
        }

    private suspend fun ProducerScope<Update>.run(
        provider: SearchProvider,
        query: Query,
        config: EngineConfig,
    ) {
        val budget = provider.budgetMillis ?: config.defaultBudgetMillis
        val timer =
            launch {
                delay(budget)
                send(Update.BudgetExpired(provider.id))
            }
        try {
            provider.search(query).collect { items ->
                timer.cancel()
                send(Update.Items(provider.id, items))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A broken provider must never break search; it just contributes nothing.
        }
        timer.cancel()
        send(Update.Done(provider.id))
    }

    private fun build(
        query: Query,
        active: List<SearchProvider>,
        progress: Progress,
        snapshot: RankingSignals,
        config: EngineConfig,
    ): SearchState {
        val ranked = active.flatMap { progress.of(it.id).items }.let { ranker.rank(it, snapshot) }
        // A turned-off provider takes its fallback row with it (no "Search the web" when web search is off).
        val fallbackRow =
            fallback
                ?.create(query)
                ?.takeIf { row -> providers.none { it.id == row.providerId } || active.any { it.id == row.providerId } }
                ?.let { RankedResult(it, ranker.score(it, snapshot)) }
        val top =
            ranked.firstOrNull { !it.result.isFallback && it.result.isPromotable }
                ?: fallbackRow.takeIf { progress.isSettled }

        val loadingSections =
            active
                .filter { it.showsLoadingIndicator && progress.of(it.id).isPending }
                .map { it.section }
                .toSet()

        val sections =
            config.orderedSections.mapNotNull { section ->
                val items =
                    ranked.filter { it.result.section == section && it !== top } +
                        listOfNotNull(fallbackRow.takeIf { section == Section.Web && it !== top })
                val loading = section in loadingSections && items.isEmpty()
                if (items.isEmpty() && !loading) null else SectionResults(section, items, config.capFor(section), loading)
            }
        return SearchState(query, top, sections, progress.isSettled)
    }

    private sealed interface Update {
        val id: ProviderId

        data class Items(
            override val id: ProviderId,
            val items: List<SearchResult>,
        ) : Update

        data class BudgetExpired(
            override val id: ProviderId,
        ) : Update

        data class Done(
            override val id: ProviderId,
        ) : Update
    }

    private data class ProviderProgress(
        val items: List<SearchResult> = emptyList(),
        val isPending: Boolean = true,
    )

    private data class Progress(
        val byProvider: Map<ProviderId, ProviderProgress>,
    ) {
        val isSettled: Boolean get() = byProvider.values.none { it.isPending }

        fun of(id: ProviderId) = byProvider.getValue(id)

        fun apply(update: Update): Progress {
            val current = of(update.id)
            val next =
                when (update) {
                    is Update.Items -> current.copy(items = update.items, isPending = false)
                    is Update.BudgetExpired, is Update.Done -> current.copy(isPending = false)
                }
            return Progress(byProvider + (update.id to next))
        }
    }
}
