package com.ekquickfind.providers

import android.content.Context
import android.content.pm.PackageManager
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph

fun interface PermissionChecker {
    fun isGranted(permission: String): Boolean

    companion object {
        fun of(context: Context) = PermissionChecker { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }
}

/** Behind "Enable … search" rows. The host decides between the system dialog and app settings. */
data class RequestPermissionAction(
    val permissions: List<String>,
    override val label: String = "Allow",
) : Action {
    override val glyph = Glyph.Lock

    override suspend fun perform(host: ActionHost): ActionOutcome =
        if (host.android().requestPermissions(permissions)) ActionOutcome.Refresh else ActionOutcome.Stay
}
