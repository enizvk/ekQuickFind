package com.ekquickfind.core.search

/** What the surface does after an action ran. */
enum class ActionOutcome {
    /** Close the search surface (for example after launching an app). */
    Close,

    /** Keep the surface open and leave results as they are. */
    Stay,

    /** Keep the surface open and re-run the current query (for example after hiding a result). */
    Refresh,
}

/**
 * Capabilities the UI layer offers to actions. Android-specific hosts extend this
 * so provider actions can launch intents without core knowing about Android.
 */
interface ActionHost {
    suspend fun copyToClipboard(
        label: String,
        text: String,
    )

    suspend fun pin(result: SearchResult)

    suspend fun unpin(result: SearchResult)

    suspend fun hide(result: ResultId)

    /** Lets the user pick one option (for example which number to call); null when cancelled. */
    suspend fun choose(
        title: String,
        options: List<String>,
    ): Int?
}

interface Action {
    val label: String
    val glyph: Glyph?

    suspend fun perform(host: ActionHost): ActionOutcome
}

/** Copies a value to the clipboard, used by quick answers. */
data class CopyAction(
    val value: String,
    override val label: String = "Copy",
) : Action {
    override val glyph: Glyph? = null

    override suspend fun perform(host: ActionHost): ActionOutcome {
        host.copyToClipboard(label = "ekQuickFind", text = value)
        return ActionOutcome.Stay
    }
}

/** Hides a result from search until it's unhidden in settings. */
data class HideAction(
    val result: ResultId,
    override val label: String = "Hide",
) : Action {
    override val glyph: Glyph = Glyph.Hide

    // The host looks up the full result by id; keeping only the id avoids a self-reference.
    override suspend fun perform(host: ActionHost): ActionOutcome {
        host.hide(result)
        return ActionOutcome.Refresh
    }
}

data class PinAction(
    val result: SearchResult,
    override val label: String = "Pin",
) : Action {
    override val glyph: Glyph = Glyph.Pin

    override suspend fun perform(host: ActionHost): ActionOutcome {
        host.pin(result)
        return ActionOutcome.Refresh
    }
}

data class UnpinAction(
    val result: SearchResult,
    override val label: String = "Unpin",
) : Action {
    override val glyph: Glyph = Glyph.Unpin

    override suspend fun perform(host: ActionHost): ActionOutcome {
        host.unpin(result)
        return ActionOutcome.Refresh
    }
}
