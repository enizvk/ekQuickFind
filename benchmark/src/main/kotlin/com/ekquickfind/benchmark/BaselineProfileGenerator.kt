package com.ekquickfind.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the code paths of the hot journey: launch, type a few letters, open the top result. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
            pressHome()
            openSearch()
            type("cam")
            type("era")
            // Clear and try a calculation and a settings query so those paths are compiled too.
            repeat(6) { device.pressKeyCode(android.view.KeyEvent.KEYCODE_DEL) }
            type("12*8")
            repeat(4) { device.pressKeyCode(android.view.KeyEvent.KEYCODE_DEL) }
            type("wifi")
            device.pressKeyCode(android.view.KeyEvent.KEYCODE_ENTER)
            device.waitForIdle()
        }
}
