package com.ekquickfind.app.tile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.data.prefs.PreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/** Adds the tile through the shell, taps it, and removes it again, so the device's Quick Settings end up unchanged. */
@HiltAndroidTest
class SearchTileTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var prefs: PreferencesRepository

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val component = "com.ekquickfind.app/com.ekquickfind.app.tile.SearchTileService"
    private lateinit var savedTiles: String

    @Before
    fun setUp() {
        hilt.inject()
        SearchLauncher.unlockScreen()
        // Snapshot the owner's Quick Settings layout so it can be put back exactly.
        savedTiles = device.executeShellCommand("settings get secure sysui_qs_tiles").trim()
        device.executeShellCommand("cmd statusbar add-tile $component")
    }

    @After
    fun tearDown() {
        // One UI drops our tile from direct layout writes, so never remove it if the owner had it.
        if (!savedTiles.contains("SearchTileService")) device.executeShellCommand("cmd statusbar remove-tile $component")
        if (savedTiles.isNotBlank() && savedTiles != "null") device.executeShellCommand("settings put secure sysui_qs_tiles $savedTiles")
        // SystemUI binds and unbinds tiles asynchronously; let it settle before the next test.
        Thread.sleep(1_500)
        device.executeShellCommand("cmd statusbar collapse")
        SearchLauncher.finishAll()
        device.pressHome()
    }

    private fun waitFor(
        timeoutMs: Long = 10_000,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(150)
        }
        return false
    }

    @Test
    fun tappingTheTileOpensSearch() {
        device.pressHome()
        device.executeShellCommand("cmd statusbar click-tile $component")
        assertTrue("search didn't open from the tile", waitFor { SearchLauncher.resumed() != null })
    }
}
