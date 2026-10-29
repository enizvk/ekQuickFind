package com.ekquickfind.data.photos

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ekquickfind.core.search.TextNormalizer
import kotlinx.coroutines.flow.Flow

enum class TagSource {
    /** Worked out on the phone from the picture itself. */
    Detected,

    /** Written into the photo's metadata by the camera or an editing app. */
    Keyword,
}

data class PhotoTagEntry(
    val label: String,
    val source: TagSource,
    val confidence: Float,
)

data class TagHit(
    val mediaId: Long,
    val label: String,
)

/** Local index of photo tags, keyed by MediaStore image id. */
interface PhotoTagStore {
    /** Photos looked at so far, tagged or not. */
    val taggedCount: Flow<Int>

    /** MediaStore id to the modified time the photo had when it was tagged. */
    suspend fun scanned(): Map<Long, Long>

    suspend fun replace(
        mediaId: Long,
        modifiedSeconds: Long,
        tags: List<PhotoTagEntry>,
    )

    suspend fun forget(mediaIds: Collection<Long>)

    /** Photos with a tag word starting with [normalizedQuery], one hit per photo. */
    suspend fun search(normalizedQuery: String): List<TagHit>

    suspend fun clear()
}

@Entity(tableName = "photo_tag", primaryKeys = ["mediaId", "tag"], indices = [Index("tag")])
data class PhotoTagRow(
    val mediaId: Long,
    /** Normalised for matching; [label] is what's shown. */
    val tag: String,
    val label: String,
    val source: TagSource,
    val confidence: Float,
)

@Entity(tableName = "photo_scan")
data class PhotoScanRow(
    @PrimaryKey val mediaId: Long,
    val modifiedSeconds: Long,
)

@Dao
abstract class PhotoTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTags(rows: List<PhotoTagRow>)

    @Query("DELETE FROM photo_tag WHERE mediaId = :mediaId")
    abstract suspend fun deleteTags(mediaId: Long)

    @Upsert
    abstract suspend fun upsertScan(row: PhotoScanRow)

    @Transaction
    open suspend fun replace(
        mediaId: Long,
        modifiedSeconds: Long,
        rows: List<PhotoTagRow>,
    ) {
        deleteTags(mediaId)
        insertTags(rows)
        upsertScan(PhotoScanRow(mediaId, modifiedSeconds))
    }

    @Query("SELECT * FROM photo_scan")
    abstract suspend fun scans(): List<PhotoScanRow>

    @Query("DELETE FROM photo_tag WHERE mediaId IN (:ids)")
    abstract suspend fun deleteTagsFor(ids: List<Long>)

    @Query("DELETE FROM photo_scan WHERE mediaId IN (:ids)")
    abstract suspend fun deleteScansFor(ids: List<Long>)

    @Transaction
    open suspend fun forget(ids: List<Long>) {
        // SQLite limits bound parameters per statement.
        ids.chunked(500).forEach {
            deleteTagsFor(it)
            deleteScansFor(it)
        }
    }

    @Query(
        """
        SELECT mediaId, label FROM photo_tag
        WHERE tag LIKE :q || '%' ESCAPE '\' OR tag LIKE '% ' || :q || '%' ESCAPE '\'
        GROUP BY mediaId ORDER BY MAX(confidence) DESC LIMIT :limit
        """,
    )
    abstract suspend fun search(
        q: String,
        limit: Int,
    ): List<TagHit>

    @Query("SELECT COUNT(*) FROM photo_scan")
    abstract fun scanCount(): Flow<Int>

    @Query("DELETE FROM photo_tag")
    abstract suspend fun clearTags()

    @Query("DELETE FROM photo_scan")
    abstract suspend fun clearScans()
}

class RoomPhotoTagStore(
    private val dao: PhotoTagDao,
) : PhotoTagStore {
    override val taggedCount: Flow<Int> = dao.scanCount()

    override suspend fun scanned(): Map<Long, Long> = dao.scans().associate { it.mediaId to it.modifiedSeconds }

    override suspend fun replace(
        mediaId: Long,
        modifiedSeconds: Long,
        tags: List<PhotoTagEntry>,
    ) {
        val rows =
            tags
                .map { PhotoTagRow(mediaId, TextNormalizer.normalize(it.label), it.label, it.source, it.confidence) }
                .filter { it.tag.isNotBlank() }
                // A keyword and a detected label can be the same word; keep the surer one.
                .groupBy { it.tag }
                .map { (_, same) -> same.maxBy { it.confidence } }
        dao.replace(mediaId, modifiedSeconds, rows)
    }

    override suspend fun forget(mediaIds: Collection<Long>) = dao.forget(mediaIds.toList())

    override suspend fun search(normalizedQuery: String): List<TagHit> {
        val q = normalizedQuery.trim()
        if (q.isEmpty()) return emptyList()
        return dao.search(q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"), MAX_HITS)
    }

    override suspend fun clear() {
        dao.clearTags()
        dao.clearScans()
    }

    private companion object {
        const val MAX_HITS = 200
    }
}
