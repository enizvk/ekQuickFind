package com.ekquickfind.core.search

import kotlinx.coroutines.flow.Flow

interface SearchProvider {
    val id: ProviderId
    val section: Section

    /** Shown in settings; the provider itself decides what to emit when a requirement is missing. */
    val requirements: Set<Requirement> get() = emptySet()

    /** Response budget for this provider; null means the engine default. */
    val budgetMillis: Long? get() = null

    /** When true the engine shows a loading row for this section until the provider first answers. */
    val showsLoadingIndicator: Boolean get() = false

    /**
     * Results for a non-blank query. May emit several times (cheap first page, then more).
     * Debouncing belongs here, not in the engine. Collection is cancelled when the query changes.
     */
    fun search(query: Query): Flow<List<SearchResult>>
}

/** Ids the user has hidden from results; providers that support hiding filter them out. */
fun interface HiddenResults {
    suspend fun ids(): Set<ResultId>
}

/** Providers that can rebuild a result from its id, so pinned and recent rows stay live (actions, icons, state). */
interface ResultResolver {
    /** The current result for [id], or null when it no longer exists or isn't accessible. */
    suspend fun resolve(id: ResultId): SearchResult?
}
