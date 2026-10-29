package com.ekquickfind.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateCheckerTest {
    private val server = MockWebServer()
    private val http = OkHttpClient()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun checker(installed: Long = 1) =
        UpdateChecker(http, server.url("/releases/update.json").toString(), installed, Dispatchers.Unconfined)

    private fun feed(
        version: Long,
        apk: String = "ekQuickFind-$version.apk",
    ) = MockResponse
        .Builder()
        .body("""{"versionCode":$version,"versionName":"0.$version.0","apk":"$apk","sha256":"abc","size":10,"notes":"Fixes"}""")
        .build()

    @Test
    fun aNewerVersionIsOfferedWithItsApkNextToTheFeed() =
        runTest {
            server.enqueue(feed(2))
            val result = checker().check() as UpdateCheck.Available
            assertEquals(2L, result.release.versionCode)
            assertEquals("0.2.0", result.release.versionName)
            assertEquals(server.url("/releases/ekQuickFind-2.apk").toString(), result.release.apkUrl)
            assertEquals("Fixes", result.release.notes)
        }

    @Test
    fun theSameOrAnOlderVersionIsUpToDate() =
        runTest {
            server.enqueue(feed(3))
            assertEquals(UpdateCheck.UpToDate, checker(installed = 3).check())
            server.enqueue(feed(2))
            assertEquals(UpdateCheck.UpToDate, checker(installed = 3).check())
        }

    @Test
    fun anAbsoluteHttpsApkLinkIsKept() =
        runTest {
            server.enqueue(feed(2, apk = "https://downloads.example.com/ekQuickFind-2.apk"))
            val result = checker().check() as UpdateCheck.Available
            assertEquals("https://downloads.example.com/ekQuickFind-2.apk", result.release.apkUrl)
        }

    @Test
    fun aPlainHttpApkLinkIsRefusedUnlessItIsOnThisDevice() =
        runTest {
            server.enqueue(feed(2, apk = "http://downloads.example.com/ekQuickFind-2.apk"))
            assertTrue(checker().check() is UpdateCheck.Failed)
        }

    @Test
    fun aServerErrorIsReportedNotThrown() =
        runTest {
            server.enqueue(MockResponse.Builder().code(500).build())
            assertTrue(checker().check() is UpdateCheck.Failed)
        }

    @Test
    fun aBrokenFeedIsReportedNotThrown() =
        runTest {
            server.enqueue(MockResponse.Builder().body("<html>not json</html>").build())
            assertTrue(checker().check() is UpdateCheck.Failed)
        }

    @Test
    fun withoutAFeedUrlNothingIsFetched() =
        runTest {
            assertEquals(UpdateCheck.NotConfigured, UpdateChecker(http, "", 1, Dispatchers.Unconfined).check())
            assertEquals(0, server.requestCount)
        }
}
