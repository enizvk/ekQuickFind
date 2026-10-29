package com.ekquickfind.app.screenshots

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ekquickfind.app.Screenshots
import com.ekquickfind.app.SearchLauncher
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
 * Landing-page shots of the search surface from the app's own window: no status bar, no other apps'
 * overlays, default settings. Runs only on request: `-e screenshots true`.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SearchScreenshotsTest {
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
        runBlocking { prefs.update { com.ekquickfind.app.testBaseline(it) } }
        SearchLauncher.launch()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 5_000)
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        saved?.let { s -> runBlocking { prefs.update { s } } }
    }

    private fun shot(
        name: String,
        query: String,
    ) {
        compose.onNodeWithTag("query").performTextClearance()
        compose.onNodeWithTag("query").performTextInput(query)
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result"), 5_000)
        // Web suggestions arrive after a short pause.
        Thread.sleep(2_000)
        compose.waitForIdle()
        Screenshots.save(name, compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test
    fun searchSurface() {
        // Only built-in apps and settings match, so nothing personal shows.
        shot("search-clock", "clock")
        shot("search-answer", "5 km in miles")
        // Suggestions follow the device's region; keep this one about the inline switch.
        runBlocking { prefs.update { it.copy(webSuggestions = false) } }
        shot("search-toggle", "bluetooth")
    }
}
