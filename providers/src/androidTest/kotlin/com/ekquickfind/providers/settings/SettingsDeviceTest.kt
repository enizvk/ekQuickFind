package com.ekquickfind.providers.settings

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Reports which table entries this device lacks; every entry must still reach a screen through its fallback chain. */
    @Test
    fun everyEntryResolvesDirectlyOrThroughItsFallback() {
        val resolver = IntentResolver.of(context)
        val missing = SettingsTable.entries.filterNot { resolver.resolves(it.action) }
        Log.i("SettingsDeviceTest", "unresolvable on this device (${missing.size}): ${missing.joinToString { it.key }}")
        val dead = missing.filterNot { resolver.resolves(it.fallbackAction) }
        assertTrue("entries with no reachable screen: ${dead.map { it.key }}", dead.isEmpty())
    }

    @Test
    fun flashlightTogglesOnAndOff() {
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        val toggles = AndroidToggleStates(context)
        assertEquals(false, toggles.state(QuickToggle.Flashlight))
        try {
            toggles.set(QuickToggle.Flashlight, true)
            Thread.sleep(500)
            assertEquals(true, toggles.state(QuickToggle.Flashlight))
        } finally {
            toggles.set(QuickToggle.Flashlight, false)
        }
        Thread.sleep(500)
        assertEquals(false, toggles.state(QuickToggle.Flashlight))
    }
}
