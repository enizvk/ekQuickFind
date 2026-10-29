package com.ekquickfind.app.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ekquickfind.app.floating.FloatingButtonService

private class AccessRow(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    /** Runtime permissions behind it; empty means "Display over other apps". */
    val permissions: List<String>,
)

private val rows =
    listOf(
        AccessRow(Icons.Outlined.Person, "Contacts", "Find people, call or text them", listOf(Manifest.permission.READ_CONTACTS)),
        AccessRow(Icons.Outlined.Event, "Calendar", "Find events and see what's next", listOf(Manifest.permission.READ_CALENDAR)),
        AccessRow(
            Icons.Outlined.Image,
            "Photos, videos and music",
            "Find your media by name, and photos by what's in them",
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
        ),
        AccessRow(
            Icons.Outlined.Bluetooth,
            "Nearby devices",
            "Turn Bluetooth on and off right from the results",
            listOf(Manifest.permission.BLUETOOTH_CONNECT),
        ),
        AccessRow(Icons.Outlined.Adjust, "Floating button", "Display over other apps: a search button on every screen", emptyList()),
    )

private fun Context.granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun Context.missingRuntime() = OnboardingPermissions.runtime.filterNot { granted(it) }

private fun Context.openOverlaySettings() =
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", packageName, null)))

/**
 * First start: what ekQuickFind can reach and one "Allow all" that asks for it in a row, so nobody has to find
 * these in system settings. Skipping is fine; everything stays in Settings.
 */
@Composable
fun OnboardingScreen(
    floatingButton: Boolean,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    // Re-read what's allowed whenever we come back (from a system dialog or the overlay page).
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val missing = remember(tick) { context.missingRuntime() }
    val canDraw = remember(tick) { Settings.canDrawOverlays(context) }
    var askedRuntime by rememberSaveable { mutableStateOf(false) }
    var askedOverlay by rememberSaveable { mutableStateOf(false) }

    // "Allow all" walks through the steps; the overlay page opens at most once, so backing out of it ends the walk.
    lateinit var allowAll: () -> Unit
    val allRequester =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            askedRuntime = true
            tick++
            allowAll()
        }
    allowAll = {
        when (val step = OnboardingSteps.next(context.missingRuntime(), askedRuntime, Settings.canDrawOverlays(context), floatingButton)) {
            is OnboardingStep.AskRuntime -> allRequester.launch(step.permissions.toTypedArray())
            OnboardingStep.OpenOverlaySettings ->
                if (!askedOverlay) {
                    askedOverlay = true
                    context.openOverlaySettings()
                }
            OnboardingStep.Done -> Unit
        }
    }
    // A single row's Allow asks for just that.
    val rowRequester = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    // The button shows up as soon as it's allowed, not on the next app start.
    LaunchedEffect(canDraw) { if (canDraw && floatingButton) FloatingButtonService.sync(context, enabled = true) }

    val allSet = missing.isEmpty() && (canDraw || !floatingButton)
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize().testTag("onboarding")) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("Welcome to ekQuickFind", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Allow access once and search reaches everything right away. It all stays on your phone, " +
                    "and you can change any of it later in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rows.filter { it.permissions.isNotEmpty() || floatingButton }.forEach { row ->
                val ok = if (row.permissions.isEmpty()) canDraw else row.permissions.all { it !in missing }
                AccessLine(row, ok) {
                    if (row.permissions.isEmpty()) context.openOverlaySettings() else rowRequester.launch(row.permissions.toTypedArray())
                }
            }
            if (allSet) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().testTag("onboarding-start")) { Text("Start searching") }
            } else {
                Button(
                    onClick = { allowAll() },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding-allow-all"),
                ) { Text("Allow all") }
                TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().testTag("onboarding-skip")) { Text("Not now") }
            }
        }
    }
}

@Composable
private fun AccessLine(
    row: AccessRow,
    allowed: Boolean,
    onAllow: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(row.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleMedium)
            Text(row.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (allowed) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "Allowed", tint = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = onAllow) { Text("Allow") }
        }
    }
}
