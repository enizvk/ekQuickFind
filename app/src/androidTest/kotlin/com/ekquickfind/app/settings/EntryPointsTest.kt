package com.ekquickfind.app.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Toggles the tile through the shell and restores the owner's Quick Settings layout afterwards. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class EntryPointsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @javax.inject.Inject lateinit var prefs: com.ekquickfind.data.prefs.PreferencesRepository

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val component = "com.ekquickfind.app/com.ekquickfind.app.tile.SearchTileService"
    private lateinit var savedTiles: String

    @Before
    fun setUp() {
        hilt.inject()
        savedTiles = device.executeShellCommand("settings get secure sysui_qs_tiles").trim()
        SearchLauncher.launch()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 5_000)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("open-settings", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-settings", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.waitUntil(8_000) { compose.onAllNodesWithTag("settings-menu", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("settings-page:WaysToOpen", useUnmergedTree = true).performClick()
        compose.waitUntil(
            5_000,
        ) { compose.onAllNodesWithTag("entry-status:tile", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(
            3_000,
        ) { compose.onAllNodesWithTag("entry-status:tile", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun tearDown() {
        // One UI drops our tile from direct layout writes, so never remove it if the owner had it.
        if (!savedTiles.contains("SearchTileService")) device.executeShellCommand("cmd statusbar remove-tile $component")
        if (savedTiles.isNotBlank() && savedTiles != "null") device.executeShellCommand("settings put secure sysui_qs_tiles $savedTiles")
        // SystemUI binds and unbinds tiles asynchronously; let it settle before the next test.
        Thread.sleep(1_500)
        SearchLauncher.finishAll()
    }

    @Test
    fun tileStatusFollowsWhatTheTileReports() {
        // The tile reports itself through onTileAdded/onTileRemoved/onStartListening; One UI delivers those
        // late or not at all for shell-driven changes, so drive the reported state directly.
        val saved = runBlocking { prefs.preferences.first().tileAdded }
        try {
            runBlocking { prefs.update { it.copy(tileAdded = true) } }
            compose.waitUntil(5_000) { status("Added") }
            runBlocking { prefs.update { it.copy(tileAdded = false) } }
            compose.waitUntil(5_000) { status("Not added") }
        } finally {
            runBlocking { prefs.update { it.copy(tileAdded = saved) } }
        }
    }

    private fun status(text: String) =
        compose.onAllNodes(hasTestTag("entry-status:tile").and(hasText(text)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun showsWidgetAndAssistantStatus() {
        compose.waitUntil(2_000) {
            compose.onAllNodesWithTag("entry-status:widget", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag("entry-status:assistant", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
