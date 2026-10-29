package com.ekquickfind.providers

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.providers.apps.AppIndex
import com.ekquickfind.providers.apps.AppsProvider
import com.ekquickfind.providers.apps.FakeAppsSource
import com.ekquickfind.providers.apps.app
import com.ekquickfind.providers.contacts.ContactRow
import com.ekquickfind.providers.contacts.ContactsProvider
import com.ekquickfind.providers.contacts.ContactsSource
import com.ekquickfind.providers.files.Access
import com.ekquickfind.providers.files.FileAccessState
import com.ekquickfind.providers.files.FilesMediaProvider
import com.ekquickfind.providers.files.MediaKind
import com.ekquickfind.providers.files.MediaRow
import com.ekquickfind.providers.files.MediaSource
import com.ekquickfind.providers.settings.DeviceSettingsProvider
import com.ekquickfind.providers.settings.IntentResolver
import com.ekquickfind.providers.settings.QuickToggle
import com.ekquickfind.providers.settings.SettingsTable
import com.ekquickfind.providers.web.WebEngine
import com.ekquickfind.providers.web.WebProvider
import com.ekquickfind.providers.web.WebSettingsSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResolversTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun appsResolveFromTheIndexAndDisappearWhenUninstalled() =
        runTest {
            val maps = app("Maps")
            val source = FakeAppsSource(apps = listOf(maps))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()
            val provider = AppsProvider(index) { emptySet() }
            val id =
                provider
                    .search(Query("maps"))
                    .first()
                    .single()
                    .id

            assertEquals("Maps", provider.resolve(id)?.title)
            assertNull(provider.resolve(ResultId("apps:gone/Main@0")))
        }

    @Test
    fun appsResolveByPackageForSuggestions() =
        runTest {
            val source = FakeAppsSource(apps = listOf(app("Maps", pkg = "com.maps"), app("Work Maps", pkg = "com.maps", badge = "Work")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()
            assertEquals(listOf("Maps"), AppsProvider(index) { emptySet() }.forPackages(listOf("com.maps", "missing")).map { it.title })
        }

    @Test
    fun contactsResolveByIdOnlyWithPermission() =
        runTest {
            val row = ContactRow(7, "lk7", "Jamie Lee", null, false, emptyList(), emptyList(), null, null)
            val source =
                object : ContactsSource {
                    override fun query(text: String) = emptyList<ContactRow>()

                    override fun byIds(ids: List<Long>) = listOf(row).filter { it.contactId in ids }
                }
            val io = StandardTestDispatcher(testScheduler)
            assertEquals("Jamie Lee", ContactsProvider(source, { true }, io).resolve(ResultId("contacts:7"))?.title)
            assertNull(ContactsProvider(source, { false }, io).resolve(ResultId("contacts:7")))
        }

    @Test
    fun settingsAndTogglesResolveByKey() =
        runTest {
            val toggles =
                com.ekquickfind.providers.settings
                    .FakeToggles()
                    .apply { QuickToggle.entries.forEach { states[it] = true } }
            val provider = DeviceSettingsProvider(SettingsTable.entries, IntentResolver { true }, toggles)
            assertEquals("Hotspot & tethering", provider.resolve(ResultId("settings:hotspot"))?.title)
            assertEquals("On", provider.resolve(ResultId("settings:toggle:Bluetooth"))?.subtitle)
            assertNull(provider.resolve(ResultId("settings:nope")))
        }

    @Test
    fun filesResolveByUri() =
        runTest {
            val uri = Uri.parse("content://media/external/images/media/5")
            val row = MediaRow(uri, "beach.jpg", "image/jpeg", MediaKind.Image, 0, "DCIM/")
            val source =
                object : MediaSource {
                    override fun query(
                        text: String,
                        kinds: Set<MediaKind>,
                    ) = emptyList<MediaRow>()

                    override fun byUri(uri: Uri) = row.takeIf { it.uri == uri }
                }
            val full = FileAccessState(Access.Full, Access.Full, Access.Full, documents = true)
            val provider = FilesMediaProvider(source, { full }, StandardTestDispatcher(testScheduler))
            assertEquals("beach.jpg", provider.resolve(ResultId("files:$uri"))?.title)
            assertNull(
                FilesMediaProvider(source, { FileAccessState.None }, StandardTestDispatcher(testScheduler)).resolve(ResultId("files:$uri")),
            )
        }

    @Test
    fun webSuggestionsResolveFromTheirText() =
        runTest {
            val provider =
                WebProvider(OkHttpClient(), {
                    WebSettingsSnapshot(true, WebEngine.forChoice(com.ekquickfind.data.prefs.SearchEngineChoice.DuckDuckGo))
                }, StandardTestDispatcher(testScheduler))
            assertEquals("weather in rome", provider.resolve(ResultId("web:suggest:weather in rome"))?.title)
        }

    @Suppress("unused")
    private val ignored = context
}
