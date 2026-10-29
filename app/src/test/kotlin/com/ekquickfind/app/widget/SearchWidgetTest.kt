package com.ekquickfind.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.assertHasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import com.ekquickfind.data.prefs.ThemeMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Plain Application: these tests don't need the Hilt graph or its startup work.
@Config(application = android.app.Application::class)
class SearchWidgetTest {
    private fun renders(
        theme: ThemeMode,
        dynamic: Boolean,
        width: Int = 320,
    ) = runGlanceAppWidgetUnitTest {
        setContext(
            androidx.test.core.app.ApplicationProvider
                .getApplicationContext(),
        )
        setAppWidgetSize(DpSize(width.dp, 56.dp))
        provideComposable { SearchBarContent(theme, dynamic) }
        onNode(hasText("Search...")).assertExists()
    }

    @Test
    fun rendersInLightTheme() = renders(ThemeMode.Light, dynamic = false)

    @Test
    fun rendersInDarkTheme() = renders(ThemeMode.Dark, dynamic = false)

    @Test
    fun rendersWithDynamicColourFollowingTheSystem() = renders(ThemeMode.System, dynamic = true)

    @Test
    fun narrowWidgetsShowAShortLabel() =
        runGlanceAppWidgetUnitTest {
            setContext(
                androidx.test.core.app.ApplicationProvider
                    .getApplicationContext(),
            )
            setAppWidgetSize(DpSize(120.dp, 56.dp))
            provideComposable { SearchBarContent(ThemeMode.System, true) }
            onNode(hasText("Search")).assertExists()
        }

    @Test
    fun tappingOpensTheSearchSurface() =
        runGlanceAppWidgetUnitTest {
            setContext(
                androidx.test.core.app.ApplicationProvider
                    .getApplicationContext(),
            )
            setAppWidgetSize(DpSize(320.dp, 56.dp))
            provideComposable { SearchBarContent(ThemeMode.System, true) }
            val context =
                androidx.test.core.app.ApplicationProvider
                    .getApplicationContext<android.content.Context>()
            onNode(hasTestTag("search-widget")).assertHasStartActivityClickAction(searchIntent(context))
        }
}
