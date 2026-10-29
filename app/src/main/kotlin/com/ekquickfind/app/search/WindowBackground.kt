package com.ekquickfind.app.search

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import com.ekquickfind.app.ui.theme.SurfaceStyle
import com.ekquickfind.data.prefs.BackgroundStyle
import java.util.function.Consumer

/**
 * Applies the chosen background to the activity window at runtime (so it can change without
 * recreating the activity) and tracks whether the system currently allows cross-window blur.
 */
class WindowBackground(
    private val activity: Activity,
) {
    private val windowManager = activity.getSystemService(WindowManager::class.java)

    /** Observed by Compose: false in power saving or when the OEM disables blur. */
    val blurAvailable = mutableStateOf(windowManager.isCrossWindowBlurEnabled || SamsungBlur.isAvailable)

    private val listener = Consumer<Boolean> { blurAvailable.value = it || SamsungBlur.isAvailable }
    private var radiusPx = 0

    fun start() = windowManager.addCrossWindowBlurEnabledListener(activity.mainExecutor, listener)

    fun stop() = windowManager.removeCrossWindowBlurEnabledListener(listener)

    fun apply(
        style: SurfaceStyle,
        blurEnabled: Boolean = true,
    ) {
        val radiusPx = (style.blurRadius.value * activity.resources.displayMetrics.density).toInt()
        val wantsBlur = blurEnabled && style.background != BackgroundStyle.Solid && radiusPx > 0
        this.radiusPx = if (wantsBlur) radiusPx else 0
        // Android's own blur where the system supports it, otherwise One UI's.
        if (!windowManager.isCrossWindowBlurEnabled && SamsungBlur.apply(activity.window.decorView, this.radiusPx)) return
        activity.window.apply {
            if (wantsBlur) {
                addFlags(
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                )
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
            attributes = attributes.apply { blurBehindRadius = if (wantsBlur) radiusPx else 0 }
        }
    }

    /** What the window is doing right now; used by tests. */
    fun currentBlurRadius(): Int = if (windowManager.isCrossWindowBlurEnabled) activity.window.attributes.blurBehindRadius else radiusPx
}
