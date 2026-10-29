package com.ekquickfind.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.R
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.BarAnimation
import com.ekquickfind.data.prefs.Density
import com.ekquickfind.data.prefs.IconSize
import com.ekquickfind.data.prefs.ItemAnimation
import com.ekquickfind.data.prefs.ResultFont
import com.ekquickfind.data.prefs.ResultWeight
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.StripPosition
import com.ekquickfind.data.prefs.SurfacePosition
import com.ekquickfind.data.prefs.TopResultStyle
import com.ekquickfind.data.prefs.UserPreferences

/** Everything the surface needs from preferences, as one immutable value. */
data class SurfaceStyle(
    val iconSize: Dp = 40.dp,
    val prominentIconSize: Dp = 48.dp,
    val tileIconSize: Dp = 44.dp,
    val rowPadding: Dp = 8.dp,
    /** Web results are short links, so they get a smaller icon and tighter rows. */
    val denseIconSize: Dp = 24.dp,
    val denseRowPadding: Dp = 4.dp,
    val position: SurfacePosition = SurfacePosition.Bottom,
    val layouts: Map<SectionKey, SectionLayout> = SectionKey.entries.associateWith { UserPreferences().layoutFor(it) },
    val background: BackgroundStyle = BackgroundStyle.FrostedGlass,
    val blurRadius: Dp = 29.dp,
    val dimAmount: Float = 0.45f,
    val solidColor: Long = 0xFF101418,
    val autoOpenKeyboard: Boolean = true,
    val enterRunsSelected: Boolean = true,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val recentsStrip: Boolean = false,
    val stripOnTop: Boolean = true,
    val selectedAtBottom: Boolean = false,
    val exactMatch: Boolean = false,
    val exactMatchButton: Boolean = true,
    /** Multiplies Android's font scale on the search screen. */
    val textScale: Float = 1f,
    /** Plays once when search opens; null when it's off. */
    val barAnimation: BarAnimation? = BarAnimation.TwinLights,
    /** Plays when a result becomes active; null when it's off, and then the highlight just moves. */
    val itemAnimation: ItemAnimation? = ItemAnimation.Settle,
    val activeItem: ActiveItemStyle = ActiveItemStyle.TintedCard,
    /** Font and weight for result text; null keeps the theme's. */
    val resultFont: FontFamily? = null,
    val resultWeight: FontWeight? = null,
    /** The best match stays in its section instead of a big card above them. */
    val inlineTop: Boolean = true,
) {
    // Web links read best as a compact list, so they ignore any saved grid or chips layout.
    fun layoutOf(section: Section): SectionLayout =
        if (isDense(section)) {
            SectionLayout.Rows
        } else {
            layouts[section.key()]
                ?: SectionLayout.Rows
        }

    fun isDense(section: Section): Boolean = section == Section.Web

    val recentLayout: SectionLayout get() = layouts[SectionKey.Recent] ?: SectionLayout.Chips
}

fun Section.key(): SectionKey = SectionKey.valueOf(name)

fun SectionKey.section(): Section? = Section.entries.firstOrNull { it.name == name }

fun UserPreferences.toSurfaceStyle(): SurfaceStyle {
    val (icon, prominent, tile) =
        when (iconSize) {
            IconSize.Small -> Triple(32.dp, 40.dp, 36.dp)
            IconSize.Medium -> Triple(40.dp, 48.dp, 44.dp)
            IconSize.Large -> Triple(48.dp, 56.dp, 54.dp)
        }
    val compact = density == Density.Compact
    return SurfaceStyle(
        iconSize = icon,
        prominentIconSize = prominent,
        tileIconSize = tile,
        rowPadding = if (compact) 4.dp else 8.dp,
        denseIconSize = icon * 0.6f,
        denseRowPadding = if (compact) 2.dp else 4.dp,
        position = position,
        layouts = SectionKey.entries.associateWith { layoutFor(it) },
        background = backgroundStyle,
        blurRadius = blurRadiusDp.dp,
        dimAmount = dimAmount,
        solidColor = solidColor,
        autoOpenKeyboard = autoOpenKeyboard,
        enterRunsSelected = enterRunsSelected,
        haptics = haptics,
        reduceMotion = reduceMotion,
        recentsStrip = recentsStrip,
        stripOnTop = stripPosition == StripPosition.Top,
        selectedAtBottom = selectedAtBottom,
        exactMatch = exactMatch,
        exactMatchButton = exactMatchButton,
        textScale = textScale,
        barAnimation = barAnimation.takeIf { barAnimationOn && !reduceMotion },
        itemAnimation = itemAnimation.takeIf { itemAnimationOn && !reduceMotion },
        activeItem = activeItemStyle,
        inlineTop = topResult == TopResultStyle.Inline,
        resultWeight =
            when (resultWeight) {
                ResultWeight.Light -> FontWeight.Light
                ResultWeight.Regular -> null
                ResultWeight.Medium -> FontWeight.Medium
                ResultWeight.Bold -> FontWeight.Bold
            },
        resultFont = resultFont.family(),
    )
}

// Bundled variable fonts (SIL OFL, licences in assets/licenses/fonts). Android's own serif and mono families
// can be swapped out by a system font style (Samsung's, for one), so the picker would seem to do nothing.
private fun variable(res: Int) =
    FontFamily(
        listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.Bold).map { weight ->
            Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        },
    )

private val LORA = variable(R.font.lora)
private val JETBRAINS_MONO = variable(R.font.jetbrains_mono)
private val ROBOTO_CONDENSED = variable(R.font.roboto_condensed)
private val NUNITO = variable(R.font.nunito)

/** The font family for this choice; null keeps the theme's. */
fun ResultFont.family(): FontFamily? =
    when (this) {
        ResultFont.Default -> null
        ResultFont.Serif -> LORA
        ResultFont.Monospace -> JETBRAINS_MONO
        ResultFont.Condensed -> ROBOTO_CONDENSED
        ResultFont.Rounded -> NUNITO
    }

/** The same typography with every text style in [family] and [weight]; null keeps each style's own. */
fun Typography.withResultFont(
    family: FontFamily?,
    weight: FontWeight?,
): Typography {
    fun TextStyle.change() = copy(fontFamily = family ?: fontFamily, fontWeight = weight ?: fontWeight)
    return copy(
        displayLarge = displayLarge.change(),
        displayMedium = displayMedium.change(),
        displaySmall = displaySmall.change(),
        headlineLarge = headlineLarge.change(),
        headlineMedium = headlineMedium.change(),
        headlineSmall = headlineSmall.change(),
        titleLarge = titleLarge.change(),
        titleMedium = titleMedium.change(),
        titleSmall = titleSmall.change(),
        bodyLarge = bodyLarge.change(),
        bodyMedium = bodyMedium.change(),
        bodySmall = bodySmall.change(),
        labelLarge = labelLarge.change(),
        labelMedium = labelMedium.change(),
        labelSmall = labelSmall.change(),
    )
}

val LocalSurfaceStyle = staticCompositionLocalOf { SurfaceStyle() }

/** Results the user pinned; rows, tiles and chips show a pin mark for these. */
val LocalPinnedIds = staticCompositionLocalOf { emptySet<com.ekquickfind.core.search.ResultId>() }

/** Whether the system is blurring behind the window right now; frosted panels need it to look right. */
val LocalBlurAvailable = staticCompositionLocalOf { true }
