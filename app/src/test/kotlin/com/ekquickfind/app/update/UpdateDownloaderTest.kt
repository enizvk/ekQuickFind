package com.ekquickfind.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class UpdateDownloaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private val bytes = "pretend this is an apk".toByteArray()
    private val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun release(sha256: String = sha) = Release(2, "0.2.0", server.url("/a.apk").toString(), sha256, bytes.size.toLong(), null)

    private fun downloader() = UpdateDownloader(OkHttpClient(), folder.root, Dispatchers.Unconfined)

    @Test
    fun aMatchingChecksumKeepsTheDownload() =
        runTest {
            server.enqueue(MockResponse.Builder().body(okio.Buffer().write(bytes)).build())
            val file = downloader().download(release())
            assertArrayEquals(bytes, file.readBytes())
        }

    @Test
    fun aWrongChecksumDeletesTheDownloadAndFails() =
        runTest {
            server.enqueue(MockResponse.Builder().body(okio.Buffer().write(bytes)).build())
            val result = runCatching { downloader().download(release(sha256 = "0".repeat(64))) }
            assertTrue(result.exceptionOrNull() is UpdateException)
            assertFalse(folder.root.walk().any { it.isFile })
        }

    @Test
    fun aServerErrorFails() =
        runTest {
            server.enqueue(MockResponse.Builder().code(404).build())
            assertTrue(runCatching { downloader().download(release()) }.exceptionOrNull() is UpdateException)
        }
}
