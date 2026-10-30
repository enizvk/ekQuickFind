package com.ekquickfind.providers

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** GETs [url] as text; suspends on OkHttp's async call so cancelling the coroutine cancels the request. */
internal suspend fun OkHttpClient.getText(url: String): String =
    suspendCancellableCoroutine { cont ->
        val call =
            newCall(
                Request
                    .Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build(),
            )
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    response.use {
                        if (!it.isSuccessful) cont.resumeWithException(IOException("HTTP ${it.code}")) else cont.resume(it.body.string())
                    }
                }
            },
        )
    }
