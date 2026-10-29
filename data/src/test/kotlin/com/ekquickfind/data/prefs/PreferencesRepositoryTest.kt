package com.ekquickfind.data.prefs

import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PreferencesRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository(file: File = folder.newFile("prefs.json").also { it.delete() }) =
        PreferencesRepository(DataStoreFactory.create(UserPreferencesSerializer, scope = backgroundScope) { file })

    @Test
    fun defaultsArePrivacyFriendly() =
        runTest {
            val prefs = repository().preferences.first()
            assertEquals(SearchEngineChoice.Google, prefs.searchEngine)
            assertTrue(prefs.webSuggestions)
            assertFalse(prefs.backupHistory)
            assertEquals(ThemeMode.System, prefs.theme)
            assertTrue(prefs.dynamicColor)
            assertTrue(prefs.disabledProviders.isEmpty())
            assertTrue(prefs.hiddenResults.isEmpty())
        }

    @Test
    fun everyFieldRoundTripsThroughTheFile() =
        runTest {
            val file = folder.newFile("roundtrip.json").also { it.delete() }
            val first = PreferencesRepository(DataStoreFactory.create(UserPreferencesSerializer, scope = backgroundScope) { file })
            first.setProviderEnabled("files", enabled = false)
            first.setSearchEngine(
                SearchEngineChoice.Custom,
                customTemplate = "https://example.com/?q=%s",
                customSuggestTemplate = "https://example.com/ac?q=%s",
            )
            first.setTheme(ThemeMode.Dark)
            first.setDynamicColor(false)
            first.hide("apps:com.example/Main@0", "Example")
            first.setWebSuggestions(false)
            first.setBackupHistory(true)

            val raw = file.readText()
            val decoded = UserPreferencesSerializer.decode(raw)
            assertEquals(setOf("files"), decoded.disabledProviders)
            assertEquals(SearchEngineChoice.Custom, decoded.searchEngine)
            assertEquals("https://example.com/?q=%s", decoded.customTemplate)
            assertEquals("https://example.com/ac?q=%s", decoded.customSuggestTemplate)
            assertEquals(ThemeMode.Dark, decoded.theme)
            assertFalse(decoded.dynamicColor)
            assertEquals(mapOf("apps:com.example/Main@0" to "Example"), decoded.hiddenResults)
            assertFalse(decoded.webSuggestions)
            assertTrue(decoded.backupHistory)
        }

    @Test
    fun reEnablingAProviderRemovesItFromTheDisabledSet() =
        runTest {
            val repo = repository()
            repo.setProviderEnabled("files", enabled = false)
            repo.setProviderEnabled("files", enabled = true)
            assertTrue(
                repo.preferences
                    .first()
                    .disabledProviders
                    .isEmpty(),
            )
        }

    @Test
    fun unhideRemovesTheEntry() =
        runTest {
            val repo = repository()
            repo.hide("a", "A")
            repo.unhide("a")
            assertTrue(
                repo.preferences
                    .first()
                    .hiddenResults
                    .isEmpty(),
            )
        }

    @Test
    fun corruptFilesFallBackToDefaults() =
        runTest {
            val file = folder.newFile("corrupt.json").apply { writeText("{not json") }
            assertEquals(UserPreferences(), repository(file).preferences.first())
        }

    @Test
    fun unknownFieldsFromNewerVersionsAreIgnored() {
        val prefs = UserPreferencesSerializer.decode("""{"theme":"Dark","futureFlag":true}""")
        assertEquals(ThemeMode.Dark, prefs.theme)
    }
}
