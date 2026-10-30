package com.ekquickfind.app.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.search.ResultChip
import com.ekquickfind.app.ui.search.ResultRow
import com.ekquickfind.app.ui.search.ResultTile
import com.ekquickfind.app.ui.search.ResultsFont
import com.ekquickfind.app.ui.search.SurfacePart
import com.ekquickfind.app.ui.search.activeHighlight
import com.ekquickfind.app.ui.search.openingGlow
import com.ekquickfind.app.ui.search.surfaceParts
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.app.ui.theme.toSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.SurfacePosition
import com.ekquickfind.data.prefs.UserPreferences

/** Sample results do nothing when tapped; the preview is a picture. */
private object PreviewAction : Action {
    override val label = "Preview"
    override val glyph: Glyph? = null

    override suspend fun perform(host: ActionHost) = ActionOutcome.Stay
}

private fun sample(
    title: String,
    subtitle: String,
    glyph: Glyph,
) = SearchResult(
    ResultId("preview:$title"),
    ProviderId("preview"),
    Section.Apps,
    title,
    subtitle,
    ResultIcon.Vector(glyph),
    1f,
    PreviewAction,
)

private val best = sample("Calculator", "App", Glyph.Calculator)
private val moreApps =
    listOf(sample("Camera", "App", Glyph.Image), sample("Clock", "App", Glyph.History), sample("Notes", "App", Glyph.Document))
private val settingsRows = listOf(sample("Wi-Fi", "Settings", Glyph.Settings), sample("Bluetooth", "Settings", Glyph.Toggle))

/**
 * The whole look in one picture, pinned at the top of Look and feel: the background over a stand-in app, the bar
 * (with its opening animation) and real result rows with the active item, in the chosen text, icons and layout.
 * [compact] shrinks it to the bar and the best match while scrolling.
 */
@Composable
internal fun LookPreview(
    prefs: UserPreferences,
    compact: Boolean,
) {
    val style = prefs.toSurfaceStyle()
    var replay by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // Opaque, so the pinned preview hides the options scrolling under it.
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.testTag("look-preview")) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(Modifier.heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                )
                Text(
                    "Preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
                if (style.barAnimation != null || style.itemAnimation != null) {
                    ReplayButton("Play the animations", "look-preview-replay") { replay += 1 }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .animateContentSize()
                    .testTag("look-preview-picture")
                    .clearAndSetSemantics { contentDescription = "Preview" },
            ) {
                Backdrop(prefs)
                CompositionLocalProvider(
                    LocalSurfaceStyle provides style,
                    LocalDensity provides
                        androidx.compose.ui.unit
                            .Density(density.density, density.fontScale * style.textScale),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bar = @Composable {
                            PreviewBar(
                                Modifier.fillMaxWidth().openingGlow(style.barAnimation, cornerRadius = 28.dp, replay = replay),
                            )
                        }
                        // Same order as the real screen: bar, recent strip and results.
                        surfaceParts(barAtTop = prefs.position == SurfacePosition.Top, stripOnTop = style.stripOnTop).forEach { part ->
                            when (part) {
                                SurfacePart.Field -> bar()
                                SurfacePart.Strip -> if (prefs.recentsStrip) PreviewStrip(prefs)
                                SurfacePart.Panel -> PreviewResults(prefs, compact, replay)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A colourful stand-in for the app underneath, blurred and dimmed like the real background. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Backdrop(prefs: UserPreferences) {
    Box(
        Modifier
            .matchParentSize()
            .then(if (prefs.backgroundStyle != BackgroundStyle.Solid) Modifier.blur((prefs.blurRadiusDp / 2).dp) else Modifier)
            .background(Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFF4FC3F7), Color(0xFF81C784)))),
    )
    val overlay = if (prefs.backgroundStyle == BackgroundStyle.Solid) Color(prefs.solidColor.toInt()) else Color.Black
    Box(Modifier.matchParentSize().background(overlay.copy(alpha = prefs.dimAmount)))
}

@Composable
private fun PreviewResults(
    prefs: UserPreferences,
    compact: Boolean,
    replay: Int,
) {
    val style = LocalSurfaceStyle.current
    PreviewPanel(prefs) {
        ResultsFont {
            Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Replays the item animation when the layout or style changes, or on Play.
                key(replay, prefs.topResult, prefs.selectedAtBottom, prefs.activeItemStyle, prefs.itemAnimation, prefs.itemAnimationOn) {
                    val card = !style.inlineTop || prefs.selectedAtBottom
                    if (card && !prefs.selectedAtBottom) BestMatchCard()
                    // Like the real list: with a card on top the best match leaves its section; otherwise it's marked
                    // in place, as a row, an icon in the grid or a chip, whichever the Apps section uses.
                    val apps = if (card) moreApps else listOf(best) + moreApps
                    SectionSample(style.layoutOf(Section.Apps), apps, selected = if (card) null else best)
                    if (!compact) SectionSample(style.layoutOf(Section.Settings), settingsRows, selected = null)
                    if (prefs.selectedAtBottom) BestMatchCard()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionSample(
    layout: SectionLayout,
    results: List<SearchResult>,
    selected: SearchResult?,
) {
    when (layout) {
        SectionLayout.Rows -> results.forEach { ResultRow(it, emptyList(), {}, selected = it == selected) }
        SectionLayout.Grid ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                results.take(GRID_SAMPLE).forEach { ResultTile(it, emptyList(), {}, Modifier.weight(1f), selected = it == selected) }
                repeat(GRID_SAMPLE - results.size.coerceAtMost(GRID_SAMPLE)) { Box(Modifier.weight(1f)) }
            }
        SectionLayout.Chips ->
            FlowRow(
                Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                results.forEach { ResultChip(it, emptyList(), {}, selected = it == selected) }
            }
    }
}

private const val GRID_SAMPLE = 4

/** The panel behind the results and the strip, like the real one. */
@Composable
private fun PreviewPanel(
    prefs: UserPreferences,
    content: @Composable () -> Unit,
) {
    val frosted = prefs.backgroundStyle == BackgroundStyle.FrostedGlass
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (frosted) 0.6f else 1f),
        border = if (frosted) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

/** The recent strip: a history mark and small chips, in its own panel. */
@Composable
private fun PreviewStrip(prefs: UserPreferences) {
    PreviewPanel(prefs) {
        Row(
            Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp).size(16.dp),
            )
            stripSamples.forEach { ResultChip(it, emptyList(), {}, small = true, tagPrefix = "preview-strip:") }
        }
    }
}

private val stripSamples = listOf(sample("Maps", "App", Glyph.Map), sample("Music", "App", Glyph.Audio))

@Composable
private fun BestMatchCard() {
    Box(Modifier.activeHighlight(selected = true, cornerRadius = 24.dp)) {
        ResultRow(best, emptyList(), {}, prominent = true)
    }
}

/** The bar as it looks in search; a picture, not a field. */
@Composable
internal fun PreviewBar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp))
    }
}
