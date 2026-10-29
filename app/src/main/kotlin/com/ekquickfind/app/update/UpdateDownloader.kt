package com.ekquickfind.app.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Downloads a release APK into [dir] and keeps it only if its SHA-256 matches the feed. */
class UpdateDownloader(
    private val http: OkHttpClient,
    private val dir: File,
    private val io: CoroutineDispatcher,
) {
    suspend fun download(
        release: Release,
        onProgress: (Float) -> Unit = {},
    ): File =
        withContext(io) {
            dir.mkdirs()
            // Only one update is ever kept around.
            dir.listFiles()?.forEach { it.delete() }
            val part = File(dir, "ekQuickFind-${release.versionCode}.apk.part")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                http.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { response ->
                    if (!response.isSuccessful) throw UpdateException("Download failed: ${response.code}")
                    val total = response.body.contentLength().takeIf { it > 0 } ?: release.size ?: -1
                    response.body.byteStream().use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                read += n
                                if (total > 0) onProgress(read.toFloat() / total)
                            }
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(release.sha256, ignoreCase = true)) throw UpdateException("Downloaded file doesn't match its checksum")
                File(dir, "ekQuickFind-${release.versionCode}.apk").also { part.renameTo(it) }
            } catch (e: Exception) {
                part.delete()
                if (e is CancellationException || e is UpdateException) throw e
                throw UpdateException("Download failed: ${e.message}", e)
            }
        }
}
