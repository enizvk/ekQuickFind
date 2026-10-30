package com.ekquickfind.providers.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * The phone's approximate position for "weather" with no place: the freshest last-known fix, else one quick
 * network fix. Never stored; callers check ACCESS_COARSE_LOCATION first.
 */
class AndroidLocation(
    private val context: Context,
) {
    @SuppressLint("MissingPermission")
    suspend fun approximate(): Pair<Double, Double>? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val last =
            manager
                .getProviders(true)
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
        if (last != null) return last.latitude to last.longitude
        if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null
        return withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, signal, context.mainExecutor) { fix ->
                    cont.resume(fix?.let { it.latitude to it.longitude })
                }
            }
        }
    }

    private companion object {
        const val FIX_TIMEOUT_MS = 4_000L
    }
}
