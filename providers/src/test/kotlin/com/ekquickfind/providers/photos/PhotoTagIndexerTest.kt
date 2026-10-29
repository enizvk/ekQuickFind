package com.ekquickfind.providers.photos

import android.net.Uri
import com.ekquickfind.data.photos.PhotoTagEntry
import com.ekquickfind.data.photos.PhotoTagStore
import com.ekquickfind.data.photos.TagHit
import com.ekquickfind.data.photos.TagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoTagIndexerTest {
    private class FakeStore : PhotoTagStore {
        val tags = mutableMapOf<Long, List<PhotoTagEntry>>()
        val modified = mutableMapOf<Long, Long>()
        override val taggedCount: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun scanned() = modified.toMap()

        override suspend fun replace(
            mediaId: Long,
            modifiedSeconds: Long,
            tags: List<PhotoTagEntry>,
        ) {
            this.tags[mediaId] = tags
            modified[mediaId] = modifiedSeconds
        }

        override suspend fun forget(mediaIds: Collection<Long>) {
            mediaIds.forEach {
                tags.remove(it)
                modified.remove(it)
            }
        }

        override suspend fun search(normalizedQuery: String) = emptyList<TagHit>()

        override suspend fun clear() {
            tags.clear()
            modified.clear()
        }
    }

    private class FakeLibrary(
        var items: List<PhotoItem>,
        val xmp: Map<Long, String> = emptyMap(),
    ) : PhotoLibrary {
        override fun photos() = items

        override fun xmp(item: PhotoItem) = xmp[item.mediaId]
    }

    private class FakeLabeler(
        val failFor: Set<Long> = emptySet(),
    ) : PhotoLabeler {
        val calls = mutableListOf<Long>()

        override suspend fun labels(uri: Uri): List<Label> {
            val id = uri.lastPathSegment!!.toLong()
            calls += id
            if (id in failFor) error("can't decode")
            return listOf(Label("Dog", 0.92f))
        }
    }

    private fun item(
        id: Long,
        modified: Long = 100,
    ) = PhotoItem(id, Uri.parse("content://media/external/images/media/$id"), modified)

    private val keywordXmp = "<rdf:li xmlns:rdf=\"x\">ignored</rdf:li><dc:subject><rdf:Bag><rdf:li>Crete</rdf:li></rdf:Bag></dc:subject>"

    @Test
    fun newPhotosGetDetectedLabelsAndTheirOwnKeywords() =
        runTest {
            val store = FakeStore()
            val indexer =
                PhotoTagIndexer(FakeLibrary(listOf(item(1)), mapOf(1L to keywordXmp)), FakeLabeler(), store, Dispatchers.Unconfined)
            assertEquals(1, indexer.run())
            assertEquals(
                listOf(PhotoTagEntry("Dog", TagSource.Detected, 0.92f), PhotoTagEntry("Crete", TagSource.Keyword, 1f)),
                store.tags[1],
            )
        }

    @Test
    fun unchangedPhotosAreNotTaggedAgainButEditedOnesAre() =
        runTest {
            val store = FakeStore()
            val library = FakeLibrary(listOf(item(1), item(2)))
            val labeler = FakeLabeler()
            val indexer = PhotoTagIndexer(library, labeler, store, Dispatchers.Unconfined)
            indexer.run()
            library.items = listOf(item(1), item(2, modified = 200))
            labeler.calls.clear()
            indexer.run()
            assertEquals(listOf(2L), labeler.calls)
            assertEquals(200L, store.modified[2])
        }

    @Test
    fun deletedPhotosAreForgotten() =
        runTest {
            val store = FakeStore()
            val library = FakeLibrary(listOf(item(1), item(2)))
            val indexer = PhotoTagIndexer(library, FakeLabeler(), store, Dispatchers.Unconfined)
            indexer.run()
            library.items = listOf(item(2))
            indexer.run()
            assertEquals(setOf(2L), store.modified.keys)
        }

    @Test
    fun aPhotoThatCantBeLabelledKeepsItsKeywordsAndIsNotRetriedEveryRun() =
        runTest {
            val store = FakeStore()
            val labeler = FakeLabeler(failFor = setOf(1))
            val indexer = PhotoTagIndexer(FakeLibrary(listOf(item(1)), mapOf(1L to keywordXmp)), labeler, store, Dispatchers.Unconfined)
            indexer.run()
            indexer.run()
            assertEquals(listOf(PhotoTagEntry("Crete", TagSource.Keyword, 1f)), store.tags[1])
            assertEquals(listOf(1L), labeler.calls)
        }

    @Test
    fun progressCountsUpToEveryPhotoThatNeededTagging() =
        runTest {
            val progress = mutableListOf<Pair<Int, Int>>()
            PhotoTagIndexer(FakeLibrary(listOf(item(1), item(2))), FakeLabeler(), FakeStore(), Dispatchers.Unconfined).run { done, total ->
                progress += done to total
            }
            assertEquals(listOf(1 to 2, 2 to 2), progress)
        }
}
