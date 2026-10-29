package com.ekquickfind.app.assist

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import com.ekquickfind.app.search.SearchActivity

/**
 * Lets ekQuickFind be the device's digital assistant: the assistant gesture (side-key long press or
 * corner swipe) then opens the search surface over whatever is on screen.
 */
class EkVoiceInteractionService : VoiceInteractionService()

class EkVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = EkSession(this)
}

private class EkSession(
    service: VoiceInteractionSessionService,
) : VoiceInteractionSession(service) {
    override fun onShow(
        args: Bundle?,
        showFlags: Int,
    ) {
        super.onShow(args, showFlags)
        // The session itself draws nothing; it hands straight over to the search surface.
        val intent =
            Intent(context, SearchActivity::class.java)
                .setAction(Intent.ACTION_ASSIST)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        args?.getString(SearchManager.QUERY)?.let { intent.putExtra(SearchManager.QUERY, it) }
        startAssistantActivity(intent)
        hide()
    }
}

/** The platform requires an assistant to name a recogniser; ekQuickFind doesn't do speech, so it says so. */
class EkRecognitionService : RecognitionService() {
    override fun onStartListening(
        recognizerIntent: Intent?,
        listener: Callback?,
    ) {
        listener?.error(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
    }

    override fun onCancel(listener: Callback?) = Unit

    override fun onStopListening(listener: Callback?) = Unit
}
