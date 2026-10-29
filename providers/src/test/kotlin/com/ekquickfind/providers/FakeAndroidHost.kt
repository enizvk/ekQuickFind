package com.ekquickfind.providers

import android.content.Context
import android.content.Intent
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult

/** Records what actions asked the host to do. */
class FakeAndroidHost(
    override val context: Context,
    var grant: Boolean = true,
    var choice: Int? = 0,
    var canStart: Boolean = true,
    /** What a started-for-result activity returns; null means it couldn't start. */
    var result: Int? = android.app.Activity.RESULT_OK,
    /** Runs while the "dialog" is open, for example to flip the fake state on Allow. */
    var onResult: (Intent) -> Unit = {},
) : AndroidActionHost {
    val started = mutableListOf<Intent>()
    val requested = mutableListOf<List<String>>()
    val choices = mutableListOf<Pair<String, List<String>>>()
    var copied: String? = null

    override fun startActivity(intent: Intent): Boolean {
        started += intent
        return canStart
    }

    val startedForResult = mutableListOf<Intent>()

    override suspend fun startForResult(intent: Intent): Int? {
        startedForResult += intent
        onResult(intent)
        return result
    }

    override suspend fun requestPermissions(permissions: List<String>): Boolean {
        requested += permissions
        return grant
    }

    override fun shouldShowRationale(permission: String) = false

    override suspend fun copyToClipboard(
        label: String,
        text: String,
    ) {
        copied = text
    }

    override suspend fun pin(result: SearchResult) = Unit

    override suspend fun unpin(result: SearchResult) = Unit

    override suspend fun hide(result: ResultId) = Unit

    override suspend fun choose(
        title: String,
        options: List<String>,
    ): Int? {
        choices += title to options
        return choice
    }
}
