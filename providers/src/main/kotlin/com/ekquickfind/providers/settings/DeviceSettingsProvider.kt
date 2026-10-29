package com.ekquickfind.providers.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.ToggleSpec
import com.ekquickfind.providers.HandlerResolver
import com.ekquickfind.providers.android
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

fun interface IntentResolver {
    fun resolves(action: String): Boolean

    companion object {
        /** Cached: the set of Settings screens doesn't change while the app runs. */
        fun of(context: Context): IntentResolver {
            val cache = ConcurrentHashMap<String, Boolean>()
            return IntentResolver { action ->
                cache.getOrPut(action) {
                    context.packageManager.resolveActivity(Intent(action), PackageManager.MATCH_DEFAULT_ONLY) != null
                }
            }
        }
    }
}

/** Opens the first screen in the chain that exists on this device. */
data class OpenSettingsAction(
    val chain: List<String>,
    private val resolver: IntentResolver,
    override val label: String = "Open",
) : Action {
    override val glyph = Glyph.Settings

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val action = chain.firstOrNull(resolver::resolves) ?: Settings.ACTION_SETTINGS
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (host.android().startActivity(intent)) ActionOutcome.Close else ActionOutcome.Stay
    }
}

/** Elapsed-time source for [FlipToggleAction]; an object so actions stay equal across result builds. */
fun interface ElapsedClock {
    fun now(): Long
}

object SystemElapsedClock : ElapsedClock {
    override fun now() = android.os.SystemClock.elapsedRealtime()
}

/** Flips a toggle in place, asks the system, asks for access, or opens the system panel, depending on what Android allows. */
data class FlipToggleAction(
    val toggle: QuickToggle,
    private val toggles: ToggleStates,
    private val resolver: IntentResolver,
    private val clock: ElapsedClock = SystemElapsedClock,
) : Action {
    override val label = "Switch"
    override val glyph = Glyph.Toggle

    override suspend fun perform(host: ActionHost): ActionOutcome =
        when (toggles.control(toggle)) {
            ToggleControl.Direct -> {
                toggles.set(toggle, toggles.state(toggle) != true)
                ActionOutcome.Refresh
            }
            ToggleControl.NeedsAccess -> {
                toggles.accessIntent(toggle)?.let { host.android().startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                ActionOutcome.Stay
            }
            ToggleControl.NeedsPermission ->
                if (host.android().requestPermissions(toggles.permissions(toggle))) confirm(host) else ActionOutcome.Stay
            ToggleControl.Confirm -> confirm(host)
            ToggleControl.SystemOnly -> openSettings(host)
        }

    private suspend fun confirm(host: ActionHost): ActionOutcome {
        val target = toggles.state(toggle) != true
        val intent = toggles.confirmIntent(toggle, target) ?: return openSettings(host)
        val started = clock.now()
        val result = host.android().startForResult(intent) ?: return openSettings(host)
        if (result == Activity.RESULT_OK) {
            // The radio takes a moment to change after Allow; wait so the switch shows the new state.
            repeat(STATE_POLLS) {
                if (toggles.state(toggle) == target) return ActionOutcome.Refresh
                delay(STATE_POLL_MS)
            }
            return ActionOutcome.Refresh
        }
        // Finishing almost instantly means the system refused without showing a dialog.
        return if (clock.now() - started < NO_DIALOG_MS) openSettings(host) else ActionOutcome.Stay
    }

    // Panels open over the search surface, so keep it open underneath.
    private fun openSettings(host: ActionHost): ActionOutcome {
        val action = listOfNotNull(toggle.action, Settings.ACTION_SETTINGS).first(resolver::resolves)
        host.android().startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ActionOutcome.Stay
    }

    private companion object {
        const val NO_DIALOG_MS = 400L
        const val STATE_POLLS = 30
        const val STATE_POLL_MS = 100L
    }
}

class DeviceSettingsProvider(
    entries: List<SettingsEntry>,
    private val resolver: IntentResolver,
    private val toggles: ToggleStates,
    private val handlers: HandlerResolver = HandlerResolver.None,
) : SearchProvider,
    ResultResolver {
    override val id = ID
    override val section = Section.Settings

    private class Indexed(
        val entry: SettingsEntry,
        val title: MatchTarget,
        val keywords: List<MatchTarget>,
    )

    private val settingsApp by lazy { handlers.packageFor(Intent(Settings.ACTION_SETTINGS)) }

    private val index = entries.map { Indexed(it, MatchTarget(it.title), it.keywords.map(::MatchTarget)) }
    private val toggleIndex = QuickToggle.entries.associateWith { t -> (listOf(t.title) + t.keywords).map(::MatchTarget) }

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val q = query.normalized
            val toggleRows =
                toggleIndex.mapNotNull { (toggle, targets) ->
                    val score = QueryMatcher.bestScore(q, targets, query.exact) ?: return@mapNotNull null
                    val state = toggles.state(toggle) ?: return@mapNotNull null
                    toggleRow(toggle, state, (score + TOGGLE_BONUS).coerceAtMost(1f))
                }
            val screens =
                index.mapNotNull { item ->
                    val titleScore = QueryMatcher.score(q, item.title, query.exact)
                    val keywordScore = QueryMatcher.bestScore(q, item.keywords, query.exact)?.times(KEYWORD_WEIGHT)
                    val score = listOfNotNull(titleScore, keywordScore).maxOrNull() ?: return@mapNotNull null
                    screenRow(item.entry, score)
                }
            emit((toggleRows + screens).sortedByDescending { it.score }.take(MAX_RESULTS))
        }

    override suspend fun resolve(id: ResultId): SearchResult? {
        val key = id.value.removePrefix("settings:")
        if (key.startsWith("toggle:")) {
            val toggle = QuickToggle.entries.firstOrNull { it.name == key.removePrefix("toggle:") } ?: return null
            val state = toggles.state(toggle) ?: return null
            return toggleRow(toggle, state, 1f)
        }
        return index.firstOrNull { it.entry.key == key }?.let { screenRow(it.entry, 1f) }
    }

    private fun screenRow(
        entry: SettingsEntry,
        score: Float,
    ) = SearchResult(
        id = ResultId("settings:${entry.key}"),
        providerId = ID,
        section = section,
        title = entry.title,
        subtitle = entry.breadcrumb,
        icon = ResultIcon.Vector(Glyph.Settings),
        score = score,
        primaryAction = OpenSettingsAction(listOf(entry.action, entry.fallbackAction, Settings.ACTION_SETTINGS).distinct(), resolver),
        handler = settingsApp,
    )

    private fun toggleRow(
        toggle: QuickToggle,
        on: Boolean,
        score: Float,
    ) = SearchResult(
        id = ResultId("settings:toggle:${toggle.name}"),
        providerId = ID,
        section = section,
        title = toggle.title,
        subtitle = if (on) "On" else "Off",
        icon = ResultIcon.Vector(if (toggle == QuickToggle.Flashlight) Glyph.Flashlight else Glyph.Toggle),
        score = score,
        primaryAction =
            toggle.action?.let { OpenSettingsAction(listOf(it, Settings.ACTION_SETTINGS), resolver) }
                ?: FlipToggleAction(toggle, toggles, resolver),
        toggle = ToggleSpec(checked = on, action = FlipToggleAction(toggle, toggles, resolver)),
        handler = settingsApp.takeIf { toggle != QuickToggle.Flashlight },
    )

    companion object {
        val ID = ProviderId("device-settings")
        private const val KEYWORD_WEIGHT = 0.95f

        // Live toggles answer the "turn it on" intent best, so they sit just above the matching screen.
        private const val TOGGLE_BONUS = 0.03f
        private const val MAX_RESULTS = 12
    }
}
