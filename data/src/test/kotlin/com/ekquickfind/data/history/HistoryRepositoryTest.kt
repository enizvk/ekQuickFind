package com.ekquickfind.data.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchEngine
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.EkDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private var now = 1_000L * 86_400_000L
    private val history = HistoryRepository(db.history()) { now }

    @After
    fun tearDown() = db.close()

    private object Noop : Action {
        override val label = "Open"
        override val glyph: Glyph? = null

        override suspend fun perform(host: ActionHost) = ActionOutcome.Close
    }

    private fun app(
        title: String,
        score: Float,
        recordable: Boolean = true,
    ) = SearchResult(
        ResultId("apps:$title"),
        ProviderId("apps"),
        Section.Apps,
        title,
        null,
        ResultIcon.Vector(Glyph.Search),
        score,
        Noop,
        isRecordable = recordable,
    )

    @Test
    fun openingSpotifyFiveTimesMovesItToTheTop() =
        runTest {
            val settings = app("Settings", 0.86f)
            val spotify = app("Spotify", 0.85f)
            val apps =
                object : SearchProvider {
                    override val id = ProviderId("apps")
                    override val section = Section.Apps

                    override fun search(query: Query) = flowOf(listOf(settings, spotify))
                }
            val engine = SearchEngine(listOf(apps), flowOf(setOf(apps.id)), history)

            assertEquals(
                "Settings",
                engine
                    .results(flowOf(Query("s")))
                    .last()
                    .top
                    ?.result
                    ?.title,
            )
            repeat(5) { history.recordLaunch(spotify) }
            assertEquals(
                "Spotify",
                engine
                    .results(flowOf(Query("s")))
                    .last()
                    .top
                    ?.result
                    ?.title,
            )
        }

    @Test
    fun nonRecordableResultsAreNotStored() =
        runTest {
            history.recordLaunch(app("Enable contacts search", 0.1f, recordable = false))
            assertTrue(history.snapshot().launches.isEmpty())
        }

    @Test
    fun snapshotIncludesPins() =
        runTest {
            history.pin(app("Camera", 0.5f))
            assertEquals(setOf(ResultId("apps:Camera")), history.snapshot().pinned)
        }

    @Test
    fun clearRemovesLaunchesButKeepsPins() =
        runTest {
            val camera = app("Camera", 0.5f)
            history.recordLaunch(camera)
            history.pin(camera)

            history.clear()

            val snapshot = history.snapshot()
            assertTrue(snapshot.launches.isEmpty())
            assertTrue(history.recentResults.first().isEmpty())
            assertEquals(setOf(ResultId("apps:Camera")), snapshot.pinned)
        }

    @Test
    fun forgettingOneRecentResultLeavesOthers() =
        runTest {
            history.recordLaunch(app("A", 0.5f))
            history.recordLaunch(app("B", 0.5f))
            history.forgetResult(ResultId("apps:A"))
            assertEquals(setOf(ResultId("apps:B")), history.snapshot().launches.keys)
        }

    @Test
    fun pruneDropsEventsOlderThanNinetyDays() =
        runTest {
            history.recordLaunch(app("Old", 0.5f))
            now += 91L * 86_400_000L
            history.recordLaunch(app("New", 0.5f))
            history.prune()
            assertEquals(setOf(ResultId("apps:New")), history.snapshot().launches.keys)
        }
}
