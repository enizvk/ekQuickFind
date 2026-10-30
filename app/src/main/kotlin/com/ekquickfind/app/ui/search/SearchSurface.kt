package com.ekquickfind.app.ui.search

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ekquickfind.app.ui.theme.LocalBlurAvailable
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.SearchState
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.SectionResults
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.SurfacePosition
import com.ekquickfind.providers.sites.KeywordHint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Builds the long-press menu for a result; lets the caller add app-level actions such as Pin. */
fun interface MenuActions {
    fun forResult(result: SearchResult): List<Action>
}

@Composable
fun SearchSurface(
    query: String,
    onQueryChange: (String) -> Unit,
    engineState: SearchState,
    /** Called with the selected result (the top result unless arrow keys moved it). */
    onSubmit: (SearchResult) -> Unit,
    onAction: (SearchResult, Action) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    menuActions: MenuActions = MenuActions { it.secondaryActions },
    expandedLayout: Boolean = false,
    /** False when cross-window blur is off (power saving and similar); the scrim compensates. */
    blurAvailable: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
    /** Flips exact match from the bar; null hides the toggle. */
    onToggleExact: (() -> Unit)? = null,
    /** A site keyword typed at the start, coloured in the bar. */
    keywordHint: KeywordHint? = null,
    /** Lets the caller attach a shared-element transition to the query bar. */
    barModifier: Modifier = Modifier,
    /** Recently opened results for the optional strip next to the bar. */
    recentStrip: List<SearchResult> = emptyList(),
    /** Shown first in the recent strip before anything is typed (the Clipboard chip). */
    stripLead: (@Composable () -> Unit)? = null,
    onClearRecents: (() -> Unit)? = null,
    /** Null hides the panel while the query is empty. */
    zeroQueryContent: (@Composable () -> Unit)? = { ZeroQueryHint() },
) {
    val style = LocalSurfaceStyle.current
    val state = remember(engineState, style.inlineTop) { if (style.inlineTop) engineState.withTopInline() else engineState }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backEdgeSign by remember { mutableFloatStateOf(1f) }
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrimTarget by animateFloatAsState(if (appear.targetState) 1f else 0f, tween(EXIT_MS), label = "scrim")
    val expanded = remember(state.query.raw) { mutableStateMapOf<Section, Boolean>() }
    // Selection follows the result itself, so late-arriving results don't yank it back to the top.
    var selectedId by remember(state.query.raw) { mutableStateOf<ResultId?>(null) }
    val rows = remember(state, expanded.toMap()) { visibleRows(state, expanded) }
    // Until arrow keys move it, the best match is selected, wherever it's shown.
    val selected = rows.indexOfFirst { it.id == (selectedId ?: engineState.top?.result?.id) }.coerceAtLeast(0)
    val atTop = style.position == SurfacePosition.Top

    val dismiss: () -> Unit = {
        if (appear.targetState) {
            appear.targetState = false
            keyboard?.hide()
            scope.launch {
                snapshotFlow { appear.isIdle && !appear.currentState }.first { it }
                onDismiss()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (style.autoOpenKeyboard) {
            focus.requestFocus()
            keyboard?.show()
        }
    }

    PredictiveBackHandler { progress ->
        try {
            progress.collect {
                backEdgeSign = if (it.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                backProgress = it.progress
            }
            onDismiss()
        } catch (e: CancellationException) {
            backProgress = 0f
            throw e
        }
    }

    val gestures = remember(atTop) { SurfaceGestures({ keyboard?.hide() }, { dismiss() }, pullSign = if (atTop) -1f else 1f) }

    val scrimColor =
        when (style.background) {
            BackgroundStyle.Solid -> Color(style.solidColor.toInt())
            else -> Color.Black
        }
    // Without blur, a stronger dim keeps it obvious the surface is open.
    val dim =
        if (style.background != BackgroundStyle.Solid &&
            !blurAvailable
        ) {
            (style.dimAmount + 0.25f).coerceAtMost(0.9f)
        } else {
            style.dimAmount
        }
    val scrimAlpha = scrimTarget * (1f - backProgress) * (1f - (gestures.pull / (DISMISS_PULL_PX * 3)).coerceIn(0f, 1f))

    val enter: EnterTransition =
        if (style.reduceMotion) {
            fadeIn(tween(ENTER_MS))
        } else {
            fadeIn(tween(ENTER_MS)) +
                slideInVertically(tween(ENTER_MS)) { if (atTop) -it / 6 else it / 6 }
        }
    val exit: ExitTransition =
        if (style.reduceMotion) {
            fadeOut(tween(EXIT_MS))
        } else {
            fadeOut(tween(EXIT_MS)) +
                slideOutVertically(tween(EXIT_MS)) { if (atTop) -it / 8 else it / 8 }
        }

    // Text size scales on top of Android's font size; sp-based text and layout grow with it.
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalBlurAvailable provides blurAvailable,
        LocalDensity provides Density(density.density, density.fontScale * style.textScale),
    ) {
        Box(
            modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = dim * scrimAlpha))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss)
                .semantics { contentDescription = "Dismiss search" }
                .testTag("scrim"),
        ) {
            AnimatedVisibility(
                visibleState = appear,
                enter = enter,
                exit = exit,
                modifier =
                    Modifier
                        .align(
                            when {
                                expandedLayout -> Alignment.Center
                                atTop -> Alignment.TopCenter
                                else -> Alignment.BottomCenter
                            },
                        ).windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Column(
                    modifier =
                        Modifier
                            .then(if (expandedLayout) Modifier.widthIn(max = 640.dp) else Modifier.fillMaxWidth())
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .graphicsLayer {
                                // Predictive back: shrink and drift toward the swipe edge before the gesture commits.
                                val scale = 1f - 0.14f * backProgress
                                scaleX = scale
                                scaleY = scale
                                translationX = backEdgeSign * 32.dp.toPx() * backProgress
                                translationY = gestures.pull * 0.5f * (if (atTop) -1f else 1f)
                                alpha = 1f - 0.3f * backProgress
                            }
                            // Taps inside the surface must not reach the scrim. A gesture detector (not clickable)
                            // keeps child semantics separate for screen readers.
                            .pointerInput(Unit) { detectTapGestures { } }
                            .testTag("surface"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val field = @Composable {
                        QueryField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSubmit = {
                                if (style.enterRunsSelected) rows.getOrNull(selected)?.let(onSubmit) else keyboard?.hide()
                            },
                            onMoveSelection = { delta ->
                                if (rows.isNotEmpty()) selectedId = rows[(selected + delta).coerceIn(0, rows.lastIndex)].id
                            },
                            focus = focus,
                            onOpenSettings = onOpenSettings,
                            modifier = barModifier,
                            exactMatch = style.exactMatch,
                            onToggleExact = onToggleExact,
                            keyword = keywordHint,
                        )
                    }
                    val lead = stripLead?.takeIf { query.isBlank() }
                    val showStrip = style.recentsStrip && (recentStrip.isNotEmpty() || lead != null)
                    // Nothing typed: the recents sit at the top inside the panel. Typing: their own strip next to it.
                    val stripInPanel = state.query.isBlank && zeroQueryContent != null
                    // No end padding: chips scroll to the panel's rounded edge instead of a square cut.
                    val stripRow = @Composable { modifier: Modifier ->
                        Box(modifier) { RecentsStrip(recentStrip, menuActions, onAction, lead, onClearRecents) }
                    }
                    val strip = @Composable {
                        if (showStrip && !stripInPanel) {
                            // The same panel as the results, so the two read as one piece.
                            Panel(Modifier.fillMaxWidth()) {
                                ResultsFont { stripRow(Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp)) }
                            }
                        }
                    }
                    val panel = @Composable { modifier: Modifier ->
                        if (!state.query.isBlank || zeroQueryContent != null) {
                            Panel(modifier) {
                                ResultsFont {
                                    if (state.query.isBlank) {
                                        Column {
                                            if (showStrip) stripRow(Modifier.padding(start = 8.dp, top = 8.dp))
                                            Box(Modifier.padding(8.dp)) { zeroQueryContent?.invoke() }
                                        }
                                    } else {
                                        val slot = rows.getOrNull(selected)?.takeIf { style.selectedAtBottom }
                                        Column {
                                            // Without a cue, results hidden below the fold (often under the selected slot) go unnoticed.
                                            val moreBelow by remember { derivedStateOf { listState.canScrollForward } }
                                            Box(Modifier.weight(1f, fill = false)) {
                                                ResultList(
                                                    state = state,
                                                    listState = listState,
                                                    expanded = expanded,
                                                    selectedId = rows.getOrNull(selected)?.id,
                                                    menuActions = menuActions,
                                                    onAction = onAction,
                                                    modifier = Modifier.nestedScroll(gestures).fadeBottomEdge(moreBelow),
                                                    slotId = slot?.id,
                                                )
                                            }
                                            val belowLabel by remember(state.sections) {
                                                derivedStateOf {
                                                    val current =
                                                        listState.layoutInfo.visibleItemsInfo
                                                            .lastOrNull()
                                                            ?.contentType as? Section
                                                    moreBelowLabel(state.sections.map { it.section.title }, current?.title)
                                                }
                                            }
                                            MoreBelowBar(
                                                visible = moreBelow,
                                                label = belowLabel,
                                                onClick = {
                                                    keyboard?.hide()
                                                    scope.launch {
                                                        listState.animateScrollBy(
                                                            listState.layoutInfo.viewportSize.height * 0.8f,
                                                        )
                                                    }
                                                },
                                            )
                                            // A scroll container that never moves, so swipes on the slot reach the same gestures as the list.
                                            slot?.let {
                                                SelectedSlot(
                                                    it,
                                                    menuActions,
                                                    onAction,
                                                    state.query.normalized,
                                                    Modifier.nestedScroll(gestures).verticalScroll(rememberScrollState()),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // The strip and the results sit close together, as one block; the bar keeps the usual gap.
                    val parts = surfaceParts(barAtTop = atTop, stripOnTop = style.stripOnTop)
                    if (atTop) field()
                    Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        parts.forEach { part ->
                            when (part) {
                                SurfacePart.Strip -> strip()
                                SurfacePart.Panel -> panel(Modifier.weight(1f, fill = false))
                                SurfacePart.Field -> Unit
                            }
                        }
                    }
                    if (!atTop) field()
                }
            }
        }
    }

    // Late results (a translation, the weather) land above the rows already shown, and the list would keep showing
    // those. Back to the top on each new query or new first row, unless you scrolled this query's results yourself.
    val currentQuery by rememberUpdatedState(state.query.raw)
    var scrolledQuery by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) scrolledQuery = currentQuery }
    }
    LaunchedEffect(state.query.raw, rows.firstOrNull()?.id) {
        if (scrolledQuery != state.query.raw && listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }
}

internal enum class SurfacePart { Field, Strip, Panel }

/** Top to bottom: the search bar stays at its edge, the strip goes above or below the results. */
internal fun surfaceParts(
    barAtTop: Boolean,
    stripOnTop: Boolean,
): List<SurfacePart> =
    listOfNotNull(
        SurfacePart.Field.takeIf { barAtTop },
        SurfacePart.Strip.takeIf { stripOnTop },
        SurfacePart.Panel,
        SurfacePart.Strip.takeIf { !stripOnTop },
        SurfacePart.Field.takeIf { !barAtTop },
    )

/** Result panel: opaque by default, translucent tinted glass with a hairline border for Frosted glass. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val frosted = LocalSurfaceStyle.current.background == BackgroundStyle.FrostedGlass
    // Without blur behind, see-through panels just show the app below sharply, so go almost opaque.
    val alpha = if (LocalBlurAvailable.current) 0.55f else 0.94f
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (frosted) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha) else MaterialTheme.colorScheme.surfaceContainer,
        // A translucent colour isn't a theme colour, so contentColorFor() can't pick text colour for it.
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (frosted) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null,
        tonalElevation = if (frosted) 0.dp else 2.dp,
        modifier = modifier.testTag("panel"),
        content = content,
    )
}

/**
 * The top result put back at the front of its own section, for the "in its section" layout. A top result whose
 * section has nothing else gets that section first, since it holds the best match.
 */
internal fun SearchState.withTopInline(): SearchState {
    val best = top ?: return this
    val home = sections.indexOfFirst { it.section == best.result.section }
    val regrouped =
        if (home >= 0) {
            sections.mapIndexed { i, s -> if (i == home) s.copy(items = listOf(best) + s.items) else s }
        } else {
            listOf(SectionResults(best.result.section, listOf(best), cap = 1)) + sections
        }
    return copy(top = null, sections = regrouped)
}

/** Rows in display order, used for keyboard selection: top result first, then visible section items. */
internal fun visibleRows(
    state: SearchState,
    expanded: Map<Section, Boolean>,
): List<SearchResult> =
    listOfNotNull(state.top?.result) +
        state.sections.flatMap { s -> (if (expanded[s.section] == true) s.items else s.visible).map { it.result } }

/** Hides the keyboard when results scroll and dismisses when the list is pulled past its edge. */
private class SurfaceGestures(
    private val hideKeyboard: () -> Unit,
    private val dismiss: () -> Unit,
    private val pullSign: Float,
) : NestedScrollConnection {
    var pull by mutableFloatStateOf(0f)

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 1f) hideKeyboard()
        val toward = available.y * pullSign
        if (pull > 0f && toward < 0f) {
            val used = maxOf(toward, -pull)
            pull += used
            return Offset(0f, used * pullSign)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val toward = available.y * pullSign
        if (source == NestedScrollSource.UserInput && toward > 0f) {
            pull += toward
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pull > DISMISS_PULL_PX) dismiss()
        pull = 0f
        return Velocity.Zero
    }
}

internal const val DISMISS_PULL_PX = 220f
internal const val ENTER_MS = 220
internal const val EXIT_MS = 160
