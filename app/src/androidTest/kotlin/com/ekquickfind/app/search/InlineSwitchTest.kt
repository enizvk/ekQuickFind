package com.ekquickfind.app.search

import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class InlineSwitchTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val camera = context.getSystemService(CameraManager::class.java)

    @Volatile private var torchOn = false
    private val callback =
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(
                cameraId: String,
                enabled: Boolean,
            ) {
                torchOn = enabled
            }
        }

    @Before
    fun setUp() {
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        camera.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        // Never leave the torch on.
        runCatching { camera.cameraIdList.forEach { camera.setTorchMode(it, false) } }
        camera.unregisterTorchCallback(callback)
        SearchLauncher.finishAll()
    }

    private val switchTag = "switch:settings:toggle:Flashlight"

    @Test
    fun theFlashlightSwitchTurnsTheTorchOnAndOffWithoutLeavingSearch() {
        compose.onNodeWithTag("query").performTextInput("torch")
        compose.waitUntil(3_000) { compose.onAllNodesWithTag(switchTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag(switchTag, useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) { torchOn }
        assertFalse("search closed", SearchLauncher.resumed() == null)

        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    hasTestTag(switchTag).and(
                        androidx.compose.ui.test
                            .isOn(),
                    ),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag(switchTag, useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) { !torchOn }
    }
}
