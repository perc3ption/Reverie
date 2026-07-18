package com.perceptiveus.reverie

import android.app.Application
import android.content.Context
import android.util.Log
import com.perceptiveus.reverie.core.entitlement.EntitlementRepository
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.core.entitlement.MockEntitlementRepository
import com.perceptiveus.reverie.core.settings.RoomSettingsRepository
import com.perceptiveus.reverie.core.settings.SettingsRepository
import com.perceptiveus.reverie.data.local.DatabaseSeeder
import com.perceptiveus.reverie.data.local.ReverieDatabase
import com.perceptiveus.reverie.data.repository.Media3PlaybackRepository
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.data.repository.SongTagRepository
import com.perceptiveus.reverie.data.import.AlbumArtCache
import com.perceptiveus.reverie.data.import.AudioMetadataReader
import com.perceptiveus.reverie.data.import.AudioMetadataWriter
import com.perceptiveus.reverie.data.import.MusicIndexer
import com.perceptiveus.reverie.data.import.MusicImportRepository
import com.perceptiveus.reverie.data.repository.RoomMusicLibraryRepository
import com.perceptiveus.reverie.data.repository.RoomPlaylistRepository
import com.perceptiveus.reverie.data.repository.RoomSongTagRepository
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    val entitlementRepository: EntitlementRepository = MockEntitlementRepository()

    val featureAccessChecker: FeatureAccessChecker = FeatureAccessChecker {
        entitlementRepository.entitlements.value
    }

    val settingsRepository: SettingsRepository = RoomSettingsRepository(
        userSettingsDao = database.userSettingsDao(),
        scope = appScope,
    )

    private val musicIndexer: MusicIndexer = MusicIndexer(
        storage = musicLibraryStorage,
        folderDao = database.musicFolderDao(),
        trackDao = database.trackDao(),
        playHistoryDao = database.playHistoryDao(),
        metadataReader = AudioMetadataReader(),
        albumArtCache = AlbumArtCache(appContext),
        featureAccessChecker = featureAccessChecker,
    )

    val musicLibraryRepository: MusicLibraryRepository = RoomMusicLibraryRepository(
        folderDao = database.musicFolderDao(),
        trackDao = database.trackDao(),
        musicIndexer = musicIndexer,
        metadataWriter = AudioMetadataWriter(),
        scope = appScope,
    )

    val musicImportRepository: MusicImportRepository = MusicImportRepository(
        context = appContext,
        storage = musicLibraryStorage,
        trackDao = database.trackDao(),
        featureAccessChecker = featureAccessChecker,
        scanLibrary = { musicLibraryRepository.scanLibrary() },
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
    val playbackRepository: PlaybackRepository = Media3PlaybackRepository(
        context = appContext,
        playHistoryDao = database.playHistoryDao(),
        scope = appScope,
    )

    init {
        appScope.launch {
            initializeLibraryStorage()
            DatabaseSeeder.seedSettingsIfNeeded(database.userSettingsDao())
            runLibraryScan()
        }
    }

    private suspend fun runLibraryScan() {
        try {
            val result = musicLibraryRepository.scanLibrary()
            Log.i(
                TAG,
                "Library scan: indexed=${result.tracksIndexed}, removed=${result.tracksRemoved}, " +
                    "folders=${result.foldersIndexed}",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Library scan failed", e)
        }
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
    }
}
