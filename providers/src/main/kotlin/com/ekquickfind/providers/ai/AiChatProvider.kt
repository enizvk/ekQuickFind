package com.ekquickfind.providers.ai

import android.content.Intent
import android.net.Uri
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
import com.ekquickfind.data.prefs.AiChat
import com.ekquickfind.providers.android
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** "Ask ChatGPT / Claude / Gemini" with the typed text; opens the app if installed (via its links), else the website. */
class AiChatProvider(
    private val enabled: suspend () -> Set<AiChat>,
    private val isInstalled: (String) -> Boolean,
) : SearchProvider {
    override val id = ID
    override val section = Section.AskAi

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val text = query.raw.trim()
            if (text.isEmpty()) {
                emit(emptyList())
                return@flow
            }
            val on = enabled()
            emit(
                AiChat.entries.filter { it in on }.mapIndexed { i, chat ->
                    SearchResult(
                        id = ResultId("ai:${chat.name.lowercase()}"),
                        providerId = ID,
                        section = section,
                        title = "Ask ${chat.label}",
                        subtitle = if (chat.prefills) "“$text”" else "Copies your question to paste in ${chat.label}",
                        icon = ResultIcon.Vector(Glyph.Chat),
                        // Keeps the fixed order inside the section.
                        score = 0.2f - i * 0.01f,
                        primaryAction = AskAiAction(chat, text),
                        isRecordable = false,
                        isPromotable = false,
                        handler = chat.packageName.takeIf(isInstalled),
                    )
                },
            )
        }

    companion object {
        val ID = ProviderId("ai")
    }
}

/** ChatGPT and Claude read the question from the link; Gemini can't, so the question is copied for pasting. */
private val AiChat.prefills get() = this != AiChat.Gemini

private fun AiChat.link(text: String): Uri =
    when (this) {
        AiChat.ChatGpt -> "https://chatgpt.com/?q=${Uri.encode(text)}"
        AiChat.Claude -> "https://claude.ai/new?q=${Uri.encode(text)}"
        AiChat.Gemini -> "https://gemini.google.com/app"
    }.toUri()

/** Claude's prefill isn't documented, so it gets the clipboard as a backup too. */
private val AiChat.copiesQuestion get() = this != AiChat.ChatGpt

class AskAiAction(
    private val chat: AiChat,
    private val text: String,
) : Action {
    override val label = "Ask ${chat.label}"
    override val glyph = Glyph.Chat

    override suspend fun perform(host: ActionHost): ActionOutcome {
        if (chat.copiesQuestion) host.copyToClipboard("Question for ${chat.label}", text)
        val intent = Intent(Intent.ACTION_VIEW, chat.link(text)).addCategory(Intent.CATEGORY_BROWSABLE)
        return if (host.android().startActivity(intent)) ActionOutcome.Close else ActionOutcome.Stay
    }
}
