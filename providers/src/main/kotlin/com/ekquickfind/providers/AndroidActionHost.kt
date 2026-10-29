package com.ekquickfind.providers

import android.content.Context
import android.content.Intent
import com.ekquickfind.core.search.ActionHost

/** The Android side of [ActionHost]; implemented by the search activity. */
interface AndroidActionHost : ActionHost {
    /** An activity context, so started activities get proper window animations. */
    val context: Context

    /** Starts an activity; returns false when nothing can handle the intent. */
    fun startActivity(intent: Intent): Boolean

    /** Starts an activity for its result (for example a system confirmation dialog); null when it can't start. */
    suspend fun startForResult(intent: Intent): Int?

    /** Asks for runtime permissions; returns true when all were granted. */
    suspend fun requestPermissions(permissions: List<String>): Boolean

    /** True after the user denied once without "don't ask again". */
    fun shouldShowRationale(permission: String): Boolean
}

internal fun ActionHost.android(): AndroidActionHost =
    this as? AndroidActionHost ?: error("Android actions need an AndroidActionHost, got ${this::class.simpleName}")
