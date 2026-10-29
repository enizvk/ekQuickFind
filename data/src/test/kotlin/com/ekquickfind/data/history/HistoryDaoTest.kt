package com.ekquickfind.data.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.ekquickfind.data.EkDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = db.history()
    private val day = 86_400_000L

    @After
    fun tearDown() = db.close()

    @Test
    fun aggregatesLaunchesPerResult() =
        runTest {
            dao.insertLaunch(LaunchEvent(resultId = "apps:spotify", providerId = "apps", at = 1))
            dao.insertLaunch(LaunchEvent(resultId = "apps:spotify", providerId = "apps", at = 5))
            dao.insertLaunch(LaunchEvent(resultId = "apps:maps", providerId = "apps", at = 3))
            val stats = dao.launchStats().associateBy { it.resultId }
            assertEquals(2, stats.getValue("apps:spotify").count)
            assertEquals(5L, stats.getValue("apps:spotify").lastAt)
            assertEquals(1, stats.getValue("apps:maps").count)
        }

    @Test
    fun queriesLaunchesByResultId() =
        runTest {
            dao.insertLaunch(LaunchEvent(resultId = "a", providerId = "apps", at = 1))
            dao.insertLaunch(LaunchEvent(resultId = "b", providerId = "apps", at = 2))
            assertEquals(1, dao.launchesOf("a").size)
        }

    @Test
    fun recentResultsAreDistinctAndNewestFirst() =
        runTest {
            dao.insertLaunch(LaunchEvent(resultId = "a", providerId = "apps", at = 1))
            dao.insertLaunch(LaunchEvent(resultId = "b", providerId = "contacts", at = 2))
            dao.insertLaunch(LaunchEvent(resultId = "a", providerId = "apps", at = 3))
            assertEquals(listOf("a", "b"), dao.recentResults(10).first().map { it.resultId })
        }

    @Test
    fun recentResultsUpdateWhenALaunchIsRecorded() =
        runTest {
            dao.recentResults(10).test {
                assertTrue(awaitItem().isEmpty())
                dao.insertLaunch(LaunchEvent(resultId = "a", providerId = "apps", at = 1))
                assertEquals(listOf("a"), awaitItem().map { it.resultId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun prunesEventsOlderThanTheCutoff() =
        runTest {
            dao.insertLaunch(LaunchEvent(resultId = "old", providerId = "apps", at = 10 * day))
            dao.insertLaunch(LaunchEvent(resultId = "new", providerId = "apps", at = 100 * day))
            dao.pruneLaunchesBefore(50 * day)
            assertEquals(listOf("new"), dao.launchStats().map { it.resultId })
        }

    @Test
    fun pinsKeepTheirOrder() =
        runTest {
            dao.pin(Pin("b", "apps", position = 1))
            dao.pin(Pin("a", "apps", position = 0))
            dao.pins().test {
                assertEquals(listOf("a", "b"), awaitItem().map { it.resultId })
                dao.unpin("a")
                assertEquals(listOf("b"), awaitItem().map { it.resultId })
                cancelAndIgnoreRemainingEvents()
            }
        }
}
