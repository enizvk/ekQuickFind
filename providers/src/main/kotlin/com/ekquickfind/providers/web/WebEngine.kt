package com.ekquickfind.providers.web

import com.ekquickfind.data.prefs.SearchEngineChoice
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import java.net.URLEncoder

/** Where web searches go and where suggestions come from. Templates use %s for the encoded query. */
data class WebEngine(
    val name: String,
    private val searchTemplate: String,
    private val suggestTemplate: String?,
) {
    fun searchUrl(query: String) = searchTemplate.replace("%s", encode(query))

    fun suggestUrl(query: String) = suggestTemplate?.replace("%s", encode(query))

    companion object {
        private val DuckDuckGo = WebEngine("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/ac/?q=%s&type=list")
        private val Google =
            WebEngine(
                "Google",
                "https://www.google.com/search?q=%s",
                "https://suggestqueries.google.com/complete/search?client=firefox&q=%s",
            )
        private val Bing = WebEngine("Bing", "https://www.bing.com/search?q=%s", "https://api.bing.com/osjson.aspx?query=%s")
        private val Brave = WebEngine("Brave", "https://search.brave.com/search?q=%s", "https://search.brave.com/api/suggest?q=%s")

        fun forChoice(
            choice: SearchEngineChoice,
            customSearch: String? = null,
            customSuggest: String? = null,
        ): WebEngine =
            when (choice) {
                SearchEngineChoice.DuckDuckGo -> DuckDuckGo
                SearchEngineChoice.Google -> Google
                SearchEngineChoice.Bing -> Bing
                SearchEngineChoice.Brave -> Brave
                SearchEngineChoice.Custom ->
                    if (customSearch?.contains("%s") == true) {
                        WebEngine("Custom", customSearch, customSuggest?.takeIf { it.contains("%s") })
                    } else {
                        DuckDuckGo
                    }
            }

        private fun encode(query: String) = URLEncoder.encode(query.trim(), Charsets.UTF_8)
    }
}

/** Parses the OpenSearch suggestions format all supported engines return: ["query", ["s1", "s2", ...], ...]. */
object OpenSearchSuggestions {
    fun parse(body: String): List<String> =
        runCatching {
            val root = Json.parseToJsonElement(body).jsonArray
            (root.getOrNull(1) as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
                .orEmpty()
        }.getOrDefault(emptyList())
}
