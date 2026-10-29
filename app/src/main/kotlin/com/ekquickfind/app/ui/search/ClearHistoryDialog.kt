package com.ekquickfind.app.ui.search

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear search history?") },
        text = { Text("Recent items and learned ranking are removed from this device. Pinned items stay.") },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm-clear")) { Text("Clear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
