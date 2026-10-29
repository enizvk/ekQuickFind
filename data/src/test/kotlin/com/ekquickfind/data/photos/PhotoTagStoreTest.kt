package com.ekquickfind.data.photos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.data.EkDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoTagStoreTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val store = RoomPhotoTagStore(db.photoTags())

    @After
    fun tearDown() = db.close()

    private fun detected(label: String) = PhotoTagEntry(label, TagSource.Detected, 0.9f)

    @Test
    fun replacingAPhotosTagsDropsTheOldOnes() =
        runTest {
            store.replace(1, modifiedSeconds = 10, listOf(detected("Dog"), detected("Grass")))
            store.replace(1, modifiedSeconds = 20, listOf(detected("Beach")))
            assertEquals(listOf(TagHit(1, "Beach")), store.search("beach"))
            assertTrue(store.search("dog").isEmpty())
            assertEquals(mapOf(1L to 20L), store.scanned())
        }

    @Test
    fun searchMatchesTheStartOfAnyWordInATag() =
        runTest {
            store.replace(1, 10, listOf(PhotoTagEntry("Golden retriever", TagSource.Keyword, 1f)))
            assertEquals(listOf(TagHit(1, "Golden retriever")), store.search("retr"))
            assertEquals(listOf(TagHit(1, "Golden retriever")), store.search("gold"))
            assertTrue(store.search("olden").isEmpty())
        }

    @Test
    fun aPhotoWithoutTagsStillCountsAsScanned() =
        runTest {
            store.replace(7, 10, emptyList())
            assertEquals(mapOf(7L to 10L), store.scanned())
            assertEquals(1, store.taggedCount.first())
        }

    @Test
    fun forgettingPhotosRemovesTheirTagsAndScanState() =
        runTest {
            store.replace(1, 10, listOf(detected("Dog")))
            store.replace(2, 10, listOf(detected("Dog")))
            store.forget(listOf(1L))
            assertEquals(listOf(TagHit(2, "Dog")), store.search("dog"))
            assertEquals(setOf(2L), store.scanned().keys)
        }

    @Test
    fun clearEmptiesEverything() =
        runTest {
            store.replace(1, 10, listOf(detected("Dog")))
            store.clear()
            assertTrue(store.search("dog").isEmpty())
            assertEquals(0, store.taggedCount.first())
        }
}
