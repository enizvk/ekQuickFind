package com.ekquickfind.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ekquickfind.app.R
import com.ekquickfind.app.search.SearchActivity
import com.ekquickfind.app.ui.theme.darkFallback
import com.ekquickfind.app.ui.theme.lightFallback
import com.ekquickfind.data.prefs.ThemeMode
import com.ekquickfind.data.prefs.userPreferencesStore
import kotlinx.coroutines.flow.first

/** Home-screen search bar. Follows the app's theme override and dynamic colour. */
class SearchWidget : GlanceAppWidget() {
    // Exact: the label adapts to whatever width the user resizes it to.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val prefs = context.userPreferencesStore.data.first()
        provideContent { SearchBarContent(prefs.theme, prefs.dynamicColor) }
    }

    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        provideContent { SearchBarContent(ThemeMode.System, dynamic = true) }
    }
}

class SearchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SearchWidget()
}

internal fun searchIntent(context: Context) = Intent(context, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
internal fun SearchBarContent(
    theme: ThemeMode,
    dynamic: Boolean,
) {
    val context = LocalContext.current
    GlanceTheme(colors = widgetColors(context, theme, dynamic)) {
        val wide = LocalSize.current.width >= 200.dp
        Row(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(28.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .clickable(actionStartActivity(searchIntent(context)))
                    .semantics { testTag = "search-widget" }
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_tile_search),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(24.dp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = if (wide) "Search..." else "Search",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 16.sp),
                maxLines = 1,
            )
        }
    }
}

/** System theme keeps day/night switching; a forced theme uses the same scheme for both. */
private fun widgetColors(
    context: Context,
    theme: ThemeMode,
    dynamic: Boolean,
): androidx.glance.color.ColorProviders {
    val light = if (dynamic) dynamicLightColorScheme(context) else lightFallback
    val dark = if (dynamic) dynamicDarkColorScheme(context) else darkFallback
    return when (theme) {
        ThemeMode.Light -> ColorProviders(light, light)
        ThemeMode.Dark -> ColorProviders(dark, dark)
        ThemeMode.System -> ColorProviders(light, dark)
    }
}
