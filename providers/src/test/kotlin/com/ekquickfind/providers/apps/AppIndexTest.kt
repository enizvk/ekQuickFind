package com.ekquickfind.providers.apps

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppIndexTest {
    @Test
    fun startsLoadingThenBuildsInBackground() =
        runTest {
            val source = FakeAppsSource(apps = listOf(app("Maps"), app("Gmail")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))

            assertEquals(AppIndexState.Loading, index.state.value)
            runCurrent()

            val ready = index.state.value as AppIndexState.Ready
            assertEquals(listOf("Maps", "Gmail"), ready.apps.map { it.entry.label })
        }

    @Test
    fun rebuildsWhenAPackageIsAdded() =
        runTest {
            val source = FakeAppsSource(apps = listOf(app("Maps")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()

            source.apps = listOf(app("Maps"), app("Signal"))
            source.changeEvents.emit(Unit)
            runCurrent()

            assertEquals(listOf("Maps", "Signal"), index.readyApps().map { it.entry.label })
        }

    @Test
    fun rebuildsWhenAPackageIsRemoved() =
        runTest {
            val source = FakeAppsSource(apps = listOf(app("Maps"), app("Signal")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()

            source.apps = listOf(app("Maps"))
            source.changeEvents.emit(Unit)
            runCurrent()

            assertEquals(listOf("Maps"), index.readyApps().map { it.entry.label })
        }

    @Test
    fun picksUpLabelChanges() =
        runTest {
            val source = FakeAppsSource(apps = listOf(app("Hangouts", pkg = "com.google.talk")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()

            source.apps = listOf(app("Google Chat", pkg = "com.google.talk"))
            source.changeEvents.emit(Unit)
            runCurrent()

            val entry = index.readyApps().single()
            assertEquals("Google Chat", entry.entry.label)
            assertEquals("google chat", entry.label.normalized)
        }

    @Test
    fun includesShortcutsOnlyWithAccess() =
        runTest {
            val shortcut = ShortcutEntry("com.android.chrome", "new_tab", "New tab", "Chrome", 0, null)
            val source = FakeAppsSource(apps = listOf(app("Chrome")), shortcuts = listOf(shortcut), shortcutAccess = false)
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            runCurrent()
            assertTrue((index.state.value as AppIndexState.Ready).shortcuts.isEmpty())

            source.shortcutAccess = true
            source.changeEvents.emit(Unit)
            runCurrent()
            assertEquals(listOf("New tab"), (index.state.value as AppIndexState.Ready).shortcuts.map { it.entry.label })
        }

    @Test
    fun awaitReadySuspendsUntilBuilt() =
        runTest {
            val source = FakeAppsSource(apps = listOf(app("Maps")))
            val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
            val ready = index.state.first { it is AppIndexState.Ready }
            assertTrue(ready is AppIndexState.Ready)
        }

    private fun AppIndex.readyApps() = (state.value as AppIndexState.Ready).apps
}
