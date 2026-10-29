package com.ekquickfind.providers.web

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.Query
import com.ekquickfind.providers.FakeAndroidHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class WebProviderTest {
    private val server = MockWebServer()
    private val cancelled = AtomicInteger()
    private val http =
        OkHttpClient
            .Builder()
            .eventListener(
                object : EventListener() {
                    override fun canceled(call: Call) {
                        cancelled.incrementAndGet()
                    }
                },
            ).build()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun engine() =
        WebEngine(
            "Test",
            server.url("/search?q=%s").toString().replace("%25s", "%s"),
            server.url("/ac?q=%s").toString().replace("%25s", "%s"),
        )

    private fun provider(
        enabled: Boolean = true,
        engine: WebEngine = engine(),
    ) = WebProvider(http, { WebSettingsSnapshot(enabled, engine) }, Dispatchers.IO, debounceMillis = 10)

    private fun suggestions(vararg s: String) = MockResponse.Builder().body("[\"q\",[${s.joinToString { "\"$it\"" }}]]").build()

    @Test
    fun returnsAtMostFiveSuggestions() =
        runTest {
            server.enqueue(suggestions("a1", "a2", "a3", "a4", "a5", "a6", "a7"))
            val rows = provider().search(Query("a")).first()
            assertEquals(listOf("a1", "a2", "a3", "a4", "a5"), rows.map { it.title })
            assertEquals("/ac?q=a", server.takeRequest().target)
        }

    @Test
    fun collapsesSuggestionsThatOnlyDifferInCaseOrSpacing() =
        runTest {
            server.enqueue(suggestions("Sound Cloud", "sound cloud", "sound  cloud ", "soundtrap"))
            val rows = provider().search(Query("sound")).first()
            assertEquals(listOf("Sound Cloud", "soundtrap"), rows.map { it.title })
        }

    @Test
    fun suggestionsAskForCompletionEmphasis() =
        runTest {
            server.enqueue(suggestions("soundcloud"))
            assertEquals(
                com.ekquickfind.core.search.Emphasis.Completion,
                provider()
                    .search(Query("sound"))
                    .first()
                    .single()
                    .emphasis,
            )
        }

    @Test
    fun makesNoRequestWhenSuggestionsAreOff() =
        runTest {
            val rows = provider(enabled = false).search(Query("weather")).toList()
            assertTrue(rows.all { it.isEmpty() })
            assertEquals(0, server.requestCount)
        }

    @Test
    fun customEngineWithoutSuggestionsMakesNoRequest() =
        runTest {
            val noSuggest = WebEngine("Custom", "https://example.com/?q=%s", null)
            assertTrue(provider(engine = noSuggest).search(Query("cats")).first().isEmpty())
            assertEquals(0, server.requestCount)
        }

    @Test
    fun offlineHidesTheSectionWithoutError() =
        runTest {
            server.close()
            assertTrue(provider().search(Query("weather")).first().isEmpty())
        }

    @Test
    fun cancellingTheSearchCancelsTheInFlightRequest() =
        runBlocking {
            server.enqueue(
                MockResponse
                    .Builder()
                    .body("[\"q\",[\"slow\"]]")
                    .headersDelay(5, TimeUnit.SECONDS)
                    .build(),
            )
            val job = launch(Dispatchers.IO) { provider().search(Query("slow")).first() }
            withTimeout(5_000) { while (server.requestCount == 0) delay(20) }
            job.cancel()
            withTimeout(3_000) { while (cancelled.get() == 0) delay(20) }
            assertEquals(1, cancelled.get())
        }

    @Test
    fun tappingASuggestionSearchesForIt() =
        runTest {
            server.enqueue(suggestions("weather in rome"))
            val row = provider().search(Query("weather")).first().single()
            val host = FakeAndroidHost(ApplicationProvider.getApplicationContext<Context>())
            row.primaryAction.perform(host)
            val intent = host.started.single()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertTrue(intent.dataString!!.endsWith("/search?q=weather+in+rome"))
        }
}
