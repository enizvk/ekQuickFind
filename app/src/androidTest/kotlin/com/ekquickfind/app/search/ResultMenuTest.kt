package com.ekquickfind.app.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.data.prefs.PreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ResultMenuTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: PreferencesRepository
    private lateinit var hiddenBefore: Set<String>

    @Before
    fun setUp() {
        hilt.inject()
        hiddenBefore =
            runBlocking {
                prefs.preferences
                    .first()
                    .hiddenResults.keys
            }
        SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        // Leave the device's hidden-apps list exactly as it was.
        runBlocking {
            prefs.preferences
                .first()
                .hiddenResults.keys
                .filter { it !in hiddenBefore }
                .forEach { prefs.unhide(it) }
        }
        SearchLauncher.finishAll()
    }

    @Test
    fun longPressShowsAppActionsAndPin() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)

        compose.onNodeWithTag("top-result").performTouchInput { longClick() }

        listOf("App info", "Hide", "Pin").forEach { compose.waitUntilAtLeastOneExists(hasText(it), 2_000) }
    }

    @Test
    fun hideRemovesTheRow() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)

        compose.onNodeWithTag("top-result").performTouchInput { longClick() }
        compose.waitUntilAtLeastOneExists(hasText("Hide"), 2_000)
        compose.onNodeWithText("Hide").performClick()
        compose.waitForIdle()

        compose.waitUntilDoesNotExist(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
    }
}
