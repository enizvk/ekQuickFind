package com.ekquickfind.app.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.SearchState
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.SectionLayout

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ResultList(
    state: SearchState,
    listState: LazyListState,
    expanded: SnapshotStateMap<Section, Boolean>,
    selectedId: ResultId?,
    menuActions: MenuActions,
    onAction: (SearchResult, Action) -> Unit,
    modifier: Modifier = Modifier,
    /** Result shown in the fixed slot by the search bar; left out of the list so it isn't shown twice. */
    slotId: ResultId? = null,
) {
    val style = LocalSurfaceStyle.current
    val highlight = state.query.normalized

    // Reduce motion keeps fades but drops movement.
    fun Modifier.motion(scope: LazyItemScope) =
        with(scope) {
            if (style.reduceMotion) animateItem(fadeInSpec = tween(120), placementSpec = null, fadeOutSpec = tween(90)) else animateItem()
        }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("results").semantics { motionReduced = style.reduceMotion },
        contentPadding = PaddingValues(8.dp),
    ) {
        state.top?.takeIf { it.result.id != slotId }?.let { top ->
            item(key = "top") {
                val topSelected = selectedId == top.result.id
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardColor(topSelected),
                    // The card itself is the highlight, so its own clip can't trim it.
                    modifier =
                        Modifier
                            .motion(this)
                            .activeHighlight(topSelected, cornerRadius = 24.dp)
                            .semantics { selected = topSelected },
                ) {
                    // Quick cross-fade when the best match changes while typing.
                    AnimatedContent(top.result, transitionSpec = {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(90))
                    }, contentKey = { it.id }, label = "top") { r ->
                        ResultRow(
                            r,
                            menuActions.forResult(r),
                            { onAction(r, it) },
                            prominent = true,
                            highlightQuery = highlight,
                        )
                    }
                }
            }
        }
        state.sections.forEach { section ->
            stickyHeader(key = "header:${section.section}", contentType = section.section) {
                SectionHeader(section.section.title, Modifier.motion(this).testTag("section-${section.section}"))
            }
            if (section.isLoading) {
                item(key = "loading:${section.section}", contentType = section.section) {
                    Box(Modifier.fillMaxWidth().padding(12.dp).motion(this), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(40.dp).testTag("loading-${section.section}"))
                    }
                }
            }
            val rows =
                (
                    if (expanded[section.section] ==
                        true
                    ) {
                        section.items
                    } else {
                        section.visible
                    }
                ).map { it.result }.filter { it.id != slotId }
            when (style.layoutOf(section.section)) {
                SectionLayout.Rows ->
                    rows.forEach { r ->
                        item(key = r.id.value, contentType = section.section) {
                            ResultRow(
                                r,
                                menuActions.forResult(r),
                                { onAction(r, it) },
                                Modifier.motion(this),
                                highlightQuery = highlight,
                                selected =
                                    selectedId == r.id,
                                dense = style.isDense(section.section),
                            )
                        }
                    }
                SectionLayout.Grid ->
                    rows.chunked(GRID_COLUMNS).forEachIndexed { i, chunk ->
                        item(key = "grid:${section.section}:$i:${chunk.first().id.value}", contentType = section.section) {
                            Row(Modifier.fillMaxWidth().motion(this), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                chunk.forEach { r ->
                                    ResultTile(
                                        r,
                                        menuActions.forResult(r),
                                        { onAction(r, it) },
                                        Modifier.weight(1f),
                                        highlight,
                                        selectedId == r.id,
                                    )
                                }
                                repeat(GRID_COLUMNS - chunk.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                SectionLayout.Chips ->
                    item(key = "chips:${section.section}", contentType = section.section) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).motion(this),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rows.forEach { r ->
                                ResultChip(r, menuActions.forResult(r), { onAction(r, it) }, selected = selectedId == r.id)
                            }
                        }
                    }
            }
            if (section.hasMore && expanded[section.section] != true) {
                item(key = "more:${section.section}", contentType = section.section) {
                    TextButton(
                        onClick = { expanded[section.section] = true },
                        modifier = Modifier.padding(start = 4.dp).motion(this).testTag("more-${section.section}"),
                    ) { Text("Show ${section.items.size - section.cap} more") }
                }
            }
        }
    }
}

/** The selected result in its fixed place at the bottom of the panel, next to the search bar. */
@Composable
internal fun SelectedSlot(
    result: SearchResult,
    menuActions: MenuActions,
    onAction: (SearchResult, Action) -> Unit,
    highlightQuery: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor(selected = true),
        modifier =
            modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .activeHighlight(selected = true, cornerRadius = 24.dp)
                .semantics { selected = true }
                .testTag("selected-slot"),
    ) {
        AnimatedContent(result, transitionSpec = {
            fadeIn(tween(140)) togetherWith fadeOut(tween(90))
        }, contentKey = { it.id }, label = "slot") { r ->
            ResultRow(r, menuActions.forResult(r), { onAction(r, it) }, prominent = true, highlightQuery = highlightQuery)
        }
    }
}

/** The top card's own colour gives way to the active-item highlight while it's selected. */
@Composable
private fun cardColor(selected: Boolean) =
    animateColorAsState(
        if (selected) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
        if (LocalSurfaceStyle.current.itemAnimation != null) tween(240) else snap(),
        label = "card",
    ).value

@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(
                        alpha =
                            if (LocalSurfaceStyle.current.background ==
                                com.ekquickfind.data.prefs.BackgroundStyle.FrostedGlass
                            ) {
                                0f
                            } else {
                                1f
                            },
                    ),
                ).padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun ZeroQueryHint() {
    // A quiet, centred note: the icon above, the text below.
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = dim, modifier = Modifier.size(20.dp))
        Text(
            "Type to search apps, contacts, settings, files, quick answers and the web.",
            style = MaterialTheme.typography.bodyMedium,
            color = dim,
            textAlign = TextAlign.Center,
        )
    }
}

internal const val GRID_COLUMNS = 5

/** Not announced; lets tests confirm which motion mode the list uses. */
val MotionReduced = SemanticsPropertyKey<Boolean>("MotionReduced")
var SemanticsPropertyReceiver.motionReduced by MotionReduced

/** Says what's still below the visible results, e.g. "More below: Settings · Web". */
internal fun moreBelowLabel(
    order: List<String>,
    current: String?,
): String {
    if (current == null || current !in order) return "More results below"
    val after = order.dropWhile { it != current }.drop(1)
    return if (after.isEmpty()) "More $current below" else "More below: ${after.joinToString(" · ")}"
}

/** A thin line between the list and the selected result while more results are below; tapping it scrolls down. */
@Composable
internal fun MoreBelowBar(
    visible: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(150)) + expandVertically(tween(150)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp)
                .testTag("more-below"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Fades the last few rows into the panel so it reads as "this continues". */
internal fun Modifier.fadeBottomEdge(active: Boolean): Modifier =
    if (!active) {
        this
    } else {
        graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
            drawContent()
            val fade = 32.dp.toPx()
            drawRect(
                Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - fade, endY = size.height),
                topLeft = Offset(0f, size.height - fade),
                blendMode = BlendMode.DstIn,
            )
        }
    }
