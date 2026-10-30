package com.ekquickfind.app.ui.settings

import com.ekquickfind.data.prefs.UserPreferences

private val defaults = UserPreferences()

/**
 * The settings panels, with which options each one holds: that's what marks a panel as changed from the defaults
 * and what its Reset puts back. Titles match the panel headings, so search can jump to them.
 */
enum class Panel(
    val title: String,
    val page: SettingsPage,
    private val fields: (UserPreferences) -> List<Any?>,
    private val resetTo: (UserPreferences) -> UserPreferences,
) {
    Sources("Search sources", SettingsPage.Sources, {
        listOf(it.disabledProviders)
    }, { it.copy(disabledProviders = defaults.disabledProviders) }),
    BeforeTyping(
        "Before you type",
        SettingsPage.Sources,
        { listOf(it.smartSuggestions, it.nextUp, it.clipboardChip) },
        { it.copy(smartSuggestions = defaults.smartSuggestions, nextUp = defaults.nextUp, clipboardChip = defaults.clipboardChip) },
    ),
    Order("Result order", SettingsPage.Sources, { listOf(it.sectionOrder) }, { it.copy(sectionOrder = defaults.sectionOrder) }),
    Layout(
        "Layout",
        SettingsPage.Sources,
        { listOf(it.iconSize, it.density, it.resultsPerSection, it.sectionLayouts) },
        {
            it.copy(
                iconSize = defaults.iconSize,
                density = defaults.density,
                resultsPerSection = defaults.resultsPerSection,
                sectionLayouts = defaults.sectionLayouts,
            )
        },
    ),
    Web(
        "Web and AI",
        SettingsPage.WebAi,
        { listOf(it.webSuggestions, it.searchEngine, it.customTemplate, it.customSuggestTemplate, it.aiChats) },
        {
            it.copy(
                webSuggestions = defaults.webSuggestions,
                searchEngine = defaults.searchEngine,
                customTemplate = defaults.customTemplate,
                customSuggestTemplate = defaults.customSuggestTemplate,
                aiChats = defaults.aiChats,
            )
        },
    ),

    // Sites you added are yours, so Reset keeps them.
    SearchSites(
        "Search sites",
        SettingsPage.WebAi,
        { listOf(it.sites, it.removedSites, it.siteSeparator, it.weatherUnits) },
        {
            it.copy(
                sites = defaults.sites,
                removedSites = defaults.removedSites,
                siteSeparator = defaults.siteSeparator,
                weatherUnits = defaults.weatherUnits,
            )
        },
    ),
    Translation(
        "Translation",
        SettingsPage.WebAi,
        { listOf(it.translateInline, it.translateTo, it.translator) },
        { it.copy(translateInline = defaults.translateInline, translateTo = defaults.translateTo, translator = defaults.translator) },
    ),
    Background(
        "Background",
        SettingsPage.Look,
        { listOf(it.backgroundStyle, it.blurRadiusDp, it.dimAmount, it.solidColor) },
        {
            it.copy(
                backgroundStyle = defaults.backgroundStyle,
                blurRadiusDp = defaults.blurRadiusDp,
                dimAmount = defaults.dimAmount,
                solidColor = defaults.solidColor,
            )
        },
    ),
    Appearance(
        "Appearance",
        SettingsPage.Look,
        { listOf(it.theme, it.dynamicColor, it.accent) },
        { it.copy(theme = defaults.theme, dynamicColor = defaults.dynamicColor, accent = defaults.accent) },
    ),
    Text(
        "Text",
        SettingsPage.Look,
        { listOf(it.textScale, it.resultFont, it.resultWeight) },
        { it.copy(textScale = defaults.textScale, resultFont = defaults.resultFont, resultWeight = defaults.resultWeight) },
    ),
    SearchBar(
        "Search bar",
        SettingsPage.Look,
        {
            listOf(it.position, it.recentsStrip, it.stripPosition, it.recentsCount, it.exactMatchButton, it.barAnimationOn, it.barAnimation)
        },
        {
            it.copy(
                position = defaults.position,
                recentsStrip = defaults.recentsStrip,
                stripPosition = defaults.stripPosition,
                recentsCount = defaults.recentsCount,
                exactMatchButton = defaults.exactMatchButton,
                barAnimationOn = defaults.barAnimationOn,
                barAnimation = defaults.barAnimation,
            )
        },
    ),
    ActiveItem(
        "Active item",
        SettingsPage.Look,
        { listOf(it.topResult, it.selectedAtBottom, it.activeItemStyle, it.itemAnimationOn, it.itemAnimation) },
        {
            it.copy(
                topResult = defaults.topResult,
                selectedAtBottom = defaults.selectedAtBottom,
                activeItemStyle = defaults.activeItemStyle,
                itemAnimationOn = defaults.itemAnimationOn,
                itemAnimation = defaults.itemAnimation,
            )
        },
    ),
    Keyboard(
        "Keyboard and feedback",
        SettingsPage.Keyboard,
        { listOf(it.autoOpenKeyboard, it.enterRunsSelected, it.haptics, it.reduceMotion) },
        {
            it.copy(
                autoOpenKeyboard = defaults.autoOpenKeyboard,
                enterRunsSelected = defaults.enterRunsSelected,
                haptics = defaults.haptics,
                reduceMotion = defaults.reduceMotion,
            )
        },
    ),
    FloatingButton(
        "Floating button",
        SettingsPage.WaysToOpen,
        { listOf(it.floatingButton, it.floatingButtonSizeDp, it.floatingButtonOpacity) },
        {
            it.copy(
                floatingButton = defaults.floatingButton,
                floatingButtonSizeDp = defaults.floatingButtonSizeDp,
                floatingButtonOpacity = defaults.floatingButtonOpacity,
            )
        },
    ),
    Privacy("Privacy", SettingsPage.Privacy, { listOf(it.backupHistory) }, { it.copy(backupHistory = defaults.backupHistory) }),
    ;

    fun isChanged(prefs: UserPreferences): Boolean = fields(prefs) != fields(defaults)

    fun reset(prefs: UserPreferences): UserPreferences = resetTo(prefs)

    companion object {
        fun byTitle(title: String): Panel? = entries.firstOrNull { it.title == title }
    }
}
