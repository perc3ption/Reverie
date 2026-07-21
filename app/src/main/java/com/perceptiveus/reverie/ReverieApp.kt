package com.perceptiveus.reverie

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.perceptiveus.reverie.core.design.ReverieTheme
import com.perceptiveus.reverie.core.design.components.MiniPlayerBar
import com.perceptiveus.reverie.core.navigation.EdgeSwipeBackHost
import com.perceptiveus.reverie.core.navigation.ReverieDestination
import com.perceptiveus.reverie.core.navigation.ReverieNavGraph
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReverieApp(container: AppContainer) {
    val themePreference by container.settingsRepository.themePreference.collectAsState()

    LaunchedEffect(Unit) {
        container.startDeferredLibraryScan()
    }

    ReverieTheme(themePreference = themePreference) {
        val navController = rememberNavController()
        val factory = remember { ReverieViewModelFactory(container) }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val playbackSnackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(container.playbackRepository) {
            container.playbackRepository.userMessages.collectLatest { message ->
                playbackSnackbarHostState.showSnackbar(message)
            }
        }

        val showBottomBar = currentRoute in ReverieDestination.bottomBarVisibleRoutes
        val miniPlayerAllowed = showBottomBar &&
            currentRoute != ReverieDestination.Player.route &&
            currentRoute != ReverieDestination.SongDetail.route &&
            currentRoute != ReverieDestination.PlaylistDetail.route &&
            currentRoute != ReverieDestination.LibraryStats.route &&
            currentRoute != ReverieDestination.AudioFx.route &&
            currentRoute != ReverieDestination.Tutorial.route &&
            currentRoute != ReverieDestination.TutorialChapter.route &&
            currentRoute != ReverieDestination.SmartPlaylists.route &&
            currentRoute != ReverieDestination.SmartPlaylistDetail.route &&
            currentRoute != ReverieDestination.SmartPlaylistEditor.route

        // Keep a tab highlighted on detail screens so the bottom bar never sits in an
        // "nothing selected" state (which can mis-fire navigation back to Home).
        val selectedTabRoute = when (currentRoute) {
            ReverieDestination.SongDetail.route,
            ReverieDestination.PlaylistDetail.route,
            ReverieDestination.LibraryStats.route,
            ReverieDestination.AudioFx.route,
            ReverieDestination.Tutorial.route,
            ReverieDestination.TutorialChapter.route,
            ReverieDestination.SmartPlaylists.route,
            ReverieDestination.SmartPlaylistDetail.route,
            ReverieDestination.SmartPlaylistEditor.route,
            -> {
                val previous = navController.previousBackStackEntry?.destination?.route
                when (previous) {
                    ReverieDestination.Player.route -> ReverieDestination.Player.route
                    ReverieDestination.Library.route -> ReverieDestination.Library.route
                    ReverieDestination.Home.route -> ReverieDestination.Home.route
                    else -> ReverieDestination.Library.route
                }
            }
            else -> currentRoute
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(playbackSnackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        if (miniPlayerAllowed) {
                            ReverieMiniPlayer(
                                playbackRepository = container.playbackRepository,
                                onClick = {
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
                        ReverieBottomBar(
                            selectedRoute = selectedTabRoute,
                            onNavigate = { destination ->
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            EdgeSwipeBackHost(modifier = Modifier.padding(innerPadding)) {
                ReverieNavGraph(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    container = container,
                    factory = factory,
                    startDestination = ReverieDestination.Home.route,
                )
            }
        }
    }
}

/**
 * Collects track + progress here so position ticks do not recompose the whole app scaffold.
 */
@Composable
private fun ReverieMiniPlayer(
    playbackRepository: PlaybackRepository,
    onClick: () -> Unit,
) {
    val track by remember(playbackRepository) {
        playbackRepository.playbackState
            .map { it.currentTrack }
            .distinctUntilChanged(::sameMiniPlayerTrack)
    }.collectAsState(initial = playbackRepository.playbackState.value.currentTrack)
    val progress by playbackRepository.playerProgress.collectAsState()

    val current = track ?: return
    MiniPlayerBar(
        track = current,
        isPlaying = progress.isPlaying,
        onPlayPause = playbackRepository::togglePlayPause,
        onNext = playbackRepository::skipToNext,
        onPrevious = playbackRepository::skipToPrevious,
        onClick = onClick,
    )
}

private fun sameMiniPlayerTrack(old: Track?, new: Track?): Boolean =
    old?.id == new?.id &&
        old?.title == new?.title &&
        old?.artist == new?.artist &&
        old?.artworkPath == new?.artworkPath

@Composable
private fun ReverieBottomBar(
    selectedRoute: String?,
    onNavigate: (ReverieDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ReverieDestination.bottomNavItems.forEach { destination ->
            val selected = selectedRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
