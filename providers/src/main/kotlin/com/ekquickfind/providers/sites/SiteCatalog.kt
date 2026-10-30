package com.ekquickfind.providers.sites

import android.content.Intent
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.data.prefs.SiteState
import com.ekquickfind.data.prefs.UserPreferences
import java.net.URLEncoder
import java.util.Locale

/** How a site opens a search: an app's own search screen, or a link (which an installed app may take). */
sealed interface SiteOpen {
    data class Link(
        val url: String,
    ) : SiteOpen

    data class AppSearch(
        val packageName: String,
        val action: String,
        val extra: String,
        val text: String,
        val type: String? = null,
    ) : SiteOpen
}

/** A ready-made site. [open] picks the app when it's installed, otherwise the website. */
class BuiltInSite(
    val id: String,
    val name: String,
    val defaultKeyword: String,
    val triggers: List<String>,
    val glyph: Glyph,
    /** The app that handles it, shown on the row when installed. */
    val appPackage: String?,
    private val title: (String) -> String,
    private val web: (q: String, locale: Locale) -> String,
    private val app: ((q: String) -> SiteOpen)? = null,
) {
    fun title(text: String) = title.invoke(text)

    fun open(
        text: String,
        isInstalled: (String) -> Boolean,
        locale: Locale,
    ): SiteOpen {
        val viaApp = app?.takeIf { appPackage != null && isInstalled(appPackage) }
        return viaApp?.invoke(text) ?: SiteOpen.Link(web(encode(text), locale))
    }
}

object SiteCatalog {
    const val WEATHER_ID = "weather"
    const val WEATHER_KEYWORD = "w"
    val weatherTriggers = listOf("weather", "forecast")

    val YouTube =
        BuiltInSite(
            "youtube",
            "YouTube",
            "yt",
            triggers = listOf("youtube"),
            glyph = Glyph.Video,
            appPackage = "com.google.android.youtube",
            title = { "Search YouTube for “$it”" },
            web = { q, _ -> "https://www.youtube.com/results?search_query=$q" },
            app = { SiteOpen.AppSearch("com.google.android.youtube", Intent.ACTION_SEARCH, "query", it) },
        )

    val Translate =
        BuiltInSite(
            "translate",
            "Google Translate",
            "tr",
            triggers = listOf("translate"),
            glyph = Glyph.Translate,
            appPackage = "com.google.android.apps.translate",
            title = { "Translate “$it”" },
            // Into the phone's language; Translate works out what the text is in.
            web = { q, locale -> "https://translate.google.com/?sl=auto&tl=${locale.language}&text=$q&op=translate" },
            app = { SiteOpen.AppSearch("com.google.android.apps.translate", Intent.ACTION_SEND, Intent.EXTRA_TEXT, it, "text/plain") },
        )

    val Maps =
        BuiltInSite(
            "maps",
            "Google Maps",
            "map",
            triggers = listOf("map", "maps"),
            glyph = Glyph.Map,
            appPackage = "com.google.android.apps.maps",
            title = { "Find “$it” on Maps" },
            web = { q, _ -> "https://www.google.com/maps/search/?api=1&query=$q" },
            // A geo: link opens Maps, or whichever map app the user prefers.
            app = { SiteOpen.Link("geo:0,0?q=${encode(it)}") },
        )

    val Play =
        BuiltInSite(
            "play",
            "Play Store",
            "play",
            triggers = emptyList(),
            glyph = Glyph.Search,
            appPackage = "com.android.vending",
            title = { "Search Play Store for “$it”" },
            web = { q, _ -> "https://play.google.com/store/search?q=$q&c=apps" },
            app = { SiteOpen.Link("market://search?q=${encode(it)}&c=apps") },
        )

    val Wikipedia =
        BuiltInSite(
            "wikipedia",
            "Wikipedia",
            "wiki",
            triggers = listOf("wikipedia"),
            glyph = Glyph.Search,
            appPackage = "org.wikipedia",
            title = { "“$it” on Wikipedia" },
            // The Wikipedia app takes these links when it's installed.
            web = { q, locale -> "https://${locale.language}.wikipedia.org/w/index.php?search=$q" },
        )

    val Reddit =
        BuiltInSite(
            "reddit",
            "Reddit",
            "r",
            triggers = listOf("reddit"),
            glyph = Glyph.Search,
            appPackage = "com.reddit.frontpage",
            title = { "Search Reddit for “$it”" },
            web = { q, _ -> "https://www.reddit.com/search/?q=$q" },
        )

    /** In the order settings lists them; weather is its own provider but shares the keyword setup. */
    val linkSites = listOf(YouTube, Translate, Maps, Play, Wikipedia, Reddit)

    /** Off until the user turns them on. */
    private val offByDefault = setOf(Wikipedia.id, Reddit.id, WEATHER_ID)

    fun onByDefault(id: String) = id !in offByDefault

    /** Whether site [id] is on: the user's choice, else its default. */
    fun isOn(
        sites: Map<String, SiteState>,
        id: String,
    ) = sites[id]?.enabled ?: onByDefault(id)

    /** On and not deleted. */
    fun isActive(
        prefs: UserPreferences,
        id: String,
    ) = id !in prefs.removedSites && isOn(prefs.sites, id)
}

internal fun encode(text: String): String = URLEncoder.encode(text.trim(), Charsets.UTF_8)

fun customOpen(
    template: String,
    text: String,
): SiteOpen = SiteOpen.Link(template.replace("%s", encode(text)))

/** A web address with %s where the search goes; https only, so a typo can't send searches in the clear. */
fun isValidTemplate(template: String): Boolean =
    template.startsWith("https://") && template.contains("%s") && template.length > "https://%s".length

/** One short word of letters or digits, so it can't swallow the separator. */
fun isValidKeyword(keyword: String): Boolean = keyword.length in 1..12 && keyword.all { it.isLetterOrDigit() }
