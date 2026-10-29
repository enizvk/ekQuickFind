package com.ekquickfind.providers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

/** Which app opens an intent by default, so results can show that app's icon. */
fun interface HandlerResolver {
    fun packageFor(intent: Intent): String?

    companion object {
        val None = HandlerResolver { null }

        fun of(context: Context): HandlerResolver =
            CachedHandlers { intent ->
                context.packageManager
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo
                    ?.packageName
            }
    }
}

/**
 * Caches lookups by what decides the handler (action, MIME type, scheme, authority), so every JPEG
 * costs one PackageManager call in total, not one per row. Ignores the system chooser ("android").
 */
class CachedHandlers(
    private val delegate: HandlerResolver,
) : HandlerResolver {
    private val cache = ConcurrentHashMap<String, Optional>()

    private data class Optional(
        val value: String?,
    )

    override fun packageFor(intent: Intent): String? {
        val key = "${intent.action}|${intent.type}|${intent.data?.scheme}|${intent.data?.authority}"
        return cache
            .getOrPut(key) { Optional(delegate.packageFor(intent)?.takeIf { it != "android" }) }
            .value
    }
}
