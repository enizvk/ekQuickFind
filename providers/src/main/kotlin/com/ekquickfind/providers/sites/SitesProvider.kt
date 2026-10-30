package com.ekquickfind.providers.sites

import android.content.Intent
import androidx.core.net.toUri
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.android
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

/** A site that's switched on, with the keyword in use. */
class ActiveSite(
    val id: String,
    val name: String,
    val keyword: String,
    val triggers: List<String>,
    val glyph: Glyph,
    private val builtIn: BuiltInSite?,
    private val template: String?,
) {
    val appPackage get() = builtIn?.appPackage

    fun title(text: String) = builtIn?.title(text) ?: "Search $name for “$text”"

    fun open(
        text: String,
        isInstalled: (String) -> Boolean,
        locale: Locale,
    ): SiteOpen = builtIn?.open(text, isInstalled, locale) ?: customOpen(template.orEmpty(), text)
}

/** A keyword recognised in the bar: which site, its icon, and how much of the text to colour. */
data class KeywordHint(
    val siteName: String,
    val glyph: Glyph,
    val end: Int,
)

/** Everything the shortcut matcher needs from preferences. */
class SitesConfig(
    val sites: List<ActiveSite>,
    val separator: String,
    /** Weather shares the keyword set-up but shows a card (see the weather provider). */
    val weatherKeyword: String?,
) {
    val keys: List<SiteKey>
        get() =
            sites.map { SiteKey(it.id, it.keyword, it.triggers) } +
                listOfNotNull(weatherKeyword?.let { SiteKey(SiteCatalog.WEATHER_ID, it, SiteCatalog.weatherTriggers) })

    /** The keyword at the start of [raw], once a space (or the separator and a space) follows it. */
    fun hint(raw: String): KeywordHint? {
        val prefix = KeywordQuery.prefix(raw, keys, separator) ?: return null
        val site = sites.firstOrNull { it.id == prefix.siteId }
        return when {
            site != null -> KeywordHint(site.name, site.glyph, prefix.end)
            prefix.siteId == SiteCatalog.WEATHER_ID -> KeywordHint("Weather", Glyph.Weather, prefix.end)
            else -> null
        }
    }

    companion object {
        fun from(prefs: UserPreferences): SitesConfig {
            val builtIns =
                SiteCatalog.linkSites.mapNotNull { site ->
                    val state = prefs.sites[site.id]
                    if (!SiteCatalog.isActive(prefs, site.id)) return@mapNotNull null
                    ActiveSite(site.id, site.name, state?.keyword ?: site.defaultKeyword, site.triggers, site.glyph, site, null)
                }
            val custom = prefs.customSites.map { ActiveSite(it.id, it.name, it.keyword, emptyList(), Glyph.Search, null, it.url) }
            val weather = prefs.sites[SiteCatalog.WEATHER_ID]
            return SitesConfig(
                builtIns + custom,
                prefs.siteSeparator,
                weatherKeyword =
                    if (SiteCatalog.isActive(prefs, SiteCatalog.WEATHER_ID)) weather?.keyword ?: SiteCatalog.WEATHER_KEYWORD else null,
            )
        }
    }
}

/** Site shortcuts: "yt: cats" (or a matching word like "youtube cats") gives one row that searches that site. */
class SitesProvider(
    private val config: suspend () -> SitesConfig,
    private val isInstalled: (String) -> Boolean,
    private val locale: () -> Locale = Locale::getDefault,
) : SearchProvider {
    override val id = ID
    override val section = Section.Sites

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val current = config()
            val match = KeywordQuery.parse(query.raw, current.keys, current.separator)
            val site = match?.takeIf { it.text.isNotEmpty() }?.let { m -> current.sites.firstOrNull { it.id == m.siteId } }
            if (match == null || site == null) {
                emit(emptyList())
                return@flow
            }
            val open = site.open(match.text, isInstalled, locale())
            emit(
                listOf(
                    SearchResult(
                        id = ResultId("site:${site.id}"),
                        providerId = ID,
                        section = section,
                        title = site.title(match.text),
                        subtitle = if (match.byKeyword) "${site.name} · “${site.keyword}”" else site.name,
                        icon = ResultIcon.Vector(site.glyph),
                        // A keyword is a clear ask; a matching word is a strong hint.
                        score = if (match.byKeyword) 1f else TRIGGER_SCORE,
                        primaryAction = OpenSiteAction(open, site.name),
                        // The search inside changes every time, so there's nothing to remember or pin.
                        isRecordable = false,
                        isPromotable = true,
                        handler = site.appPackage?.takeIf(isInstalled),
                    ),
                ),
            )
        }

    companion object {
        val ID = ProviderId("sites")
        private const val TRIGGER_SCORE = 0.6f
    }
}

data class OpenSiteAction(
    val target: SiteOpen,
    val siteName: String,
) : Action {
    override val label = "Open in $siteName"
    override val glyph = Glyph.Web

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val intent =
            when (target) {
                is SiteOpen.Link ->
                    Intent(Intent.ACTION_VIEW, target.url.toUri()).also {
                        // Web links go to an app that handles the site, else the browser; geo: and market: aren't browsable.
                        if (target.url.startsWith("https://")) it.addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                is SiteOpen.AppSearch ->
                    Intent(target.action)
                        .setPackage(target.packageName)
                        .putExtra(target.extra, target.text)
                        .also { intent -> target.type?.let { intent.type = it } }
            }
        return if (host.android().startActivity(intent)) ActionOutcome.Close else ActionOutcome.Stay
    }
}
