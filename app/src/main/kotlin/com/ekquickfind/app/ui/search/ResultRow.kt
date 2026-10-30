package com.ekquickfind.app.ui.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.Emphasis
import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.SearchResult

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultRow(
    result: SearchResult,
    menuActions: List<Action>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    /** Normalised query; matched characters in the title are highlighted. */
    highlightQuery: String = "",
    selected: Boolean = false,
    /** Compact one-line style used for web links. */
    dense: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val exactHighlight = LocalSurfaceStyle.current.exactMatch
    val title =
        remember(
            exactHighlight,
            result.title,
            highlightQuery,
            accent,
            result.emphasis,
        ) { highlighted(result.title, highlightQuery, accent, result.emphasis, exactHighlight) }
    val haptics = LocalHapticFeedback.current
    val style = LocalSurfaceStyle.current
    val compact = dense && !prominent
    val iconSize =
        when {
            prominent -> style.prominentIconSize
            compact -> style.denseIconSize
            else -> style.iconSize
        }
    val verticalPadding =
        when {
            prominent -> style.rowPadding + 4.dp
            compact -> style.denseRowPadding
            else -> style.rowPadding
        }

    Box(modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .activeHighlight(selected, cornerRadius = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .semantics { this.selected = selected }
                    .combinedClickable(
                        onClick = { onAction(result.primaryAction) },
                        onLongClick =
                            if (menuActions.isEmpty()) {
                                null
                            } else {
                                {
                                    if (style.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuOpen = true
                                }
                            },
                        onLongClickLabel = "More actions",
                    ).padding(horizontal = 12.dp, vertical = verticalPadding)
                    .testTag(if (prominent) "top-result" else "result:${result.id.value}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp),
        ) {
            ResultIconView(result.icon, iconSize, result.handler)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        title,
                        style =
                            when {
                                prominent -> MaterialTheme.typography.titleLarge
                                compact -> MaterialTheme.typography.bodyMedium
                                else -> MaterialTheme.typography.bodyLarge
                            },
                        fontWeight = if (prominent) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = result.titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    result.badge?.let { Badge(it) }
                    PinMark(result)
                }
                result.subtitle?.let { subtitle ->
                    // Cross-fades state changes such as a toggle going from Off to On.
                    Crossfade(subtitle, animationSpec = tween(150), label = "subtitle") {
                        Text(
                            it,
                            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            result.toggle?.let { t ->
                Switch(
                    checked = t.checked,
                    onCheckedChange = { onAction(t.action) },
                    modifier = Modifier.testTag("switch:${result.id.value}"),
                )
            }
            result.inlineActions.forEach { action ->
                FilledTonalIconButton(onClick = { onAction(action) }, modifier = Modifier.size(40.dp)) {
                    action.glyph?.let { Icon(it.vector(), contentDescription = action.label) }
                }
            }
        }
        ActionMenu(menuOpen, menuActions, { menuOpen = false }, onAction)
    }
}

@Composable
internal fun ActionMenu(
    open: Boolean,
    actions: List<Action>,
    onClose: () -> Unit,
    onAction: (Action) -> Unit,
) {
    DropdownMenu(expanded = open, onDismissRequest = onClose, shape = RoundedCornerShape(16.dp)) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.label) },
                leadingIcon = action.glyph?.let { g -> { Icon(g.vector(), contentDescription = null) } },
                onClick = {
                    onClose()
                    onAction(action)
                },
            )
        }
    }
}

internal fun highlighted(
    title: String,
    query: String,
    color: androidx.compose.ui.graphics.Color,
    emphasis: Emphasis = Emphasis.Match,
    exact: Boolean = false,
): AnnotatedString {
    val target = MatchTarget(title)
    val ranges =
        when {
            query.isEmpty() -> emptyList()
            emphasis == Emphasis.Completion -> QueryMatcher.completion(query, target)
            else -> QueryMatcher.highlight(query, target, exact)
        }
    return buildAnnotatedString {
        append(title)
        ranges.forEach { addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), it.first, it.last + 1) }
    }
}

/** Small pin shown on pinned results so it's clear a pin took effect. */
@Composable
internal fun PinMark(
    result: SearchResult,
    modifier: Modifier = Modifier,
) {
    if (result.id !in com.ekquickfind.app.ui.theme.LocalPinnedIds.current) return
    Icon(
        androidx.compose.material.icons.Icons.Outlined.PushPin,
        contentDescription = "Pinned",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(16.dp).testTag("pin-mark:${result.id.value}"),
    )
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * The row icon. A handler app (the app that opens the result) replaces plain glyphs, and sits as a
 * small badge over thumbnails and avatars, so every result shows which app it belongs to.
 */
@Composable
fun ResultIconView(
    icon: ResultIcon,
    size: Dp,
    handler: String? = null,
) {
    val handlerIcon = handler?.let { ResultIcon.PackageIcon(it, null, CURRENT_USER) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            icon is ResultIcon.Vector && handlerIcon != null ->
                AsyncImage(model = handlerIcon, contentDescription = null, modifier = Modifier.size(size))
            else -> BaseIcon(icon, size)
        }
        if (handlerIcon != null && (icon is ResultIcon.Thumbnail || icon is ResultIcon.Avatar)) {
            AsyncImage(
                model = handlerIcon,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).size(size * 0.42f).testTag("handler-badge"),
            )
        }
    }
}

@Composable
private fun BaseIcon(
    icon: ResultIcon,
    size: Dp,
) {
    when (icon) {
        is ResultIcon.PackageIcon, is ResultIcon.ShortcutIcon ->
            AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(size))
        is ResultIcon.Thumbnail ->
            Box(
                Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon.fallback.vector(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                AsyncImage(
                    model = icon,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(size).testTag("thumbnail"),
                )
            }
        is ResultIcon.Avatar ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(size)) {
                if (icon.uri != null) {
                    AsyncImage(model = icon.uri, contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            icon.initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        is ResultIcon.Vector ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(size)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon.glyph.vector(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier =
                            Modifier.size(
                                size * 0.55f,
                            ),
                    )
                }
            }
    }
}

/** Serial meaning "the profile this app runs in", for handler icons. */
const val CURRENT_USER = -1L
