package com.ekquickfind.app.search

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ContactsPermissionTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        assumeTrue(
            "contacts already granted on this device",
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED,
        )
        SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        device.pressBack()
        SearchLauncher.finishAll()
    }

    private fun waitForFocus(pattern: Regex): Boolean {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (device
                    .executeShellCommand(
                        "dumpsys window",
                    ).lines()
                    .any { "mCurrentFocus" in it && pattern.containsMatchIn(it) }
            ) {
                return true
            }
            Thread.sleep(200)
        }
        return false
    }

    @Test
    fun tappingTheEnableRowAsksForContactsPermission() {
        // A query nothing else matches, so the enable row is on screen.
        compose.onNodeWithTag("query").performTextInput("zzqqx")
        compose.waitUntilAtLeastOneExists(hasText("Enable contacts search"), 3_000)

        compose.onNodeWithText("Enable contacts search").performClick()
        compose.waitForIdle()

        // Either the system permission dialog or, after a permanent denial, the app's settings page.
        val permissionUi = Regex("com\\.(google\\.)?android\\.(permissioncontroller|settings)")
        assertTrue("no permission UI appeared", waitForFocus(permissionUi))
    }
}
