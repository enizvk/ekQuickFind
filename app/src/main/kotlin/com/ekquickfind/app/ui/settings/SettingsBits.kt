package com.ekquickfind.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ekquickfind.data.prefs.UserPreferences
import kotlinx.coroutines.delay

/** What panels need to show "changed" and reset themselves, without passing it through every section. */
internal val LocalSettingsPrefs = staticCompositionLocalOf<UserPreferences?> { null }
internal val LocalSettingsActions = staticCompositionLocalOf<SettingsActions?> { null }

/** The panel settings search jumped to; it flashes once. */
internal val LocalFocusedPanel = staticCompositionLocalOf<MutableState<String?>> { mutableStateOf(null) }

/** A dot and "Reset" when the panel differs from the defaults. */
@Composable
internal fun PanelResetLine(title: String) {
    val panel = Panel.byTitle(title) ?: return
    val prefs = LocalSettingsPrefs.current ?: return
    val actions = LocalSettingsActions.current ?: return
    if (!panel.isChanged(prefs)) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag("changed:$title"),
        )
        TextButton(onClick = { actions.update { panel.reset(it) } }, modifier = Modifier.testTag("reset-panel:$title")) {
            Text("Reset", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A short flash on the panel settings search opened, so the eye finds it. */
@Composable
internal fun Modifier.flashWhenFocused(title: String): Modifier {
    val focused = LocalFocusedPanel.current
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(focused.value) {
        if (focused.value == title) {
            on = true
            delay(FLASH_MS)
            on = false
            focused.value = null
        }
    }
    val alpha by animateFloatAsState(if (on) 1f else 0f, tween(300), label = "flash")
    return graphicsLayer {
        scaleX = 1f + 0.015f * alpha
        scaleY = 1f + 0.015f * alpha
    }
}

/** Rarer options, folded away until asked for. */
@Composable
internal fun MoreOptions(
    tag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(tag) { mutableStateOf(false) }
    SettingsDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("more:$tag"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (open) "Fewer options" else "More options",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    AnimatedVisibility(open) { Column(content = content) }
}

/** A small ⓘ that explains an option in a sentence or two, with an example. */
@Composable
internal fun InfoTip(
    title: String,
    text: String,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp).testTag("info:$title")) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = "About $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Got it") } },
        )
    }
}

private const val FLASH_MS = 1_200L
