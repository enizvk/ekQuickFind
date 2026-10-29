package com.ekquickfind.benchmark

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real minified (R8) build through UI Automator, since Hilt-based UI tests need
 * unobfuscated hooks. Catches classes or serializers that R8 stripped by mistake.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
    fun open() {
        device.wakeUp()
        device.pressHome()
        val intent =
            instrumentation.context.packageManager
                .getLaunchIntentForPackage(PACKAGE)!!
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.context.startActivity(intent)
        assertTrue("search didn't open", device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 5_000))
        device.waitForIdle()
    }

    @After
    fun close() {
        device.pressHome()
    }

    private fun type(text: String) {
        device.executeShellCommand("input text $text")
        device.waitForIdle()
    }

    @Test
    fun calculatorAnswersInline() {
        type("12*8")
        assertNotNull("no answer", device.wait(Until.findObject(By.text("96")), 3_000))
    }

    @Test
    fun appsSettingsAndWebResultsAppear() {
        type("wifi")
        assertNotNull("no settings result", device.wait(Until.findObject(By.textContains("Network & internet")), 3_000))
        assertNotNull(
            "no web fallback",
            device.wait(Until.findObject(By.textContains("Search the web")), 4_000) ?: device.findObject(By.text("Web")),
        )
    }

    @Test
    fun settingsScreenOpens() {
        val gear = device.wait(Until.findObject(By.desc("ekQuickFind settings")), 3_000)
        assertNotNull("no settings button", gear)
        gear.click()
        assertNotNull("settings didn't open", device.wait(Until.findObject(By.text("Search sources")), 3_000))
        device.pressBack()
    }
}
