package com.ekquickfind.providers.files

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.data.photos.TagHit
import com.ekquickfind.providers.FakeAndroidHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilesMediaProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = 1_000_000L

    private fun row(
        name: String,
        kind: MediaKind,
        modified: Long = now,
        mime: String? = null,
        path: String = "Download/",
    ) = MediaRow(Uri.parse("content://media/external/${kind.name.lowercase()}/media/${name.hashCode()}"), name, mime, kind, modified, path)

    private val photo = row("beach.jpg", MediaKind.Image, mime = "image/jpeg", path = "DCIM/Camera/")
    private val oldReport = row("report.pdf", MediaKind.Document, now - 10 * 86_400, "application/pdf")
    private val newReport = row("report.pdf", MediaKind.Document, now - 3_600, "application/pdf")
    private val song = row("beach boys.mp3", MediaKind.Audio, mime = "audio/mpeg", path = "Music/")

    private class FakeSource(
        val rows: List<MediaRow>,
    ) : MediaSource {
        var lastKinds: Set<MediaKind>? = null
        val queries = mutableListOf<String>()

        override fun byImageIds(ids: Collection<Long>) =
            rows.filter {
                it.kind == MediaKind.Image &&
                    it.uri.lastPathSegment!!.toLong() in ids
            }

        override fun query(
            text: String,
            kinds: Set<MediaKind>,
        ): List<MediaRow> {
            lastKinds = kinds
            queries += text
            return rows
                .filter {
                    it.kind in kinds &&
                        it.displayName.contains(
                            text,
                            ignoreCase = true,
                        )
                }.sortedByDescending { it.modifiedSeconds }
        }
    }

    private fun TestScope.provider(
        access: FileAccessState,
        source: MediaSource = FakeSource(listOf(photo, oldReport, newReport, song)),
        tags: PhotoTagSearch = PhotoTagSearch.None,
    ) = FilesMediaProvider(source, { access }, StandardTestDispatcher(testScheduler), nowSeconds = { now }, photoTags = tags)

    private val photoId = photo.uri.lastPathSegment!!.toLong()

    @Test
    fun aPhotoIsFoundByItsTagWithTheTagInTheSubtitle() =
        runTest {
            val tags = PhotoTagSearch { q -> if (q == "dog") listOf(TagHit(photoId, "Dog")) else emptyList() }
            val rows = provider(full, tags = tags).search(Query("dog")).first()
            assertEquals(listOf("beach.jpg"), rows.map { it.title })
            assertTrue(rows.single().subtitle!!.startsWith("Dog · "))
        }

    @Test
    fun aPhotoMatchingByNameAndByTagShowsOnce() =
        runTest {
            val tags = PhotoTagSearch { listOf(TagHit(photoId, "Beach")) }
            val rows = provider(full, tags = tags).search(Query("beach")).first()
            assertEquals(listOf("beach.jpg", "beach boys.mp3"), rows.map { it.title })
        }

    @Test
    fun queriesShorterThanThreeLettersDontSearchTags() =
        runTest {
            var asked = false
            val tags =
                PhotoTagSearch {
                    asked = true
                    emptyList()
                }
            provider(full, tags = tags).search(Query("do")).first()
            assertFalse(asked)
        }

    private val full = FileAccessState(images = Access.Full, video = Access.Full, audio = Access.Full, documents = true)

    @Test
    fun noPermissionShowsOneEnableRow() =
        runTest {
            val rows = provider(FileAccessState.None).search(Query("beach")).first()
            assertEquals(listOf("Enable file search"), rows.map { it.title })
            val host = FakeAndroidHost(context)
            assertEquals(ActionOutcome.Refresh, rows.single().primaryAction.perform(host))
            assertEquals(
                listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
                host.requested.single(),
            )
        }

    @Test
    fun partialPhotoAccessSearchesSelectedPhotosAndOffersFullAccess() =
        runTest {
            val source = FakeSource(listOf(photo, song))
            val access = FileAccessState(images = Access.Partial, video = Access.Partial, audio = Access.None, documents = false)
            val rows = provider(access, source).search(Query("beach")).first()
            assertEquals(setOf(MediaKind.Image, MediaKind.Video), source.lastKinds)
            assertEquals(listOf("beach.jpg", "Allow access to all photos and videos"), rows.map { it.title })
            assertFalse(rows.last().isRecordable)
        }

    @Test
    fun fullAccessSearchesEveryKind() =
        runTest {
            val titles = provider(full).search(Query("beach")).first().map { it.title }
            assertEquals(listOf("beach.jpg", "beach boys.mp3"), titles)
        }

    @Test
    fun extensionQueryListsThoseFilesNewestFirst() =
        runTest {
            val rows = provider(full).search(Query("pdf")).first()
            assertEquals(listOf("report.pdf", "report.pdf"), rows.map { it.title })
            assertTrue(rows[0].subtitle!!.endsWith("1 hour ago"))
            assertTrue(rows[1].subtitle!!.endsWith("10 days ago"))
        }

    @Test
    fun sameNameTiesBreakByRecency() =
        runTest {
            val rows = provider(full).search(Query("report")).first()
            assertEquals(listOf("Download · 1 hour ago", "Download · 10 days ago"), rows.map { it.subtitle })
        }

    @Test
    fun documentQueriesWithoutAllFilesAccessOfferIt() =
        runTest {
            val access = full.copy(documents = false)
            val rows = provider(access).search(Query("pdf")).first()
            val offer = rows.single { it.title == "Include documents and downloads" }
            val host = FakeAndroidHost(context)
            offer.primaryAction.perform(host)
            assertEquals(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, host.started.single().action)
        }

    @Test
    fun imagesAndVideosGetThumbnailsOthersGetTypeIcons() =
        runTest {
            val rows = provider(full).search(Query("beach")).first()
            assertTrue(rows[0].icon is ResultIcon.Thumbnail)
            assertEquals(ResultIcon.Vector(Glyph.Audio), rows[1].icon)
        }

    @Test
    fun openUsesTheDefaultHandlerWithReadAccess() =
        runTest {
            val host = FakeAndroidHost(context)
            provider(full)
                .search(Query("beach.jpg"))
                .first()
                .first()
                .primaryAction
                .perform(host)
            val intent = host.started.single()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("image/jpeg", intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        }

    @Test
    fun withoutAHandlerTheSystemChooserIsShown() =
        runTest {
            val host = FakeAndroidHost(context, canStart = false)
            provider(full)
                .search(Query("beach.jpg"))
                .first()
                .first()
                .primaryAction
                .perform(host)
            assertEquals(listOf(Intent.ACTION_VIEW, Intent.ACTION_CHOOSER), host.started.map { it.action })
        }

    @Test
    fun longPressOffersShareAndShowInFolder() =
        runTest {
            val actions =
                provider(full)
                    .search(Query("beach.jpg"))
                    .first()
                    .first()
                    .secondaryActions
            assertEquals(listOf("Share", "Show in folder"), actions.map { it.label })

            val host = FakeAndroidHost(context)
            actions.first().perform(host)
            val chooser = host.started.single()
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals(photo.uri, send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        }
}
