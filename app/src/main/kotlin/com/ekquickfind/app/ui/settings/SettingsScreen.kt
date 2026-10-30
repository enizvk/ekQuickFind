package com.ekquickfind.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ekquickfind.app.BuildConfig
import com.ekquickfind.app.floating.FloatingButtonService
import com.ekquickfind.app.floating.FloatingButtonState
import com.ekquickfind.app.ui.search.ClearHistoryDialog
import com.ekquickfind.app.ui.theme.ekColorScheme
import com.ekquickfind.app.ui.theme.family
import com.ekquickfind.data.prefs.AccentColor
import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.AiChat
import com.ekquickfind.data.prefs.BackgroundStyle
import com.ekquickfind.data.prefs.BarAnimation
import com.ekquickfind.data.prefs.Density
import com.ekquickfind.data.prefs.IconSize
import com.ekquickfind.data.prefs.ItemAnimation
import com.ekquickfind.data.prefs.MAX_RECENTS
import com.ekquickfind.data.prefs.MAX_TEXT_SCALE
import com.ekquickfind.data.prefs.MIN_TEXT_SCALE
import com.ekquickfind.data.prefs.ResultFont
import com.ekquickfind.data.prefs.ResultWeight
import com.ekquickfind.data.prefs.SearchEngineChoice
import com.ekquickfind.data.prefs.SectionKey
import com.ekquickfind.data.prefs.SectionLayout
import com.ekquickfind.data.prefs.StripPosition
import com.ekquickfind.data.prefs.SurfacePosition
import com.ekquickfind.data.prefs.ThemeMode
import com.ekquickfind.data.prefs.TopResultStyle
import com.ekquickfind.data.prefs.UserPreferences
import kotlin.math.roundToInt

/** Everything the settings screen can change. Kept as plain callbacks so the screen is easy to preview and test. */
class SettingsActions(
    val update: ((UserPreferences) -> UserPreferences) -> Unit,
    val setProviderEnabled: (String, Boolean) -> Unit,
    val move: (SectionKey, Boolean) -> Unit,
    val resetOrder: () -> Unit,
    val setLayout: (SectionKey, SectionLayout) -> Unit,
    val unhide: (String) -> Unit,
    val clearHistory: () -> Unit,
    val openExcludedApps: () -> Unit = {},
    val setPhotoTags: (Boolean) -> Unit = {},
    val refreshPhotoCount: () -> Unit = {},
    val resetSettings: () -> Unit = {},
)

data class PhotoTagProgress(
    val done: Int,
    val total: Int,
)

private data class ProviderRow(
    val id: String,
    val title: String,
    val description: String,
)

private val providerRows =
    listOf(
        ProviderRow("apps", "Apps", "Installed apps and work profile apps"),
        ProviderRow("contacts", "Contacts", "People, with call and message actions"),
        ProviderRow("calendar", "Calendar", "Events by title or place, and what's next"),
        ProviderRow("device-settings", "Device settings", "Settings screens and quick toggles"),
        ProviderRow("files", "Files", "Photos, videos, music and documents"),
        ProviderRow("quick-answers", "Quick answers", "Calculator and unit conversion"),
    )

/** One card or row each, so there's no layout to pick. */
private val fixedLayoutSections = setOf(SectionKey.QuickAnswers, SectionKey.Translation, SectionKey.Web, SectionKey.Sites)

private val sectionNames =
    mapOf(
        SectionKey.QuickAnswers to "Answer",
        SectionKey.Translation to "Translation",
        SectionKey.Apps to "Apps",
        SectionKey.Sites to "Search with",
        SectionKey.Contacts to "Contacts",
        SectionKey.Calendar to "Calendar",
        SectionKey.Settings to "Settings",
        SectionKey.Files to "Files",
        SectionKey.Web to "Web",
        SectionKey.AskAi to "Ask AI",
        SectionKey.Recent to "Recent (empty search)",
    )

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    actions: SettingsActions,
    onBack: () -> Unit,
    barModifier: Modifier = Modifier,
    entryPoints: @Composable () -> Unit = {},
    updates: @Composable () -> Unit = {},
    photoTagProgress: PhotoTagProgress? = null,
    /** Null shows the menu of pages. */
    page: SettingsPage? = null,
    /** A panel title to scroll to and flash, from settings search. */
    focus: String? = null,
    onOpenPage: (SettingsPage) -> Unit = {},
    onOpenPanel: (SettingsPage, String) -> Unit = { target, _ -> onOpenPage(target) },
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        ClearHistoryDialog(onConfirm = {
            confirmClear = false
            actions.clearHistory()
        }, onDismiss = { confirmClear = false })
    }
    var confirmReset by remember { mutableStateOf(false) }
    if (confirmReset) {
        ResetSettingsDialog(onConfirm = {
            confirmReset = false
            actions.resetSettings()
        }, onDismiss = { confirmReset = false })
    }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize().testTag("settings")) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = barModifier.padding(12.dp),
            ) {
                TopAppBar(
                    title = { Text(page?.title ?: "Settings") },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings-back"),
                        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0),
                )
            }
            // Each page is a list of named panels, so settings search can scroll to one by its title.
            val panels: List<Pair<String, @Composable () -> Unit>> =
                when (page) {
                    null -> listOf(panel("menu") { SettingsMenu(onOpenPage, onOpenPanel) })
                    SettingsPage.Sources ->
                        listOf(
                            panel("Search sources") { SourcesSection(prefs, actions, photoTagProgress) },
                            panel("Before you type") { BeforeTypingSection(prefs, actions) },
                            panel("Result order") { OrderSection(prefs, actions) },
                            panel("Layout") { ResultsLayoutSection(prefs, actions) },
                        )
                    SettingsPage.WebAi ->
                        listOf(
                            panel("Web and AI") { WebSection(prefs, actions) },
                            panel("Search sites") { SearchSitesSection(prefs, actions) },
                            panel("Translation") { TranslationSection(prefs, actions) },
                        )
                    SettingsPage.Look ->
                        listOf(
                            panel("Quick looks") { QuickLooks(prefs, actions) },
                            panel("Background") { BackgroundSection(prefs, actions) },
                            panel("Appearance") { AppearanceSection(prefs, actions) },
                            panel("Text") { TextSection(prefs, actions) },
                            panel("Search bar") { SearchBarSection(prefs, actions) },
                            panel("Active item") { ActiveItemSection(prefs, actions) },
                        )
                    SettingsPage.Keyboard -> listOf(panel("Keyboard and feedback") { KeyboardSection(prefs, actions) })
                    SettingsPage.WaysToOpen ->
                        listOf(
                            panel("Ways to open") { entryPoints() },
                            panel("Floating button") { FloatingButtonSection(prefs, actions) },
                        )
                    SettingsPage.Privacy -> listOf(panel("Privacy") { PrivacySection(prefs, actions) { confirmClear = true } })
                    SettingsPage.About ->
                        listOfNotNull(
                            panel("About") { AboutSection() },
                            (panel("Updates") { updates() }).takeIf { BuildConfig.UPDATE_FEED_URL.isNotBlank() },
                            panel("Reset") { ResetSection { confirmReset = true } },
                        )
                }
            val listState = rememberLazyListState()
            val focused = remember { mutableStateOf(focus) }
            // The Look page's pinned preview sits above its panels.
            val pinnedPreview = page == SettingsPage.Look
            LaunchedEffect(focus) {
                val index = panels.indexOfFirst { it.first == focus }
                if (index >= 0) listState.animateScrollToItem(index + if (pinnedPreview) 1 else 0)
            }
            CompositionLocalProvider(
                LocalSettingsPageTitle provides page?.title,
                LocalSettingsPrefs provides prefs,
                LocalSettingsActions provides actions,
                LocalFocusedPanel provides focused,
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.testTag("settings-list"),
                ) {
                    if (pinnedPreview) {
                        stickyHeader(key = "preview") {
                            // Shrinks to a strip once the first panel scrolls away.
                            val compact by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }
                            LookPreview(prefs, compact)
                        }
                    }
                    items(panels, key = { it.first }) { it.second() }
                }
            }
        }
    }
}

private const val TOP_RESULT_INFO =
    "Featured shows the best match as a big card above everything. In its section keeps it in its usual place, highlighted."

/** A page's panel by its title, so settings search can find it in the list. */
private fun panel(
    title: String,
    content: @Composable () -> Unit,
): Pair<String, @Composable () -> Unit> = title to content

/** Title of the open settings page, so a panel with the same name skips its heading. */
private val LocalSettingsPageTitle = staticCompositionLocalOf<String?> { null }

/** Top-level settings pages, shown as a menu like the system Settings app. */
enum class SettingsPage(
    val title: String,
    val summary: String,
) {
    Sources("Search sources", "What to search, result order and layout"),
    WebAi("Web and AI", "Web search, search engine, site shortcuts, weather and Ask AI"),
    Look("Look and feel", "Background, theme, text, search bar and active item"),
    Keyboard("Keyboard and feedback", "Enter key, haptics and motion"),
    WaysToOpen("Ways to open", "Home screen bar, Quick Settings tile and assistant"),
    Privacy("Privacy", "History, backups and hidden results"),

    // The store build is updated by its store, so it has no updates part.
    About(
        "App",
        if (BuildConfig.UPDATE_FEED_URL.isNotBlank()) "Version, GitHub page and automatic updates" else "Version and GitHub page",
    ),
}

private fun SettingsPage.icon(): ImageVector =
    when (this) {
        SettingsPage.Sources -> Icons.AutoMirrored.Outlined.ManageSearch
        SettingsPage.WebAi -> Icons.Outlined.Public
        SettingsPage.Look -> Icons.Outlined.Palette
        SettingsPage.Keyboard -> Icons.Outlined.Keyboard
        SettingsPage.WaysToOpen -> Icons.Outlined.TouchApp
        SettingsPage.Privacy -> Icons.Outlined.Shield
        SettingsPage.About -> Icons.Outlined.Info
    }

@Composable
private fun SettingsMenu(
    onOpen: (SettingsPage) -> Unit,
    onOpenPanel: (SettingsPage, String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    SettingsSearchField(query) { query = it }
    if (query.isNotBlank()) {
        SettingsSearchResults(SettingsIndex.search(query)) { entry -> onOpenPanel(entry.page, entry.panel.title) }
        return
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().testTag("settings-menu"),
    ) {
        Column {
            SettingsPage.entries.forEachIndexed { i, page ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(page) }
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .testTag("settings-page:${page.name}"),
                ) {
                    Icon(page.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f).padding(start = 20.dp, end = 12.dp)) {
                        Text(page.title, style = MaterialTheme.typography.titleMedium)
                        Text(page.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(
    text: String,
    icon: ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp).heightIn(min = 40.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).testTag("settings-section:$text"),
        )
        PanelResetLine(text)
    }
}

/** A titled panel, a clear step lighter (dark) or darker (light) than the page so groups read at a glance. */
@Composable
internal fun SettingsGroup(
    title: String,
    icon: ImageVector? = null,
    /** Rarer options, folded under "More options". */
    more: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // On a page that is just this panel, the page title already says it; the Reset line still shows.
    if (title != LocalSettingsPageTitle.current) {
        SectionTitle(title, icon)
    } else {
        Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.End) { PanelResetLine(title) }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .flashWhenFocused(title)
                .testTag("settings-group:$title"),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            content()
            more?.let { MoreOptions(title, it) }
        }
    }
}

/** Hairline between rows in a panel, so neighbouring options don't run together. */
@Composable
internal fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String,
    /** A short explanation behind an ⓘ next to the title. */
    info: String? = null,
    trailingExtra: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                info?.let { InfoTip(title, it) }
            }
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            trailingExtra?.invoke()
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> Choice(
    label: String,
    options: List<T>,
    selected: T,
    name: (T) -> String,
    onSelect: (T) -> Unit,
    tag: String,
    info: String? = null,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            info?.let { InfoTip(label, it) }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    modifier = Modifier.testTag("$tag:${name(option)}"),
                ) { Text(name(option), maxLines = 1) }
            }
        }
    }
}

// --- Sources & permissions ---

private enum class Grant { Granted, Partial, Missing }

private fun Context.granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun SourcesSection(
    prefs: UserPreferences,
    actions: SettingsActions,
    photoTagProgress: PhotoTagProgress?,
) {
    val context = LocalContext.current
    // Re-read permission state whenever the user comes back (for example from system settings).
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        tick++
        actions.refreshPhotoCount()
    }
    val requester = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    fun openAppSettings() =
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )

    SettingsGroup("Search sources", Icons.AutoMirrored.Outlined.ManageSearch) {
        providerRows.forEachIndexed { i, row ->
            if (i > 0) SettingsDivider()
            val enabled = row.id !in prefs.disabledProviders
            SwitchRow(row.title, row.description, enabled, { actions.setProviderEnabled(row.id, it) }, "provider:${row.id}") {
                tick.let { _ -> }
                when (row.id) {
                    "contacts" -> {
                        val grant = if (context.granted(Manifest.permission.READ_CONTACTS)) Grant.Granted else Grant.Missing
                        PermissionLine(
                            grant,
                            "contacts",
                            onGrant = { requester.launch(arrayOf(Manifest.permission.READ_CONTACTS)) },
                            onManage = ::openAppSettings,
                        )
                    }
                    "calendar" -> {
                        val grant = if (context.granted(Manifest.permission.READ_CALENDAR)) Grant.Granted else Grant.Missing
                        PermissionLine(
                            grant,
                            "calendar",
                            onGrant = { requester.launch(arrayOf(Manifest.permission.READ_CALENDAR)) },
                            onManage = ::openAppSettings,
                        )
                    }
                    "files" -> {
                        val media =
                            when {
                                context.granted(Manifest.permission.READ_MEDIA_IMAGES) -> Grant.Granted
                                context.granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Grant.Partial
                                else -> Grant.Missing
                            }
                        PermissionLine(
                            media,
                            "files",
                            onGrant = {
                                requester.launch(
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO,
                                        Manifest.permission.READ_MEDIA_AUDIO,
                                    ),
                                )
                            },
                            onManage = ::openAppSettings,
                        )
                        val docs = Environment.isExternalStorageManager()
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            },
                            modifier = Modifier.testTag("all-files-access"),
                        ) { Text(if (docs) "Documents and downloads: allowed" else "Include documents and downloads") }
                    }
                    "apps" -> {
                        val excluded = prefs.hiddenResults.keys.count { it.startsWith("apps:") }
                        TextButton(onClick = actions.openExcludedApps, modifier = Modifier.testTag("open-excluded-apps")) {
                            Text(if (excluded == 0) "Excluded apps" else "Excluded apps ($excluded)")
                        }
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                            Text("Usage access for suggested apps")
                        }
                    }
                }
            }
            if (row.id == "files") {
                tick.let { _ -> }
                PhotoTagsRow(
                    prefs.photoTags,
                    context.granted(Manifest.permission.READ_MEDIA_IMAGES),
                    photoTagProgress,
                    actions.setPhotoTags,
                )
            }
        }
    }
}

/** What shows before anything is typed, besides pins and recents. */
@Composable
private fun BeforeTypingSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Before you type", Icons.Outlined.AutoAwesome) {
        SwitchRow(
            "Smart suggestions",
            "What you usually open at this time, or with the headphones or car that are connected. Learned on this phone only.",
            prefs.smartSuggestions,
            { v -> actions.update { it.copy(smartSuggestions = v) } },
            "smart-suggestions",
            info =
                "Opened Spotify a few times with the car connected? It's suggested again whenever the car is connected. " +
                    "Morning apps come up in the morning. All of it is worked out on the phone from your own history.",
        )
        SettingsDivider()
        SwitchRow(
            "Next up",
            "The calendar event happening now or in the next 12 hours (needs Calendar search)",
            prefs.nextUp,
            { v -> actions.update { it.copy(nextUp = v) } },
            "next-up",
        )
        SettingsDivider()
        SwitchRow(
            "Clipboard",
            "A Clipboard chip when you've copied text. It's only read when you tap it, and nothing is kept.",
            prefs.clipboardChip,
            { v -> actions.update { it.copy(clipboardChip = v) } },
            "clipboard-chip-switch",
        )
    }
}

/** Photo tags sit under Files: they search the same photos by what's in them. */
@Composable
private fun PhotoTagsRow(
    on: Boolean,
    canReadPhotos: Boolean,
    progress: PhotoTagProgress?,
    onChange: (Boolean) -> Unit,
) {
    // Indented with an accent bar: it's an option of Files, not a source of its own.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = 20.dp)) {
        Box(
            Modifier
                .padding(vertical = 10.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
        )
        Column(Modifier.weight(1f)) {
            SwitchRow(
                "Photo tags",
                "Find photos by what's in them, like dog, beach or food, and by keywords saved in them. Tagged on this phone, nothing is uploaded.",
                on,
                onChange,
                "photo-tags",
            ) {
                val status =
                    when {
                        !on -> null
                        !canReadPhotos -> "Needs photo access (allow Files above)"
                        progress == null -> null
                        progress.done >= progress.total -> "All %,d photos tagged".format(progress.total)
                        else -> "Tagged %,d of %,d photos".format(progress.done, progress.total)
                    }
                status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canReadPhotos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp).testTag("photo-tags-status"),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionLine(
    grant: Grant,
    key: String,
    onGrant: () -> Unit,
    onManage: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (text, color) =
            when (grant) {
                Grant.Granted -> "Permission granted" to MaterialTheme.colorScheme.primary
                Grant.Partial -> "Limited access" to MaterialTheme.colorScheme.tertiary
                Grant.Missing -> "Permission needed" to MaterialTheme.colorScheme.error
            }
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("permission:$key"))
        Spacer(Modifier.width(8.dp))
        if (grant == Grant.Granted) {
            TextButton(onClick = onManage) { Text("Manage") }
        } else {
            TextButton(onClick = onGrant, modifier = Modifier.testTag("grant:$key")) { Text("Allow") }
        }
    }
}

// --- Web ---

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WebSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Web and AI", Icons.Outlined.Public) {
        val webOn = WEB !in prefs.disabledProviders
        SwitchRow(
            "Web search",
            "Web suggestions and the \u201cSearch the web\u201d row. Off keeps search on this phone only.",
            webOn,
            { actions.setProviderEnabled(WEB, it) },
            "web-search",
        )
        SettingsDivider()
        if (webOn) {
            SwitchRow(
                "Web suggestions",
                "Sends what you type to ${prefs.searchEngine.label()} after a short pause. Off means no network use at all.",
                prefs.webSuggestions,
                { on -> actions.update { it.copy(webSuggestions = on) } },
                "web-suggestions",
            )
            Text(
                "Search engine",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
            FlowRow(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchEngineChoice.entries.forEach { engine ->
                    FilterChip(
                        selected = prefs.searchEngine == engine,
                        onClick = { actions.update { it.copy(searchEngine = engine) } },
                        label = { Text(engine.label()) },
                        modifier = Modifier.testTag("engine:${engine.name}"),
                    )
                }
            }
            if (prefs.searchEngine == SearchEngineChoice.Custom) {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prefs.customTemplate.orEmpty(),
                        onValueChange = { v -> actions.update { it.copy(customTemplate = v) } },
                        label = { Text("Search URL with %s") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = prefs.customSuggestTemplate.orEmpty(),
                        onValueChange = { v -> actions.update { it.copy(customSuggestTemplate = v) } },
                        label = { Text("Suggestions URL with %s (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        // Ask AI opens apps, so it stays available with web search off.
        SettingsDivider()
        AskAiChoices(prefs.aiChats) { chats -> actions.update { it.copy(aiChats = chats) } }
    }
}

private const val WEB = "web"

/** Which AI chats appear under "Ask AI" in results. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AskAiChoices(
    selected: Set<AiChat>,
    onChange: (Set<AiChat>) -> Unit,
) {
    Text(
        "Ask AI",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
    Text(
        "Shortcuts that open the chat app (or its website) with what you typed",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp),
    )
    FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiChat.entries.forEach { chat ->
            val on = chat in selected
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) selected - chat else selected + chat) },
                label = { Text(chat.label) },
                modifier = Modifier.testTag("ask-ai:${chat.name}"),
            )
        }
    }
}

private fun SearchEngineChoice.label() =
    when (this) {
        SearchEngineChoice.DuckDuckGo -> "DuckDuckGo"
        SearchEngineChoice.Google -> "Google"
        SearchEngineChoice.Bing -> "Bing"
        SearchEngineChoice.Brave -> "Brave Search"
        SearchEngineChoice.Custom -> "Custom"
    }

// --- Result order ---

@Composable
private fun OrderSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    // Local copy so rows follow the finger; it's saved once the drag ends.
    var order by remember(prefs.sectionOrder) { mutableStateOf(prefs.sectionOrder) }
    var dragged by remember { mutableStateOf<SectionKey?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val haptics = LocalHapticFeedback.current

    fun endDrag() {
        dragged = null
        dragOffset = 0f
        actions.update { it.copy(sectionOrder = order) }
    }

    SettingsGroup("Result order", Icons.AutoMirrored.Outlined.Sort) {
        Text(
            "Drag to change which results come first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        )
        order.forEachIndexed { i, key ->
            key(key) {
                val isDragged = dragged == key
                val lift by animateDpAsState(if (isDragged) 6.dp else 0.dp, label = "lift")
                val name = sectionNames.getValue(key)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDragged) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                    shadowElevation = lift,
                    modifier =
                        Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                            .onSizeChanged { rowHeight = it.height }
                            .semantics {
                                // Screen readers can't drag; give them the same moves as actions.
                                customActions =
                                    listOfNotNull(
                                        CustomAccessibilityAction(
                                            "Move $name up",
                                        ) { actions.move(key, true).let { true } }.takeIf { i > 0 },
                                        CustomAccessibilityAction("Move $name down") {
                                            actions.move(key, false).let { true }
                                        }.takeIf { i < order.lastIndex },
                                    )
                            }.testTag("order:${key.name}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    ) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Outlined.DragHandle,
                            contentDescription = "Drag $name",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .testTag("drag:${key.name}")
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                dragged = key
                                                dragOffset = 0f
                                            },
                                            onDragEnd = ::endDrag,
                                            onDragCancel = ::endDrag,
                                        ) { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                            val from = order.indexOf(key)
                                            val to = dragTarget(from, dragOffset, rowHeight, order.size)
                                            if (to != from) {
                                                order = order.moved(from, to)
                                                // The row now sits in its new slot, so the finger is that much closer to it.
                                                dragOffset -= (to - from) * rowHeight
                                                if (prefs.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                            }
                                        }
                                    }.padding(12.dp),
                        )
                    }
                }
            }
        }
        TextButton(onClick = actions.resetOrder, modifier = Modifier.padding(start = 4.dp).testTag("reset-order")) { Text("Reset order") }
    }
}

// --- Layout ---

@Composable
private fun ResultsLayoutSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup(
        "Layout",
        Icons.Outlined.GridView,
        more = {
            Choice(
                "Results per section",
                listOf(3, 4, 6, 8),
                prefs.resultsPerSection,
                { it.toString() },
                { v -> actions.update { it.copy(resultsPerSection = v) } },
                "per-section",
                info = "How many results each section shows before its “More” button.",
            )
        },
    ) {
        Choice("Icon size", IconSize.entries, prefs.iconSize, { it.name }, { v -> actions.update { it.copy(iconSize = v) } }, "icon-size")
        Choice(
            "Density",
            Density.entries,
            prefs.density,
            { it.name },
            { v -> actions.update { it.copy(density = v) } },
            "density",
            info = "Compact puts more results on screen with less space between them.",
        )
        // Answers, site shortcuts and web links have one fixed layout.
        sectionNames.keys.filter { it !in fixedLayoutSections }.forEach { key ->
            PictureChoice(sectionNames.getValue(key), SectionLayout.entries, prefs.layoutFor(key), {
                it.name
            }, { actions.setLayout(key, it) }, "layout:${key.name}") { LayoutPicture(it) }
        }
    }
}

@Composable
private fun SearchBarSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup(
        "Search bar",
        Icons.Outlined.Search,
        more = {
            if (prefs.recentsStrip) {
                PictureChoice("Recent strip position", StripPosition.entries, prefs.stripPosition, {
                    if (it == StripPosition.Top) "Above results" else "Below results"
                }, { v -> actions.update { it.copy(stripPosition = v) } }, "strip-position") { StripPicture(it) }
            }
            RecentsCountField(prefs.recentsCount) { v -> actions.update { it.copy(recentsCount = v) } }
            SettingsDivider()
            SwitchRow(
                "Exact match button",
                "A quote-mark button in the bar that searches for whole words only",
                prefs.exactMatchButton,
                // Hidden, it couldn't be turned off again, so exact match goes off with it.
                { v -> actions.update { it.copy(exactMatchButton = v, exactMatch = it.exactMatch && v) } },
                "exact-button-switch",
                info = "With it on, “map” finds only names with the word “map”, not “Maps” or “Bitmap”. Tap it in the bar to switch.",
            )
        },
    ) {
        PictureChoice("Position", SurfacePosition.entries, prefs.position, {
            it.name
        }, { v -> actions.update { it.copy(position = v) } }, "position") { PositionPicture(it) }
        SettingsDivider()
        SwitchRow(
            "Recent items strip",
            "Recently opened items as small chips next to the bar, even while typing",
            prefs.recentsStrip,
            { v -> actions.update { it.copy(recentsStrip = v) } },
            "recents-strip-toggle",
        )
        SettingsDivider()
        SwitchRow(
            "Animation when search opens",
            motionNote(prefs) ?: "A short glow around the bar for a few seconds",
            prefs.barAnimationOn,
            { v -> actions.update { it.copy(barAnimationOn = v) } },
            "bar-animation-switch",
        )
        if (prefs.barAnimationOn) {
            AnimationChips(BarAnimation.entries, prefs.barAnimation, { it.label() }, { v ->
                actions.update { it.copy(barAnimation = v) }
            }, "bar-animation")
        }
    }
}

// --- Text ---

private fun ResultFont.label() =
    when (this) {
        ResultFont.Default -> "Default"
        ResultFont.Serif -> "Serif (Lora)"
        ResultFont.Monospace -> "Mono (JetBrains Mono)"
        ResultFont.Condensed -> "Condensed (Roboto Condensed)"
        ResultFont.Rounded -> "Rounded (Nunito)"
    }

@Composable
private fun TextSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup(
        "Text",
        Icons.Outlined.TextFields,
        more = {
            Choice("Weight", ResultWeight.entries, prefs.resultWeight, { it.name }, { v ->
                actions.update { it.copy(resultWeight = v) }
            }, "result-weight", info = "Regular keeps the usual mix of light and bold text; the others make all result text that weight.")
        },
    ) {
        TextSizeSlider(prefs.textScale) { v -> actions.update { it.copy(textScale = v) } }
        SettingsDivider()
        Text(
            "Font",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        // Each name in its own font, like the system's font style list.
        ResultFont.entries.forEach { font ->
            val base = MaterialTheme.typography.bodyLarge
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = prefs.resultFont == font, role = Role.RadioButton) {
                        actions.update { it.copy(resultFont = font) }
                    }.padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("result-font:${font.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = prefs.resultFont == font, onClick = null)
                Text(
                    font.label(),
                    style = base.copy(fontFamily = font.family() ?: base.fontFamily),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/** The usual small-A-to-big-A slider; 80 % to 160 % in steps of 10. */
@Composable
private fun TextSizeSlider(
    value: Float,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row {
            Text("Text size", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${(value * 100).roundToInt()} %",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = value,
                onValueChange = { onChange((it * 10).roundToInt() / 10f) },
                valueRange = MIN_TEXT_SCALE..MAX_TEXT_SCALE,
                steps = ((MAX_TEXT_SCALE - MIN_TEXT_SCALE) * 10).roundToInt() - 1,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).testTag("text-size-slider"),
            )
            Text("A", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** A small number box; only 1 to [MAX_RECENTS] is saved, anything else just shows as an error. */
@Composable
private fun RecentsCountField(
    value: Int,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val valid = text.toIntOrNull() in 1..MAX_RECENTS
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Recent items shown", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Before you type, in the list or the strip (1 to $MAX_RECENTS)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompactField(
            value = text,
            onValueChange = { typed ->
                text = typed.filter(Char::isDigit).take(2)
                text.toIntOrNull()?.takeIf { it in 1..MAX_RECENTS }?.let(onChange)
            },
            isError = !valid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.padding(start = 12.dp).width(56.dp).testTag("recents-count"),
        )
    }
}

// --- Active item ---

private fun ActiveItemStyle.label() =
    when (this) {
        ActiveItemStyle.TintedCard -> "Tinted card"
        ActiveItemStyle.SoftGlow -> "Soft glow"
        ActiveItemStyle.Gradient -> "Gradient"
    }

private fun BarAnimation.label() =
    when (this) {
        BarAnimation.LightSweep -> "Light sweep"
        BarAnimation.ColourPulse -> "Colour pulse"
        BarAnimation.ShimmerRing -> "Shimmer ring"
        BarAnimation.Breathe -> "Breathe"
        BarAnimation.TwinLights -> "Twin lights"
    }

private fun ItemAnimation.label() =
    when (this) {
        ItemAnimation.Settle -> "Settle"
        ItemAnimation.Lift -> "Lift"
        ItemAnimation.LightSweep -> "Light sweep"
        ItemAnimation.ColourPulse -> "Colour pulse"
        ItemAnimation.ShimmerRing -> "Shimmer ring"
        ItemAnimation.Breathe -> "Breathe"
        ItemAnimation.TwinLights -> "Twin lights"
    }

private fun motionNote(prefs: UserPreferences) = if (prefs.reduceMotion) "Off while Reduce motion is on" else null

@Composable
private fun ActiveItemSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Active item", Icons.Outlined.AutoAwesome) {
        PictureChoice("Top result", TopResultStyle.entries, prefs.topResult, {
            when (it) {
                TopResultStyle.Featured -> "Featured"
                TopResultStyle.Inline -> "In its section"
            }
        }, { v ->
            actions.update {
                it.copy(topResult = v)
            }
        }, "top-result", info = TOP_RESULT_INFO) {
            TopResultPicture(it)
        }
        SettingsDivider()
        SwitchRow(
            "Selected result by the search bar",
            "The best match sits right above the bar, where your thumb is",
            prefs.selectedAtBottom,
            { v -> actions.update { it.copy(selectedAtBottom = v) } },
            "selected-at-bottom",
            info =
                "Handy with the bar at the bottom: the result Enter opens is always just above the keyboard, " +
                    "and the rest scroll above it.",
        )
        SettingsDivider()
        PictureChoice("Style", ActiveItemStyle.entries, prefs.activeItemStyle, {
            it.label()
        }, { v -> actions.update { it.copy(activeItemStyle = v) } }, "active-item") { ActiveItemPicture(it) }
        SettingsDivider()
        SwitchRow(
            "Animation when it becomes active",
            motionNote(prefs) ?: "Plays once on the best match, and on a result arrow keys move to",
            prefs.itemAnimationOn,
            { v -> actions.update { it.copy(itemAnimationOn = v) } },
            "item-animation-switch",
        )
        if (prefs.itemAnimationOn) {
            AnimationChips(ItemAnimation.entries, prefs.itemAnimation, { it.label() }, { v ->
                actions.update { it.copy(itemAnimation = v) }
            }, "item-animation")
        }
    }
}

@Composable
private fun <T> AnimationChips(
    options: List<T>,
    selected: T,
    name: (T) -> String,
    onSelect: (T) -> Unit,
    tag: String,
) {
    // Too many names for one segmented row, so they wrap as chips.
    FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(name(option)) },
                modifier = Modifier.testTag("$tag:${name(option)}"),
            )
        }
    }
}

@Composable
internal fun ReplayButton(
    description: String,
    tag: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Icon(Icons.Outlined.PlayArrow, contentDescription = description)
    }
}

// --- Background ---

@Composable
private fun BackgroundSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup(
        "Background",
        Icons.Outlined.Wallpaper,
        more = {
            SliderRow("Dim behind search", prefs.dimAmount, 0f..0.9f, "${(prefs.dimAmount * 100).roundToInt()}%", "dim") { v ->
                actions.update { it.copy(dimAmount = v) }
            }
            if (prefs.backgroundStyle != BackgroundStyle.Solid) {
                SliderRow("Blur strength", prefs.blurRadiusDp.toFloat(), 0f..60f, "${prefs.blurRadiusDp} dp", "blur") { v ->
                    actions.update { it.copy(blurRadiusDp = v.roundToInt()) }
                }
            }
        },
    ) {
        PictureChoice(
            "Style",
            BackgroundStyle.entries,
            prefs.backgroundStyle,
            { if (it == BackgroundStyle.FrostedGlass) "Frosted" else it.name },
            { v -> actions.update { it.copy(backgroundStyle = v) } },
            "background",
            info =
                "Blur softens the app behind search. Frosted also lays a see-through panel under the results. " +
                    "Solid hides the app behind.",
        ) { BackgroundPicture(it) }
        if (prefs.backgroundStyle == BackgroundStyle.Solid) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SOLID_SWATCHES.forEach { argb ->
                    val selected = prefs.solidColor == argb
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            .clickable { actions.update { it.copy(solidColor = argb) } }
                            .testTag("swatch:${argb.toString(16)}"),
                    )
                }
            }
        }
    }
}

private val SOLID_SWATCHES = listOf(0xFF101418, 0xFF000000, 0xFF1B1F3B, 0xFF0B3D2E, 0xFF3B0B2E, 0xFFF2F2F2)

/** The floating button over other apps; visible while this panel is open so changes show live. */
@Composable
private fun FloatingButtonSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val canDraw = remember(tick) { Settings.canDrawOverlays(context) }
    // Keep the service in step with the switch and the permission (granted in system settings).
    LaunchedEffect(prefs.floatingButton, canDraw) { FloatingButtonService.sync(context, prefs.floatingButton && canDraw) }
    DisposableEffect(Unit) {
        FloatingButtonState.previewing.value = true
        onDispose { FloatingButtonState.previewing.value = false }
    }

    fun askForOverlay() =
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null)))

    SettingsGroup("Floating button", Icons.Outlined.Adjust) {
        SwitchRow(
            "Show floating button",
            "A small button over other apps that opens search. Drag it anywhere; it settles on the nearest edge.",
            prefs.floatingButton,
            { on ->
                actions.update { it.copy(floatingButton = on) }
                if (on && !canDraw) askForOverlay()
            },
            "floating-button",
        ) {
            if (prefs.floatingButton && !canDraw) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Needs “Display over other apps”",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    TextButton(onClick = ::askForOverlay, modifier = Modifier.testTag("floating-allow")) { Text("Allow") }
                }
            }
        }
        if (prefs.floatingButton) {
            SettingsDivider()
            SliderRow("Size", prefs.floatingButtonSizeDp.toFloat(), 36f..80f, "${prefs.floatingButtonSizeDp} dp", "floating-size") { v ->
                actions.update { it.copy(floatingButtonSizeDp = v.roundToInt()) }
            }
            SliderRow(
                "Opacity",
                prefs.floatingButtonOpacity,
                0.1f..1f,
                "${(prefs.floatingButtonOpacity * 100).roundToInt()}%",
                "floating-opacity",
            ) { v -> actions.update { it.copy(floatingButtonOpacity = v) } }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    tag: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.testTag("slider:$tag"))
    }
}

// --- Keyboard, feedback, motion ---

@Composable
private fun KeyboardSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Keyboard and feedback", Icons.Outlined.Keyboard) {
        SwitchRow("Open keyboard automatically", null, prefs.autoOpenKeyboard, { v ->
            actions.update {
                it.copy(autoOpenKeyboard = v)
            }
        }, "auto-keyboard")
        SettingsDivider()
        SwitchRow(
            "Enter runs the selected result",
            "Otherwise Enter just hides the keyboard",
            prefs.enterRunsSelected,
            { v -> actions.update { it.copy(enterRunsSelected = v) } },
            "enter-runs",
            info = "Type “cal” and press Enter to open Calculator straight away.",
        )
        SettingsDivider()
        SwitchRow("Haptic feedback", "A light tap when you open or long-press a result", prefs.haptics, { v ->
            actions.update {
                it.copy(haptics = v)
            }
        }, "haptics")
        SettingsDivider()
        SwitchRow(
            "Reduce motion",
            "Calmer: fades instead of slides, and no glows or animations",
            prefs.reduceMotion,
            { v -> actions.update { it.copy(reduceMotion = v) } },
            "reduce-motion",
            info = "Turns off the search bar and active item animations too, whatever they're set to.",
        )
    }
}

// --- Appearance ---

@Composable
private fun AppearanceSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Appearance", Icons.Outlined.Palette) {
        Choice("Theme", ThemeMode.entries, prefs.theme, { it.name }, { v -> actions.update { it.copy(theme = v) } }, "theme")
        SettingsDivider()
        SwitchRow("Dynamic colour", "Use colours from your wallpaper", prefs.dynamicColor, { v ->
            actions.update {
                it.copy(dynamicColor = v)
            }
        }, "dynamic-color")
        val dark =
            when (prefs.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
        // The screen is already tinted with the chosen accent, so ask for the untinted one for the System swatch.
        val systemAccent = ekColorScheme(dark, prefs.dynamicColor).primary
        SettingsDivider()
        AccentPicker(prefs.accent, systemAccent) { v -> actions.update { it.copy(accent = v) } }
    }
}

/** Swatches for the accent colour; System keeps the wallpaper (or default) accent. */
@Composable
private fun AccentPicker(
    selected: AccentColor,
    systemAccent: Color,
    onPick: (AccentColor) -> Unit,
) {
    Text(
        "Accent colour",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
    Text(
        "Highlights, the selected result, links and switches",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp),
    )
    FlowRow(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AccentColor.entries.forEach { accent ->
            val isSelected = accent == selected
            // System shows today's theme accent with a wallpaper mark.
            val fill = accent.argb?.let { Color(it) } ?: systemAccent
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onPick(accent) }
                        .semantics { contentDescription = "${accent.name} accent" }
                        .testTag("accent:${accent.name}"),
            ) {
                when {
                    isSelected -> Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    accent == AccentColor.System ->
                        Icon(Icons.Outlined.Wallpaper, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// --- Privacy ---

@Composable
private fun PrivacySection(
    prefs: UserPreferences,
    actions: SettingsActions,
    onClear: () -> Unit,
) {
    SettingsGroup("Privacy", Icons.Outlined.Shield) {
        val network =
            when {
                WEB in prefs.disabledProviders -> "Web search is off, so ekQuickFind makes no network requests."
                prefs.webSuggestions -> "The only network use is web suggestions from ${prefs.searchEngine.label()}, which are on."
                else -> "Web suggestions are off, so ekQuickFind makes no network requests."
            }
        val updateHost = BuildConfig.UPDATE_FEED_URL.toUri().host
        val updatesNote = updateHost?.let { " With automatic updates on, it also checks $it once a day." }.orEmpty()
        Text(
            "Search history, pins, hidden items and settings are stored only on this phone. No analytics, no accounts. $network$updatesNote",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("privacy-summary"),
        )
        SettingsDivider()
        SwitchRow(
            "Include history in backups",
            "Off keeps history out of cloud backups and device transfers",
            prefs.backupHistory,
            { v -> actions.update { it.copy(backupHistory = v) } },
            "backup-history",
        )
        SettingsDivider()
        TextButton(
            onClick = onClear,
            modifier = Modifier.padding(start = 4.dp).testTag("settings-clear-history"),
        ) { Text("Clear search history") }
        if (prefs.hiddenResults.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Hidden from search", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 24.dp))
            prefs.hiddenResults.forEach { (id, title) ->
                ListItem(
                    headlineContent = { Text(title) },
                    trailingContent = {
                        TextButton(
                            onClick = { actions.unhide(id) },
                            modifier = Modifier.testTag("unhide:$title"),
                        ) { Text("Unhide") }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

// --- About ---

private val PROJECT_PAGE = BuildConfig.PROJECT_PAGE

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val version =
        remember(context) {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }
    SettingsGroup("About", Icons.Outlined.Info) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("about")) {
            AboutLine("GitHub", PROJECT_PAGE, "about-github") {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://$PROJECT_PAGE".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            AboutLine("Version", version, "about-version")
            AboutLine("Requires", "Android ${BuildConfig.MIN_ANDROID} or newer", "about-requires")
        }
    }
}

@Composable
private fun ResetSection(onReset: () -> Unit) {
    SettingsGroup("Reset", Icons.Outlined.RestartAlt) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Puts every setting back to how it was when you installed ekQuickFind. History, pins and hidden results stay.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onReset, modifier = Modifier.testTag("reset-settings")) { Text("Reset all settings") }
        }
    }
}

@Composable
private fun ResetSettingsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset all settings?") },
        text = {
            Text(
                "Every setting goes back to its default, and photo tags are turned off. History, pins and hidden results stay.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm-reset")) { Text("Reset") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AboutLine(
    label: String,
    value: String,
    tag: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            // Links read as links.
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(tag),
        )
    }
}
