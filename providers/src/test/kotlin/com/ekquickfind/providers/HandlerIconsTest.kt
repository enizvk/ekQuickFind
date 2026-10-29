package com.ekquickfind.providers

import android.content.Intent
import android.net.Uri
import com.ekquickfind.core.search.Query
import com.ekquickfind.data.prefs.SearchEngineChoice
import com.ekquickfind.providers.contacts.ContactRow
import com.ekquickfind.providers.contacts.ContactsProvider
import com.ekquickfind.providers.files.Access
import com.ekquickfind.providers.files.FileAccessState
import com.ekquickfind.providers.files.FilesMediaProvider
import com.ekquickfind.providers.files.MediaKind
import com.ekquickfind.providers.files.MediaRow
import com.ekquickfind.providers.settings.DeviceSettingsProvider
import com.ekquickfind.providers.settings.IntentResolver
import com.ekquickfind.providers.settings.SettingsTable
import com.ekquickfind.providers.web.WebEngine
import com.ekquickfind.providers.web.WebFallback
import com.ekquickfind.providers.web.WebSettingsSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HandlerIconsTest {
    /** Pretends the device has these default handlers, and counts lookups. */
    private class FakeHandlers : HandlerResolver {
        var lookups = 0

        override fun packageFor(intent: Intent): String? {
            lookups++
            return when {
                intent.action == android.provider.Settings.ACTION_SETTINGS -> "com.android.settings"
                intent.type?.startsWith("image/") == true -> "com.sec.android.gallery3d"
                intent.type == "application/pdf" -> "com.google.android.apps.docs"
                intent.data?.scheme == "https" -> "com.sec.android.app.sbrowser"
                intent.data?.authority == "com.android.contacts" -> "com.samsung.android.app.contacts"
                else -> null
            }
        }
    }

    private val toggles =
        com.ekquickfind.providers.settings
            .FakeToggles()

    @Test
    fun settingsRowsUseTheSettingsApp() =
        runTest {
            val provider = DeviceSettingsProvider(SettingsTable.entries, IntentResolver { true }, toggles, CachedHandlers(FakeHandlers()))
            assertEquals(
                "com.android.settings",
                provider
                    .search(Query("hotspot"))
                    .first()
                    .first()
                    .handler,
            )
        }

    @Test
    fun contactsUseTheDefaultContactsApp() =
        runTest {
            val row = ContactRow(1, "lk", "Jamie Lee", null, false, emptyList(), emptyList(), null, null)
            val provider =
                ContactsProvider({ listOf(row) }, { true }, StandardTestDispatcher(testScheduler), CachedHandlers(FakeHandlers()))
            assertEquals(
                "com.samsung.android.app.contacts",
                provider
                    .search(Query("jamie"))
                    .first()
                    .single()
                    .handler,
            )
        }

    @Test
    fun filesUseTheDefaultViewerForTheirTypeAndLookupsAreCachedPerType() =
        runTest {
            val handlers = FakeHandlers()
            val rows =
                listOf(
                    MediaRow(Uri.parse("content://media/external/images/media/1"), "a.jpg", "image/jpeg", MediaKind.Image, 0, "DCIM/"),
                    MediaRow(Uri.parse("content://media/external/images/media/2"), "ab.jpg", "image/jpeg", MediaKind.Image, 0, "DCIM/"),
                    MediaRow(Uri.parse("content://media/external/file/3"), "a.pdf", "application/pdf", MediaKind.Document, 0, "Download/"),
                )
            val full = FileAccessState(Access.Full, Access.Full, Access.Full, documents = true)
            val provider =
                FilesMediaProvider({ _, _ -> rows }, { full }, StandardTestDispatcher(testScheduler), handlers = CachedHandlers(handlers))
            val results = provider.search(Query("a")).first().filter { it.isRecordable }
            assertEquals(
                mapOf(
                    "a.jpg" to "com.sec.android.gallery3d",
                    "ab.jpg" to "com.sec.android.gallery3d",
                    "a.pdf" to "com.google.android.apps.docs",
                ),
                results.associate { it.title to it.handler },
            )
            assertEquals(2, handlers.lookups)
        }

    @Test
    fun webRowsUseTheDefaultBrowser() {
        val policy =
            com.ekquickfind.providers.web.NetworkPolicy {
                WebSettingsSnapshot(
                    true,
                    WebEngine.forChoice(SearchEngineChoice.DuckDuckGo),
                )
            }
        val fallback = WebFallback(policy, CachedHandlers(FakeHandlers())).create(Query("cats"))
        assertEquals("com.sec.android.app.sbrowser", fallback.handler)
    }

    @Test
    fun theChooserIsNotAHandler() {
        val chooserOnly = HandlerResolver { "android" }
        assertEquals(null, CachedHandlers(chooserOnly).packageFor(Intent(Intent.ACTION_VIEW)))
    }
}
