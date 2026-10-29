package com.ekquickfind.app.ui.search

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.app.ui.theme.withResultFont

/** Shows [content] in the chosen result font and weight; the search bar and settings keep the theme font. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ResultsFont(content: @Composable () -> Unit) {
    val style = LocalSurfaceStyle.current
    val family = style.resultFont
    val weight = style.resultWeight
    if (family == null && weight == null) {
        content()
        return
    }
    val typography = MaterialTheme.typography
    MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = remember(typography, family, weight) { typography.withResultFont(family, weight) },
        content = content,
    )
}
