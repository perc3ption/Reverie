package com.perceptiveus.reverie

import android.app.Application
import android.content.Context
import android.util.Log
import com.perceptiveus.reverie.core.entitlement.EntitlementRepository
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.core.entitlement.PlayBillingEntitlementRepository
import com.perceptiveus.reverie.core.settings.RoomSettingsRepository
import com.perceptiveus.reverie.core.settings.SettingsRepository
import com.perceptiveus.reverie.data.local.DatabaseSeeder
import com.perceptiveus.reverie.data.local.ReverieDatabase
import com.perceptiveus.reverie.data.repository.LibraryStatsRepository
import com.perceptiveus.reverie.data.repository.Media3PlaybackRepository
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.data.repository.RoomLibraryStatsRepository
import com.perceptiveus.reverie.data.repository.RoomMusicLibraryRepository
import com.perceptiveus.reverie.data.repository.RoomPlaylistRepository
import com.perceptiveus.reverie.data.repository.RoomSongTagRepository
import com.perceptiveus.reverie.data.repository.RoomSmartPlaylistRepository
import com.perceptiveus.reverie.data.repository.SmartPlaylistRepository
import com.perceptiveus.reverie.data.repository.SongTagRepository
import com.perceptiveus.reverie.data.import.AlbumArtCache
import com.perceptiveus.reverie.data.import.AudioMetadataReader
import com.perceptiveus.reverie.data.import.AudioMetadataWriter
import com.perceptiveus.reverie.data.import.MusicIndexer
import com.perceptiveus.reverie.data.import.MusicImportRepository
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple service locator until a DI framework (e.g. Hilt) is added.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val application: Application =
        (context.applicationContext as? Application)
            ?: error("AppContainer requires an Application context")
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: ReverieDatabase = ReverieDatabase.getInstance(appContext)

    /** On-disk library at Android/media/<package>/Reverie/ */
    val musicLibraryStorage: MusicLibraryStorage = MusicLibraryStorage(appContext)

    val entitlementRepository: EntitlementRepository = PlayBillingEntitlementRepository(
        context = appContext,
        scope = appScope,
    )

    val featureAccessChecker: FeatureAccessChecker = FeatureAccessChecker {
        entitlementRepository.entitlements.value
    }

    val settingsRepository: SettingsRepository = RoomSettingsRepository(
        userSettingsDao = database.userSettingsDao(),
        scope = appScope,
    )

    private val albumArtCache: AlbumArtCache = AlbumArtCache(appContext)

    private val musicIndexer: MusicIndexer = MusicIndexer(
        storage = musicLibraryStorage,
        folderDao = database.musicFolderDao(),
        trackDao = database.trackDao(),
        playHistoryDao = database.playHistoryDao(),
        metadataReader = AudioMetadataReader(),
        albumArtCache = albumArtCache,
        featureAccessChecker = featureAccessChecker,
    )

    val musicLibraryRepository: MusicLibraryRepository = RoomMusicLibraryRepository(
        appContext = appContext,
        folderDao = database.musicFolderDao(),
        trackDao = database.trackDao(),
        musicIndexer = musicIndexer,
        metadataWriter = AudioMetadataWriter(),
        albumArtCache = albumArtCache,
        featureAccessChecker = featureAccessChecker,
        storage = musicLibraryStorage,
        scope = appScope,
    )

    val musicImportRepository: MusicImportRepository = MusicImportRepository(
        context = appContext,
        storage = musicLibraryStorage,
        trackDao = database.trackDao(),
        featureAccessChecker = featureAccessChecker,
        scanLibrary = { scanLibrary() },
    )
    val playlistRepository: PlaylistRepository = RoomPlaylistRepository(
        playlistDao = database.playlistDao(),
        trackDao = database.trackDao(),
        featureAccessChecker = featureAccessChecker,
        scope = appScope,
    )
    val songTagRepository: SongTagRepository = RoomSongTagRepository(
        songTagDao = database.songTagDao(),
        featureAccessChecker = featureAccessChecker,
    )
    val libraryStatsRepository: LibraryStatsRepository = RoomLibraryStatsRepository(
        trackDao = database.trackDao(),
        playlistDao = database.playlistDao(),
        playHistoryDao = database.playHistoryDao(),
    )
    val smartPlaylistRepository: SmartPlaylistRepository = RoomSmartPlaylistRepository(
        smartPlaylistDao = database.smartPlaylistDao(),
        trackDao = database.trackDao(),
        playHistoryDao = database.playHistoryDao(),
        songTagDao = database.songTagDao(),
        featureAccessChecker = featureAccessChecker,
    )
    val playbackRepository: PlaybackRepository = Media3PlaybackRepository(
        context = appContext,
        playHistoryDao = database.playHistoryDao(),
        scope = appScope,
        audioFxSettings = settingsRepository.audioFxSettings,
    )

    private val _libraryScanInProgress = MutableStateFlow(false)
    /** True while an import, manual, or first-install library scan is running. */
    val libraryScanInProgress: StateFlow<Boolean> = _libraryScanInProgress.asStateFlow()

    private val libraryPrefs =
        appContext.getSharedPreferences(LIBRARY_PREFS, Context.MODE_PRIVATE)
    private val initialScanStarted = AtomicBoolean(false)

    init {
        appScope.launch {
            settingsRepository.audioFxSettings.collect { settings ->
                com.perceptiveus.reverie.playback.audiofx.AudioFxController.apply(settings)
            }
        }
        appScope.launch {
            initializeLibraryStorage()
            DatabaseSeeder.seedSettingsIfNeeded(database.userSettingsDao())
        }
    }

    /**
     * One-time scan after first install (empty library). Skipped on later launches
     * and for upgrades that already have indexed tracks.
     */
    fun startInitialLibraryScanIfNeeded() {
        if (libraryPrefs.getBoolean(KEY_INITIAL_SCAN_DONE, false)) return
        if (!initialScanStarted.compareAndSet(false, true)) return
        appScope.launch {
            try {
                // Upgrading users already have Room data — don't walk disk again.
                if (database.trackDao().countTracks() > 0) {
                    markInitialScanDone()
                    return@launch
                }
                delay(FIRST_FRAME_SCAN_DELAY_MS)
                scanLibrary()
                markInitialScanDone()
            } catch (e: Exception) {
                Log.e(TAG, "Initial library scan failed", e)
                // Leave the flag unset so the next launch can retry.
                initialScanStarted.set(false)
            }
        }
    }

    /**
     * Walks the on-disk library and syncs Room. Used after import, manual Scan Library,
     * and the one-time first-install scan — not on every app start.
     */
    suspend fun scanLibrary(): LibraryScanResult {
        _libraryScanInProgress.value = true
        try {
            val result = musicLibraryRepository.scanLibrary()
            Log.i(
                TAG,
                "Library scan: indexed=${result.tracksIndexed}, unchanged=${result.tracksUnchanged}, " +
                    "removed=${result.tracksRemoved}, folders=${result.foldersIndexed}",
            )
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Library scan failed", e)
            throw e
        } finally {
            _libraryScanInProgress.value = false
        }
    }

    private fun markInitialScanDone() {
        libraryPrefs.edit().putBoolean(KEY_INITIAL_SCAN_DONE, true).apply()
    }

    private fun initializeLibraryStorage() {
        try {
            val root = musicLibraryStorage.initialize()
            Log.i(TAG, "Music library ready at ${root.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize music library storage", e)
        }
    }

    companion object {
        private const val TAG = "AppContainer"
        private const val LIBRARY_PREFS = "reverie_library"
        private const val KEY_INITIAL_SCAN_DONE = "initial_library_scan_done"
        /** Let the first Compose frame paint before the first-install walk. */
        private const val FIRST_FRAME_SCAN_DELAY_MS = 400L
    }
}
