package com.ekquickfind.app

import android.app.Activity
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.ekquickfind.app.search.SearchActivity

/**
 * ActivityScenario can't drive a singleInstance activity (it reclaims focus from the new task),
 * so tests start the search surface with a plain intent and look it up afterwards.
 */
object SearchLauncher {
    fun launch(configure: Intent.() -> Unit = {}): SearchActivity {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        unlockScreen()
        val intent =
            Intent(instrumentation.targetContext, SearchActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .apply(configure)
        instrumentation.targetContext.startActivity(intent)
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            resumed()?.let { return it }
            Thread.sleep(50)
        }
        error("SearchActivity did not resume")
    }

    /** Wakes the device and dismisses a non-secure lock screen so the activity can resume. */
    fun unlockScreen() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf("input keyevent KEYCODE_WAKEUP", "wm dismiss-keyguard").forEach { cmd ->
            automation.executeShellCommand(cmd).close()
        }
        Thread.sleep(300)
    }

    fun finishAll() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val registry = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STARTED, Stage.STOPPED, Stage.CREATED)
                .flatMap { registry.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
        Thread.sleep(500)
    }

    fun resumed(): SearchActivity? {
        var found: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull { it is SearchActivity }
        }
        return found as SearchActivity?
    }
}
