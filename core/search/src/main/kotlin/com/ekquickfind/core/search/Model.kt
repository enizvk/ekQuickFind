package com.ekquickfind.core.search

/** Stable id of a provider, used for toggles and history. */
@JvmInline
value class ProviderId(
    val value: String,
)

/** Stable id of a result across app runs, used for history, pins and hiding. */
@JvmInline
value class ResultId(
    val value: String,
)

/** Result sections, in the order they're shown. [weight] scales text scores when ranking across sections. */
enum class Section(
    val title: String,
    val weight: Float,
) {
    QuickAnswers("Answer", 2.0f),

    /** The "tr hello" translation card, under its own heading. */
    Translation("Translation", 1.9f),

    /** Site shortcuts ("yt: cats"); weighted so a keyword beats any app whose name starts the same. */
    Sites("Search with", 1.5f),
    Apps("Apps", 1.0f),
    Contacts("Contacts", 0.9f),
    Calendar("Calendar", 0.85f),
    Settings("Settings", 0.85f),
    Files("Files", 0.8f),
    Web("Web", 0.5f),

    /** "Ask ChatGPT / Claude / Gemini" shortcuts; never the top result. */
    AskAi("Ask AI", 0.3f),
}

/** What a provider needs from the system. Used by the settings screen to show status. */
sealed interface Requirement {
    data class Permission(
        val name: String,
    ) : Requirement

    data class SpecialAccess(
        val name: String,
    ) : Requirement

    data object Network : Requirement
}

/** Built-in vector glyphs; the UI maps them to real icons. */
enum class Glyph {
    Calculator,
    Convert,
    Settings,
    Toggle,
    Web,
    Search,
    Suggestion,
    Contact,
    Call,
    Message,
    File,
    Image,
    Video,
    Audio,
    Document,
    Folder,
    Share,
    Info,
    Uninstall,
    Hide,
    Pin,
    Unpin,
    Shortcut,
    Lock,
    Flashlight,
    History,
    Chat,
    Calendar,
    Translate,
    Map,
    Weather,
}

sealed interface ResultIcon {
    /** Launcher icon of an activity in a given user profile. */
    data class PackageIcon(
        val packageName: String,
        val activityName: String?,
        val userSerial: Long,
    ) : ResultIcon

    /** Shortcut icon published by an app. */
    data class ShortcutIcon(
        val packageName: String,
        val shortcutId: String,
        val userSerial: Long,
    ) : ResultIcon

    /** Image or video thumbnail behind a content:// uri. */
    data class Thumbnail(
        val uri: String,
        val fallback: Glyph,
    ) : ResultIcon

    /** Photo or avatar behind a content:// uri, shown round. */
    data class Avatar(
        val uri: String?,
        val initials: String,
    ) : ResultIcon

    data class Vector(
        val glyph: Glyph,
    ) : ResultIcon
}

/** How a title highlights the query: matched text (default) or, for suggestions, the part the user hasn't typed. */
enum class Emphasis { Match, Completion }

/** An on/off switch shown on the result itself; [action] flips it (or asks how). */
data class ToggleSpec(
    val checked: Boolean,
    val action: Action,
)

/** The query as typed plus its normalised form. */
data class Query(
    val raw: String,
    /** Only the typed text as whole words counts: no partial words, typos, initials or scattered words. */
    val exact: Boolean = false,
) {
    val normalized: String = TextNormalizer.normalize(raw)
    val isBlank: Boolean get() = normalized.isEmpty()
}

data class SearchResult(
    val id: ResultId,
    val providerId: ProviderId,
    val section: Section,
    val title: String,
    val subtitle: String? = null,
    val icon: ResultIcon,
    /** Provider-local text match quality, 0..1. */
    val score: Float,
    val primaryAction: Action,
    val secondaryActions: List<Action> = emptyList(),
    /** Small inline buttons shown on the row itself (for example call / message). */
    val inlineActions: List<Action> = emptyList(),
    /** Badge such as "Work" shown next to the title. */
    val badge: String? = null,
    /** True for the always-present "Search the web" row; only top result when nothing else matched. */
    val isFallback: Boolean = false,
    /** False for rows like "Enable contacts search" or quick answers that shouldn't go into history or pins. */
    val isRecordable: Boolean = true,
    /** Whether this row may become the top result. Quick answers are promotable but not recordable. */
    val isPromotable: Boolean = isRecordable,
    val emphasis: Emphasis = Emphasis.Match,
    /** Package of the app that opens this result; the UI shows its icon (or a badge over thumbnails). */
    val handler: String? = null,
    val toggle: ToggleSpec? = null,
    /** Lines the title may wrap to; most rows are one line, a translation needs more. */
    val titleMaxLines: Int = 1,
)
