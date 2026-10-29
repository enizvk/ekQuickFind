package com.ekquickfind.app.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SearchActivityTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var activity: SearchActivity

    @Before
    fun setUp() {
        device.wakeUp()
        activity = SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        device.pressHome()
    }

    @Test
    fun opensWithTheQueryFieldFocusedAndTheKeyboardShown() {
        compose.onNodeWithTag("query").assertIsFocused()
        compose.waitUntil(5_000) {
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
    }

    @Test
    fun typingShowsATopResultAndAnAppsSection() {
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result"), 3_000)
        // Section headers are plain text nodes; look them up in the unmerged tree.
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("section-Apps", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun goOnTheKeyboardLaunchesTheTopResultAndClosesSearch() {
        compose.onNodeWithTag("query").performTextInput("cam")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Camera")), 3_000)

        compose.onNodeWithTag("query").performImeAction()
        // The v2 rule queues UI coroutines until the test lets the UI go idle.
        compose.waitForIdle()

        // The camera hides from accessibility, so ask the window manager which window has focus.
        assertTrue("camera didn't open", waitForFocusedPackage(CAMERA_PACKAGE, 5_000))
        compose.waitUntil(3_000) { activity.isFinishing || activity.isDestroyed }
    }

    private fun waitForFocusedPackage(
        pkg: String,
        timeoutMs: Long,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (device.executeShellCommand("dumpsys window").lines().any { "mCurrentFocus" in it && pkg in it }) return true
            Thread.sleep(200)
        }
        return false
    }

    private fun hasAnyDescendantText(text: String) =
        androidx.compose.ui.test
            .hasAnyDescendant(hasText(text))

    private companion object {
        const val CAMERA_PACKAGE = "com.sec.android.app.camera"
    }
}
