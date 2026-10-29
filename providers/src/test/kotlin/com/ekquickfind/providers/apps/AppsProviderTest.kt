package com.ekquickfind.providers.apps

import com.ekquickfind.core.search.HideAction
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsProviderTest {
    private val chrome = app("Chrome", pkg = "com.android.chrome")
    private val settings = app("Settings", pkg = "com.android.settings", system = true)
    private val slack = app("Slack", pkg = "com.slack", badge = "Work")
    private val gmail = app("Gmail", pkg = "com.google.android.gm")
    private val maps = app("Google Maps", pkg = "com.google.android.apps.maps")

    private fun kotlinx.coroutines.test.TestScope.provider(
        source: FakeAppsSource = FakeAppsSource(apps = listOf(chrome, settings, slack, gmail, maps)),
        hidden: Set<ResultId> = emptySet(),
    ): AppsProvider {
        val index = AppIndex(source, backgroundScope, StandardTestDispatcher(testScheduler))
        return AppsProvider(index) { hidden }
    }

    private suspend fun AppsProvider.titles(q: String) = search(Query(q)).first().map { it.title }

    @Test
    fun matchesByLabel() =
        runTest {
            // Gmail by its name, Google Maps by its initials.
            assertEquals(listOf("Gmail", "Google Maps"), provider().titles("gm"))
        }

    @Test
    fun matchesByPackageName() =
        runTest {
            assertEquals(listOf("Chrome"), provider().titles("com.android.chrome"))
        }

    @Test
    fun workProfileAppsCarryABadgeAndTheirOwnProfile() =
        runTest {
            val result = provider().search(Query("slack")).first().single()
            assertEquals("Work", result.badge)
            assertEquals(ResultIcon.PackageIcon("com.slack", "com.slack.Main", 10), result.icon)
        }

    @Test
    fun hiddenAppsAreExcluded() =
        runTest {
            val provider = provider(hidden = setOf(AppsProvider.resultId(chrome)))
            assertTrue(provider.titles("chrome").isEmpty())
        }

    @Test
    fun shortcutsAppearAsLabelDotAppWhenAccessIsGranted() =
        runTest {
            val newTab = ShortcutEntry("com.android.chrome", "new_tab", "New tab", "Chrome", 0, null)
            val source = FakeAppsSource(apps = listOf(chrome), shortcuts = listOf(newTab), shortcutAccess = true)
            assertEquals(listOf("New tab · Chrome"), provider(source).titles("new tab"))
        }

    @Test
    fun shortcutsAreHiddenWithoutAccess() =
        runTest {
            val newTab = ShortcutEntry("com.android.chrome", "new_tab", "New tab", "Chrome", 0, null)
            val source = FakeAppsSource(apps = listOf(chrome), shortcuts = listOf(newTab), shortcutAccess = false)
            assertTrue(provider(source).titles("new tab").isEmpty())
        }

    @Test
    fun waitsForTheIndexBeforeAnswering() =
        runTest {
            val provider = provider()
            val answer = async { provider.titles("chrome") }
            assertFalse(answer.isCompleted)
            runCurrent()
            assertEquals(listOf("Chrome"), answer.await())
        }

    @Test
    fun userAppsOfferAppInfoUninstallAndHide() =
        runTest {
            val actions =
                provider()
                    .search(Query("chrome"))
                    .first()
                    .single()
                    .secondaryActions
            assertEquals(listOf("App info", "Uninstall", "Hide"), actions.map { it.label })
            assertTrue(actions.last() is HideAction)
        }

    @Test
    fun systemAndWorkAppsCannotBeUninstalledFromHere() =
        runTest {
            val provider = provider()
            listOf("settings", "slack").forEach { q ->
                val labels =
                    provider
                        .search(Query(q))
                        .first()
                        .single()
                        .secondaryActions
                        .map { it.label }
                assertFalse("$q offers uninstall: $labels", "Uninstall" in labels)
            }
        }

    @Test
    fun resultIdsAreStableAndProfileSpecific() {
        assertEquals(ResultId("apps:com.slack/com.slack.Main@10"), AppsProvider.resultId(slack))
    }
}
