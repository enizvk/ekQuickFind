package com.ekquickfind.providers.translate

import com.ekquickfind.providers.getText
import com.ekquickfind.providers.sites.encode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

sealed interface TranslateOutcome {
    /** [from] is the language the translator found the text in. */
    data class Done(
        val text: String,
        val from: String,
    ) : TranslateOutcome

    /** The offline language is still downloading. */
    data class Preparing(
        val language: String,
    ) : TranslateOutcome

    data object Failed : TranslateOutcome
}

/** Translates text into the language with code [to] ("bg"), working out what it's written in. */
interface TextTranslator {
    /** Shown on the card, like "Google Translate". */
    val name: String

    suspend fun translate(
        text: String,
        to: String,
    ): TranslateOutcome
}

/** Google's web translator: same results as translate.google.com, no key, but unofficial and may change. */
class GoogleWebTranslator(
    private val http: OkHttpClient,
) : TextTranslator {
    override val name = "Google Translate"

    override suspend fun translate(
        text: String,
        to: String,
    ): TranslateOutcome =
        try {
            GoogleTranslateJson.parse(
                http.getText("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$to&dt=t&q=${encode(text)}"),
            ) ?: TranslateOutcome.Failed
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            TranslateOutcome.Failed
        }
}

object GoogleTranslateJson {
    /** The answer is nested arrays: [[[piece, original, …], …], null, "en", …]. */
    fun parse(body: String): TranslateOutcome.Done? =
        runCatching {
            val root = Json.parseToJsonElement(body).jsonArray
            val text = root[0].jsonArray.joinToString("") { it.jsonArray[0].jsonPrimitive.content }.trim()
            val from = root[2].jsonPrimitive.content
            TranslateOutcome.Done(text, from).takeIf { text.isNotEmpty() }
        }.getOrNull()
}
