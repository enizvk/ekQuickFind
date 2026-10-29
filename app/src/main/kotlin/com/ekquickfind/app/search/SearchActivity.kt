package com.ekquickfind.app.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.SingletonImageLoader
import com.ekquickfind.app.icons.EkImageLoader
import com.ekquickfind.app.settings.SettingsViewModel
import com.ekquickfind.app.shortcuts.ShortcutsPublisher
import com.ekquickfind.app.ui.search.SearchSurface
import com.ekquickfind.app.ui.search.ZeroQueryContent
import com.ekquickfind.app.ui.settings.ExcludedAppsScreen
import com.ekquickfind.app.ui.settings.SettingsActions
import com.ekquickfind.app.ui.settings.SettingsPage
import com.ekquickfind.app.ui.settings.SettingsScreen
import com.ekquickfind.app.ui.theme.EkQuickFindTheme
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.app.ui.theme.toSurfaceStyle
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.PinAction
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.UnpinAction
import com.ekquickfind.data.prefs.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalSharedTransitionApi::class)
@AndroidEntryPoint
class SearchActivity : ComponentActivity() {
    private val viewModel: SearchViewModel by viewModels()

    @javax.inject.Inject lateinit var updates: com.ekquickfind.app.update.Updates
    private lateinit var host: ActivityActionHost

    /** Set by the "Settings" launcher shortcut; the nav host consumes it. */
    private val openSettingsRequest = mutableStateOf(false)
    lateinit var background: WindowBackground
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        host =
            ActivityActionHost(
                this,
                onHide = viewModel::hide,
                onPin = viewModel::pin,
                onUnpin = { viewModel.unpin(it.id) },
            )
        background = WindowBackground(this)
        applyIntent(intent)
        // Registered here rather than in Application so icons also load under the Hilt test application.
        SingletonImageLoader.setSafe { EkImageLoader.create(applicationContext) }

        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val style = remember(prefs) { prefs.toSurfaceStyle() }
            val dark =
                when (prefs.theme) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
            val blurAvailable by background.blurAvailable
            val nav = rememberNavController()
            val wantsSettings by openSettingsRequest
            LaunchedEffect(wantsSettings) {
                if (wantsSettings) {
                    nav.navigate(SETTINGS) { launchSingleTop = true }
                    openSettingsRequest.value = false
                }
            }

            EkQuickFindTheme(
                dark = dark,
                dynamicColor = prefs.dynamicColor,
                reduceMotion = prefs.reduceMotion,
                accent =
                    prefs.accent.argb
                        ?.let {
                            androidx.compose.ui.graphics
                                .Color(it)
                        },
            ) {
                CompositionLocalProvider(LocalSurfaceStyle provides style) {
                    SharedTransitionLayout {
                        NavHost(
                            navController = nav,
                            startDestination = SEARCH,
                            enterTransition = { fadeIn(tween(200)) },
                            exitTransition = { fadeOut(tween(150)) },
                            popEnterTransition = { fadeIn(tween(200)) },
                            popExitTransition = { fadeOut(tween(150)) },
                        ) {
                            composable(SEARCH) {
                                LaunchedEffect(style, blurAvailable) { background.apply(style, blurEnabled = true) }
                                val bar = Modifier.sharedBounds(rememberSharedContentState(BAR_KEY), this@composable)
                                SearchRoute(blurAvailable, bar) { nav.navigate(SETTINGS) }
                            }
                            composable(SETTINGS) {
                                // Settings is opaque; no point blurring behind it.
                                LaunchedEffect(Unit) { background.apply(style, blurEnabled = false) }
                                SettingsRoute(null, nav, Modifier.sharedBounds(rememberSharedContentState(BAR_KEY), this@composable))
                            }
                            composable("$SETTINGS/{page}") { entry ->
                                LaunchedEffect(Unit) { background.apply(style, blurEnabled = false) }
                                val page =
                                    entry.arguments?.getString("page")?.let { name ->
                                        SettingsPage.entries.firstOrNull {
                                            it.name ==
                                                name
                                        }
                                    }
                                SettingsRoute(page, nav, Modifier)
                            }
                            composable(EXCLUDED_APPS) {
                                val settings: SettingsViewModel = hiltViewModel()
                                val apps by settings.excludableApps.collectAsStateWithLifecycle()
                                val filter by settings.appFilter.collectAsStateWithLifecycle()
                                ExcludedAppsScreen(
                                    apps = apps,
                                    filter = filter,
                                    onFilter = { settings.appFilter.value = it },
                                    onExclude = { app, excluded -> settings.setExcluded(app, excluded) },
                                    onBack = { nav.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SettingsRoute(
        page: SettingsPage?,
        nav: androidx.navigation.NavController,
        barModifier: Modifier,
    ) {
        val settings: SettingsViewModel = hiltViewModel()
        val settingsPrefs by settings.preferences.collectAsStateWithLifecycle()
        val photoTagProgress by settings.photoTagProgress.collectAsStateWithLifecycle()
        SettingsScreen(
            prefs = settingsPrefs,
            actions =
                SettingsActions(
                    update = { settings.update(it) },
                    setProviderEnabled = { id, on -> settings.setProviderEnabled(id, on) },
                    move = { key, up -> settings.move(key, up) },
                    resetOrder = { settings.resetOrder() },
                    setLayout = { key, layout -> settings.setLayout(key, layout) },
                    unhide = { settings.unhide(it) },
                    clearHistory = { settings.clearHistory() },
                    openExcludedApps = { nav.navigate(EXCLUDED_APPS) },
                    setPhotoTags = { settings.setPhotoTags(it) },
                    refreshPhotoCount = { settings.refreshPhotoCount() },
                ),
            onBack = { nav.popBackStack() },
            entryPoints = {
                com.ekquickfind.app.ui.settings
                    .EntryPointsSection(settingsPrefs.tileAdded)
            },
            barModifier = barModifier,
            updates = {
                com.ekquickfind.app.ui.settings
                    .UpdatesSection()
            },
            photoTagProgress = photoTagProgress,
            page = page,
            onOpenPage = { nav.navigate("$SETTINGS/${it.name}") },
        )
    }

    @androidx.compose.runtime.Composable
    private fun SearchRoute(
        blurAvailable: Boolean,
        barModifier: Modifier,
        openSettings: () -> Unit,
    ) {
        val query by viewModel.query.collectAsStateWithLifecycle()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val zeroQuery by viewModel.zeroQueryState.collectAsStateWithLifecycle()
        val pinned by viewModel.pinnedIds.collectAsStateWithLifecycle()
        val style = LocalSurfaceStyle.current
        val view = LocalView.current
        val scope = rememberCoroutineScope()
        val widthClass = calculateWindowSizeClass(this).widthSizeClass

        fun run(
            result: SearchResult?,
            action: Action,
        ) = scope.launch {
            if (viewModel.perform(result, action, host) == ActionOutcome.Close) {
                if (style.haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                dismiss()
            }
        }

        host.choice.value?.let { request -> ChoiceDialog(request) }
        CompositionLocalProvider(com.ekquickfind.app.ui.theme.LocalPinnedIds provides pinned) {
            SearchSurface(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                state = state,
                onSubmit = { selected -> run(selected, selected.primaryAction) },
                onAction = { result, action -> run(result, action) },
                onDismiss = ::dismiss,
                menuActions = { result -> result.secondaryActions + pinToggle(result, pinned) },
                expandedLayout = widthClass != WindowWidthSizeClass.Compact,
                blurAvailable = blurAvailable,
                onOpenSettings = openSettings,
                barModifier = barModifier,
                // Recent items only; pinned ones already have their own section.
                recentStrip = zeroQuery.recent,
                zeroQueryContent =
                    if (zeroQuery.showsPanel(recentsInStrip = style.recentsStrip)) {
                        {
                            ZeroQueryContent(
                                state = zeroQuery,
                                menuActions = { result -> pinToggle(result, pinned) },
                                onAction = { result, action -> run(result, action) },
                                onQuery = viewModel::onQueryChange,
                                onForgetRecent = { viewModel.forgetRecent(it.id) },
                                onClearHistory = { viewModel.clearHistory() },
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }

    private fun pinToggle(
        result: SearchResult,
        pinned: Set<ResultId>,
    ): List<Action> =
        when {
            !result.isRecordable -> emptyList()
            result.id in pinned -> listOf(UnpinAction(result))
            else -> listOf(PinAction(result))
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntent(intent)
    }

    /** SEARCH and WEB_SEARCH carry the query in SearchManager.QUERY; ASSIST may carry it too. Shortcuts use their own actions. */
    private fun applyIntent(intent: Intent?) {
        when (intent?.action) {
            ShortcutsPublisher.ACTION_OPEN_SETTINGS -> {
                openSettingsRequest.value = true
                return
            }
            ShortcutsPublisher.ACTION_OPEN_RESULT -> {
                openFromShortcut(intent)
                return
            }
        }
        val query = intent?.getStringExtra(SearchManager.QUERY)?.takeIf { it.isNotBlank() } ?: return
        viewModel.onQueryChange(query)
    }

    private fun openFromShortcut(intent: Intent) {
        val provider = intent.getStringExtra(ShortcutsPublisher.EXTRA_PROVIDER_ID) ?: return
        val id = intent.getStringExtra(ShortcutsPublisher.EXTRA_RESULT_ID) ?: return
        lifecycleScope.launch {
            val result = viewModel.resolve(provider, id)
            // Pinned item no longer exists (uninstalled app, deleted file): just show search.
            if (result != null && viewModel.perform(result, result.primaryAction, host) == ActionOutcome.Close) finish()
        }
    }

    override fun onStart() {
        super.onStart()
        background.start()
        updates.uiVisible = true
        com.ekquickfind.app.floating.FloatingButtonState.appVisible.value = true
    }

    override fun onStop() {
        super.onStop()
        background.stop()
        updates.uiVisible = false
        com.ekquickfind.app.floating.FloatingButtonState.appVisible.value = false
        // Next time the surface opens it starts fresh.
        viewModel.reset()
    }

    private fun dismiss() {
        lifecycleScope.launch { finish() }
    }

    private companion object {
        const val SEARCH = "search"
        const val SETTINGS = "settings"
        const val EXCLUDED_APPS = "excluded-apps"
        const val BAR_KEY = "search-bar"
    }
}
