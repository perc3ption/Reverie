package com.perceptiveus.reverie.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.perceptiveus.reverie.AppContainer
import com.perceptiveus.reverie.ReverieViewModelFactory
import com.perceptiveus.reverie.feature.audiofx.AudioFxScreen
import com.perceptiveus.reverie.feature.audiofx.AudioFxViewModel
import com.perceptiveus.reverie.feature.home.HomeScreen
import com.perceptiveus.reverie.feature.home.HomeViewModel
import com.perceptiveus.reverie.feature.importmusic.ImportMusicScreen
import com.perceptiveus.reverie.feature.importmusic.ImportMusicViewModel
import com.perceptiveus.reverie.feature.library.LibraryScreen
import com.perceptiveus.reverie.feature.library.LibraryTab
import com.perceptiveus.reverie.feature.library.LibraryViewModel
import com.perceptiveus.reverie.feature.library.PlaylistDetailScreen
import com.perceptiveus.reverie.feature.library.PlaylistDetailViewModel
import com.perceptiveus.reverie.feature.library.SongDetailScreen
import com.perceptiveus.reverie.feature.library.SongDetailViewModel
import com.perceptiveus.reverie.feature.player.PlayerScreen
import com.perceptiveus.reverie.feature.player.PlayerViewModel
import com.perceptiveus.reverie.feature.premium.PremiumFeaturesScreen
import com.perceptiveus.reverie.feature.search.SearchScreen
import com.perceptiveus.reverie.feature.search.SearchViewModel
import com.perceptiveus.reverie.feature.settings.SettingsScreen
import com.perceptiveus.reverie.feature.settings.SettingsViewModel
import com.perceptiveus.reverie.feature.stats.LibraryStatsScreen
import com.perceptiveus.reverie.feature.stats.LibraryStatsViewModel
import com.perceptiveus.reverie.feature.tutorial.TutorialChapterScreen
import com.perceptiveus.reverie.feature.tutorial.TutorialHubScreen
import com.perceptiveus.reverie.feature.tutorial.TutorialTryIt
import com.perceptiveus.reverie.feature.tutorial.TutorialViewModel
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistDetailScreen
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistDetailViewModel
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistEditorScreen
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistEditorViewModel
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistListScreen
import com.perceptiveus.reverie.feature.smartplaylist.SmartPlaylistListViewModel
import kotlinx.coroutines.launch

private const val LIBRARY_TAB_KEY = "library_tab"

@Composable
fun ReverieNavGraph(
    navController: NavHostController,
    container: AppContainer,
    factory: ReverieViewModelFactory,
    modifier: Modifier = Modifier,
    startDestination: String = ReverieDestination.Home.route,
) {
    val scope = rememberCoroutineScope()

    fun navigateToLibrary(tab: LibraryTab) {
        navController.navigate(ReverieDestination.Library.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        // Set after navigate so restoreState cannot overwrite the requested tab.
        navController.getBackStackEntry(ReverieDestination.Library.route)
            .savedStateHandle[LIBRARY_TAB_KEY] = tab.name
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ReverieDestination.Home.route) {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = viewModel,
                onNavigateToImport = {
                    navController.navigate(ReverieDestination.ImportMusic.route)
                },
                onNavigateToLibrary = { navigateToLibrary(LibraryTab.FOLDERS) },
                onNavigateToLibraryPlaylists = { navigateToLibrary(LibraryTab.PLAYLISTS) },
                onNavigateToPlayer = {
                    navController.navigate(ReverieDestination.Player.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
                onNavigateToSearch = {
                    navController.navigate(ReverieDestination.Search.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSongDetails = { track ->
                    navController.navigate(ReverieDestination.SongDetail.createRoute(track.id))
                },
                onNavigateToStats = {
                    navController.navigate(ReverieDestination.LibraryStats.route)
                },
                onNavigateToSmartPlaylists = {
                    navController.navigate(ReverieDestination.SmartPlaylists.route)
                },
                onNavigateToAudioFx = {
                    navController.navigate(ReverieDestination.AudioFx.route)
                },
                onNavigateToTutorial = {
                    navController.navigate(ReverieDestination.Tutorial.route)
                },
            )
        }

        composable(ReverieDestination.Library.route) { entry ->
            val viewModel: LibraryViewModel = viewModel(factory = factory)
            val requestedTabName by entry.savedStateHandle
                .getStateFlow<String?>(LIBRARY_TAB_KEY, null)
                .collectAsState()
            LibraryScreen(
                viewModel = viewModel,
                requestedTab = requestedTabName?.let { name ->
                    runCatching { LibraryTab.valueOf(name) }.getOrNull()
                },
                onRequestedTabConsumed = {
                    entry.savedStateHandle.remove<String>(LIBRARY_TAB_KEY)
                },
                onPremiumFeatureClick = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
                onSongDetailsClick = { track ->
                    navController.navigate(ReverieDestination.SongDetail.createRoute(track.id))
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(ReverieDestination.PlaylistDetail.createRoute(playlist.id))
                },
                onSmartPlaylistClick = { smartPlaylistId ->
                    navController.navigate(ReverieDestination.SmartPlaylistDetail.createRoute(smartPlaylistId))
                },
                onNavigateToPlayer = {
                    navController.navigate(ReverieDestination.Player.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSearch = {
                    navController.navigate(ReverieDestination.Search.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToStats = {
                    navController.navigate(ReverieDestination.LibraryStats.route)
                },
                onNavigateToSmartPlaylists = {
                    navController.navigate(ReverieDestination.SmartPlaylists.route)
                },
                onNavigateToImport = {
                    navController.navigate(ReverieDestination.ImportMusic.route)
                },
                onNavigateToAudioFx = {
                    navController.navigate(ReverieDestination.AudioFx.route)
                },
            )
        }

        composable(ReverieDestination.LibraryStats.route) {
            val viewModel: LibraryStatsViewModel = viewModel(factory = factory)
            LibraryStatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
            )
        }

        composable(ReverieDestination.AudioFx.route) {
            val viewModel: AudioFxViewModel = viewModel(factory = factory)
            AudioFxScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
            )
        }

        composable(ReverieDestination.Tutorial.route) {
            val viewModel: TutorialViewModel = viewModel(factory = factory)
            TutorialHubScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onOpenChapter = { chapter ->
                    navController.navigate(ReverieDestination.TutorialChapter.createRoute(chapter.id))
                },
                onStartImport = {
                    navController.navigate(ReverieDestination.ImportMusic.route)
                },
            )
        }

        composable(
            route = ReverieDestination.TutorialChapter.route,
            arguments = listOf(
                navArgument(ReverieDestination.TUTORIAL_CHAPTER_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { entry ->
            val chapterId = entry.arguments
                ?.getString(ReverieDestination.TUTORIAL_CHAPTER_ID_ARG)
                .orEmpty()
            val viewModel: TutorialViewModel = viewModel(factory = factory)
            val progress by viewModel.progress.collectAsState()
            val chapter = viewModel.chapter(chapterId)
            if (chapter == null) {
                LaunchedEffect(chapterId) { navController.navigateUp() }
            } else {
                TutorialChapterScreen(
                    chapter = chapter,
                    completed = progress.isCompleted(chapter.id),
                    onNavigateBack = { navController.navigateUp() },
                    onMarkComplete = { viewModel.markChapterCompleted(chapter.id) },
                    onTryIt = { action ->
                        when (action) {
                            TutorialTryIt.IMPORT ->
                                navController.navigate(ReverieDestination.ImportMusic.route)
                            TutorialTryIt.LIBRARY ->
                                navController.navigate(ReverieDestination.Library.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            TutorialTryIt.PLAYER ->
                                navController.navigate(ReverieDestination.Player.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            TutorialTryIt.SEARCH ->
                                navController.navigate(ReverieDestination.Search.route)
                            TutorialTryIt.AUDIO_FX ->
                                navController.navigate(ReverieDestination.AudioFx.route)
                            TutorialTryIt.SMART_PLAYLISTS ->
                                navController.navigate(ReverieDestination.SmartPlaylists.route)
                            TutorialTryIt.STATS ->
                                navController.navigate(ReverieDestination.LibraryStats.route)
                            TutorialTryIt.SETTINGS ->
                                navController.navigate(ReverieDestination.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            TutorialTryIt.NONE -> Unit
                        }
                    },
                )
            }
        }

        composable(ReverieDestination.SmartPlaylists.route) {
            val viewModel: SmartPlaylistListViewModel = viewModel(factory = factory)
            SmartPlaylistListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onCreateClick = {
                    navController.navigate(ReverieDestination.SmartPlaylistEditor.createRoute())
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(ReverieDestination.SmartPlaylistDetail.createRoute(playlist.id))
                },
                onNavigateToPlayer = {
                    navController.navigate(ReverieDestination.Player.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(
            route = ReverieDestination.SmartPlaylistDetail.route,
            arguments = listOf(
                navArgument(ReverieDestination.SMART_PLAYLIST_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val smartPlaylistId = backStackEntry.arguments
                ?.getString(ReverieDestination.SMART_PLAYLIST_ID_ARG)
                .orEmpty()
            val viewModel: SmartPlaylistDetailViewModel = viewModel(
                key = smartPlaylistId,
                factory = SmartPlaylistDetailViewModel.factory(
                    playlistId = smartPlaylistId,
                    smartPlaylistRepository = container.smartPlaylistRepository,
                    playbackRepository = container.playbackRepository,
                ),
            )
            SmartPlaylistDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onEditClick = {
                    navController.navigate(
                        ReverieDestination.SmartPlaylistEditor.createRoute(smartPlaylistId),
                    )
                },
                onNavigateToPlayer = {
                    navController.navigate(ReverieDestination.Player.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSongDetailsClick = { track ->
                    navController.navigate(ReverieDestination.SongDetail.createRoute(track.id))
                },
            )
        }

        composable(
            route = ReverieDestination.SmartPlaylistEditor.route,
            arguments = listOf(
                navArgument(ReverieDestination.SMART_PLAYLIST_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments
                ?.getString(ReverieDestination.SMART_PLAYLIST_ID_ARG)
                .orEmpty()
            val playlistId = rawId.takeUnless { it == ReverieDestination.SmartPlaylistEditor.NEW_ID }
            val viewModel: SmartPlaylistEditorViewModel = viewModel(
                key = rawId,
                factory = SmartPlaylistEditorViewModel.factory(
                    playlistId = playlistId,
                    smartPlaylistRepository = container.smartPlaylistRepository,
                    songTagRepository = container.songTagRepository,
                ),
            )
            SmartPlaylistEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onSaved = { savedId ->
                    navController.navigate(ReverieDestination.SmartPlaylistDetail.createRoute(savedId)) {
                        popUpTo(ReverieDestination.SmartPlaylists.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(ReverieDestination.Search.route) {
            val viewModel: SearchViewModel = viewModel(factory = factory)
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onSongDetailsClick = { track ->
                    navController.navigate(ReverieDestination.SongDetail.createRoute(track.id))
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(ReverieDestination.PlaylistDetail.createRoute(playlist.id))
                },
                onNavigateToPlayer = {
                    navController.navigate(ReverieDestination.Player.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(
            route = ReverieDestination.SongDetail.route,
            arguments = listOf(
                navArgument(ReverieDestination.SONG_TRACK_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val trackId = backStackEntry.arguments
                ?.getString(ReverieDestination.SONG_TRACK_ID_ARG)
                .orEmpty()
            val viewModel: SongDetailViewModel = viewModel(
                key = trackId,
                factory = SongDetailViewModel.factory(
                    application = container.application,
                    trackId = trackId,
                    musicLibraryRepository = container.musicLibraryRepository,
                    playlistRepository = container.playlistRepository,
                    songTagRepository = container.songTagRepository,
                    playbackRepository = container.playbackRepository,
                    featureAccessChecker = container.featureAccessChecker,
                ),
            )
            SongDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
                onNavigateToPlaylist = { playlist ->
                    navController.navigate(ReverieDestination.PlaylistDetail.createRoute(playlist.id))
                },
            )
        }

        composable(
            route = ReverieDestination.PlaylistDetail.route,
            arguments = listOf(
                navArgument(ReverieDestination.PLAYLIST_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments
                ?.getString(ReverieDestination.PLAYLIST_ID_ARG)
                .orEmpty()
            val viewModel: PlaylistDetailViewModel = viewModel(
                key = playlistId,
                factory = PlaylistDetailViewModel.factory(
                    application = container.application,
                    playlistId = playlistId,
                    playlistRepository = container.playlistRepository,
                    musicLibraryRepository = container.musicLibraryRepository,
                    playbackRepository = container.playbackRepository,
                ),
            )
            PlaylistDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onSongClick = { track ->
                    navController.navigate(ReverieDestination.SongDetail.createRoute(track.id))
                },
                onNavigateToPlayer = {
                    navController.navigate(ReverieDestination.Player.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        composable(ReverieDestination.Player.route) {
            val viewModel: PlayerViewModel = viewModel(factory = factory)
            PlayerScreen(
                viewModel = viewModel,
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
                onNavigateToSongDetails = { track ->
                    navController.navigate(ReverieDestination.SongDetail.createRoute(track.id))
                },
            )
        }

        composable(ReverieDestination.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
            )
        }

        composable(ReverieDestination.ImportMusic.route) {
            val viewModel: ImportMusicViewModel = viewModel(factory = factory)
            ImportMusicScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
            )
        }

        composable(ReverieDestination.PremiumFeatures.route) {
            PremiumFeaturesScreen(
                featureAccessChecker = container.featureAccessChecker,
                onNavigateBack = { navController.navigateUp() },
                onTogglePremiumForTesting = {
                    scope.launch {
                        val current = container.entitlementRepository.entitlements.value.isPremium
                        container.entitlementRepository.setPremiumForTesting(!current)
                    }
                },
            )
        }
    }
}
