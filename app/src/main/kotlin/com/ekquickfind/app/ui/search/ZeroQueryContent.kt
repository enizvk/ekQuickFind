package com.ekquickfind.app.ui.search

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.search.ZeroQueryState
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.data.prefs.SectionLayout

/** Local-only action: forgets a recent row without touching the result itself. */
private class ForgetAction(
    val onForget: () -> Unit,
) : Action {
    override val label = "Remove from recent"
    override val glyph = Glyph.History

    override suspend fun perform(host: ActionHost): ActionOutcome {
        onForget()
        return ActionOutcome.Stay
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZeroQueryContent(
    state: ZeroQueryState,
    menuActions: MenuActions,
    onAction: (SearchResult, Action) -> Unit,
    onQuery: (String) -> Unit,
    onForgetRecent: (SearchResult) -> Unit,
    onClearHistory: () -> Unit = {},
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        ClearHistoryDialog(
            onConfirm = {
                confirmClear = false
                onClearHistory()
            },
            onDismiss = { confirmClear = false },
        )
    }
    if (state.isEmpty) {
        ZeroQueryHint()
        return
    }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .animateContentSize()
            .testTag("zero-query"),
    ) {
        RowsSection("Pinned", state.pinned, menuActions, onAction)
        val style = LocalSurfaceStyle.current
        val clear = @Composable {
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.testTag("clear-history")) { Text("Clear history") }
        }
        // With the recents strip on, recent items already sit next to the bar; don't repeat them here.
        val recent = if (style.recentsStrip) emptyList() else state.recent
        RowsSection(
            title = "Recent",
            rows = recent,
            menuActions = { r -> menuActions.forResult(r) + ForgetAction { onForgetRecent(r) } },
            onAction = onAction,
            layout = style.recentLayout,
            trailing = clear,
        )
        RowsSection("Suggested", state.suggested, menuActions, onAction)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowsSection(
    title: String,
    rows: List<SearchResult>,
    menuActions: MenuActions,
    onAction: (SearchResult, Action) -> Unit,
    layout: SectionLayout = SectionLayout.Rows,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (rows.isEmpty()) return
    Header(title, trailing)
    when (layout) {
        SectionLayout.Rows -> rows.forEach { r -> ResultRow(r, menuActions.forResult(r), { onAction(r, it) }) }
        SectionLayout.Grid ->
            rows.chunked(GRID_COLUMNS).forEach { chunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    chunk.forEach { r -> ResultTile(r, menuActions.forResult(r), { onAction(r, it) }, Modifier.weight(1f)) }
                    repeat(GRID_COLUMNS - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        SectionLayout.Chips ->
            FlowRow(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { rows.forEach { r -> ResultChip(r, menuActions.forResult(r), { onAction(r, it) }) } }
    }
}

@Composable
private fun Header(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).padding(vertical = 4.dp).testTag("zero-$title"),
        )
        trailing?.invoke()
    }
}
