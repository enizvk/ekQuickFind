package com.ekquickfind.app.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.UserPreferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ExcludedAppsUiTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: PreferencesRepository
    private lateinit var saved: UserPreferences

    @Before
    fun setUp() {
        hilt.inject()
        saved = runBlocking { prefs.preferences.first() }
        runBlocking { prefs.update { com.ekquickfind.app.testBaseline(it) } }
        SearchLauncher.launch()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 5_000)
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        runBlocking { prefs.update { saved } }
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun back() {
        val activity = SearchLauncher.resumed() ?: return
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun calculatorShown(): Boolean {
        compose.onNodeWithTag("query").performTextInput("calculator")
        Thread.sleep(1_200)
        compose.waitForIdle()
        return compose
            .onAllNodes(
                hasTestTag("top-result").and(hasText("Calculator")),
                useUnmergedTree = false,
            ).fetchSemanticsNodes()
            .isNotEmpty()
    }

    @Test
    fun excludingAnAppRemovesItFromResultsAndIncludingBringsItBack() {
        compose.waitUntil(5_000) { exists("open-settings") }
        compose.onNodeWithTag("open-settings", useUnmergedTree = true).performClick()
        compose.waitUntil(8_000) { exists("settings-page:Sources") }
        compose.onNodeWithTag("settings-page:Sources", useUnmergedTree = true).performClick()
        compose.waitUntil(8_000) { exists("open-excluded-apps") }
        compose.onNodeWithTag("open-excluded-apps", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { exists("exclude-filter") }
        compose.onNodeWithTag("exclude-filter").performTextInput("calculator")
        compose.waitUntil(5_000) { exists("exclude:Calculator") }
        compose.onNodeWithTag("exclude:Calculator", useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) {
            runBlocking {
                prefs.preferences
                    .first()
                    .hiddenResults.values
                    .contains("Calculator")
            }
        }

        back()
        compose.waitUntil(3_000) { exists("settings") }
        back()
        compose.waitUntil(3_000) { exists("query") }
        assertTrue("excluded app still shown", !calculatorShown())

        runBlocking { prefs.update { it.copy(hiddenResults = it.hiddenResults.filterValues { v -> v != "Calculator" }) } }
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    hasTestTag("top-result").and(hasText("Calculator")),
                    useUnmergedTree = false,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
