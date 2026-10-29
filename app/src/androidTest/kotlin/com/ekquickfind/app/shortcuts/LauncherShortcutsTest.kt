package com.ekquickfind.app.shortcuts

import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutManager
import android.os.Process
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.app.search.SearchActivity
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.providers.apps.AppsProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class LauncherShortcutsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject lateinit var history: HistoryRepository

    @Inject lateinit var apps: AppsProvider

    @Inject lateinit var zeroQuery: com.ekquickfind.app.search.ZeroQuerySource
    private val syncScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val shortcuts = context.getSystemService(ShortcutManager::class.java)
    private lateinit var savedPins: List<String>

    @Before
    fun setUp() {
        hilt.inject()
        savedPins = runBlocking { history.pins.first().map { it.resultId } }
        // The Hilt test application doesn't run EkQuickFindApp.onCreate, so start the same sync here.
        ShortcutSync(zeroQuery, ShortcutsPublisher(context)).start(syncScope)
        SearchLauncher.unlockScreen()
    }

    @After
    fun tearDown() {
        syncScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        SearchLauncher.finishAll()
        device.pressHome()
    }

    private fun waitFor(
        timeoutMs: Long = 8_000,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(200)
        }
        return false
    }

    private fun focusedPackage(pkg: String) =
        device
            .executeShellCommand(
                "dumpsys window",
            ).lines()
            .any { "mCurrentFocus" in it && pkg in it }

    @Test
    fun staticShortcutsAreRegistered() {
        val ids = shortcuts.manifestShortcuts.map { it.id }.toSet()
        assertTrue("got $ids", ids.containsAll(setOf("new_search", "settings")))
    }

    @Test
    fun settingsShortcutOpensSettings() {
        context.startActivity(
            Intent(
                "com.ekquickfind.app.OPEN_SETTINGS",
            ).setClass(context, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        compose.waitUntil(8_000) { compose.onAllNodesWithTag("settings", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun pinnedAppBecomesAShortcutThatOpensItAndUnpinningRemovesIt() {
        val calculator = runBlocking { apps.forPackages(listOf(calculatorPackage())).single() }
        val shortcutId = "pin:${calculator.id.value}"
        val alreadyPinned = calculator.id.value in savedPins
        try {
            runBlocking { history.pin(calculator) }
            assertTrue("shortcut not published", waitFor { shortcuts.dynamicShortcuts.any { it.id == shortcutId } })

            val intent =
                shortcuts.dynamicShortcuts
                    .single { it.id == shortcutId }
                    .intent!!
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            assertTrue("calculator didn't open", waitFor { focusedPackage(calculatorPackage()) })

            runBlocking { history.unpin(calculator.id) }
            assertTrue("shortcut not removed", waitFor { shortcuts.dynamicShortcuts.none { it.id == shortcutId } })
        } finally {
            // Leave the owner's pins exactly as they were.
            runBlocking { if (alreadyPinned) history.pin(calculator) else history.unpin(ResultId(calculator.id.value)) }
        }
    }

    private fun calculatorPackage(): String {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        return launcherApps
            .getActivityList(
                null,
                Process.myUserHandle(),
            ).first { it.label.toString() == "Calculator" }
            .componentName.packageName
    }
}
