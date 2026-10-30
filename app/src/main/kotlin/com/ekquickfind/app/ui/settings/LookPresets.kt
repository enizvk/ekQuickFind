package com.ekquickfind.app.ui.settings

import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.BarAnimation
import com.ekquickfind.data.prefs.Density
import com.ekquickfind.data.prefs.IconSize
import com.ekquickfind.data.prefs.ItemAnimation
import com.ekquickfind.data.prefs.TopResultStyle
import com.ekquickfind.data.prefs.UserPreferences

/** One-tap looks; they only change how things look (not theme, text or behaviour), and everything stays adjustable. */
enum class LookPreset(
    val title: String,
    val description: String,
) {
    Minimal("Minimal", "Solid, still and compact"),
    Balanced("Balanced", "The default look"),
    Showcase("Showcase", "Frosted glass, big icons and glows"),
    ;

    fun isApplied(prefs: UserPreferences): Boolean = prefs.withLook(this) == prefs
}

fun UserPreferences.withLook(preset: LookPreset): UserPreferences {
    val d = UserPreferences()
    return when (preset) {
        LookPreset.Minimal ->
            copy(
                backgroundStyle = BackgroundStyle.Solid,
                blurRadiusDp = d.blurRadiusDp,
                dimAmount = d.dimAmount,
                iconSize = IconSize.Small,
                density = Density.Compact,
                activeItemStyle = ActiveItemStyle.TintedCard,
                topResult = TopResultStyle.Inline,
                barAnimationOn = false,
                barAnimation = d.barAnimation,
                itemAnimationOn = false,
                itemAnimation = d.itemAnimation,
            )
        LookPreset.Balanced ->
            copy(
                backgroundStyle = d.backgroundStyle,
                blurRadiusDp = d.blurRadiusDp,
                dimAmount = d.dimAmount,
                iconSize = d.iconSize,
                density = d.density,
                activeItemStyle = d.activeItemStyle,
                topResult = d.topResult,
                barAnimationOn = d.barAnimationOn,
                barAnimation = d.barAnimation,
                itemAnimationOn = d.itemAnimationOn,
                itemAnimation = d.itemAnimation,
            )
        LookPreset.Showcase ->
            copy(
                backgroundStyle = BackgroundStyle.FrostedGlass,
                blurRadiusDp = SHOWCASE_BLUR,
                dimAmount = d.dimAmount,
                iconSize = IconSize.Large,
                density = Density.Comfortable,
                activeItemStyle = ActiveItemStyle.Gradient,
                topResult = TopResultStyle.Featured,
                barAnimationOn = true,
                barAnimation = BarAnimation.ShimmerRing,
                itemAnimationOn = true,
                itemAnimation = ItemAnimation.TwinLights,
            )
    }
}

private const val SHOWCASE_BLUR = 40
