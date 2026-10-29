package com.ekquickfind.app.settings

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ekquickfind.app.Screenshots
import com.ekquickfind.app.SearchLauncher
import com.ekquickfind.app.search.SearchActivity
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.IconSize
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.SurfacePosition
import com.ekquickfind.data.prefs.UserPreferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface HistoryEntryPoint {
    fun history(): com.ekquickfind.data.history.HistoryRepository
}

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SettingsFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)

    @get:Rule(order = 2)
    val compose = createEmptyComposeRule()

    @Inject lateinit var prefs: PreferencesRepository
    private lateinit var saved: UserPreferences
    private lateinit var activity: SearchActivity
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        hilt.inject()
        saved = runBlocking { prefs.preferences.first() }
        runBlocking { prefs.update { UserPreferences(hiddenResults = it.hiddenResults) } }
        activity = SearchLauncher.launch()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 3_000)
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        runBlocking { prefs.update { saved } }
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun set(transform: (UserPreferences) -> UserPreferences) {
        runBlocking { prefs.update(transform) }
        compose.waitForIdle()
    }

    /** Opens settings and, when given, one of its pages from the menu. */
    private fun openSettings(page: String? = null) {
        compose.onNodeWithTag("open-settings", useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) { exists("settings-menu") }
        page?.let { openPage(it) }
    }

    private fun openPage(page: String) {
        compose.onNodeWithTag("settings-page:$page", useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) { !exists("settings-menu") }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun settingsOpensOnAMenuAndEachPageOpensAndReturns() {
        openSettings()
        listOf("Sources", "WebAi", "Results", "Look", "Keyboard", "WaysToOpen", "Privacy", "About").forEach { page ->
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("settings-page:$page"))
            openPage(page)
            back()
            compose.waitUntil(3_000) { exists("settings-menu") }
        }
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag(tag))
    }

    private fun sectionOrder(): List<String> =
        listOf("Answer", "Apps", "Contacts", "Settings", "Files", "Web")
            .mapNotNull { name ->
                val key = mapOf("Answer" to "QuickAnswers").getOrDefault(name, name)
                compose
                    .onAllNodesWithTag("section-$key", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .firstOrNull()
                    ?.let { key to it.boundsInRoot.top }
            }.sortedBy { it.second }
            .map { it.first }

    // 11.2: provider switches

    @Test
    fun aboutShowsTheGitHubPageAndThePlainVersion() {
        openSettings("About")
        scrollTo("about")
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName!!
        compose.onNode(hasTestTag("about-github"), useUnmergedTree = true).assertTextEquals(com.ekquickfind.app.BuildConfig.PROJECT_PAGE)
        // Just the version name; the build number isn't shown.
        compose.onNode(hasTestTag("about-version"), useUnmergedTree = true).assertTextEquals(version)
    }

    @Test
    fun disablingFilesInSettingsHidesFileResults() {
        val name = "ekquickfindsettings${System.currentTimeMillis()}"
        val image = insertImage(name)
        try {
            compose.onNodeWithTag("query").performTextInput(name)
            compose.waitUntilAtLeastOneExists(hasText("$name.png"), 5_000)
            // Clear the query to reveal the settings button, then turn Files off.
            compose.onNodeWithTag("query").let { node -> repeat(name.length) { node.performKeyInput { pressKey(Key.Backspace) } } }
            openSettings("Sources")
            compose.onNodeWithTag("provider:files", useUnmergedTree = true).performClick()
            compose.waitUntil(2_000) { runBlocking { "files" in prefs.preferences.first().disabledProviders } }
            // Page, then menu, then back to search.
            back()
            back()
            compose.waitUntil(3_000) { exists("query") }

            compose.onNodeWithTag("query").performTextInput(name)
            Thread.sleep(1_000)
            compose.waitForIdle()
            assertFalse("file result still shown", exists("section-Files"))
        } finally {
            context.contentResolver.delete(image, null, null)
        }
    }

    // 11.3: appearance override

    @Test
    fun forcedThemeOverridesTheSystem() {
        fun panelLuminance(): Double {
            // Let theme cross-fades finish before sampling.
            Thread.sleep(600)
            compose.waitForIdle()
            val bmp = compose.onNodeWithTag("panel", useUnmergedTree = true).captureToImage().asAndroidBitmap()
            val c = bmp.getPixel(bmp.width / 2, bmp.height / 2)
            return (0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c)) / 255
        }
        set { it.copy(theme = com.ekquickfind.data.prefs.ThemeMode.Light, dynamicColor = false) }
        val light = panelLuminance()
        set { it.copy(theme = com.ekquickfind.data.prefs.ThemeMode.Dark, dynamicColor = false) }
        val dark = panelLuminance()
        assertTrue("light=$light dark=$dark", light > 0.6 && dark < 0.3)
    }

    // 11.5: privacy summary

    @Test
    fun privacySummaryReflectsTheSuggestionsSetting() {
        openSettings("Privacy")
        scrollTo("privacy-summary")
        compose.waitUntilAtLeastOneExists(hasTestTag("privacy-summary").and(hasText("which are on", substring = true)), 2_000)
        back()
        openPage("WebAi")
        scrollTo("web-suggestions")
        compose.onNodeWithTag("web-suggestions", useUnmergedTree = true).performClick()
        back()
        openPage("Privacy")
        scrollTo("privacy-summary")
        compose.waitUntilAtLeastOneExists(hasTestTag("privacy-summary").and(hasText("no network requests", substring = true)), 2_000)
    }

    // 15.11: section order

    @Test
    fun movingSettingsAboveAppsReordersTheSurface() {
        // Few rows per section so both headers are on screen (lazy lists only compose what's visible).
        set { it.copy(resultsPerSection = 3, sectionLayouts = mapOf(SectionKey.Apps to SectionLayout.Rows)) }
        compose.onNodeWithTag("query").performTextInput("sett")
        compose.waitUntil(3_000) { exists("section-Apps") && exists("section-Settings") }
        assertTrue(sectionOrder().indexOf("Apps") < sectionOrder().indexOf("Settings"))

        set {
            it.copy(
                sectionOrder =
                    listOf(
                        SectionKey.QuickAnswers,
                        SectionKey.Settings,
                        SectionKey.Apps,
                        SectionKey.Contacts,
                        SectionKey.Files,
                        SectionKey.Web,
                    ),
            )
        }
        compose.waitUntil(3_000) { sectionOrder().let { o -> o.indexOf("Settings") in 0 until o.indexOf("Apps") } }
    }

    @Test
    fun draggingSettingsAboveAppsChangesTheStoredOrder() {
        openSettings("Results")
        scrollTo("reset-order")
        val row =
            compose
                .onNodeWithTag("order:Apps", useUnmergedTree = true)
                .fetchSemanticsNode()
                .size.height
        // Default order is Answer, Apps, Contacts, Settings: two rows up puts Settings right after Answer.
        compose.onNodeWithTag("drag:Settings", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(10) {
                moveBy(
                    androidx.compose.ui.geometry
                        .Offset(0f, -row * 2.1f / 10),
                )
            }
            up()
        }
        compose.waitUntil(2_000) {
            runBlocking {
                prefs.preferences
                    .first()
                    .sectionOrder
                    .take(3) ==
                    listOf(SectionKey.QuickAnswers, SectionKey.Settings, SectionKey.Apps)
            }
        }
        compose.onNodeWithTag("reset-order", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { runBlocking { prefs.preferences.first().sectionOrder == SectionKey.defaultOrder } }
    }

    // Accent colour

    @Test
    fun pickingAnAccentSavesItAndSystemRestoresTheDefault() {
        openSettings("Look")
        scrollTo("accent:Red")
        compose.onNodeWithTag("accent:Red", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { runBlocking { prefs.preferences.first().accent == com.ekquickfind.data.prefs.AccentColor.Red } }
        compose.onNodeWithTag("accent:System", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { runBlocking { prefs.preferences.first().accent == com.ekquickfind.data.prefs.AccentColor.System } }
    }

    // More-below indicator

    @Test
    fun aMorePillShowsWhenResultsContinueBelowAndScrollsToThem() {
        set { it.copy(resultsPerSection = 8, selectedAtBottom = true, sectionLayouts = mapOf(SectionKey.Apps to SectionLayout.Rows)) }
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) { exists("more-below") }

        fun scrolled() =
            compose
                .onNodeWithTag("results")
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
                .value()
        assertEquals(0f, scrolled())
        compose.onNodeWithTag("more-below", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { scrolled() > 0f }
    }

    @Test
    fun noMorePillWhenEverythingFits() {
        compose.onNodeWithTag("query").performTextInput("zzqxv")
        compose.waitForIdle()
        Thread.sleep(1_000)
        assertFalse(exists("more-below"))
    }

    // Web search off

    @Test
    fun turningOffWebSearchRemovesWebResultsAndItsOptions() {
        openSettings("WebAi")
        scrollTo("web-search")
        compose.onNodeWithTag("web-search", useUnmergedTree = true).performClick()
        compose.waitUntil(2_000) { runBlocking { "web" in prefs.preferences.first().disabledProviders } }
        assertFalse("web options still shown", exists("web-suggestions"))

        // Page back to the menu, menu back to search.
        compose.onNodeWithTag("settings-back", useUnmergedTree = true).performClick()
        compose.waitUntil(3_000) { exists("settings-menu") }
        compose.onNodeWithTag("settings-back", useUnmergedTree = true).performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("query"), 3_000)
        compose.onNodeWithTag("query").performTextInput("zzqxv")
        compose.waitForIdle()
        Thread.sleep(1_000)
        assertFalse("web section shown", exists("section-Web"))
        assertTrue(
            "web fallback shown",
            compose.onAllNodes(hasText("Search the web", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    // 15.12 + 15.7: layouts

    @Test
    fun settingsCanShowAsChipsAndAppsAsGridOrRows() {
        set { it.copy(sectionLayouts = mapOf(SectionKey.Settings to SectionLayout.Chips)) }
        compose.onNodeWithTag("query").performTextInput("display")
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("chip:settings:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        Screenshots.screen("layout-settings-chips")

        compose.onNodeWithTag("query").let { node -> repeat(7) { node.performKeyInput { pressKey(Key.Backspace) } } }
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("tile:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        Screenshots.screen("layout-apps-grid")

        set { it.copy(sectionLayouts = mapOf(SectionKey.Apps to SectionLayout.Rows)) }
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("tile:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isEmpty()
        }
    }

    // 15.4: sizing and position

    @Test
    fun largeIconsCompactDensityAndTopPositionApplyLive() {
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) { exists("section-Apps") }
        val bottomTop =
            compose
                .onNodeWithTag("query")
                .fetchSemanticsNode()
                .boundsInRoot.top

        set {
            it.copy(
                iconSize = IconSize.Large,
                density = com.ekquickfind.data.prefs.Density.Compact,
                sectionLayouts =
                    mapOf(
                        SectionKey.Apps to SectionLayout.Rows,
                    ),
            )
        }
        Screenshots.screen("style-large-compact")

        set { it.copy(position = SurfacePosition.Top) }
        compose.waitUntil(3_000) {
            compose
                .onNodeWithTag("query")
                .fetchSemanticsNode()
                .boundsInRoot.top < bottomTop / 2
        }
        Screenshots.screen("style-top")
    }

    // 15.3: background styles

    @Test
    fun backgroundStylesChangeTheWindowAndScrim() {
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) { exists("section-Apps") }

        set { it.copy(backgroundStyle = BackgroundStyle.Blur, blurRadiusDp = 30) }
        assertTrue(activity.background.currentBlurRadius() > 0)
        Screenshots.screen("background-blur")

        set { it.copy(backgroundStyle = BackgroundStyle.FrostedGlass) }
        assertTrue(activity.background.currentBlurRadius() > 0)
        Screenshots.screen("background-frosted")

        set { it.copy(backgroundStyle = BackgroundStyle.Solid, solidColor = 0xFF0B3D2E, dimAmount = 0.85f) }
        assertEquals(0, activity.background.currentBlurRadius())
        Screenshots.screen("background-solid")
    }

    // 15.5: keyboard behaviour

    // 15.17: selected result by the search bar

    @Test
    fun selectedResultCanSitBelowTheSections() {
        set { it.copy(sectionLayouts = mapOf(SectionKey.Apps to SectionLayout.Rows), resultsPerSection = 3) }
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) { exists("section-Apps") && exists("top-result") }

        fun topBottom() =
            compose
                .onNodeWithTag("top-result", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.bottom

        fun appsHeaderTop() =
            compose
                .onNodeWithTag("section-Apps", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue("top result should start above the sections", topBottom() <= appsHeaderTop())

        set { it.copy(selectedAtBottom = true) }
        compose.waitUntil(3_000) { exists("selected-slot") }
        val queryTop =
            compose
                .onNodeWithTag("query")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue("selected result should sit below the sections", topBottom() > appsHeaderTop())
        assertTrue("and above the search bar", topBottom() <= queryTop)
        Screenshots.screen("selected-at-bottom")
    }

    @Test
    fun enterOffOnlyHidesTheKeyboard() {
        set { it.copy(enterRunsSelected = false) }
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
        compose.onNodeWithTag("query").performImeAction()
        compose.waitForIdle()
        Thread.sleep(800)
        assertFalse("search closed although Enter is off", activity.isFinishing)
    }

    @Test
    fun arrowKeysMoveTheSelection() {
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) { exists("section-Apps") }
        set { it.copy(sectionLayouts = mapOf(SectionKey.Apps to SectionLayout.Rows)) }
        compose.onNodeWithTag("query").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
        }
        compose.waitForIdle()
        Screenshots.screen("selection-third-row")
        val selected =
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .isSelected(),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
        assertEquals("exactly one selected row", 1, selected.size)
        val first = compose.onNodeWithTag("top-result", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue("selection should have moved off the top result", selected.single().id != first.id)
    }

    @Test
    fun reduceMotionSwitchesTheListToFadesOnly() {
        compose.onNodeWithTag("query").performTextInput("s")
        compose.waitUntil(3_000) { exists("results") }

        fun reduced() =
            compose
                .onNodeWithTag(
                    "results",
                    useUnmergedTree = true,
                ).fetchSemanticsNode()
                .config[com.ekquickfind.app.ui.search.MotionReduced]
        assertFalse(reduced())
        set { it.copy(reduceMotion = true) }
        compose.waitUntil(2_000) { reduced() }
    }

    @Test
    fun recentResultsShowAsChipsByDefault() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
        // Record a launch without actually opening the app.
        runBlocking {
            val history =
                dagger.hilt.android.EntryPointAccessors
                    .fromApplication(context, HistoryEntryPoint::class.java)
                    .history()
            history.recordLaunch(activityTopResult())
        }
        compose.onNodeWithTag("query").let { node -> repeat(10) { node.performKeyInput { pressKey(Key.Backspace) } } }
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("chip:apps:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        runBlocking {
            dagger.hilt.android.EntryPointAccessors
                .fromApplication(context, HistoryEntryPoint::class.java)
                .history()
                .clear()
        }
    }

    @Test
    fun recentsStripIsOffByDefaultAndShowsRecentChipsWhenOn() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
        val history =
            dagger.hilt.android.EntryPointAccessors
                .fromApplication(context, HistoryEntryPoint::class.java)
                .history()
        runBlocking { history.recordLaunch(activityTopResult()) }
        compose.waitForIdle()
        assertFalse("strip shown although off", exists("recents-strip"))

        set { it.copy(recentsStrip = true) }
        // Visible while typing, not only on the empty screen.
        compose.waitUntil(3_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTagStartingWith("strip:apps:"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        Screenshots.screen("recents-strip")
        runBlocking { history.clear() }
    }

    @Test
    fun pinnedItemsAreNotRepeatedInTheRecentsStrip() {
        compose.onNodeWithTag("query").performTextInput("calculator")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Calculator")), 3_000)
        val history =
            dagger.hilt.android.EntryPointAccessors
                .fromApplication(context, HistoryEntryPoint::class.java)
                .history()
        val calculator = activityTopResult()
        runBlocking {
            history.recordLaunch(calculator)
            history.pin(calculator)
        }
        set { it.copy(recentsStrip = true) }
        compose.onNodeWithTag("query").let { node -> repeat(10) { node.performKeyInput { pressKey(Key.Backspace) } } }
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("zero-Pinned", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(
            "pinned item repeated in the strip",
            compose.onAllNodesWithTag("strip:${calculator.id.value}", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun activityTopResult(): com.ekquickfind.core.search.SearchResult {
        var top: com.ekquickfind.core.search.SearchResult? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            top =
                androidx.lifecycle
                    .ViewModelProvider(activity)[com.ekquickfind.app.search.SearchViewModel::class.java]
                    .state.value.top
                    ?.result
        }
        return top!!
    }

    @Test
    fun keyboardStaysClosedWhenAutoOpenIsOff() {
        SearchLauncher.finishAll()
        set { it.copy(autoOpenKeyboard = false) }
        activity = SearchLauncher.launch()
        Thread.sleep(1_200)
        val ime = ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        assertFalse("keyboard opened although auto-open is off", ime)
    }

    private fun insertImage(name: String): Uri {
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EkQuickFindTest")
            }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)!!
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.CYAN) }
        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return uri
    }
}
