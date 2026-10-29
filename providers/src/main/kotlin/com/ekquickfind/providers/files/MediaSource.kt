package com.ekquickfind.providers.files

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns

/** Kinds of files, each behind its own permission tier. */
enum class MediaKind(
    val mediaType: Int,
) {
    Image(FileColumns.MEDIA_TYPE_IMAGE),
    Video(FileColumns.MEDIA_TYPE_VIDEO),
    Audio(FileColumns.MEDIA_TYPE_AUDIO),

    /** Documents, downloads and other files; only visible with All files access. */
    Document(FileColumns.MEDIA_TYPE_DOCUMENT),
}

data class MediaRow(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val kind: MediaKind,
    val modifiedSeconds: Long,
    val relativePath: String?,
)

fun interface MediaSource {
    /** Files whose name contains [text], newest first. Blocking; call off the main thread. */
    fun query(
        text: String,
        kinds: Set<MediaKind>,
    ): List<MediaRow>

    /** One file by its content uri (used to rebuild pinned and recent rows). */
    fun byUri(uri: Uri): MediaRow? = null

    /** Photos by MediaStore id, for tag matches. */
    fun byImageIds(ids: Collection<Long>): List<MediaRow> = emptyList()
}

class MediaStoreSource(
    private val resolver: ContentResolver,
) : MediaSource {
    override fun query(
        text: String,
        kinds: Set<MediaKind>,
    ): List<MediaRow> {
        if (kinds.isEmpty() || text.isBlank()) return emptyList()
        val mediaTypes =
            kinds.flatMap {
                if (it ==
                    MediaKind.Document
                ) {
                    listOf(FileColumns.MEDIA_TYPE_DOCUMENT, FileColumns.MEDIA_TYPE_NONE)
                } else {
                    listOf(it.mediaType)
                }
            }
        val selection = "${FileColumns.DISPLAY_NAME} LIKE ? ESCAPE '\\' AND ${FileColumns.MEDIA_TYPE} IN (${mediaTypes.joinToString(
            ",",
        ) { "?" }})"
        val args = arrayOf("%${escapeLike(text.trim())}%") + mediaTypes.map(Int::toString)
        val queryArgs =
            Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${FileColumns.DATE_MODIFIED} DESC")
                putInt(ContentResolver.QUERY_ARG_LIMIT, MAX_ROWS)
            }
        val rows = mutableListOf<MediaRow>()
        resolver.query(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), PROJECTION, queryArgs, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val kind = kindOf(c.getInt(3))
                rows +=
                    MediaRow(
                        uri = uriFor(kind, id),
                        displayName = c.getString(1) ?: continue,
                        mimeType = c.getString(2),
                        kind = kind,
                        modifiedSeconds = c.getLong(4),
                        relativePath = c.getString(5),
                    )
            }
        }
        return rows
    }

    override fun byImageIds(ids: Collection<Long>): List<MediaRow> {
        if (ids.isEmpty()) return emptyList()
        val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val rows = mutableListOf<MediaRow>()
        ids.chunked(500).forEach { chunk ->
            val selection = "${FileColumns._ID} IN (${chunk.joinToString(",") { "?" }})"
            resolver.query(images, IMAGE_PROJECTION, selection, chunk.map(Long::toString).toTypedArray(), null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    rows +=
                        MediaRow(
                            ContentUris.withAppendedId(images, id),
                            name,
                            c.getString(2),
                            MediaKind.Image,
                            c.getLong(3),
                            c.getString(4),
                        )
                }
            }
        }
        return rows
    }

    override fun byUri(uri: Uri): MediaRow? =
        runCatching {
            resolver.query(uri, PROJECTION, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use null
                MediaRow(uri, c.getString(1) ?: return@use null, c.getString(2), kindOf(c.getInt(3)), c.getLong(4), c.getString(5))
            }
        }.getOrNull()

    private fun kindOf(mediaType: Int) = MediaKind.entries.firstOrNull { it.mediaType == mediaType } ?: MediaKind.Document

    // Type-specific URIs let each file open in the right app (gallery, player, viewer).
    private fun uriFor(
        kind: MediaKind,
        id: Long,
    ): Uri {
        val base =
            when (kind) {
                MediaKind.Image -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                MediaKind.Video -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                MediaKind.Audio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                MediaKind.Document -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            }
        return ContentUris.withAppendedId(base, id)
    }

    private companion object {
        const val MAX_ROWS = 50
        val PROJECTION =
            arrayOf(
                FileColumns._ID,
                FileColumns.DISPLAY_NAME,
                FileColumns.MIME_TYPE,
                FileColumns.MEDIA_TYPE,
                FileColumns.DATE_MODIFIED,
                FileColumns.RELATIVE_PATH,
            )

        // The Images table has no media_type column, so photos by id use their own projection.
        val IMAGE_PROJECTION =
            arrayOf(
                FileColumns._ID,
                FileColumns.DISPLAY_NAME,
                FileColumns.MIME_TYPE,
                FileColumns.DATE_MODIFIED,
                FileColumns.RELATIVE_PATH,
            )

        fun escapeLike(text: String) = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
