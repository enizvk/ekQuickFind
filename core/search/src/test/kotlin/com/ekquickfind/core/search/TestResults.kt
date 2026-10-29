package com.ekquickfind.core.search

/** Test helpers for building results and providers. */
object NoopAction : Action {
    override val label = "Open"
    override val glyph: Glyph? = null

    override suspend fun perform(host: ActionHost) = ActionOutcome.Close
}

fun result(
    title: String,
    score: Float = 0.5f,
    section: Section = Section.Apps,
    provider: String = section.name.lowercase(),
    id: String = "$provider:$title",
    isFallback: Boolean = false,
    isRecordable: Boolean = true,
) = SearchResult(
    id = ResultId(id),
    providerId = ProviderId(provider),
    section = section,
    title = title,
    icon = ResultIcon.Vector(Glyph.Search),
    score = score,
    primaryAction = NoopAction,
    isFallback = isFallback,
    isRecordable = isRecordable,
)

const val DAY_MS = 24L * 60 * 60 * 1000
