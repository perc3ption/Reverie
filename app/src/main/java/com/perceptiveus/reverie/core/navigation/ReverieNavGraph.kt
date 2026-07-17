package com.perceptiveus.reverie.core.navigation

import androidx.compose.runtime.Composable
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
import com.perceptiveus.reverie.feature.home.HomeScreen
import com.perceptiveus.reverie.feature.home.HomeViewModel
import com.perceptiveus.reverie.feature.importmusic.ImportMusicScreen
import com.perceptiveus.reverie.feature.importmusic.ImportMusicViewModel
import com.perceptiveus.reverie.feature.library.LibraryScreen
import com.perceptiveus.reverie.feature.library.LibraryViewModel
import com.perceptiveus.reverie.feature.library.PlaylistDetailScreen
import com.perceptiveus.reverie.feature.library.PlaylistDetailViewModel
import com.perceptiveus.reverie.feature.library.SongDetailScreen
import com.perceptiveus.reverie.feature.library.SongDetailViewModel
import com.perceptiveus.reverie.feature.player.PlayerScreen
import com.perceptiveus.reverie.feature.player.PlayerViewModel
import com.perceptiveus.reverie.feature.premium.PremiumFeaturesScreen
import com.perceptiveus.reverie.feature.settings.SettingsScreen
import com.perceptiveus.reverie.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun ReverieNavGraph(
    navController: NavHostController,
    container: AppContainer,
    factory: ReverieViewModelFactory,
    modifier: Modifier = Modifier,
    startDestination: String = ReverieDestination.Home.route,
) {
    val scope = rememberCoroutineScope()

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
                onNavigateToLibrary = {
                    navController.navigate(ReverieDestination.Library.route) {
                        popUpTo(ReverieDestination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
            )
        }

        composable(ReverieDestination.Library.route) {
            val viewModel: LibraryViewModel = viewModel(factory = factory)
            LibraryScreen(
                viewModel = viewModel,
                onPremiumFeatureClick = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
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
                onNavigateBack = { navController.popBackStack() },
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
                onNavigateBack = { navController.popBackStack() },
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
            SettingsScreen(viewModel = viewModel)
        }

        composable(ReverieDestination.ImportMusic.route) {
            val viewModel: ImportMusicViewModel = viewModel(factory = factory)
            ImportMusicScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ReverieDestination.PremiumFeatures.route) {
            PremiumFeaturesScreen(
                featureAccessChecker = container.featureAccessChecker,
                onNavigateBack = { navController.popBackStack() },
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
