package com.ekquickfind.providers.apps

import com.ekquickfind.core.search.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class AppIndexCacheTest {
    private class MemoryCache(
        var stored: List<AppEntry> = emptyList(),
    ) : AppLabelCache {
        override suspend fun read() = stored

        override suspend fun write(apps: List<AppEntry>) {
            stored = apps
        }
    }

    @Test
    fun servesCachedLabelsWhileTheLiveIndexIsStillLoading() =
        runTest {
            val cache = MemoryCache(listOf(app("Maps"), app("Gmail")))
            val source = FakeAppsSource(apps = listOf(app("Maps"), app("Gmail"), app("Signal")))
            // The live build runs on a dispatcher that never runs anything, so it never finishes in this test.
            val stalled =
                object : CoroutineDispatcher() {
                    override fun dispatch(
                        context: CoroutineContext,
                        block: Runnable,
                    ) = Unit
                }
            val index = AppIndex(source, backgroundScope, stalled, cache)
            runCurrent()

            val provider = AppsProvider(index) { emptySet() }
            assertEquals(listOf("Maps"), provider.search(Query("maps")).first().map { it.title })
            assertEquals(0, source.loads)
        }

    @Test
    fun writesTheLiveIndexBackToTheCache() =
        runTest {
            val cache = MemoryCache()
            val source = FakeAppsSource(apps = listOf(app("Signal")))
            AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler), cache)
            runCurrent()
            assertEquals(listOf("Signal"), cache.stored.map { it.label })
        }

    @Test
    fun liveIndexReplacesCachedEntries() =
        runTest {
            val cache = MemoryCache(listOf(app("Old App")))
            val source = FakeAppsSource(apps = listOf(app("New App")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler), cache)
            runCurrent()
            assertEquals(listOf("New App"), (index.state.value as AppIndexState.Ready).apps.map { it.entry.label })
        }
}
