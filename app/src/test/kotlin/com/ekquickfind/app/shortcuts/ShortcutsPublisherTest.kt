package com.ekquickfind.app.shortcuts

import android.content.Context
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ShortcutsPublisherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(ShortcutManager::class.java)

    private object Noop : Action {
        override val label = "Open"
        override val glyph: Glyph? = null

        override suspend fun perform(host: ActionHost) = ActionOutcome.Close
    }

    private fun pinned(title: String) =
        SearchResult(ResultId("apps:$title"), ProviderId("apps"), Section.Apps, title, null, ResultIcon.Vector(Glyph.Search), 1f, Noop)

    @Test
    fun publishesUpToThreePinnedResultsInPinOrder() {
        ShortcutsPublisher(context).publish(listOf(pinned("Calculator"), pinned("Maps"), pinned("Camera"), pinned("Slack")))
        val shortcuts = manager.dynamicShortcuts.sortedBy { it.rank }
        assertEquals(listOf("Calculator", "Maps", "Camera"), shortcuts.map { it.shortLabel.toString() })
    }

    @Test
    fun shortcutsOpenTheResultThroughSearchActivity() {
        ShortcutsPublisher(context).publish(listOf(pinned("Calculator")))
        val intent = manager.dynamicShortcuts.single().intent!!
        assertEquals(ShortcutsPublisher.ACTION_OPEN_RESULT, intent.action)
        assertEquals("apps:Calculator", intent.getStringExtra(ShortcutsPublisher.EXTRA_RESULT_ID))
        assertEquals("apps", intent.getStringExtra(ShortcutsPublisher.EXTRA_PROVIDER_ID))
    }

    @Test
    fun unpinningRemovesItsShortcut() {
        val publisher = ShortcutsPublisher(context)
        publisher.publish(listOf(pinned("Calculator"), pinned("Maps")))
        publisher.publish(listOf(pinned("Maps")))
        assertEquals(listOf("Maps"), manager.dynamicShortcuts.map { it.shortLabel.toString() })
    }
}
