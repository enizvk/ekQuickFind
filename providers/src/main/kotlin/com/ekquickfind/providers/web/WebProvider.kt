package com.ekquickfind.providers.web

import android.content.Intent
import androidx.core.net.toUri
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Emphasis
import com.ekquickfind.core.search.FallbackFactory
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.Requirement
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.TextNormalizer
import com.ekquickfind.providers.HandlerResolver
import com.ekquickfind.providers.android
import com.ekquickfind.providers.getText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.IOException

data class WebSettingsSnapshot(
    val suggestionsEnabled: Boolean,
    val engine: WebEngine,
)

/**
 * The single place that decides whether the app may touch the network. Everything web-related
 * reads it right before acting, so "suggestions off" really means zero requests.
 */
fun interface NetworkPolicy {
    suspend fun current(): WebSettingsSnapshot
}

/** Opens a web search in the default browser. Reads the engine when tapped so a settings change applies at once. */
data class SearchWebAction(
    val query: String,
    private val policy: NetworkPolicy,
    override val label: String = "Search the web",
) : Action {
    override val glyph = Glyph.Web

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val url = policy.current().engine.searchUrl(query)
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        return if (host.android().startActivity(intent)) ActionOutcome.Close else ActionOutcome.Stay
    }
}

/** The always-present "Search the web for …" row. */
class WebFallback(
    private val policy: NetworkPolicy,
    private val handlers: HandlerResolver = HandlerResolver.None,
) : FallbackFactory {
    private val browser by lazy { handlers.packageFor(browserProbe()) }

    override fun create(query: Query) =
        SearchResult(
            id = ResultId("web:fallback"),
            providerId = WebProvider.ID,
            section = Section.Web,
            title = "Search the web for “${query.raw.trim()}”",
            icon = ResultIcon.Vector(Glyph.Web),
            score = 0f,
            primaryAction = SearchWebAction(query.raw.trim(), policy),
            isFallback = true,
            isRecordable = false,
            handler = browser,
        )
}

/** Any https link resolves to the default browser. */
internal fun browserProbe() = Intent(Intent.ACTION_VIEW, "https://example.com".toUri()).addCategory(Intent.CATEGORY_BROWSABLE)

class WebProvider(
    private val http: OkHttpClient,
    private val policy: NetworkPolicy,
    private val io: CoroutineDispatcher,
    private val debounceMillis: Long = 250,
    private val handlers: HandlerResolver = HandlerResolver.None,
) : SearchProvider,
    ResultResolver {
    override val id = ID
    override val section = Section.Web
    override val requirements = setOf(Requirement.Network)

    // Network answers take longer than on-device ones; give them more room before the surface settles.
    override val budgetMillis = 1_500L

    private val browser by lazy { handlers.packageFor(browserProbe()) }

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val settings = policy.current()
            val url = settings.engine.suggestUrl(query.raw).takeIf { settings.suggestionsEnabled }
            if (url == null) {
                emit(emptyList())
                return@flow
            }
            delay(debounceMillis)
            val body =
                try {
                    withContext(io) { http.getText(url) }
                } catch (_: IOException) {
                    emit(emptyList())
                    return@flow
                }
            val typed = query.normalized
            // Engines sometimes repeat a suggestion with different case or spacing; keep the first spelling.
            val suggestions =
                OpenSearchSuggestions
                    .parse(body)
                    .filter { TextNormalizer.normalize(it) != typed }
                    .distinctBy { TextNormalizer.normalize(it) }
                    .take(MAX_SUGGESTIONS)
            emit(suggestions.mapIndexed { i, s -> suggestionRow(s, i) })
        }

    override suspend fun resolve(id: ResultId): SearchResult? =
        id.value
            .removePrefix("web:suggest:")
            .takeIf { it != id.value && it.isNotBlank() }
            ?.let { suggestionRow(it, 0) }

    private fun suggestionRow(
        text: String,
        index: Int,
    ) = SearchResult(
        id = ResultId("web:suggest:$text"),
        providerId = ID,
        section = section,
        title = text,
        icon = ResultIcon.Vector(Glyph.Suggestion),
        // Keep the engine's order.
        score = 0.6f - index * 0.05f,
        primaryAction = SearchWebAction(text, policy),
        emphasis = Emphasis.Completion,
        handler = browser,
    )

    companion object {
        val ID = ProviderId("web")
        private const val MAX_SUGGESTIONS = 5
    }
}
