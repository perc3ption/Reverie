package com.perceptiveus.reverie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.perceptiveus.reverie.feature.audiofx.AudioFxViewModel
import com.perceptiveus.reverie.feature.home.HomeViewModel
import com.perceptiveus.reverie.feature.importmusic.ImportMusicViewModel
import com.perceptiveus.reverie.feature.library.LibraryViewModel
import com.perceptiveus.reverie.feature.player.PlayerViewModel
import com.perceptiveus.reverie.feature.search.SearchViewModel
import com.perceptiveus.reverie.feature.settings.SettingsViewModel
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistListViewModel
import com.perceptiveus.reverie.feature.stats.LibraryStatsViewModel
import com.perceptiveus.reverie.feature.tutorial.TutorialViewModel

class ReverieViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                container.musicLibraryRepository,
                container.playbackRepository,
                container.playlistRepository,
                container.featureAccessChecker,
                container.settingsRepository,
            )
            modelClass.isAssignableFrom(ImportMusicViewModel::class.java) -> ImportMusicViewModel(
                container.musicLibraryRepository,
                container.musicImportRepository,
                container.musicLibraryStorage,
                container.featureAccessChecker,
            )
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(
                container.musicLibraryRepository,
                container.playlistRepository,
                container.playbackRepository,
                container.featureAccessChecker,
            )
            modelClass.isAssignableFrom(LibraryStatsViewModel::class.java) -> LibraryStatsViewModel(
                container.libraryStatsRepository,
                container.featureAccessChecker,
            )
            modelClass.isAssignableFrom(AudioFxViewModel::class.java) -> AudioFxViewModel(
                container.settingsRepository,
                container.featureAccessChecker,
            )
            modelClass.isAssignableFrom(TutorialViewModel::class.java) -> TutorialViewModel(
                container.settingsRepository,
                container.musicLibraryRepository,
            )
            modelClass.isAssignableFrom(SmartPlaylistListViewModel::class.java) -> SmartPlaylistListViewModel(
                container.smartPlaylistRepository,
                container.featureAccessChecker,
            )
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(
                container.application,
                container.playbackRepository,
                container.musicLibraryRepository,
                container.featureAccessChecker,
            )
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(
                container.musicLibraryRepository,
                container.playlistRepository,
                container.playbackRepository,
            )
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                container.settingsRepository,
                container.entitlementRepository,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
