package com.ekquickfind.providers.apps

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.core.net.toUri
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.providers.android

private fun ActionHost.user(serial: Long) = android().context.getSystemService(UserManager::class.java).getUserForSerialNumber(serial)

private fun ActionHost.launcherApps() = android().context.getSystemService(LauncherApps::class.java)

data class LaunchAppAction(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
    override val label: String = "Open",
) : Action {
    override val glyph: Glyph? = null

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val user = host.user(userSerial) ?: return ActionOutcome.Stay
        host.launcherApps().startMainActivity(ComponentName(packageName, activityName), user, null, null)
        return ActionOutcome.Close
    }
}

data class AppInfoAction(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
    override val label: String = "App info",
) : Action {
    override val glyph = Glyph.Info

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val user = host.user(userSerial) ?: return ActionOutcome.Stay
        host.launcherApps().startAppDetailsActivity(ComponentName(packageName, activityName), user, null, null)
        return ActionOutcome.Close
    }
}

/** Only offered for personal-profile, non-system apps. */
data class UninstallAction(
    val packageName: String,
    override val label: String = "Uninstall",
) : Action {
    override val glyph = Glyph.Uninstall

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val intent = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
        return if (host.android().startActivity(intent)) ActionOutcome.Refresh else ActionOutcome.Stay
    }
}

data class LaunchShortcutAction(
    val packageName: String,
    val shortcutId: String,
    val userSerial: Long,
    override val label: String = "Open",
) : Action {
    override val glyph = Glyph.Shortcut

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val user = host.user(userSerial) ?: return ActionOutcome.Stay
        host.launcherApps().startShortcut(packageName, shortcutId, null, null, user)
        return ActionOutcome.Close
    }
}
