package com.ekquickfind.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.search.activeHighlight
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.StripPosition
import com.ekquickfind.data.prefs.SurfacePosition
import com.ekquickfind.data.prefs.TopResultStyle

/** Like [Choice], but each option is a small picture of what it does, with its name below. */
@Composable
internal fun <T> PictureChoice(
    label: String,
    options: List<T>,
    selected: T,
    name: (T) -> String,
    onSelect: (T) -> Unit,
    tag: String,
    info: String? = null,
    picture: @Composable (T) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            info?.let { InfoTip(label, it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val on = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border =
                        BorderStroke(
                            if (on) 2.dp else 1.dp,
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                this.selected = on
                                role = Role.RadioButton
                            }.testTag("$tag:${name(option)}"),
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) { picture(option) }
                        Text(
                            name(option),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.bar(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    color: Color,
) = drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(h / 2))

/** Rows, a grid of icons or chips, as tiny sketches. */
@Composable
internal fun LayoutPicture(layout: SectionLayout) {
    val ink = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val u = size.height / 10
        when (layout) {
            SectionLayout.Rows ->
                repeat(3) { i ->
                    val y = u * (1 + i * 3)
                    drawCircle(ink, u, Offset(size.width * 0.2f, y + u))
                    bar(size.width * 0.32f, y + u * 0.5f, size.width * 0.5f, u, soft)
                }
            SectionLayout.Grid ->
                repeat(2) { r ->
                    repeat(3) { c ->
                        val s = u * 3.2f
                        val x = size.width / 2 - s * 1.8f + c * s * 1.2f
                        drawRoundRect(if (r == 0 && c == 0) ink else soft, Offset(x, u + r * s * 1.2f), Size(s, s), CornerRadius(u))
                    }
                }
            SectionLayout.Chips ->
                listOf(0.55f to 0.25f, 0.4f to 0.6f).forEachIndexed { i, (w, x) ->
                    bar(size.width * (x - w / 2), u * (2 + i * 4), size.width * w, u * 2.4f, if (i == 0) ink else soft)
                }
        }
    }
}

/** A phone with the bar at the top or the bottom. */
@Composable
internal fun PositionPicture(position: SurfacePosition) {
    val ink = MaterialTheme.colorScheme.primary
    val frame = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        val h = size.height
        val w = h * 0.55f
        val x = (size.width - w) / 2
        drawRoundRect(frame, Offset(x, 0f), Size(w, h), CornerRadius(w * 0.18f), style = Stroke(2.dp.toPx()))
        val barY = if (position == SurfacePosition.Top) h * 0.12f else h * 0.78f
        bar(x + w * 0.15f, barY, w * 0.7f, h * 0.1f, ink)
    }
}

/** The three backgrounds over the same colourful "app". */
@Composable
internal fun BackgroundPicture(style: BackgroundStyle) {
    val app = Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFF4FC3F7), Color(0xFF81C784)))
    Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .then(
                    if (style ==
                        BackgroundStyle.Solid
                    ) {
                        Modifier
                    } else {
                        Modifier.blur(6.dp)
                    },
                ).background(app),
        )
        when (style) {
            BackgroundStyle.Solid -> Box(Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceContainer))
            BackgroundStyle.FrostedGlass ->
                Box(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.35f)),
                )
            BackgroundStyle.Blur -> Box(Modifier.fillMaxWidth().height(40.dp).background(Color.Black.copy(alpha = 0.25f)))
        }
    }
}

/** A sample row marked in [style], drawn with the real highlight. */
@Composable
internal fun ActiveItemPicture(style: ActiveItemStyle) {
    val base = LocalSurfaceStyle.current
    CompositionLocalProvider(LocalSurfaceStyle provides base.copy(activeItem = style, itemAnimation = null, inlineTop = false)) {
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .height(30.dp)
                .activeHighlight(selected = true, cornerRadius = 12.dp),
        )
    }
}

/** A big card on top, or the best match marked inside its list. */
@Composable
internal fun TopResultPicture(style: TopResultStyle) {
    val ink = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val u = size.height / 10
        val x = size.width * 0.15f
        val w = size.width * 0.7f
        if (style == TopResultStyle.Featured) {
            drawRoundRect(ink.copy(alpha = 0.3f), Offset(x, 0f), Size(w, u * 4), CornerRadius(u * 1.5f))
            bar(x, u * 5.5f, w * 0.8f, u, soft)
            bar(x, u * 8f, w * 0.6f, u, soft)
        } else {
            // A row of app icons with the best match marked where it is.
            val s = u * 3.4f
            val gap = u * 1.1f
            val left = (size.width - (4 * s + 3 * gap)) / 2
            repeat(4) { i ->
                val ix = left + i * (s + gap)
                drawRoundRect(if (i == 0) ink else soft, Offset(ix, u * 2), Size(s, s), CornerRadius(u))
                if (i == 0) {
                    drawRoundRect(
                        ink,
                        Offset(ix - u * 0.7f, u * 1.3f),
                        Size(s + u * 1.4f, s + u * 1.4f),
                        CornerRadius(u * 1.4f),
                        style =
                            Stroke(
                                u * 0.4f,
                            ),
                    )
                }
                bar(ix, u * 7.2f, s, u * 0.8f, soft)
            }
        }
    }
}

/** A phone with the recent strip above or below the results, next to the bar. */
@Composable
internal fun StripPicture(position: StripPosition) {
    val ink = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val frame = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        val h = size.height
        val w = h * 0.55f
        val x = (size.width - w) / 2
        drawRoundRect(frame, Offset(x, 0f), Size(w, h), CornerRadius(w * 0.18f), style = Stroke(2.dp.toPx()))
        // Results in the middle, bar at the bottom; the strip of chips sits above or below the results.
        drawRoundRect(soft, Offset(x + w * 0.15f, h * 0.3f), Size(w * 0.7f, h * 0.3f), CornerRadius(w * 0.08f))
        bar(x + w * 0.15f, h * 0.82f, w * 0.7f, h * 0.08f, soft)
        val stripY = if (position == StripPosition.Top) h * 0.16f else h * 0.66f
        repeat(3) { i -> bar(x + w * (0.15f + i * 0.25f), stripY, w * 0.2f, h * 0.08f, ink) }
    }
}
