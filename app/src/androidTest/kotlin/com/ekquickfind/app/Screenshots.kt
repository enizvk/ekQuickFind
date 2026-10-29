package com.ekquickfind.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Saves review screenshots under the app's external files dir (pulled with adb after the run). */
object Screenshots {
    fun dir(): File =
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }

    fun save(
        name: String,
        bitmap: Bitmap,
    ): File = File(dir(), "$name.png").apply { outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }

    /** Captures the whole screen, including the blurred app behind a translucent window. */
    fun screen(name: String): File = save(name, InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
}
