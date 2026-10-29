package com.ekquickfind.data.prefs

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
enum class SearchEngineChoice { DuckDuckGo, Google, Bing, Brave, Custom }

@Serializable
enum class ThemeMode { System, Light, Dark }

@Serializable
enum class BackgroundStyle { Blur, FrostedGlass, Solid }

@Serializable
enum class IconSize { Small, Medium, Large }

@Serializable
enum class Density { Compact, Comfortable }

@Serializable
enum class SurfacePosition { Bottom, Top }

/** Accent for highlights, the selected result, links and switches. System follows the wallpaper (or the default theme). */
@Serializable
enum class AccentColor(
    val argb: Long?,
) {
    System(null),
    Cyan(0xFF14AFCE),
    Blue(0xFF2F6BFF),
    Teal(0xFF00A38C),
    Green(0xFF34A853),
    Amber(0xFFFFB300),
    Orange(0xFFFF6D00),
    Red(0xFFE53935),
    Pink(0xFFE91E78),
    Purple(0xFF8E44D9),
}

/** AI chat apps offered under "Ask AI", with the package that handles them. */
@Serializable
enum class AiChat(
    val label: String,
    val packageName: String,
) {
    ChatGpt("ChatGPT", "com.openai.chatgpt"),
    Claude("Claude", "com.anthropic.claude"),
    Gemini("Gemini", "com.google.android.apps.bard"),
}

/** Where the recents strip sits: above or below the results panel. */
@Serializable
enum class StripPosition { Top, Bottom }

@Serializable
enum class SectionLayout { Rows, Grid, Chips }

/** Result sections plus the two empty-state lists, as stored in preferences. */
@Serializable
enum class SectionKey {
    QuickAnswers,
    Apps,
    Contacts,
    Settings,
    Files,
    Web,
    AskAi,
    Recent,

    // Recent searches were removed; kept so layouts saved by older versions still decode.
    RecentSearches,
    ;

    companion object {
        /** Order of result sections; Recent lists aren't part of it. */
        val defaultOrder = listOf(QuickAnswers, Apps, Contacts, Settings, Files, Web, AskAi)
        val defaultLayouts = mapOf(Apps to SectionLayout.Grid, Recent to SectionLayout.Chips, AskAi to SectionLayout.Chips)
    }
}

@Serializable
data class UserPreferences(
    val disabledProviders: Set<String> = emptySet(),
    val searchEngine: SearchEngineChoice = SearchEngineChoice.DuckDuckGo,
    /** URL with %s where the query goes; used when [searchEngine] is Custom. */
    val customTemplate: String? = null,
    /** Optional OpenSearch suggestion URL with %s; a custom engine shows no suggestions without it. */
    val customSuggestTemplate: String? = null,
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val accent: AccentColor = AccentColor.System,
    /** Hidden result id -> title, so settings can list what to unhide. */
    val hiddenResults: Map<String, String> = emptyMap(),
    val webSuggestions: Boolean = true,
    /** History stays out of cloud backups unless this is on. */
    val backupHistory: Boolean = false,
    // Personalisation
    val backgroundStyle: BackgroundStyle = BackgroundStyle.FrostedGlass,
    val blurRadiusDp: Int = 29,
    /** How dark the area behind the surface gets, 0..0.9, for every background style. */
    val dimAmount: Float = 0.45f,
    /** ARGB used by the Solid background style. */
    val solidColor: Long = 0xFF101418,
    val iconSize: IconSize = IconSize.Medium,
    val density: Density = Density.Comfortable,
    val resultsPerSection: Int = 4,
    val position: SurfacePosition = SurfacePosition.Bottom,
    val autoOpenKeyboard: Boolean = true,
    val enterRunsSelected: Boolean = true,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    /** Small recent-result chips next to the search bar, visible while typing too. */
    val recentsStrip: Boolean = false,
    val stripPosition: StripPosition = StripPosition.Top,
    /** Daily update check on Wi-Fi that installs new versions (only when the build has an update source). */
    val autoUpdate: Boolean = true,
    /** Tag photos on the phone and search those tags; off by default because tagging takes battery once. */
    val photoTags: Boolean = false,
    /** AI chats offered under "Ask AI"; empty hides the section. */
    val aiChats: Set<AiChat> = AiChat.entries.toSet(),
    /** Round button over other apps that opens search (needs "Display over other apps"). */
    val floatingButton: Boolean = false,
    val floatingButtonSizeDp: Int = 52,
    /** 0.1..1; lower lets more of the app underneath show through. */
    val floatingButtonOpacity: Float = 0.85f,
    /** Which edge it rests on and how far down (0 top .. 1 bottom); it starts on the right. */
    val floatingButtonOnRight: Boolean = true,
    val floatingButtonY: Float = 0.4f,
    /** Show the selected result in a fixed slot just above the search bar instead of first in the list. */
    val selectedAtBottom: Boolean = false,
    /** Set by the Quick Settings tile itself when the user adds or removes it. */
    val tileAdded: Boolean = false,
    val sectionOrder: List<SectionKey> = SectionKey.defaultOrder,
    /** Overrides only; sections not listed use [SectionKey.defaultLayouts] or Rows. */
    val sectionLayouts: Map<SectionKey, SectionLayout> = emptyMap(),
) {
    fun layoutFor(section: SectionKey): SectionLayout = sectionLayouts[section] ?: SectionKey.defaultLayouts[section] ?: SectionLayout.Rows

    fun sanitized() =
        copy(
            blurRadiusDp = blurRadiusDp.coerceIn(0, 60),
            dimAmount = dimAmount.coerceIn(0f, 0.9f),
            resultsPerSection = resultsPerSection.coerceIn(3, 8),
            floatingButtonSizeDp = floatingButtonSizeDp.coerceIn(36, 80),
            floatingButtonOpacity = floatingButtonOpacity.coerceIn(0.1f, 1f),
            floatingButtonY = floatingButtonY.coerceIn(0f, 1f),
            sectionOrder = (sectionOrder.filter { it in SectionKey.defaultOrder } + SectionKey.defaultOrder).distinct(),
        )
}

object UserPreferencesSerializer : Serializer<UserPreferences> {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override val defaultValue = UserPreferences()

    fun decode(text: String): UserPreferences = json.decodeFromString(UserPreferences.serializer(), text).sanitized()

    override suspend fun readFrom(input: InputStream): UserPreferences =
        try {
            decode(input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Unreadable preferences", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Unreadable preferences", e)
        }

    override suspend fun writeTo(
        t: UserPreferences,
        output: OutputStream,
    ) = output.write(json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray())
}

class PreferencesRepository(
    private val store: DataStore<UserPreferences>,
) {
    val preferences: Flow<UserPreferences> = store.data.catch { emit(UserPreferences()) }

    suspend fun setProviderEnabled(
        providerId: String,
        enabled: Boolean,
    ) = update { it.copy(disabledProviders = if (enabled) it.disabledProviders - providerId else it.disabledProviders + providerId) }

    suspend fun setSearchEngine(
        choice: SearchEngineChoice,
        customTemplate: String? = null,
        customSuggestTemplate: String? = null,
    ) = update {
        it.copy(
            searchEngine = choice,
            customTemplate = customTemplate ?: it.customTemplate,
            customSuggestTemplate = customSuggestTemplate ?: it.customSuggestTemplate,
        )
    }

    suspend fun setTheme(mode: ThemeMode) = update { it.copy(theme = mode) }

    suspend fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    suspend fun hide(
        resultId: String,
        title: String,
    ) = update { it.copy(hiddenResults = it.hiddenResults + (resultId to title)) }

    suspend fun unhide(resultId: String) = update { it.copy(hiddenResults = it.hiddenResults - resultId) }

    suspend fun setWebSuggestions(enabled: Boolean) = update { it.copy(webSuggestions = enabled) }

    suspend fun setBackupHistory(enabled: Boolean) = update { it.copy(backupHistory = enabled) }

    suspend fun moveSection(
        section: SectionKey,
        up: Boolean,
    ) = update { p ->
        val order = p.sectionOrder.toMutableList()
        val i = order.indexOf(section)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in order.indices) return@update p
        order[i] = order[j].also { order[j] = section }
        p.copy(sectionOrder = order)
    }

    suspend fun resetSectionOrder() = update { it.copy(sectionOrder = SectionKey.defaultOrder) }

    suspend fun setLayout(
        section: SectionKey,
        layout: SectionLayout,
    ) = update { it.copy(sectionLayouts = it.sectionLayouts + (section to layout)) }

    /** For simple field edits from the settings screen. */
    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.updateData { transform(it).sanitized() }
    }
}

/** Process-wide store; DataStore must have exactly one instance per file. */
val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore("preferences.json", UserPreferencesSerializer)
