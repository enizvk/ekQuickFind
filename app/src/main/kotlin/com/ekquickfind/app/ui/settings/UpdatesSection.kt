package com.ekquickfind.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekquickfind.app.update.UpdateState
import com.ekquickfind.app.update.UpdatesViewModel

@Composable
fun UpdatesSection(vm: UpdatesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val auto by vm.autoUpdate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Permissions are granted in system screens; re-read them when the owner comes back.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val canInstall = remember(tick) { vm.canInstall() }
    val notificationsAllowed =
        remember(tick) { context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    // The store build has no update source (its store updates it), so there's nothing to show.
    if (state is UpdateState.NotConfigured) return
    SettingsGroup("Updates", Icons.Outlined.SystemUpdate) {
        Status(state, vm, onConfirm = { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
        SwitchRow(
            "Update automatically",
            "Checks once a day on Wi-Fi and installs new versions",
            auto,
            { vm.setAutoUpdate(it) },
            "auto-update",
        )
        if (!canInstall) {
            ActionLine("Allow ekQuickFind to install its updates", "Allow", "allow-install") {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            }
        }
        if (auto && !notificationsAllowed) {
            ActionLine("Allow notifications to hear when an update needs you", "Allow", "allow-notifications") {
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun Status(
    state: UpdateState,
    vm: UpdatesViewModel,
    onConfirm: (Intent) -> Unit,
) {
    when (state) {
        UpdateState.NotConfigured -> Unit
        UpdateState.Idle -> ActionLine("Not checked yet", "Check now", "update-action") { vm.check() }
        UpdateState.Checking -> Progress("Checking for updates", null)
        UpdateState.UpToDate -> ActionLine("You have the latest version", "Check now", "update-action") { vm.check() }
        is UpdateState.Available ->
            Column {
                ActionLine("Version ${state.release.versionName} is available", "Install", "update-action") { vm.install(state.release) }
                state.release.notes?.let { Hint(it, "update-notes") }
            }
        is UpdateState.Downloading -> Progress("Downloading ${state.release.versionName}", state.progress)
        is UpdateState.Installing -> Progress("Installing ${state.release.versionName}", null)
        is UpdateState.NeedsConfirmation ->
            ActionLine(
                "Confirm the install to finish",
                "Confirm",
                "update-action",
            ) { onConfirm(state.confirm) }
        is UpdateState.Failed ->
            ActionLine("Couldn't update: ${state.reason}", "Try again", "update-action") {
                state.release?.let { vm.install(it) } ?: vm.check()
            }
    }
}

@Composable
private fun ActionLine(
    text: String,
    action: String,
    tag: String,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).testTag("update-status"))
        TextButton(onClick = onClick, modifier = Modifier.testTag(tag)) { Text(action) }
    }
}

@Composable
private fun Progress(
    text: String,
    progress: Float?,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            if (progress != null) "$text… ${(progress * 100).toInt()}%" else "$text…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("update-status"),
        )
        if (progress != null) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    }
}

@Composable
private fun Hint(
    text: String,
    tag: String,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag(tag),
    )
}
