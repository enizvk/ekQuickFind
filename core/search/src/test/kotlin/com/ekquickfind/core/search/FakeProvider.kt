package com.ekquickfind.core.search

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeProvider(
    name: String,
    override val section: Section,
    private val delayMillis: Long = 0,
    override val budgetMillis: Long? = null,
    override val showsLoadingIndicator: Boolean = false,
    private val delayFor: (Query) -> Long = { delayMillis },
    private val results: (Query) -> List<SearchResult>,
) : SearchProvider {
    override val id = ProviderId(name)
    val calls = mutableListOf<String>()

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            calls += query.raw
            val wait = delayFor(query)
            if (wait > 0) delay(wait)
            emit(results(query))
        }
}
