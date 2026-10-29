package com.ekquickfind.app.ui.settings

import android.app.StatusBarManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ekquickfind.app.R
import com.ekquickfind.app.tile.SearchTileService
import com.ekquickfind.app.widget.SearchWidgetReceiver

/** Ways to open ekQuickFind besides its icon, each with whether it's already set up. */
@Composable
fun EntryPointsSection(tileAdded: Boolean) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val widgetPlaced = remember(tick) { widgetCount(context) > 0 }
    val isAssistant = remember(tick) { context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT) }

    SettingsGroup("Ways to open", Icons.Outlined.TouchApp) {
        EntryRow(
            title = "Home screen search bar",
            status = if (widgetPlaced) "Added" else "Not added",
            action = "Add to home screen",
            key = "widget",
        ) { requestWidget(context) }
        SettingsDivider()
        EntryRow(
            title = "Quick Settings tile",
            status = if (tileAdded) "Added" else "Not added",
            action = "Add tile",
            key = "tile",
        ) { requestTile(context) }
        SettingsDivider()
        EntryRow(
            title = "Digital assistant",
            status = if (isAssistant) "ekQuickFind opens with the assistant gesture" else "Not the default assistant",
            action = "Choose assistant",
            key = "assistant",
        ) { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
    }
}

@Composable
private fun EntryRow(
    title: String,
    status: String,
    action: String,
    key: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("entry-status:$key"))
        },
        trailingContent = { TextButton(onClick = onClick, modifier = Modifier.testTag("entry-action:$key")) { Text(action) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private fun widgetComponent(context: Context) = ComponentName(context, SearchWidgetReceiver::class.java)

private fun widgetCount(context: Context) = AppWidgetManager.getInstance(context).getAppWidgetIds(widgetComponent(context)).size

private fun requestWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(widgetComponent(context), null, null)
}

private fun requestTile(context: Context) {
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, SearchTileService::class.java),
        "Search",
        Icon.createWithResource(context, R.drawable.ic_tile_search),
        context.mainExecutor,
    ) { }
}
