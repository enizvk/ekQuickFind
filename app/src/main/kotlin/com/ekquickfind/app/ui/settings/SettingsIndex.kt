package com.ekquickfind.app.ui.settings

import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.TextNormalizer

/** One thing you can look up in settings: what it's called, other words for it, and the panel it's in. */
data class SettingEntry(
    val title: String,
    val words: String,
    val panel: Panel,
) {
    val page get() = panel.page
}

/** Search over settings, so "blur" or "font" jumps straight to the right panel. */
object SettingsIndex {
    val entries =
        listOf(
            SettingEntry("Apps, contacts, calendar and files", "sources permissions photo tags", Panel.Sources),
            SettingEntry("Smart suggestions", "suggested before you type headphones car", Panel.BeforeTyping),
            SettingEntry("Next up", "calendar event", Panel.BeforeTyping),
            SettingEntry("Clipboard chip", "copied paste", Panel.BeforeTyping),
            SettingEntry("Result order", "sections order drag", Panel.Order),
            SettingEntry("Icon size", "icons big small", Panel.Layout),
            SettingEntry("Density", "compact spacing", Panel.Layout),
            SettingEntry("Results per section", "more how many", Panel.Layout),
            SettingEntry("Section layout", "rows grid chips", Panel.Layout),
            SettingEntry("Web suggestions", "internet online", Panel.Web),
            SettingEntry("Search engine", "google duckduckgo bing brave custom", Panel.Web),
            SettingEntry("Ask AI", "chatgpt claude gemini", Panel.Web),
            SettingEntry("Site shortcuts", "youtube translate maps play wikipedia reddit keyword", Panel.SearchSites),
            SettingEntry("Restore default sites", "deleted removed sites back", Panel.SearchSites),
            SettingEntry("Keyword separator", "colon dash", Panel.SearchSites),
            SettingEntry("Translation", "translate language offline google tr", Panel.Translation),
            SettingEntry("Add your own site", "custom url", Panel.SearchSites),
            SettingEntry("Weather", "temperature units celsius fahrenheit location", Panel.SearchSites),
            SettingEntry("Background style", "blur frosted glass solid", Panel.Background),
            SettingEntry("Blur strength", "blur", Panel.Background),
            SettingEntry("Dim", "darkness dimming", Panel.Background),
            SettingEntry("Theme", "dark light mode night", Panel.Appearance),
            SettingEntry("Accent colour", "color wallpaper material you", Panel.Appearance),
            SettingEntry("Text size", "bigger smaller font size", Panel.Text),
            SettingEntry("Font", "typeface serif mono condensed rounded", Panel.Text),
            SettingEntry("Font weight", "bold light", Panel.Text),
            SettingEntry("Search bar position", "top bottom one handed", Panel.SearchBar),
            SettingEntry("Recent items strip", "recents chips", Panel.SearchBar),
            SettingEntry("Recent items shown", "recents count limit", Panel.SearchBar),
            SettingEntry("Exact match button", "whole words quote", Panel.SearchBar),
            SettingEntry("Search bar animation", "light sweep pulse shimmer breathe twin lights glow", Panel.SearchBar),
            SettingEntry("Top result", "featured best match card in its section", Panel.ActiveItem),
            SettingEntry("Selected result by the search bar", "one handed bottom", Panel.ActiveItem),
            SettingEntry("Active item style", "highlight tinted soft glow gradient", Panel.ActiveItem),
            SettingEntry("Active item animation", "settle lift twin lights", Panel.ActiveItem),
            SettingEntry("Open keyboard automatically", "keyboard", Panel.Keyboard),
            SettingEntry("Enter runs the selected result", "enter key", Panel.Keyboard),
            SettingEntry("Haptic feedback", "vibration haptics", Panel.Keyboard),
            SettingEntry("Reduce motion", "animations motion accessibility", Panel.Keyboard),
            SettingEntry("Floating button", "bubble overlay over other apps size transparency", Panel.FloatingButton),
            SettingEntry("History backup", "backup cloud history privacy", Panel.Privacy),
        )

    fun search(
        query: String,
        limit: Int = 12,
    ): List<SettingEntry> {
        val q = TextNormalizer.normalize(query)
        if (q.isEmpty()) return emptyList()
        return entries
            .mapNotNull { e ->
                val title = QueryMatcher.score(q, MatchTarget(e.title))
                // Other words count a little less than the name itself.
                val words = QueryMatcher.score(q, MatchTarget(e.words))?.times(WORDS_WEIGHT)
                listOfNotNull(title, words).maxOrNull()?.let { e to it }
            }.sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private const val WORDS_WEIGHT = 0.9f
}
