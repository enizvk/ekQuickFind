package com.ekquickfind.app.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.theme.LocalBlurAvailable
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.data.prefs.BackgroundStyle

@Composable
internal fun QueryField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMoveSelection: (Int) -> Unit,
    focus: FocusRequester,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val frosted = LocalSurfaceStyle.current.background == BackgroundStyle.FrostedGlass
    val alpha = if (LocalBlurAvailable.current) 0.6f else 0.95f
    val container = MaterialTheme.colorScheme.surfaceContainerHighest.let { if (frosted) it.copy(alpha = alpha) else it }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focus)
                // Hardware keyboards: arrows move the selected result.
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> onMoveSelection(1).let { true }
                        Key.DirectionUp -> onMoveSelection(-1).let { true }
                        else -> false
                    }
                }.testTag("query"),
        placeholder = { Text("Search...", maxLines = 1) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedContent(
                targetState = query.isEmpty(),
                transitionSpec = { (fadeIn(tween(120)) + scaleIn(tween(120))) togetherWith (fadeOut(tween(90)) + scaleOut(tween(90))) },
                label = "trailing",
            ) { empty ->
                Row {
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
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = container,
                unfocusedContainerColor = container,
            ),
    )
}
