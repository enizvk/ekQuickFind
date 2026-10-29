package com.ekquickfind.core.search.quick

import app.cash.turbine.test
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.FakeProvider
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.RankingSignals
import com.ekquickfind.core.search.SearchEngine
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuickAnswersProviderTest {
    private val provider = QuickAnswersProvider()

    @Test
    fun `calculation becomes a single answer row with the expression as subtitle`() =
        runTest {
            val row = provider.search(Query("12*8+3")).first().single()
            assertEquals("99", row.title)
            assertEquals("12*8+3 =", row.subtitle)
            assertEquals(Section.QuickAnswers, row.section)
            assertFalse(row.isRecordable)
            assertTrue(row.isPromotable)
        }

    @Test
    fun `conversion becomes an answer row`() =
        runTest {
            val row = provider.search(Query("5 km in miles")).first().single()
            assertEquals("3.11 mi", row.title)
            assertEquals("5 km =", row.subtitle)
        }

    @Test
    fun `non-math queries produce nothing`() =
        runTest {
            assertTrue(provider.search(Query("camera")).first().isEmpty())
        }

    @Test
    fun `tapping the answer copies the value`() =
        runTest {
            val host = RecordingHost()
            val outcome =
                provider
                    .search(Query("2+2"))
                    .first()
                    .single()
                    .primaryAction
                    .perform(host)
            assertEquals("4", host.copied)
            assertEquals(ActionOutcome.Stay, outcome)
        }

    @Test
    fun `answer is the top result in the same tick as the query even with slow providers`() =
        runTest {
            val files = FakeProvider("files", Section.Files, delayMillis = 500) { listOf(result("2+2.pdf", 1f, Section.Files)) }
            val engine =
                SearchEngine(
                    providers = listOf(provider, files),
                    enabledProviders = flowOf(setOf(provider.id, files.id)),
                    signals = { RankingSignals.None },
                )
            engine.results(flowOf(Query("2+2"))).test {
                val first = awaitItem()
                assertEquals(0, testScheduler.currentTime)
                assertEquals("4", first.top?.result?.title)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `provider id is stable`() {
        assertEquals(ProviderId("quick-answers"), provider.id)
    }

    private class RecordingHost : ActionHost {
        var copied: String? = null

        override suspend fun copyToClipboard(
            label: String,
            text: String,
        ) {
            copied = text
        }

        override suspend fun pin(result: SearchResult) = Unit

        override suspend fun unpin(result: SearchResult) = Unit

        override suspend fun hide(result: com.ekquickfind.core.search.ResultId) = Unit

        override suspend fun choose(
            title: String,
            options: List<String>,
        ): Int? = null
    }
}
