package com.ekquickfind.app.screenshots

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import com.ekquickfind.app.Screenshots
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.app.ui.settings.SettingsPage
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.UserPreferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Landing-page screenshots of every settings page, captured from the app's own window (no status bar,
 * no other apps' overlays). Runs only on request: `-e screenshots true`.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SettingsScreenshotsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: PreferencesRepository
    private var saved: UserPreferences? = null

    @Before
    fun setUp() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("screenshots") == "true")
        hilt.inject()
        saved = runBlocking { prefs.preferences.first() }
        runBlocking { prefs.update { UserPreferences(hiddenResults = it.hiddenResults) } }
        SearchLauncher.launch()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 5_000)
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        saved?.let { s -> runBlocking { prefs.update { s } } }
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun shot(name: String) {
        // Let entry animations settle.
        Thread.sleep(500)
        compose.waitForIdle()
        Screenshots.save(name, compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun everySettingsPage() {
        compose.onNodeWithTag("open-settings", useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) { exists("settings-menu") }
        shot("settings-00-menu")
        SettingsPage.entries.forEachIndexed { i, page ->
            val tag = "settings-page:${page.name}"
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag(tag))
            // The store build has no Updates page.
            if (!exists(tag)) return@forEachIndexed
            compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            compose.waitUntil(3_000) { !exists("settings-menu") }
            val name = "settings-%02d-%s".format(i + 1, page.title.lowercase().replace(' ', '-'))
            shot(name)
            if (page in LONG_PAGES) {
                compose.onNodeWithTag("settings-list").performTouchInput { swipeUp() }
                shot("$name-2")
            }
            compose.onNodeWithTag("settings-back", useUnmergedTree = true).performClick()
            compose.waitUntil(3_000) { exists("settings-menu") }
        }
    }

    private companion object {
        val LONG_PAGES = setOf(SettingsPage.Sources, SettingsPage.Results, SettingsPage.Look)
    }
}
