package com.ekquickfind.data.prefs

import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersonalisationPreferencesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun defaults() {
        val p = UserPreferences()
        assertEquals(BackgroundStyle.FrostedGlass, p.backgroundStyle)
        assertEquals(29, p.blurRadiusDp)
        assertEquals(0.45f, p.dimAmount, 0f)
        assertEquals(IconSize.Medium, p.iconSize)
        assertEquals(Density.Comfortable, p.density)
        assertEquals(4, p.resultsPerSection)
        assertEquals(SurfacePosition.Bottom, p.position)
        assertTrue(p.autoOpenKeyboard)
        assertTrue(p.enterRunsSelected)
        assertTrue(p.haptics)
        assertFalse(p.reduceMotion)
        assertFalse(p.selectedAtBottom)
        assertFalse(p.recentsStrip)
        assertTrue(p.autoUpdate)
        assertEquals(AccentColor.System, p.accent)
        assertFalse(p.photoTags)
        assertTrue(p.floatingButton)
        assertEquals(SearchEngineChoice.Google, p.searchEngine)
        assertEquals(52, p.floatingButtonSizeDp)
        assertEquals(0.85f, p.floatingButtonOpacity, 0f)
        assertTrue(p.floatingButtonOnRight)
        assertFalse(p.exactMatch)
        assertTrue(p.exactMatchButton)
        assertEquals(5, p.recentsCount)
        assertTrue(p.smartSuggestions)
        assertTrue(p.nextUp)
        assertTrue(p.clipboardChip)
        assertFalse(p.onboardingDone)
        assertEquals("", p.siteSeparator)
        assertTrue(p.barAnimationOn)
        assertTrue(p.itemAnimationOn)
        assertEquals(ItemAnimation.TwinLights, p.itemAnimation)
        assertEquals(ActiveItemStyle.TintedCard, p.activeItemStyle)
        assertEquals(1f, p.textScale, 0f)
        assertEquals(ResultFont.Default, p.resultFont)
        assertEquals(ResultWeight.Regular, p.resultWeight)
        assertEquals(TopResultStyle.Inline, p.topResult)
        assertEquals(BarAnimation.TwinLights, p.barAnimation)
        assertEquals(StripPosition.Top, p.stripPosition)
        assertEquals(SectionKey.defaultOrder, p.sectionOrder)
        assertEquals(SectionLayout.Grid, p.layoutFor(SectionKey.Apps))
        assertEquals(SectionLayout.Chips, p.layoutFor(SectionKey.Recent))
        assertEquals(SectionLayout.Chips, p.layoutFor(SectionKey.Settings))
    }

    @Test
    fun resettingRestoresEverySettingButKeepsHiddenResultsAndTheTileState() {
        val customised =
            UserPreferences(
                theme = ThemeMode.Dark,
                textScale = 1.4f,
                floatingButton = true,
                photoTags = true,
                resultFont = ResultFont.Serif,
                hiddenResults = mapOf("apps:com.example" to "Example"),
                tileAdded = true,
                onboardingDone = true,
                recentsClearedAt = 123L,
            )
        val reset = customised.resetToDefaults()
        assertEquals(
            UserPreferences(
                hiddenResults = mapOf("apps:com.example" to "Example"),
                tileAdded = true,
                onboardingDone = true,
                recentsClearedAt = 123L,
            ),
            reset,
        )
    }

    @Test
    fun recentItemsCountStaysBetweenOneAndTwenty() {
        assertEquals(1, UserPreferences(recentsCount = 0).sanitized().recentsCount)
        assertEquals(20, UserPreferences(recentsCount = 40).sanitized().recentsCount)
        assertEquals(7, UserPreferences(recentsCount = 7).sanitized().recentsCount)
    }

    @Test
    fun aSiteSeparatorIsAFewSymbolsOrNothing() {
        assertEquals(":", UserPreferences(siteSeparator = " : ").sanitized().siteSeparator)
        assertEquals("->", UserPreferences(siteSeparator = "->").sanitized().siteSeparator)
        assertEquals("", UserPreferences(siteSeparator = "ab").sanitized().siteSeparator)
        assertEquals("", UserPreferences(siteSeparator = "::::").sanitized().siteSeparator)
        assertEquals("", UserPreferences(siteSeparator = ": -").sanitized().siteSeparator)
    }

    @Test
    fun textSizeStaysWithinTheSliderRange() {
        assertEquals(0.8f, UserPreferences(textScale = 0.5f).sanitized().textScale, 0f)
        assertEquals(1.6f, UserPreferences(textScale = 3f).sanitized().textScale, 0f)
        assertEquals(1.2f, UserPreferences(textScale = 1.2f).sanitized().textScale, 0f)
    }

    @Test
    fun theFloatingButtonCanBeAlmostSeeThroughButNotInvisible() {
        assertEquals(0.1f, UserPreferences(floatingButtonOpacity = 0.1f).sanitized().floatingButtonOpacity, 0f)
        assertEquals(0.1f, UserPreferences(floatingButtonOpacity = 0.02f).sanitized().floatingButtonOpacity, 0f)
    }

    @Test
    fun preferencesSavedWithRecentSearchesStillLoad() {
        val decoded =
            UserPreferencesSerializer.decode(
                """{"rememberRecentSearches":false,"sectionLayouts":{"RecentSearches":"Rows"},"haptics":false}""",
            )
        assertFalse(decoded.haptics)
    }

    @Test
    fun everyPersonalisationFieldRoundTrips() =
        runTest {
            val file = folder.newFile("p.json").also { it.delete() }
            val repo = PreferencesRepository(DataStoreFactory.create(UserPreferencesSerializer, scope = backgroundScope) { file })
            repo.update {
                it.copy(
                    backgroundStyle = BackgroundStyle.Solid,
                    blurRadiusDp = 40,
                    dimAmount = 0.7f,
                    solidColor = 0xFF223344,
                    iconSize = IconSize.Large,
                    density = Density.Compact,
                    resultsPerSection = 6,
                    position = SurfacePosition.Top,
                    autoOpenKeyboard = false,
                    enterRunsSelected = false,
                    haptics = false,
                    reduceMotion = true,
                    selectedAtBottom = true,
                    recentsStrip = true,
                    autoUpdate = false,
                    accent = AccentColor.Red,
                    floatingButton = true,
                    floatingButtonSizeDp = 64,
                    floatingButtonOpacity = 0.5f,
                    floatingButtonOnRight = false,
                    floatingButtonY = 0.7f,
                    stripPosition = StripPosition.Bottom,
                    sectionOrder = listOf(SectionKey.Settings, SectionKey.Apps),
                    sectionLayouts = mapOf(SectionKey.Settings to SectionLayout.Chips),
                )
            }
            val decoded = UserPreferencesSerializer.decode(file.readText())
            assertEquals(BackgroundStyle.Solid, decoded.backgroundStyle)
            assertEquals(40, decoded.blurRadiusDp)
            assertEquals(0.7f, decoded.dimAmount, 0f)
            assertEquals(0xFF223344, decoded.solidColor)
            assertEquals(IconSize.Large, decoded.iconSize)
            assertEquals(Density.Compact, decoded.density)
            assertEquals(6, decoded.resultsPerSection)
            assertEquals(SurfacePosition.Top, decoded.position)
            assertFalse(decoded.autoOpenKeyboard)
            assertFalse(decoded.enterRunsSelected)
            assertFalse(decoded.haptics)
            assertTrue(decoded.reduceMotion)
            assertTrue(decoded.selectedAtBottom)
            assertTrue(decoded.recentsStrip)
            assertFalse(decoded.autoUpdate)
            assertEquals(AccentColor.Red, decoded.accent)
            assertTrue(decoded.floatingButton)
            assertEquals(64, decoded.floatingButtonSizeDp)
            assertEquals(0.5f, decoded.floatingButtonOpacity, 0f)
            assertFalse(decoded.floatingButtonOnRight)
            assertEquals(0.7f, decoded.floatingButtonY, 0f)
            assertEquals(StripPosition.Bottom, decoded.stripPosition)
            // A partial order is completed with the remaining sections in default order.
            assertEquals(
                listOf(
                    SectionKey.Settings,
                    SectionKey.Apps,
                    SectionKey.QuickAnswers,
                    SectionKey.Translation,
                    SectionKey.Sites,
                    SectionKey.Contacts,
                    SectionKey.Calendar,
                    SectionKey.Files,
                    SectionKey.Web,
                    SectionKey.AskAi,
                ),
                decoded.sectionOrder,
            )
            assertEquals(SectionLayout.Chips, decoded.layoutFor(SectionKey.Settings))
            // Unset sections keep their defaults.
            assertEquals(SectionLayout.Grid, decoded.layoutFor(SectionKey.Apps))
        }

    @Test
    fun movingASectionSwapsItWithItsNeighbour() =
        runTest {
            val file = folder.newFile("m.json").also { it.delete() }
            val repo = PreferencesRepository(DataStoreFactory.create(UserPreferencesSerializer, scope = backgroundScope) { file })
            repo.moveSection(SectionKey.Settings, up = true)
            repo.moveSection(SectionKey.Settings, up = true)
            val order = repo.preferences.first().sectionOrder
            assertEquals(
                listOf(
                    SectionKey.QuickAnswers,
                    SectionKey.Translation,
                    SectionKey.Sites,
                    SectionKey.Apps,
                    SectionKey.Settings,
                    SectionKey.Contacts,
                    SectionKey.Calendar,
                    SectionKey.Files,
                    SectionKey.Web,
                    SectionKey.AskAi,
                ),
                order,
            )
            repo.resetSectionOrder()
            assertEquals(SectionKey.defaultOrder, repo.preferences.first().sectionOrder)
        }

    @Test
    fun valuesAreClampedToSaneRanges() {
        val p = UserPreferences(blurRadiusDp = 500, dimAmount = 3f, resultsPerSection = 99).sanitized()
        assertEquals(60, p.blurRadiusDp)
        assertEquals(0.9f, p.dimAmount, 0f)
        assertEquals(8, p.resultsPerSection)
    }

    @Test
    fun translationShowsInlineWithGoogleIntoThePhonesLanguageByDefault() {
        val p = UserPreferences()
        assertEquals(true, p.translateInline)
        assertEquals("", p.translateTo)
        assertEquals(Translator.Google, p.translator)
    }

    @Test
    fun aNewSectionJoinsASavedOrderNextToWhereItBelongs() {
        val saved =
            listOf(
                SectionKey.Apps,
                SectionKey.QuickAnswers,
                SectionKey.Sites,
                SectionKey.Contacts,
                SectionKey.Calendar,
                SectionKey.Settings,
                SectionKey.Files,
                SectionKey.Web,
                SectionKey.AskAi,
            )
        assertEquals(
            listOf(SectionKey.Apps, SectionKey.QuickAnswers, SectionKey.Translation) + saved.drop(2),
            UserPreferences(sectionOrder = saved).sanitized().sectionOrder,
        )
    }

    @Test
    fun noReadyMadeSiteIsRemovedByDefault() {
        assertEquals(emptySet<String>(), UserPreferences().removedSites)
    }
}
