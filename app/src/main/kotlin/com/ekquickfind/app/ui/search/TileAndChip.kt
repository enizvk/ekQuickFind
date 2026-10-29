package com.ekquickfind.app.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.SearchResult

/** Long-press opens the same menu as rows; shared by tiles and chips. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.resultGestures(
    result: SearchResult,
    menuActions: List<Action>,
    onAction: (Action) -> Unit,
    openMenu: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val style = LocalSurfaceStyle.current
    return combinedClickable(
        onClick = { onAction(result.primaryAction) },
        onLongClick =
            if (menuActions.isEmpty()) {
                null
            } else {
                {
                    if (style.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    openMenu()
                }
            },
        onLongClickLabel = "More actions",
    )
}

/** Small icon with a one-line label, for the compact grid. */
@Composable
internal fun ResultTile(
    result: SearchResult,
    menuActions: List<Action>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    highlightQuery: String = "",
    selected: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val exactHighlight = LocalSurfaceStyle.current.exactMatch
    val label =
        remember(
            exactHighlight,
            result.title,
            highlightQuery,
            accent,
            result.emphasis,
        ) { highlighted(result.title, highlightQuery, accent, result.emphasis, exactHighlight) }
    Box(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .activeHighlight(selected, cornerRadius = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .semantics { this.selected = selected }
                    .resultGestures(result, menuActions, onAction) { menuOpen = true }
                    .padding(vertical = 8.dp, horizontal = 2.dp)
                    .testTag("tile:${result.id.value}"),
        ) {
            Box {
                ResultIconView(result.icon, LocalSurfaceStyle.current.tileIconSize, result.handler)
                result.badge?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Text(
                            it.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        ActionMenu(menuOpen, menuActions, { menuOpen = false }, onAction)
    }
}

/** One-line row of small chips for recently opened results, shown next to the search bar. */
@Composable
internal fun RecentsStrip(
    recent: List<SearchResult>,
    menuActions: MenuActions,
    onAction: (SearchResult, Action) -> Unit,
) {
    // A history mark and tonal chips set it apart from the result sections.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("recents-strip")) {
        Icon(
            Icons.Outlined.History,
            contentDescription = "Recently opened",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(18.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(recent, key = { it.id.value }) { r ->
                ResultChip(r, menuActions.forResult(r), { onAction(r, it) }, small = true, tagPrefix = "strip:")
            }
        }
    }
}

/** Compact pill with icon and title, for chip layouts. */
@Composable
internal fun ResultChip(
    result: SearchResult,
    menuActions: List<Action>,
    onAction: (Action) -> Unit,
    selected: Boolean = false,
    small: Boolean = false,
    tagPrefix: String = "chip:",
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CircleShape,
            color = if (small) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (small) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .semantics { this.selected = selected }
                    .resultGestures(result, menuActions, onAction) { menuOpen = true }
                    .testTag("$tagPrefix${result.id.value}"),
        ) {
            val pad = if (small) 4.dp else 6.dp
            Row(
                // Inside the chip's background, so the tint shows on top of it.
                Modifier
                    .activeHighlight(selected, cornerRadius = PILL_CORNER)
                    .padding(start = pad, end = if (small) 10.dp else 14.dp, top = pad, bottom = pad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ResultIconView(result.icon, if (small) 20.dp else 24.dp, result.handler)
                PinMark(result)
                Text(
                    result.title,
                    style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (small) Modifier.widthIn(max = 120.dp) else Modifier,
                )
            }
        }
        ActionMenu(menuOpen, menuActions, { menuOpen = false }, onAction)
    }
}
