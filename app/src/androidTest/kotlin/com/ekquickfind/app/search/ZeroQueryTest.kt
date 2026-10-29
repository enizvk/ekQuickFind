package com.ekquickfind.app.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.data.history.HistoryRepository
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
class ZeroQueryTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var history: HistoryRepository

    @Inject lateinit var prefs: com.ekquickfind.data.prefs.PreferencesRepository
    private lateinit var savedPrefs: com.ekquickfind.data.prefs.UserPreferences

    private fun wipe() =
        runBlocking {
            history.clear()
            history.pins.first().forEach { history.unpin(ResultId(it.resultId)) }
        }

    @Before
    fun setUp() {
        hilt.inject()
        // Run on default settings (the device owner's own choices are restored afterwards).
        savedPrefs = runBlocking { prefs.preferences.first() }
        runBlocking {
            prefs.update {
                com.ekquickfind.data.prefs
                    .UserPreferences(hiddenResults = it.hiddenResults)
            }
        }
        wipe()
        SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        wipe()
        runBlocking { prefs.update { savedPrefs } }
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun freshInstallShowsTheHint() {
        compose.waitUntilAtLeastOneExists(hasText("Type to search", substring = true), 3_000)
    }

    @Test
    fun pinningShowsTheResultUnderPinnedAndUnpinningRemovesIt() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
        compose.onNodeWithTag("top-result").performTouchInput { longClick() }
        compose.waitUntilAtLeastOneExists(hasText("Pin"), 2_000)
        compose.onNodeWithText("Pin").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("query").performTextClearance()
        compose.waitUntil(3_000) { exists("zero-Pinned") }
        compose.waitUntilAtLeastOneExists(hasText("Calculator"), 2_000)

        compose.onNodeWithText("Calculator").performTouchInput { longClick() }
        compose.waitUntilAtLeastOneExists(hasText("Unpin"), 2_000)
        compose.onNodeWithText("Unpin").performClick()
        compose.waitForIdle()

        compose.waitUntil(3_000) { !exists("zero-Pinned") }
    }

    /** Top result for [text], taken from the view model so nothing is actually launched. */
    private fun topResultFor(text: String): com.ekquickfind.core.search.SearchResult {
        compose.onNodeWithTag("query").performTextClearance()
        compose.onNodeWithTag("query").performTextInput(text)
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result"), 3_000)
        compose.waitForIdle()
        val vm = androidx.lifecycle.ViewModelProvider(SearchLauncher.resumed()!!)[SearchViewModel::class.java]
        return vm.state.value.top!!
            .result
    }

    @Test
    fun clearHistoryEmptiesRecentsButKeepsPins() {
        val calculator = topResultFor("calculator")
        val clock = topResultFor("clock")
        runBlocking {
            history.recordLaunch(calculator)
            history.pin(clock)
        }
        compose.onNodeWithTag("query").performTextClearance()
        compose.waitUntil(3_000) { exists("zero-Recent") && exists("zero-Pinned") }

        compose.onNodeWithTag("clear-history", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { exists("confirm-clear") }
        compose.onNodeWithTag("confirm-clear", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        compose.waitUntil(3_000) { !exists("zero-Recent") }
        assertTrue(exists("zero-Pinned"))
    }

    @Test
    fun pinnedResultsCarryAPinMarkUntilUnpinned() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
        compose.onNodeWithTag("top-result").performTouchInput { longClick() }
        compose.waitUntilAtLeastOneExists(hasText("Pin"), 2_000)
        compose.onNodeWithText("Pin").performClick()
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("pin-mark:apps:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }

        compose.onNodeWithTag("top-result").performTouchInput { longClick() }
        compose.waitUntilAtLeastOneExists(hasText("Unpin"), 2_000)
        compose.onNodeWithText("Unpin").performClick()
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("pin-mark:apps:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isEmpty()
        }
    }
}
