package com.ekquickfind.core.search

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchEngineTest {
    private fun engine(
        vararg providers: SearchProvider,
        enabled: Set<String> = providers.map { it.id.value }.toSet(),
        signals: RankingSignals = RankingSignals.None,
        fallback: FallbackFactory? = null,
        config: EngineConfig = EngineConfig(),
    ) = SearchEngine(
        providers = providers.toList(),
        enabledProviders = flowOf(enabled.map(::ProviderId).toSet()),
        signals = { signals },
        fallback = fallback,
        config = config,
    )

    private fun titlesIn(
        state: SearchState,
        section: Section,
    ) = state.sections
        .firstOrNull { it.section == section }
        ?.items
        ?.map { it.result.title }
        .orEmpty()

    private fun allTitles(state: SearchState) =
        listOfNotNull(state.top?.result?.title) + state.sections.flatMap { s -> s.items.map { it.result.title } }

    @Test
    fun `fast providers show before slow ones finish`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps", 0.9f), result("Mail", 0.6f)) }
            val files = FakeProvider("files", Section.Files, delayMillis = 300) { listOf(result("map.pdf", 0.9f, Section.Files)) }

            engine(apps, files).results(flowOf(Query("ma"))).test {
                val first = awaitItem()
                assertEquals(listOf(Section.Apps), first.sections.map { it.section })
                assertFalse(first.isSettled)

                val second = awaitItem()
                assertEquals(listOf(Section.Apps, Section.Files), second.sections.map { it.section })
                assertTrue(second.isSettled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `results for a stale query never appear once the newer query is running`() =
        runTest {
            val files =
                FakeProvider("files", Section.Files, delayFor = { if (it.raw == "ma") 500 else 50 }) { q ->
                    listOf(result("file for ${q.raw}", 0.9f, Section.Files))
                }
            val queries = MutableSharedFlow<Query>()
            engine(files).results(queries).test {
                queries.emit(Query("ma"))
                advanceTimeBy(100)
                queries.emit(Query("map"))
                advanceTimeBy(1_000)

                val states = cancelAndConsumeRemainingEvents().mapNotNull { (it as? app.cash.turbine.Event.Item)?.value }
                val titles = states.flatMap(::allTitles)
                assertTrue("file for ma" !in titles, "stale result leaked: $titles")
                assertEquals(
                    "file for map",
                    states
                        .last()
                        .top
                        ?.result
                        ?.title,
                )
                assertEquals(listOf("ma", "map"), files.calls)
            }
        }

    @Test
    fun `a timed out provider stops blocking and its late results are appended`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps", 0.9f), result("Mail", 0.6f)) }
            val slow = FakeProvider("files", Section.Files, delayMillis = 1_000) { listOf(result("late.pdf", 0.5f, Section.Files)) }

            engine(apps, slow).results(flowOf(Query("ma"))).test {
                val first = awaitItem()
                assertFalse(first.isSettled)

                val afterBudget = awaitItem()
                assertTrue(afterBudget.isSettled)
                assertEquals(700, testScheduler.currentTime)
                assertEquals(listOf(Section.Apps), afterBudget.sections.map { it.section })

                val late = awaitItem()
                assertEquals(listOf("late.pdf"), titlesIn(late, Section.Files))
                assertEquals(1_000, testScheduler.currentTime)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a provider can set its own budget`() =
        runTest {
            val slow = FakeProvider("web", Section.Web, delayMillis = 2_000, budgetMillis = 1_500) { emptyList() }
            engine(slow).results(flowOf(Query("x"))).test {
                // Nothing is emitted until something happens, so the first state is the budget expiring.
                assertTrue(awaitItem().isSettled)
                assertEquals(1_500, testScheduler.currentTime)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `disabled providers are never invoked`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps")) }
            val files = FakeProvider("files", Section.Files) { listOf(result("pdf", section = Section.Files)) }

            val states = engine(apps, files, enabled = setOf("apps")).results(flowOf(Query("p"))).toList()

            assertTrue(files.calls.isEmpty())
            assertTrue(states.none { s -> s.sections.any { it.section == Section.Files } })
        }

    @Test
    fun `toggling a provider re-runs the current query`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps")) }
            val files = FakeProvider("files", Section.Files) { listOf(result("map.pdf", section = Section.Files)) }
            val enabled = MutableSharedFlow<Set<ProviderId>>(replay = 1)
            val engine = SearchEngine(listOf(apps, files), enabled, signals = { RankingSignals.None })

            engine.results(flowOf(Query("map"))).test {
                enabled.emit(setOf(ProviderId("apps"), ProviderId("files")))
                assertTrue("map.pdf" in allTitles(expectMostRecentSettled()))
                enabled.emit(setOf(ProviderId("apps")))
                assertTrue("map.pdf" !in allTitles(expectMostRecentSettled()))
                assertEquals(1, files.calls.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sections follow the configured order and react to changes`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Display app", 0.9f), result("Displayer", 0.5f)) }
            val settings =
                FakeProvider("settings", Section.Settings) {
                    listOf(result("Display", 0.95f, Section.Settings), result("Display size", 0.6f, Section.Settings))
                }
            val configs = MutableSharedFlow<EngineConfig>(replay = 1)
            val engine =
                SearchEngine(
                    listOf(apps, settings),
                    flowOf(setOf(apps.id, settings.id)),
                    signals = { RankingSignals.None },
                    configs = configs,
                )

            engine.results(flowOf(Query("display"))).test {
                configs.emit(EngineConfig())
                val default = expectMostRecentSettled()
                assertEquals(listOf(Section.Apps, Section.Settings), default.sections.map { it.section })
                configs.emit(EngineConfig(sectionOrder = listOf(Section.Settings, Section.Apps)))
                val reordered = expectMostRecentSettled()
                assertEquals(listOf(Section.Settings, Section.Apps), reordered.sections.map { it.section })
                // Order doesn't change which result is best overall.
                assertEquals(default.top?.result?.title, reordered.top?.result?.title)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sections missing from a partial order keep their default place at the end`() {
        val config = EngineConfig(sectionOrder = listOf(Section.Web, Section.Files))
        assertEquals(
            listOf(Section.Web, Section.Files, Section.QuickAnswers, Section.Apps, Section.Contacts, Section.Settings, Section.AskAi),
            config.orderedSections,
        )
    }

    @Test
    fun `a failing provider is treated as empty`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps")) }
            val broken =
                object : SearchProvider {
                    override val id = ProviderId("broken")
                    override val section = Section.Files

                    override fun search(query: Query): Flow<List<SearchResult>> = flow { error("boom") }
                }
            val last = engine(apps, broken).results(flowOf(Query("m"))).toList().last()
            assertTrue(last.isSettled)
            assertEquals("Maps", last.top?.result?.title)
        }

    @Test
    fun `blank query emits the empty state without calling providers`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps")) }
            val states = engine(apps).results(flowOf(Query("   "))).toList()
            assertEquals(listOf(SearchState.empty(Query("   "))), states)
            assertTrue(apps.calls.isEmpty())
        }

    @Test
    fun `history signals reorder results`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Settings", 0.86f), result("Spotify", 0.85f)) }
            val signals = RankingSignals(launches = mapOf(ResultId("apps:Spotify") to LaunchStats(5, 0)), now = 0)
            val last = engine(apps, signals = signals).results(flowOf(Query("s"))).toList().last()
            assertEquals("Spotify", last.top?.result?.title)
        }

    @Test
    fun `providers that show loading get a loading section while pending`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps, delayMillis = 200, showsLoadingIndicator = true) { listOf(result("Maps")) }
            val quick = FakeProvider("quick", Section.QuickAnswers) { emptyList() }
            engine(apps, quick).results(flowOf(Query("m"))).test {
                val loading = awaitItem().sections.single()
                assertEquals(Section.Apps, loading.section)
                assertTrue(loading.isLoading)
                assertFalse(awaitItem().sections.any { it.isLoading })
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- 2.6 section caps, empty sections and best match ---

    @Test
    fun `sections cap visible rows and report how many are hidden`() =
        runTest {
            val files = FakeProvider("files", Section.Files) { (1..12).map { result("f$it", 0.5f - it / 100f, Section.Files) } }
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Top", 0.99f)) }
            val last = engine(files, apps, config = EngineConfig(sectionCap = 4)).results(flowOf(Query("f"))).toList().last()

            val section = last.sections.single { it.section == Section.Files }
            assertEquals(4, section.visible.size)
            assertTrue(section.hasMore)
            assertEquals(12, section.items.size)
            assertEquals(listOf("f1", "f2", "f3", "f4"), section.visible.map { it.result.title })
        }

    @Test
    fun `per-section caps override the default`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { (1..10).map { result("a$it", 0.5f) } }
            val config = EngineConfig(sectionCap = 4, sectionCaps = mapOf(Section.Apps to 6))
            val last = engine(apps, config = config).results(flowOf(Query("a"))).toList().last()
            assertEquals(
                6,
                last.sections
                    .single()
                    .visible.size,
            )
        }

    @Test
    fun `empty sections are hidden`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Maps")) }
            val contacts = FakeProvider("contacts", Section.Contacts) { emptyList() }
            val last = engine(apps, contacts).results(flowOf(Query("m"))).toList().last()
            assertTrue(last.sections.none { it.section == Section.Contacts })
        }

    @Test
    fun `best match is promoted and removed from its section`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Camera", 0.9f), result("Calendar", 0.5f)) }
            val last = engine(apps).results(flowOf(Query("cam"))).toList().last()
            assertEquals("Camera", last.top?.result?.title)
            assertEquals(listOf("Calendar"), titlesIn(last, Section.Apps))
        }

    @Test
    fun `web fallback becomes the top result when nothing matches`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { emptyList() }
            val fallback = FallbackFactory { q -> result("Search the web for ${q.raw}", 0f, Section.Web, isFallback = true) }
            val last = engine(apps, fallback = fallback).results(flowOf(Query("cats"))).toList().last()
            assertEquals("Search the web for cats", last.top?.result?.title)
            assertTrue(last.sections.isEmpty())
        }

    @Test
    fun `web fallback stays at the end of the web section when something else matches`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { listOf(result("Cats app", 0.9f)) }
            val web = FakeProvider("web", Section.Web) { listOf(result("cats meme", 0.95f, Section.Web)) }
            val fallback = FallbackFactory { q -> result("Search the web for ${q.raw}", 0f, Section.Web, isFallback = true) }
            val last = engine(apps, web, fallback = fallback).results(flowOf(Query("cats"))).toList().last()
            assertEquals("Cats app", last.top?.result?.title)
            assertEquals(listOf("cats meme", "Search the web for cats"), titlesIn(last, Section.Web))
        }

    @Test
    fun `turning off the web provider also drops the web fallback`() =
        runTest {
            val apps = FakeProvider("apps", Section.Apps) { emptyList() }
            val web = FakeProvider("web", Section.Web) { listOf(result("cats meme", 0.95f, Section.Web)) }
            val fallback = FallbackFactory { q -> result("Search the web for ${q.raw}", 0f, Section.Web, isFallback = true) }
            val last = engine(apps, web, enabled = setOf("apps"), fallback = fallback).results(flowOf(Query("cats"))).toList().last()
            assertNull(last.top)
            assertTrue(last.sections.isEmpty())
        }

    @Test
    fun `rows that are not recordable are never the top result`() =
        runTest {
            val contacts =
                FakeProvider(
                    "contacts",
                    Section.Contacts,
                ) { listOf(result("Enable contacts search", 1f, Section.Contacts, isRecordable = false)) }
            val last = engine(contacts).results(flowOf(Query("a"))).toList().last()
            assertNull(last.top)
            assertEquals(listOf("Enable contacts search"), titlesIn(last, Section.Contacts))
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<SearchState>.expectMostRecentSettled(): SearchState {
        var state = awaitItem()
        while (!state.isSettled) state = awaitItem()
        return state
    }
}
