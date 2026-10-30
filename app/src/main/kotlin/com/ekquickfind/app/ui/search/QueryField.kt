package com.ekquickfind.app.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.theme.LocalBlurAvailable
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.providers.sites.KeywordHint

@Composable
internal fun QueryField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMoveSelection: (Int) -> Unit,
    focus: FocusRequester,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    exactMatch: Boolean = false,
    onToggleExact: (() -> Unit)? = null,
    keyword: KeywordHint? = null,
) {
    val style = LocalSurfaceStyle.current
    val accent = MaterialTheme.colorScheme.primary
    // The keyword fades into colour as it's recognised; Reduce motion just switches.
    val glow by animateFloatAsState(
        if (keyword != null) 1f else 0f,
        if (style.reduceMotion) snap() else tween(KEYWORD_FADE_MS),
        label = "keyword",
    )
    val highlight = remember(keyword?.end, glow, accent) { KeywordHighlight(keyword?.end ?: 0, accent, glow) }
    val frosted = LocalSurfaceStyle.current.background == BackgroundStyle.FrostedGlass
    val alpha = if (LocalBlurAvailable.current) 0.6f else 0.95f
    val container = MaterialTheme.colorScheme.surfaceContainerHighest.let { if (frosted) it.copy(alpha = alpha) else it }
    val fieldColors =
        TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = container,
            unfocusedContainerColor = container,
        )
    val shape = RoundedCornerShape(28.dp)
    val interaction = remember { MutableInteractionSource() }
    // Where the text lies, so the keyword gets a rounded pill behind it (a text background can only be square).
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val leadingIcon: @Composable () -> Unit = {
        AnimatedContent(
            targetState = keyword?.glyph,
            transitionSpec = {
                if (style.reduceMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.6f) + fadeIn()) togetherWith
                        fadeOut(tween(90))
                }
            },
            label = "keyword-icon",
        ) { glyph ->
            if (glyph == null) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            } else {
                Icon(
                    glyph.vector(),
                    contentDescription = "Searching ${keyword?.siteName}",
                    tint = accent,
                    modifier = Modifier.testTag("keyword-icon"),
                )
            }
        }
    }
    val trailingIcon: @Composable () -> Unit = {
        AnimatedContent(
            targetState = query.isEmpty(),
            transitionSpec = { (fadeIn(tween(120)) + scaleIn(tween(120))) togetherWith (fadeOut(tween(90)) + scaleOut(tween(90))) },
            label = "trailing",
        ) { empty ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Small toggle: only whole words as typed count (no partial words, typos or initials).
                onToggleExact?.let { toggle ->
                    FilledTonalIconToggleButton(
                        checked = exactMatch,
                        onCheckedChange = { toggle() },
                        modifier = Modifier.size(32.dp).testTag("exact-toggle"),
                    ) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            contentDescription = if (exactMatch) "Exact match on" else "Exact match off",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                when {
                    !empty ->
                        IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear") }
                    onOpenSettings != null ->
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open-settings")) {
                            Icon(Icons.Outlined.Settings, contentDescription = "ekQuickFind settings")
                        }
                }
            }
        }
    }
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    BasicTextField(
        value = fieldValueFor(field, query),
        onValueChange = {
            field = it
            if (it.text != query) onQueryChange(it.text)
        },
        modifier =
            modifier
                .fillMaxWidth()
                .openingGlow(LocalSurfaceStyle.current.barAnimation, cornerRadius = 28.dp)
                .focusRequester(focus)
                // Hardware keyboards: arrows move the selected result.
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> onMoveSelection(1).let { true }
                        Key.DirectionUp -> onMoveSelection(-1).let { true }
                        else -> false
                    }
                }.defaultMinSize(minHeight = TextFieldDefaults.MinHeight)
                .testTag("query"),
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(accent),
        visualTransformation = highlight,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        singleLine = true,
        interactionSource = interaction,
        onTextLayout = { layout = it },
        decorationBox = { inner ->
            TextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = {
                    Box(Modifier.keywordPill({ layout }, keyword?.end ?: 0, accent, glow)) { inner() }
                },
                enabled = true,
                singleLine = true,
                visualTransformation = highlight,
                interactionSource = interaction,
                placeholder = { Text("Search...", maxLines = 1) },
                // The site's icon stands in for the search icon while a keyword is typed.
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                shape = shape,
                colors = fieldColors,
            )
        },
    )
}

private const val KEYWORD_FADE_MS = 220

/** Text set from outside (another app's search, Clear) puts the cursor at the end; typing keeps it where it is. */
internal fun fieldValueFor(
    current: TextFieldValue,
    query: String,
): TextFieldValue = if (current.text == query) current else TextFieldValue(query, TextRange(query.length))

/** Colours the first [end] characters (the site keyword) in the accent and bold; the pill behind is drawn separately. */
private class KeywordHighlight(
    private val end: Int,
    private val accent: Color,
    private val glow: Float,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (end <= 0 || glow <= 0f || end > text.length) return TransformedText(text, OffsetMapping.Identity)
        val styled =
            AnnotatedString
                .Builder(text)
                .apply {
                    addStyle(
                        SpanStyle(
                            color = accent.copy(alpha = 0.55f + 0.45f * glow),
                            fontWeight = FontWeight.SemiBold,
                        ),
                        0,
                        end,
                    )
                }.toAnnotatedString()
        return TransformedText(styled, OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is KeywordHighlight && other.end == end && other.accent == accent && other.glow == glow

    override fun hashCode() = (end * 31 + accent.hashCode()) * 31 + glow.hashCode()
}

/**
 * A rounded, lightly padded tint behind the first [end] characters. Skipped while long text scrolls sideways,
 * since the keyword may be out of view then.
 */
private fun Modifier.keywordPill(
    layout: () -> TextLayoutResult?,
    end: Int,
    accent: Color,
    glow: Float,
): Modifier =
    drawBehind {
        val text = layout() ?: return@drawBehind
        if (end <= 0 || glow <= 0f || end > text.layoutInput.text.length || text.size.width > size.width) return@drawBehind
        val first = text.getBoundingBox(0)
        val last = text.getBoundingBox(end - 1)
        val padX = 4.dp.toPx()
        val padY = 1.dp.toPx()
        drawRoundRect(
            color = accent.copy(alpha = 0.16f * glow),
            topLeft = Offset(first.left - padX, first.top - padY),
            size = Size(last.right - first.left + 2 * padX, first.height + 2 * padY),
            cornerRadius = CornerRadius(8.dp.toPx()),
        )
    }
