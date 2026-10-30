package com.ekquickfind.providers.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.nl.translate.Translator as MlTranslator

/** Languages both translators handle (ML Kit's list), as codes like "bg". */
object TranslateLanguages {
    val codes: List<String> by lazy { TranslateLanguage.getAllLanguages() }
}

/** ML Kit on the phone: works offline once each language (about 30 MB) is downloaded. */
class OnDeviceTranslator : TextTranslator {
    override val name = "On this phone"

    private val identifier by lazy { LanguageIdentification.getClient() }
    private val models by lazy { RemoteModelManager.getInstance() }
    private val clients = mutableMapOf<String, MlTranslator>()
    private val downloading = mutableSetOf<String>()

    override suspend fun translate(
        text: String,
        to: String,
    ): TranslateOutcome =
        runCatching {
            val target = TranslateLanguage.fromLanguageTag(to) ?: return TranslateOutcome.Failed
            // "und" when it can't tell; short text is most often English.
            val source = TranslateLanguage.fromLanguageTag(identifier.identifyLanguage(text).await()) ?: TranslateLanguage.ENGLISH
            if (source == target) return TranslateOutcome.Done(text, source)
            val key = "$source>$target"
            val client =
                clients.getOrPut(key) {
                    Translation.getClient(
                        TranslatorOptions
                            .Builder()
                            .setSourceLanguage(source)
                            .setTargetLanguage(target)
                            .build(),
                    )
                }
            val missing = listOf(target, source).firstOrNull { !models.isModelDownloaded(TranslateRemoteModel.Builder(it).build()).await() }
            if (missing != null) {
                // Download in the background; the card says it's getting ready until then.
                if (downloading.add(key)) client.downloadModelIfNeeded().addOnCompleteListener { downloading.remove(key) }
                return TranslateOutcome.Preparing(missing)
            }
            TranslateOutcome.Done(client.translate(text).await(), source)
        }.getOrElse { if (it is kotlinx.coroutines.CancellationException) throw it else TranslateOutcome.Failed }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
