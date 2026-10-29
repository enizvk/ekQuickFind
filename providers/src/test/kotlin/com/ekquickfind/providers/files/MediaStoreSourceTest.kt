package com.ekquickfind.providers.files

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaStoreSourceTest {
    private lateinit var fake: FakeMediaContentProvider
    private val source = MediaStoreSource(ApplicationProvider.getApplicationContext<Context>().contentResolver)

    @Before
    fun setUp() {
        fake = Robolectric.buildContentProvider(FakeMediaContentProvider::class.java).create(MediaStore.AUTHORITY).get()
        fake.add("beach.jpg", "image/jpeg", FileColumns.MEDIA_TYPE_IMAGE, 300, "DCIM/Camera/")
        fake.add("beach-trip.mp4", "video/mp4", FileColumns.MEDIA_TYPE_VIDEO, 200, "DCIM/Camera/")
        fake.add("beach boys.mp3", "audio/mpeg", FileColumns.MEDIA_TYPE_AUDIO, 100, "Music/")
        fake.add("report.pdf", "application/pdf", FileColumns.MEDIA_TYPE_DOCUMENT, 50)
        fake.add("report.pdf", "application/pdf", FileColumns.MEDIA_TYPE_DOCUMENT, 900)
    }

    private fun names(
        q: String,
        kinds: Set<MediaKind> = MediaKind.entries.toSet(),
    ) = source.query(q, kinds).map { it.displayName }

    @Test
    fun matchesDisplayNameNewestFirst() = assertEquals(listOf("beach.jpg", "beach-trip.mp4", "beach boys.mp3"), names("beach"))

    @Test
    fun onlyQueriesGrantedKinds() = assertEquals(listOf("beach.jpg"), names("beach", setOf(MediaKind.Image)))

    @Test
    fun noKindsMeansNoQuery() = assertTrue(names("beach", emptySet()).isEmpty())

    @Test
    fun recencyOrdersSameNamedFiles() {
        val reports = source.query("report", setOf(MediaKind.Document))
        assertEquals(listOf(900L, 50L), reports.map { it.modifiedSeconds })
    }

    @Test
    fun buildsTypeSpecificContentUris() {
        val image = source.query("beach.jpg", setOf(MediaKind.Image)).single()
        assertTrue(
            image.uri.toString().startsWith(
                MediaStore.Images.Media
                    .getContentUri(MediaStore.VOLUME_EXTERNAL)
                    .toString(),
            ),
        )
        assertEquals("DCIM/Camera/", image.relativePath)
        assertEquals(MediaKind.Image, image.kind)
    }

    @Test
    fun asksForAtMostFiftyRowsAndSelectedColumns() {
        names("beach")
        assertEquals(50, fake.lastQueryArgs!!.getInt(ContentResolver.QUERY_ARG_LIMIT))
    }

    @Test
    fun likeWildcardsAreLiteral() = assertTrue(names("%").isEmpty())
}
