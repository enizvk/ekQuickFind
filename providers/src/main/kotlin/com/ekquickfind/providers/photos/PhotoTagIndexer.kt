package com.ekquickfind.providers.photos

import android.net.Uri
import com.ekquickfind.data.photos.PhotoTagEntry
import com.ekquickfind.data.photos.PhotoTagStore
import com.ekquickfind.data.photos.TagSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class PhotoItem(
    val mediaId: Long,
    val uri: Uri,
    val modifiedSeconds: Long,
)

/** The photos on the phone; blocking, call off the main thread. */
interface PhotoLibrary {
    /** Newest first, so recent photos become searchable first. */
    fun photos(): List<PhotoItem>

    fun xmp(item: PhotoItem): String?

    fun count(): Int = photos().size
}

data class Label(
    val text: String,
    val confidence: Float,
)

/** Says what's in a picture ("Dog", "Beach"), on the phone. */
fun interface PhotoLabeler {
    suspend fun labels(uri: Uri): List<Label>
}

/**
 * Brings the tag index up to date: tags new and edited photos, forgets deleted ones.
 * Safe to stop at any point; the next run carries on where this one left off.
 */
class PhotoTagIndexer(
    private val library: PhotoLibrary,
    private val labeler: PhotoLabeler,
    private val store: PhotoTagStore,
    private val io: CoroutineDispatcher,
) {
    /** Returns how many photos were (re)tagged. */
    suspend fun run(onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }): Int {
        val photos = withContext(io) { library.photos() }
        val seen = store.scanned()
        store.forget(seen.keys - photos.map { it.mediaId }.toSet())
        val todo = photos.filter { seen[it.mediaId] != it.modifiedSeconds }
        todo.forEachIndexed { i, photo ->
            coroutineContext.ensureActive()
            // A photo that can't be decoded still gets its keywords, and isn't retried every run.
            val detected =
                try {
                    labeler.labels(photo.uri)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            val keywords = withContext(io) { runCatching { XmpKeywords.parse(library.xmp(photo)) }.getOrDefault(emptyList()) }
            store.replace(
                photo.mediaId,
                photo.modifiedSeconds,
                detected.map { PhotoTagEntry(it.text, TagSource.Detected, it.confidence) } +
                    keywords.map { PhotoTagEntry(it, TagSource.Keyword, 1f) },
            )
            onProgress(i + 1, todo.size)
        }
        return todo.size
    }
}
