package com.ekquickfind.app.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ChoiceDialog(request: ChoiceRequest) {
    AlertDialog(
        onDismissRequest = { request.answer.complete(null) },
        title = { Text(request.title) },
        text = {
            Column {
                request.options.forEachIndexed { index, option ->
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { request.answer.complete(index) }
                                .padding(vertical = 14.dp)
                                .testTag("choice-$index"),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { request.answer.complete(null) }) { Text("Cancel") } },
    )
}
