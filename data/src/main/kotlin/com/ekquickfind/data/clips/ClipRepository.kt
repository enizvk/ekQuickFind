package com.ekquickfind.data.clips

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Clip(
    val text: String,
    val at: Long,
)

/** The rules for remembered clips: newest first, no repeats, the last [MAX] within [KEEP_MS]. */
object ClipHistory {
    const val MAX = 10
    const val KEEP_MS = 7L * 24 * 60 * 60 * 1000

    fun add(
        clips: List<Clip>,
        text: String,
        now: Long,
    ): List<Clip> = fresh(listOf(Clip(text, now)) + clips.filter { it.text != text }, now).take(MAX)

    fun fresh(
        clips: List<Clip>,
        now: Long,
    ): List<Clip> = clips.filter { now - it.at < KEEP_MS }
}

/**
 * Clips the user opened with the Clipboard chip. Kept in a small file in the app's no-backup folder,
 * so they never go into a cloud backup, even with history backup on.
 */
class ClipRepository(
    private val file: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val state = MutableStateFlow<List<Clip>?>(null)
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    val clips: Flow<List<Clip>> =
        state
            .onStart { load() }
            .filterNotNull()
            .map { ClipHistory.fresh(it, clock()) }

    suspend fun remember(text: String) {
        val clip = text.trim()
        if (clip.isEmpty()) return
        change { ClipHistory.add(it, clip, clock()) }
    }

    suspend fun forget(text: String) = change { list -> list.filter { it.text != text } }

    suspend fun clear() = change { emptyList() }

    private suspend fun load(): List<Clip> = lock.withLock { state.value ?: read().also { state.value = it } }

    // Reads and writes under one lock, so two quick changes can't both start from the same old list.
    private suspend fun change(transform: (List<Clip>) -> List<Clip>) =
        lock.withLock {
            val next = transform(state.value ?: read())
            withContext(io) {
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(ListSerializer(Clip.serializer()), next))
            }
            state.value = next
        }

    private suspend fun read(): List<Clip> =
        withContext(io) {
            runCatching { json.decodeFromString(ListSerializer(Clip.serializer()), file.readText()) }.getOrDefault(emptyList())
        }
}
