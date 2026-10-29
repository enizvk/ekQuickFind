package com.ekquickfind.app.ui.search

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.pointerInputTap(onClick: () -> Unit): Modifier = pointerInput(onClick) { detectTapGestures { onClick() } }
