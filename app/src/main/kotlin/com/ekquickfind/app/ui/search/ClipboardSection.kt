package com.ekquickfind.app.ui.search

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
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
 * that doesn't show Android's "pasted from your clipboard" message. Android answers only once the window has focus.
 */
fun clipboardHasText(context: Context): Boolean {
    val description = context.getSystemService(ClipboardManager::class.java).primaryClipDescription ?: return false
    return description.hasMimeType("text/*") && !description.isSensitive()
}

private fun ClipDescription.isSensitive() = extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true

/** The current clip's text, or null when there's none or Android marks it sensitive. This read shows Android's message. */
internal fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (clipboard.primaryClipDescription?.isSensitive() != false) return null
    return clipboard.primaryClip
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.trim()
        ?.ifEmpty { null }
}

/**
 * Remembered clips, newest first, and how to remember or forget one. [open] is whether the clips are showing;
 * [onOpen] reads the current clip and shows them. [chipInStrip] puts the Clipboard chip in the recent strip.
 */
class ClipboardSlot(
    val clips: List<String>,
    val remember: (String) -> Unit,
    val forget: (String) -> Unit,
    val open: Boolean,
    val onOpen: () -> Unit,
    val chipInStrip: Boolean = false,
)

/**
 * A Clipboard chip. Tapping it reads the current clip (and remembers it), then shows the remembered clips as
 * pages to swipe through, with dots below saying which one you're on. While open, new clips show up by themselves.
 */
@Composable
internal fun ClipboardSection(
    slot: ClipboardSlot,
    onSearch: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        if (!slot.open) {
            // With the recent strip on, the chip sits in the strip instead.
            if (slot.chipInStrip) return@Column
            AssistChip(
                onClick = slot.onOpen,
                label = { Text("Clipboard") },
                leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("clipboard-chip"),
            )
            return@Column
        }
        // Open means the user asked for the clipboard: pick up what they copy meanwhile, both when they come
        // back to search and when the clip changes while search is in front (Android only tells the focused app).
        val focused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(focused) { if (focused) readClipboard(context)?.let(slot.remember) }
        DisposableEffect(Unit) {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val listener = ClipboardManager.OnPrimaryClipChangedListener { readClipboard(context)?.let(slot.remember) }
            clipboard.addPrimaryClipChangedListener(listener)
            onDispose { clipboard.removePrimaryClipChangedListener(listener) }
        }
        val clips = slot.clips
        if (clips.isEmpty()) {
            Text(
                "Nothing copied yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
            return@Column
        }
        val pager = rememberPagerState { clips.size }
        // A newly copied clip goes to the front; show it.
        LaunchedEffect(clips.first()) { pager.animateScrollToPage(0) }
        HorizontalPager(state = pager, pageSpacing = 8.dp, modifier = Modifier.testTag("clipboard-pager")) { page ->
            ClipCard(clips[page], onSearch, onForget = slot.forget)
        }
        if (clips.size > 1) PageDots(count = clips.size, current = pager.currentPage)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClipCard(
    text: String,
    onSearch: (String) -> Unit,
    onForget: (String) -> Unit,
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().testTag("clipboard-card"),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { onSearch(text) }, label = { Text("Search") }, modifier = Modifier.testTag("clipboard-search"))
                // Puts this clip back on the clipboard, handy for an older one; Android shows its own "Copied" note.
                AssistChip(onClick = {
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("ekQuickFind", text))
                }, label = { Text("Copy") }, modifier = Modifier.testTag("clipboard-copy"))
                when (ClipKind.of(text)) {
                    ClipKind.Link ->
                        AssistChip(onClick = {
                            val url = text.trim().let { if (it.startsWith("www.", ignoreCase = true)) "https://$it" else it }
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }, label = { Text("Open link") })
                    ClipKind.Phone ->
                        AssistChip(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${text.filter { it.isDigit() || it == '+' }}".toUri()))
                        }, label = { Text("Call") })
                    ClipKind.Text -> Unit
                }
                AssistChip(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                }, label = { Text("Share") })
                AssistChip(onClick = { onForget(text) }, label = { Text("Forget") }, modifier = Modifier.testTag("clipboard-forget"))
            }
        }
    }
}

/** One dot per page; the current one stretches into a pill. */
@Composable
private fun PageDots(
    count: Int,
    current: Int,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).testTag("clipboard-dots"),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 16.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** The Clipboard chip as a small strip chip, first in the recent strip. */
@Composable
internal fun ClipboardStripChip(onOpen: () -> Unit) {
    // Same box as a small ResultChip (padding, 20dp icon slot, gap, text), so it lines up with the recents.
    // A plain clickable, not Surface(onClick), which would pad it to a 48dp touch box and push it down.
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onOpen).testTag("clipboard-chip"),
    ) {
        Row(
            Modifier.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text("Clipboard", style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
