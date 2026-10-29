package com.ekquickfind.app.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/** What a release publishes next to its APK as `update.json`. */
@Serializable
internal data class UpdateFeed(
    val versionCode: Long,
    val versionName: String,
    /** Relative to the feed's own URL, or absolute. */
    val apk: String,
    val sha256: String,
    val size: Long? = null,
    val notes: String? = null,
)

data class Release(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val size: Long?,
    val notes: String?,
)

sealed interface UpdateCheck {
    /** No feed URL was built in, so updates are off. */
    data object NotConfigured : UpdateCheck

    data object UpToDate : UpdateCheck

    data class Available(
        val release: Release,
    ) : UpdateCheck

    data class Failed(
        val reason: String,
    ) : UpdateCheck
}

class UpdateException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Reads the update feed and says whether a newer version than [installedVersion] is out. */
class UpdateChecker(
    private val http: OkHttpClient,
    private val feedUrl: String,
    private val installedVersion: Long,
    private val io: CoroutineDispatcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = feedUrl.isNotBlank() && feedUrl.toHttpUrlOrNull() != null

    suspend fun check(): UpdateCheck {
        if (!isConfigured) return UpdateCheck.NotConfigured
        val feed = feedUrl.toHttpUrlOrNull()!!
        return try {
            val parsed = json.decodeFromString<UpdateFeed>(fetch(feed))
            if (parsed.versionCode <= installedVersion) return UpdateCheck.UpToDate
            UpdateCheck.Available(
                Release(parsed.versionCode, parsed.versionName, apkUrl(feed, parsed.apk), parsed.sha256, parsed.size, parsed.notes),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateCheck.Failed(e.message ?: e::class.simpleName.orEmpty())
        }
    }

    private suspend fun fetch(url: HttpUrl): String =
        withContext(io) {
            http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw UpdateException("Update server answered ${response.code}")
                response.body.string()
            }
        }

    private fun apkUrl(
        feed: HttpUrl,
        link: String,
    ): String {
        val url = feed.resolve(link) ?: throw UpdateException("Bad APK link: $link")
        // Plain http could be tampered with on the way; only allowed for a server on this device (testing).
        if (!url.isHttps && url.host !in LOCAL_HOSTS) throw UpdateException("APK link must use https")
        return url.toString()
    }

    private companion object {
        val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1")
    }
}
