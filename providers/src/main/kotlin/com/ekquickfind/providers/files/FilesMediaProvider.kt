package com.ekquickfind.providers.files

import android.Manifest
import android.content.Intent
import androidx.core.net.toUri
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.Requirement
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.TextNormalizer
import com.ekquickfind.providers.HandlerResolver
import com.ekquickfind.providers.RequestPermissionAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class FilesMediaProvider(
    private val source: MediaSource,
    private val access: FileAccess,
    private val io: CoroutineDispatcher,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
    private val handlers: HandlerResolver = HandlerResolver.None,
    private val photoTags: PhotoTagSearch = PhotoTagSearch.None,
) : SearchProvider,
    ResultResolver {
    override val id = ID
    override val section = Section.Files
    override val requirements =
        setOf(
            Requirement.Permission(Manifest.permission.READ_MEDIA_IMAGES),
            Requirement.Permission(Manifest.permission.READ_MEDIA_VIDEO),
            Requirement.Permission(Manifest.permission.READ_MEDIA_AUDIO),
            Requirement.SpecialAccess("All files access"),
        )

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val state = access.current()
            if (!state.anyMedia && !state.documents) {
                emit(listOf(enableRow()))
                return@flow
            }
            // MediaStore LIKE queries are the slowest search we run; wait for a short typing pause.
            delay(DEBOUNCE_MS)
            val text = query.raw.trim()
            val extension = text.removePrefix(".").lowercase().takeIf { it in KNOWN_EXTENSIONS }
            val rows = withContext(io) { source.query(if (extension != null) ".$extension" else text, state.searchableKinds) }
            val byName = rows.mapNotNull { toResult(it, query.normalized, extension) }
            val names = byName.map { it.id }.toSet()
            // Photos found by a tag; one already found by its name isn't listed twice.
            val byTag = if (extension == null && MediaKind.Image in state.searchableKinds) tagResults(query.normalized) else emptyList()
            val results = (byName + byTag.filter { it.id !in names }).sortedByDescending { it.score }
            emit(results + accessRows(state, extension))
        }

    override suspend fun resolve(id: ResultId): SearchResult? {
        val uri =
            id.value
                .removePrefix("files:")
                .takeIf { it.startsWith("content://") }
                ?.toUri() ?: return null
        val row = withContext(io) { source.byUri(uri) } ?: return null
        if (row.kind !in access.current().searchableKinds) return null
        return toResult(row, TextNormalizer.normalize(row.displayName), null)
    }

    private suspend fun tagResults(q: String): List<SearchResult> {
        // One or two letters would match half the tags; wait for a real word.
        if (q.length < TAG_MIN_LENGTH) return emptyList()
        val hits = photoTags.matches(q).associate { it.mediaId to it.label }
        if (hits.isEmpty()) return emptyList()
        val rows = withContext(io) { source.byImageIds(hits.keys) }
        return rows.mapNotNull { row ->
            val label =
                row.uri.lastPathSegment
                    ?.toLongOrNull()
                    ?.let(hits::get) ?: return@mapNotNull null
            val score = (QueryMatcher.bestScore(q, listOf(MatchTarget(label))) ?: TAG_FLOOR) * TAG_WEIGHT
            resultFor(row, score, label)
        }
    }

    private fun toResult(
        row: MediaRow,
        q: String,
        extension: String?,
    ): SearchResult? {
        val score =
            if (extension != null) {
                EXTENSION_SCORE
            } else {
                val base = row.displayName.substringBeforeLast('.')
                QueryMatcher.bestScore(q, listOf(MatchTarget(base), MatchTarget(row.displayName))) ?: return null
            }
        return resultFor(row, score, tag = null)
    }

    private fun resultFor(
        row: MediaRow,
        score: Float,
        tag: String?,
    ): SearchResult {
        val visual = row.kind == MediaKind.Image || row.kind == MediaKind.Video
        val where = "${folderName(row.relativePath)} · ${relativeTime(row.modifiedSeconds)}"
        return SearchResult(
            id = ResultId("files:${row.uri}"),
            providerId = ID,
            section = section,
            title = row.displayName,
            subtitle = if (tag != null) "$tag · $where" else where,
            icon = if (visual) ResultIcon.Thumbnail(row.uri.toString(), glyphFor(row)) else ResultIcon.Vector(glyphFor(row)),
            score = score,
            primaryAction = OpenFileAction(row.uri, row.mimeType),
            secondaryActions = listOf(ShareFileAction(row.uri, row.mimeType), ShowInFolderAction(row.relativePath)),
            handler = handlers.packageFor(Intent(Intent.ACTION_VIEW).setDataAndType(row.uri, row.mimeType ?: "*/*")),
        )
    }

    private fun accessRows(
        state: FileAccessState,
        extension: String?,
    ) = buildList {
        if (state.images == Access.Partial || state.video == Access.Partial) {
            add(
                infoRow(
                    "partial",
                    "Allow access to all photos and videos",
                    "Only photos you picked are searchable",
                    RequestPermissionAction(listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)),
                ),
            )
        }
        if (!state.documents && extension in DOCUMENT_EXTENSIONS) {
            add(infoRow("documents", "Include documents and downloads", "Needs All files access", AllFilesAccessAction))
        }
    }

    private fun enableRow() =
        infoRow(
            "enable",
            "Enable file search",
            "Find photos, videos and music by name",
            RequestPermissionAction(
                listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
            ),
        )

    private fun infoRow(
        key: String,
        title: String,
        subtitle: String,
        action: com.ekquickfind.core.search.Action,
    ) = SearchResult(
        id = ResultId("files:$key"),
        providerId = ID,
        section = section,
        title = title,
        subtitle = subtitle,
        icon = ResultIcon.Vector(Glyph.Lock),
        score = 0.1f,
        primaryAction = action,
        isRecordable = false,
    )

    private fun folderName(relativePath: String?) = relativePath?.trimEnd('/')?.substringAfterLast('/')?.ifEmpty { null } ?: "Storage"

    private fun relativeTime(modifiedSeconds: Long): String {
        val minutes = (nowSeconds() - modifiedSeconds).coerceAtLeast(0) / 60

        fun plural(
            n: Long,
            unit: String,
        ) = "$n $unit${if (n == 1L) "" else "s"} ago"
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> plural(minutes, "minute")
            minutes < 60 * 24 -> plural(minutes / 60, "hour")
            minutes < 60 * 24 * 30 -> plural(minutes / (60 * 24), "day")
            minutes < 60 * 24 * 365 -> plural(minutes / (60 * 24 * 30), "month")
            else -> plural(minutes / (60 * 24 * 365), "year")
        }
    }

    private fun glyphFor(row: MediaRow) =
        when (row.kind) {
            MediaKind.Image -> Glyph.Image
            MediaKind.Video -> Glyph.Video
            MediaKind.Audio -> Glyph.Audio
            MediaKind.Document ->
                if (row.displayName.substringAfterLast('.').lowercase() in
                    DOCUMENT_EXTENSIONS
                ) {
                    Glyph.Document
                } else {
                    Glyph.File
                }
        }

    companion object {
        val ID = ProviderId("files")
        private const val DEBOUNCE_MS = 120L
        private const val EXTENSION_SCORE = 0.6f
        private const val TAG_MIN_LENGTH = 3

        // Tag matches rank a little below name matches, which are more deliberate.
        private const val TAG_WEIGHT = 0.9f
        private const val TAG_FLOOR = 0.5f
        private val DOCUMENT_EXTENSIONS =
            setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "odt", "rtf", "epub", "zip", "apk")
        private val KNOWN_EXTENSIONS =
            DOCUMENT_EXTENSIONS +
                setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "mp4", "mkv", "mov", "mp3", "m4a", "flac", "wav", "ogg")
    }
}
