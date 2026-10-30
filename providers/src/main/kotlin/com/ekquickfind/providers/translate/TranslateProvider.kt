package com.ekquickfind.providers.translate

import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.CopyAction
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.Translator
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.sites.KeywordQuery
import com.ekquickfind.providers.sites.OpenSiteAction
import com.ekquickfind.providers.sites.SiteCatalog
import com.ekquickfind.providers.sites.SitesConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Locale

/** "tr my house" shows the translation as a card in the results; the Translate site row stays below it. */
class TranslateProvider(
    private val prefs: suspend () -> UserPreferences,
    private val translators: (Translator) -> TextTranslator,
    /** Switches the translator to on-device, for when Google can't be reached. */
    private val useOffline: suspend () -> Unit,
    private val isInstalled: (String) -> Boolean,
    private val io: CoroutineDispatcher,
    private val locale: () -> Locale = Locale::getDefault,
) : SearchProvider {
    override val id = ID
    override val section = Section.Translation

    // Network answers take longer than on-device ones.
    override val budgetMillis = 2_500L

    private val cache = mutableMapOf<String, TranslateOutcome.Done>()

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val p = prefs()
            val config = SitesConfig.from(p)
            val match =
                KeywordQuery
                    .parse(query.raw, config.keys, config.separator)
                    ?.takeIf { it.siteId == SiteCatalog.Translate.id && p.translateInline && it.text.length >= MIN_CHARS }
            if (match == null) {
                emit(emptyList())
                return@flow
            }
            // Wait for a pause in typing before translating.
            delay(DEBOUNCE_MS)
            val translator = translators(p.translator)
            val target = p.translateTo.ifEmpty { locale().language }
            val card = withContext(io) { card(match.text, target, translator, p.translator, match.byKeyword) }
            emit(listOfNotNull(card))
        }

    private suspend fun card(
        text: String,
        target: String,
        translator: TextTranslator,
        engine: Translator,
        byKeyword: Boolean,
    ): SearchResult? {
        var to = target
        var outcome = translate(text, to, translator, engine)
        // Already in the language you translate into: show it in English instead.
        if (outcome is TranslateOutcome.Done && outcome.from == target) {
            if (target == ENGLISH) return null
            to = ENGLISH
            outcome = translate(text, to, translator, engine)
        }
        val openInTranslate = OpenSiteAction(SiteCatalog.Translate.open(text, isInstalled, locale()), SiteCatalog.Translate.name)
        return when (outcome) {
            is TranslateOutcome.Done ->
                result(
                    title = outcome.text,
                    subtitle = "${languageName(outcome.from)} → ${languageName(to)} · ${translator.name}",
                    action = CopyAction(outcome.text, "Copy translation"),
                    more = listOf(openInTranslate),
                    score = if (byKeyword) 1f else TRIGGER_SCORE,
                    lines = TITLE_LINES,
                )
            is TranslateOutcome.Preparing ->
                result(
                    title = "Getting ${languageName(outcome.language)} ready for offline use…",
                    subtitle = "A one-time download of about 30 MB",
                    action = openInTranslate,
                    more = emptyList(),
                    score = TRIGGER_SCORE,
                )
            TranslateOutcome.Failed ->
                if (engine == Translator.Google) {
                    result(
                        "Translation unavailable",
                        "Tap to translate on the phone instead",
                        UseOfflineAction(useOffline),
                        listOf(openInTranslate),
                    )
                } else {
                    result("Translation unavailable", "Couldn't translate on the phone", openInTranslate, emptyList())
                }
        }
    }

    private suspend fun translate(
        text: String,
        to: String,
        translator: TextTranslator,
        engine: Translator,
    ): TranslateOutcome {
        val key = "$engine|$to|$text"
        cache[key]?.let { return it }
        return translator.translate(text, to).also { if (it is TranslateOutcome.Done) remember(key, it) }
    }

    private fun remember(
        key: String,
        done: TranslateOutcome.Done,
    ) {
        if (cache.size >= CACHE_SIZE) cache.remove(cache.keys.first())
        cache[key] = done
    }

    private fun languageName(code: String) = languageName(code, locale())

    private fun result(
        title: String,
        subtitle: String,
        action: Action,
        more: List<Action>,
        score: Float = TRIGGER_SCORE,
        lines: Int = 1,
    ) = SearchResult(
        id = ResultId("translate:card"),
        providerId = ID,
        section = section,
        title = title,
        subtitle = subtitle,
        icon = ResultIcon.Vector(Glyph.Translate),
        score = score,
        primaryAction = action,
        secondaryActions = more,
        isRecordable = false,
        isPromotable = true,
        titleMaxLines = lines,
    )

    companion object {
        val ID = ProviderId("translate")
        private const val ENGLISH = "en"
        private const val MIN_CHARS = 2
        private const val DEBOUNCE_MS = 300L
        private const val CACHE_SIZE = 50
        private const val TITLE_LINES = 4
        private const val TRIGGER_SCORE = 0.9f
    }
}

/** "bg" as "Bulgarian", in [locale]'s words. */
fun languageName(
    code: String,
    locale: Locale,
): String =
    Locale
        .forLanguageTag(code)
        .getDisplayLanguage(locale)
        .replaceFirstChar { it.titlecase(locale) }
        .ifEmpty { code }

/** Turns on offline translation and searches again. */
class UseOfflineAction(
    private val switch: suspend () -> Unit,
) : Action {
    override val label = "Translate on the phone"
    override val glyph = Glyph.Translate

    override suspend fun perform(host: ActionHost): ActionOutcome {
        switch()
        return ActionOutcome.Refresh
    }
}
