package com.ekquickfind.app.screenshots

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.Screenshots
import com.ekquickfind.app.ui.search.SearchSurface
import com.ekquickfind.app.ui.theme.EkQuickFindTheme
import com.ekquickfind.core.search.SearchState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun render(
        name: String,
        state: SearchState = SampleStates.mixed,
        dark: Boolean,
        dynamic: Boolean,
        size: DpSize? = null,
        expanded: Boolean = false,
        blurAvailable: Boolean = true,
    ): Bitmap {
        compose.setContent {
            val content = @androidx.compose.runtime.Composable {
                EkQuickFindTheme(dark = dark, dynamicColor = dynamic) {
                    Box(Modifier.fillMaxSize()) {
                        SearchSurface(
                            query = state.query.raw,
                            onQueryChange = {},
                            engineState = state,
                            onSubmit = {},
                            onAction = { _, _ -> },
                            onDismiss = {},
                            expandedLayout = expanded,
                            blurAvailable = blurAvailable,
                        )
                    }
                }
            }
            if (size != null) DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) { content() } else content()
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        Screenshots.save(name, bitmap)
        return bitmap
    }

    /** Average luminance of a horizontal line through the results panel (just inside the surface's top edge). */
    private fun panelLuminance(bitmap: Bitmap): Double {
        val bounds = compose.onNodeWithTag("surface", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val y = (bounds.top + 40f).toInt().coerceIn(0, bitmap.height - 1)
        val samples = (bitmap.width / 4 until bitmap.width * 3 / 4 step 8).map { bitmap.getPixel(it, y) }
        return samples.map { c -> (0.2126 * ((c shr 16) and 0xff) + 0.7152 * ((c shr 8) and 0xff) + 0.0722 * (c and 0xff)) / 255 }.average()
    }

    @Test
    fun light() {
        val bmp = render("theme-light", dark = false, dynamic = false)
        assertTrue("light panel too dark: ${panelLuminance(bmp)}", panelLuminance(bmp) > 0.5)
    }

    @Test
    fun dark() {
        val bmp = render("theme-dark", dark = true, dynamic = false)
        assertTrue("dark panel too light: ${panelLuminance(bmp)}", panelLuminance(bmp) < 0.35)
    }

    @Test
    fun dynamicLight() {
        render("theme-dynamic-light", dark = false, dynamic = true)
    }

    @Test
    fun dynamicDark() {
        render("theme-dynamic-dark", dark = true, dynamic = true)
    }

    @Test
    fun loadingAppsSection() {
        render("loading-apps", state = SampleStates.loading, dark = true, dynamic = true)
    }

    @Test
    fun withoutBlurTheScrimDimsMoreToCompensate() {
        var blur by mutableStateOf(true)
        compose.setContent {
            EkQuickFindTheme(dark = true, dynamicColor = false) {
                SearchSurface(
                    query = "m",
                    onQueryChange = {},
                    // Short panel, so the top of the screen is pure scrim.
                    engineState = SampleStates.loading,
                    onSubmit = {},
                    onAction = { _, _ -> },
                    onDismiss = {},
                    blurAvailable = blur,
                )
            }
        }

        fun scrimLuminance(): Double {
            // Let the scrim fade-in finish before measuring.
            compose.mainClock.advanceTimeBy(1_000)
            compose.waitForIdle()
            val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
            // Top-left corner is scrim only; the surface sits at the bottom.
            val c = bmp.getPixel(bmp.width / 10, bmp.height / 20)
            return (0.2126 * ((c shr 16) and 0xff) + 0.7152 * ((c shr 8) and 0xff) + 0.0722 * (c and 0xff)) / 255
        }
        val withBlur = scrimLuminance()
        blur = false
        val withoutBlur = scrimLuminance()
        assertTrue("fallback should be darker: $withBlur -> $withoutBlur", withoutBlur < withBlur)
    }

    @Test
    fun tabletWidthUsesCentredDialog() {
        val bmp = render("tablet-expanded", dark = true, dynamic = true, size = DpSize(900.dp, 1200.dp), expanded = true)
        // In the centred layout the left edge at mid-height is scrim, not panel.
        val edge = bmp.getPixel(bmp.width / 20, bmp.height / 2)
        val centre = bmp.getPixel(bmp.width / 2, bmp.height / 2)
        assertTrue("expected scrim at edge and panel in centre", edge != centre)
    }
}
