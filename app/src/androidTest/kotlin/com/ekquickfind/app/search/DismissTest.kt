package com.ekquickfind.app.search

import androidx.activity.BackEventCompat
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ekquickfind.app.Screenshots
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class DismissTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var activity: SearchActivity

    @Inject lateinit var prefs: com.ekquickfind.data.prefs.PreferencesRepository
    private lateinit var savedPrefs: com.ekquickfind.data.prefs.UserPreferences

    @Before
    fun setUp() {
        hilt.inject()
        // Gestures depend on layout, so run on default settings and restore the owner's afterwards.
        savedPrefs = runBlocking { prefs.preferences.first() }
        runBlocking {
            prefs.update {
                com.ekquickfind.data.prefs
                    .UserPreferences(hiddenResults = it.hiddenResults)
            }
        }
        activity = SearchLauncher.launch()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 3_000)
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        runBlocking { prefs.update { savedPrefs } }
    }

    private fun awaitClosed() = compose.waitUntil(3_000) { activity.isFinishing || activity.isDestroyed }

    private fun imeVisible() =
        ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true

    @Test
    fun tappingTheScrimCloses() {
        // Top of the screen is scrim; the surface is anchored at the bottom.
        device.click(device.displayWidth / 2, device.displayHeight / 12)
        awaitClosed()
    }

    @Test
    fun theSurfaceAnimatesOutBeforeTheWindowCloses() {
        device.click(device.displayWidth / 2, device.displayHeight / 12)
        // The exit animation takes ~160 ms; the window must still be there right after the tap.
        Thread.sleep(40)
        assertFalse("closed without animating out", activity.isFinishing)
        awaitClosed()
    }

    @Test
    fun backCloses() {
        device.pressBack()
        // First back may only hide the keyboard.
        if (!activity.isFinishing) device.pressBack()
        awaitClosed()
    }

    @Test
    fun predictiveBackPreviewsAShrinkBeforeCommitting() {
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result"), 3_000)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val dispatcher = activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 1500f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(300f, 1500f, 0.8f, BackEventCompat.EDGE_LEFT))
        }
        compose.waitForIdle()
        Screenshots.screen("predictive-back-80pct")
        assertFalse("must not close before the gesture commits", activity.isFinishing)

        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        awaitClosed()
    }

    @Test
    fun cancellingPredictiveBackKeepsTheSurfaceOpen() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val dispatcher = activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 1500f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(200f, 1500f, 0.5f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackCancelled()
        }
        compose.waitForIdle()
        Thread.sleep(300)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun scrollingResultsUpHidesTheKeyboard() {
        compose.onNodeWithTag("query").performTextInput("a")
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("results", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { imeVisible() }

        // Real input through the system, like a finger; synthetic Compose swipes skip the IME path.
        val x = device.displayWidth / 2
        device.swipe(x, device.displayHeight * 45 / 100, x, device.displayHeight * 18 / 100, 15)

        compose.waitUntil(3_000) { !imeVisible() }
    }

    @Test
    fun swipingUpOnTheSelectedResultByTheBarHidesTheKeyboard() {
        runBlocking { prefs.update { it.copy(selectedAtBottom = true) } }
        compose.onNodeWithTag("query").performTextInput("a")
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("selected-slot", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { imeVisible() }

        val slot = compose.onNodeWithTag("selected-slot", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        val x = slot.center.x.toInt()
        val y = slot.center.y.toInt()
        device.swipe(x, y, x, y - device.displayHeight / 4, 15)

        compose.waitUntil(3_000) { !imeVisible() }
    }

    @Test
    fun pullingDownAtTheTopOfResultsCloses() {
        compose.onNodeWithTag("query").performTextInput("a")
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("results", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

        val x = device.displayWidth / 2
        device.swipe(x, device.displayHeight * 20 / 100, x, device.displayHeight * 55 / 100, 10)

        awaitClosed()
    }
}
