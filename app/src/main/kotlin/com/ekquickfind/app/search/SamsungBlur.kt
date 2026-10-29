package com.ekquickfind.app.search

import android.graphics.Color
import android.util.Log
import android.view.View
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * One UI doesn't support Android's cross-window blur (FLAG_BLUR_BEHIND does nothing), but it has its
 * own real-time blur for what's behind a view: `View.semSetBlurInfo(SemBlurInfo)`. Not in the SDK, so
 * it's reached by reflection and simply reports unavailable on other phones.
 */
internal object SamsungBlur {
    private class Api(
        val builder: Constructor<*>,
        val windowMode: Int,
        val setRadius: Method,
        val setBackgroundColor: Method,
        val build: Method,
        val setBlurInfo: Method,
    )

    private val api: Api? by lazy {
        runCatching {
            val info = Class.forName("android.view.SemBlurInfo")
            val builder = Class.forName("android.view.SemBlurInfo\$Builder")
            Api(
                builder = builder.getConstructor(Int::class.javaPrimitiveType),
                windowMode = info.getField("BLUR_MODE_WINDOW").getInt(null),
                setRadius = builder.getMethod("setRadius", Int::class.javaPrimitiveType),
                setBackgroundColor = builder.getMethod("setBackgroundColor", Int::class.javaPrimitiveType),
                build = builder.getMethod("build"),
                setBlurInfo = View::class.java.getMethod("semSetBlurInfo", info),
            )
        }.onFailure { Log.d(TAG, "Samsung blur not available: $it") }
            .getOrNull()
    }

    val isAvailable: Boolean get() = api != null

    /** Blurs everything behind [view] by [radiusPx]; 0 removes the blur. Returns false if the call failed. */
    fun apply(
        view: View,
        radiusPx: Int,
    ): Boolean {
        val api = api ?: return false
        return runCatching {
            val info =
                if (radiusPx > 0) {
                    val builder = api.builder.newInstance(api.windowMode)
                    api.setRadius.invoke(builder, radiusPx)
                    // The dim is drawn by the surface itself, so the blur layer stays untinted.
                    api.setBackgroundColor.invoke(builder, Color.TRANSPARENT)
                    api.build.invoke(builder)
                } else {
                    null
                }
            api.setBlurInfo.invoke(view, info)
            true
        }.onFailure { Log.w(TAG, "Samsung blur failed", it) }
            .getOrDefault(false)
    }

    private const val TAG = "SamsungBlur"
}
