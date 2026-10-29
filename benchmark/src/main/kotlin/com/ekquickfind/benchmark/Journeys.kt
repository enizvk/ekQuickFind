package com.ekquickfind.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE = "com.ekquickfind.app"

/** Opens search and waits until the query field is on screen. */
fun MacrobenchmarkScope.openSearch() {
    startActivityAndWait()
    device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 5_000)
}

/** Types a query one key at a time, like a person would, and waits for results. */
fun MacrobenchmarkScope.type(text: String) {
    text.forEach { c ->
        device.executeShellCommand("input text $c")
        Thread.sleep(120)
    }
    device.waitForIdle()
}
