package com.ekquickfind.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ekquickfind.data.prefs.UserPreferences

/** One-tap looks above the detailed options; the one that matches the current settings is marked. */
@Composable
internal fun QuickLooks(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Quick looks", Icons.Outlined.Style) {
        Text(
            "Start from a look, then fine-tune anything below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LookPreset.entries.forEach { preset ->
                val applied = preset.isApplied(prefs)
                Surface(
                    onClick = { actions.update { it.withLook(preset) } },
                    shape = RoundedCornerShape(16.dp),
                    color =
                        if (applied) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = if (applied) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                selected = applied
                                role = Role.RadioButton
                            }.testTag("look:${preset.name}"),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(preset.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
