package com.ekquickfind.app.di

import android.content.Context
import com.ekquickfind.app.BuildConfig
import com.ekquickfind.app.search.AudioOutputs
import com.ekquickfind.app.search.LaunchContext
import com.ekquickfind.app.search.PastLaunch
import com.ekquickfind.app.search.SmartSuggestions
import com.ekquickfind.app.search.UsageSuggestions
import com.ekquickfind.app.search.ZeroQuerySource
import com.ekquickfind.app.search.engineConfigFor
import com.ekquickfind.app.update.UpdateChecker
import com.ekquickfind.app.update.UpdateDownloader
import com.ekquickfind.app.update.UpdateInstaller
import com.ekquickfind.app.update.Updates
import com.ekquickfind.core.search.HiddenResults
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchEngine
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SignalsSource
import com.ekquickfind.core.search.quick.QuickAnswersProvider
import com.ekquickfind.data.EkDatabase
import com.ekquickfind.data.clips.ClipRepository
import com.ekquickfind.data.history.HistoryRepository
import com.ekquickfind.data.photos.PhotoTagStore
import com.ekquickfind.data.photos.RoomPhotoTagStore
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.data.prefs.userPreferencesStore
import com.ekquickfind.providers.HandlerResolver
import com.ekquickfind.providers.PermissionChecker
import com.ekquickfind.providers.ai.AiChatProvider
import com.ekquickfind.providers.apps.AppIndex
import com.ekquickfind.providers.apps.AppsProvider
import com.ekquickfind.providers.apps.AppsSource
import com.ekquickfind.providers.apps.LauncherAppsSource
import com.ekquickfind.providers.apps.RoomAppLabelCache
import com.ekquickfind.providers.calendar.CalendarProvider
import com.ekquickfind.providers.calendar.ContentResolverCalendarSource
import com.ekquickfind.providers.contacts.ContactsProvider
import com.ekquickfind.providers.contacts.ContentResolverContactsSource
import com.ekquickfind.providers.files.FileAccess
import com.ekquickfind.providers.files.FilesMediaProvider
import com.ekquickfind.providers.files.MediaStoreSource
import com.ekquickfind.providers.files.PhotoTagSearch
import com.ekquickfind.providers.photos.MediaStorePhotoLibrary
import com.ekquickfind.providers.photos.MlKitPhotoLabeler
import com.ekquickfind.providers.photos.PhotoLibrary
import com.ekquickfind.providers.photos.PhotoTagIndexer
import com.ekquickfind.providers.settings.AndroidToggleStates
import com.ekquickfind.providers.settings.DeviceSettingsProvider
import com.ekquickfind.providers.settings.IntentResolver
import com.ekquickfind.providers.settings.SettingsTable
import com.ekquickfind.providers.web.NetworkPolicy
import com.ekquickfind.providers.web.WebEngine
import com.ekquickfind.providers.web.WebFallback
import com.ekquickfind.providers.web.WebProvider
import com.ekquickfind.providers.web.WebSettingsSnapshot
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun preferences(
        @ApplicationContext context: Context,
    ): PreferencesRepository = PreferencesRepository(context.userPreferencesStore)

    @Provides
    @Singleton
    fun http(): OkHttpClient =
        OkHttpClient
            .Builder()
            .callTimeout(3, TimeUnit.SECONDS)
            .build()

    @Provides
    fun networkPolicy(prefs: PreferencesRepository): NetworkPolicy =
        NetworkPolicy {
            val p = prefs.preferences.first()
            WebSettingsSnapshot(p.webSuggestions, WebEngine.forChoice(p.searchEngine, p.customTemplate, p.customSuggestTemplate))
        }

    @Provides
    @Singleton
    fun handlers(
        @ApplicationContext context: Context,
    ): HandlerResolver = HandlerResolver.of(context)

    @Provides
    fun hiddenResults(prefs: PreferencesRepository): HiddenResults =
        HiddenResults {
            prefs.preferences
                .first()
                .hiddenResults.keys
                .map(::ResultId)
                .toSet()
        }
}

/** Separate so instrumented tests can swap in an in-memory database and never touch real history. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): EkDatabase = EkDatabase.create(context)
}

@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {
    @Provides
    @Singleton
    fun appsSource(
        @ApplicationContext context: Context,
    ): AppsSource = LauncherAppsSource(context)

    @Provides
    @Singleton
    fun appIndex(
        source: AppsSource,
        @AppScope scope: CoroutineScope,
        db: EkDatabase,
    ): AppIndex = AppIndex(source, scope, Dispatchers.IO, RoomAppLabelCache(db.appLabels()))

    @Provides
    @IntoSet
    fun quickAnswers(): SearchProvider = QuickAnswersProvider()

    @Provides
    @IntoSet
    fun deviceSettings(
        @ApplicationContext context: Context,
        handlers: HandlerResolver,
    ): SearchProvider = DeviceSettingsProvider(SettingsTable.entries, IntentResolver.of(context), AndroidToggleStates(context), handlers)

    @Provides
    @IntoSet
    fun web(
        http: OkHttpClient,
        policy: NetworkPolicy,
        handlers: HandlerResolver,
    ): SearchProvider = WebProvider(http, policy, Dispatchers.IO, handlers = handlers)

    @Provides
    @IntoSet
    fun files(
        @ApplicationContext context: Context,
        handlers: HandlerResolver,
        prefs: PreferencesRepository,
        photoTags: PhotoTagStore,
    ): SearchProvider =
        FilesMediaProvider(
            MediaStoreSource(context.contentResolver),
            FileAccess.of(context),
            Dispatchers.IO,
            handlers = handlers,
            photoTags = PhotoTagSearch { q -> if (prefs.preferences.first().photoTags) photoTags.search(q) else emptyList() },
        )

    @Provides
    @IntoSet
    fun askAi(
        @ApplicationContext context: Context,
        prefs: PreferencesRepository,
    ): SearchProvider =
        AiChatProvider(
            enabled = { prefs.preferences.first().aiChats },
            isInstalled = { context.packageManager.getLaunchIntentForPackage(it) != null },
        )

    @Provides
    @Singleton
    fun photoTagStore(db: EkDatabase): PhotoTagStore = RoomPhotoTagStore(db.photoTags())

    @Provides
    fun photoLibrary(
        @ApplicationContext context: Context,
    ): PhotoLibrary = MediaStorePhotoLibrary(context.contentResolver)

    @Provides
    fun photoTagIndexer(
        @ApplicationContext context: Context,
        library: PhotoLibrary,
        store: PhotoTagStore,
    ): PhotoTagIndexer = PhotoTagIndexer(library, MlKitPhotoLabeler(context), store, Dispatchers.IO)

    @Provides
    @IntoSet
    fun contacts(
        @ApplicationContext context: Context,
    ): SearchProvider =
        ContactsProvider(
            ContentResolverContactsSource(context.contentResolver),
            PermissionChecker.of(context),
            Dispatchers.IO,
        )

    @Provides
    @Singleton
    fun calendarProvider(
        @ApplicationContext context: Context,
    ): CalendarProvider =
        CalendarProvider(
            ContentResolverCalendarSource(context.contentResolver),
            PermissionChecker.of(context),
            Dispatchers.IO,
        )

    @Provides
    @IntoSet
    fun calendar(provider: CalendarProvider): SearchProvider = provider

    @Provides
    @Singleton
    fun appsProvider(
        index: AppIndex,
        hidden: HiddenResults,
    ): AppsProvider = AppsProvider(index, hidden)

    @Provides
    @IntoSet
    fun apps(provider: AppsProvider): SearchProvider = provider
}

/** Tiles per grid row on phones. */
const val GRID_COLUMNS = 5

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    /** Remembered clips live in the no-backup folder, so they never reach a cloud backup. */
    @Provides
    @Singleton
    fun clips(
        @ApplicationContext context: Context,
    ): ClipRepository = ClipRepository(File(context.noBackupFilesDir, "clips.json"))

    @Provides
    @Singleton
    fun history(
        db: EkDatabase,
        @ApplicationContext context: Context,
    ): HistoryRepository = HistoryRepository(db.history(), audioDevice = AudioOutputs(context)::current)

    @Provides
    fun signals(history: HistoryRepository): SignalsSource = history

    /** Providers that can rebuild a result from its id (pins, recents, launcher shortcuts). */
    @Provides
    fun resolvers(providers: Set<@JvmSuppressWildcards SearchProvider>): Map<ProviderId, @JvmSuppressWildcards ResultResolver> =
        providers.filterIsInstance<ResultResolver>().associateBy { (it as SearchProvider).id }

    @Provides
    fun zeroQuery(
        history: HistoryRepository,
        resolvers: Map<ProviderId, @JvmSuppressWildcards ResultResolver>,
        apps: AppsProvider,
        calendar: CalendarProvider,
        prefs: PreferencesRepository,
        @ApplicationContext context: Context,
    ): ZeroQuerySource {
        val usage = UsageSuggestions(context)
        val audio = AudioOutputs(context)
        return ZeroQuerySource(
            history = history,
            resolvers = resolvers,
            apps = apps,
            suggestedPackages = { usage.topPackages(it) },
            smartPicks = { limit ->
                if (!prefs.preferences.first().smartSuggestions) {
                    emptyList()
                } else {
                    val now = System.currentTimeMillis()
                    val past =
                        history.launchesSince(now - SMART_HISTORY_MS).map {
                            PastLaunch(it.providerId, it.resultId, it.at, LaunchContext.at(it.at, it.audioDevice))
                        }
                    SmartSuggestions.rank(past, LaunchContext.at(now, audio.current()), now, limit)
                }
            },
            nextUp = {
                val p = prefs.preferences.first()
                if (p.nextUp && CalendarProvider.ID.value !in p.disabledProviders) calendar.nextUp() else null
            },
        )
    }

    private const val SMART_HISTORY_MS = 30L * 24 * 60 * 60 * 1000

    @Provides
    fun engine(
        providers: Set<@JvmSuppressWildcards SearchProvider>,
        prefs: PreferencesRepository,
        signals: SignalsSource,
        policy: NetworkPolicy,
        handlers: HandlerResolver,
    ): SearchEngine {
        val all = providers.map { it.id }.toSet()
        return SearchEngine(
            providers = providers.toList(),
            enabledProviders = prefs.preferences.map { p -> all - p.disabledProviders.map(::ProviderId).toSet() },
            signals = signals,
            fallback = WebFallback(policy, handlers),
            configs = prefs.preferences.map { engineConfigFor(it, GRID_COLUMNS) },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {
    @Provides
    @Singleton
    fun updates(
        @ApplicationContext context: Context,
        http: OkHttpClient,
    ): Updates {
        // The shared client gives up after 3 s (fine for suggestions); an APK needs longer.
        val downloads =
            http
                .newBuilder()
                .callTimeout(0, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        return Updates(
            UpdateChecker(downloads, BuildConfig.UPDATE_FEED_URL, BuildConfig.VERSION_CODE.toLong(), Dispatchers.IO),
            UpdateDownloader(downloads, java.io.File(context.cacheDir, "updates"), Dispatchers.IO),
            UpdateInstaller(context),
        )
    }
}
