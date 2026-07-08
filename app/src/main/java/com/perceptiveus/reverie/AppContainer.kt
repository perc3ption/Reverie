package com.perceptiveus.reverie

import android.content.Context
import com.perceptiveus.reverie.core.entitlement.EntitlementRepository
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.core.entitlement.MockEntitlementRepository
import com.perceptiveus.reverie.core.settings.RoomSettingsRepository
import com.perceptiveus.reverie.core.settings.SettingsRepository
import com.perceptiveus.reverie.data.local.DatabaseSeeder
import com.perceptiveus.reverie.data.local.ReverieDatabase
import com.perceptiveus.reverie.data.repository.FakePlaybackRepository
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.data.repository.RoomMusicLibraryRepository
import com.perceptiveus.reverie.data.repository.RoomPlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple service locator until a DI framework (e.g. Hilt) is added.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: ReverieDatabase = ReverieDatabase.getInstance(context)

    val entitlementRepository: EntitlementRepository = MockEntitlementRepository()

    val featureAccessChecker: FeatureAccessChecker = FeatureAccessChecker {
        entitlementRepository.entitlements.value
    }

    val settingsRepository: SettingsRepository = RoomSettingsRepository(
        userSettingsDao = database.userSettingsDao(),
        scope = appScope,
    )
    val musicLibraryRepository: MusicLibraryRepository = RoomMusicLibraryRepository(
        folderDao = database.musicFolderDao(),
        trackDao = database.trackDao(),
        scope = appScope,
    )
    val playlistRepository: PlaylistRepository = RoomPlaylistRepository(
        playlistDao = database.playlistDao(),
        trackDao = database.trackDao(),
        featureAccessChecker = featureAccessChecker,
        scope = appScope,
    )
    val playbackRepository: PlaybackRepository = FakePlaybackRepository()

    init {
        appScope.launch {
            DatabaseSeeder.seedIfEmpty(
                folderDao = database.musicFolderDao(),
                trackDao = database.trackDao(),
                playlistDao = database.playlistDao(),
                playHistoryDao = database.playHistoryDao(),
                userSettingsDao = database.userSettingsDao(),
            )
        }
    }
}
