package com.ekquickfind.app.ui.search

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

enum class ClipKind {
    Link,
    Phone,
    Text,
    ;

    companion object {
        private val link = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE)
        private val phone = Regex("^\\+?[0-9 ()./-]+$")

        fun of(text: String): ClipKind {
            val t = text.trim()
            return when {
                link.matches(t) -> Link
                phone.matches(t) && t.count(Char::isDigit) in 6..15 -> Phone
                else -> Text
            }
        }
    }
}

/**
 * True when the clipboard holds text that isn't marked sensitive (passwords). Only the description is checked:
 * that doesn't show Android's "pasted from your clipboard" message, and nothing is read yet.
 */
fun clipboardHasText(context: Context): Boolean {
    val description = context.getSystemService(ClipboardManager::class.java).primaryClipDescription ?: return false
    return description.hasMimeType("text/*") && description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) != true
}

/** A Clipboard chip; the clip is only read when it's tapped, then shown with what can be done with it. Nothing is kept. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClipboardSection(onSearch: (String) -> Unit) {
    val context = LocalContext.current
    var clip by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        val text = clip
        if (text == null) {
            AssistChip(
                onClick = {
                    clip =
                        context
                            .getSystemService(ClipboardManager::class.java)
                            .primaryClip
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                },
                label = { Text("Clipboard") },
                leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("clipboard-chip"),
            )
            return@Column
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().testTag("clipboard-card"),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text.ifEmpty { "The clipboard is empty" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (text.isNotEmpty()) {
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { onSearch(text) },
                            label = { Text("Search") },
                            modifier = Modifier.testTag("clipboard-search"),
                        )
                        when (ClipKind.of(text)) {
                            ClipKind.Link ->
                                AssistChip(onClick = {
                                    val url = text.trim().let { if (it.startsWith("www.", ignoreCase = true)) "https://$it" else it }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                }, label = { Text("Open link") })
                            ClipKind.Phone ->
                                AssistChip(onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, "tel:${text.filter { it.isDigit() || it == '+' }}".toUri()),
                                    )
                                }, label = { Text("Call") })
                            ClipKind.Text -> Unit
                        }
                        AssistChip(onClick = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                            context.startActivity(Intent.createChooser(send, null))
                        }, label = { Text("Share") })
                    }
                }
            }
        }
    }
}
