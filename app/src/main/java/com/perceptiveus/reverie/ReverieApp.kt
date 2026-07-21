package com.perceptiveus.reverie

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.perceptiveus.reverie.core.design.ReverieTheme
import com.perceptiveus.reverie.core.design.components.MiniPlayerBar
import com.perceptiveus.reverie.core.design.navUnderGlow
import com.perceptiveus.reverie.core.navigation.EdgeSwipeBackHost
import com.perceptiveus.reverie.core.navigation.ReverieDestination
import com.perceptiveus.reverie.core.navigation.ReverieNavGraph
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun ReverieApp(container: AppContainer) {
    val themePreference by container.settingsRepository.themePreference.collectAsState()
    val libraryScanInProgress by container.libraryScanInProgress.collectAsState()
    var showLibraryScanStatus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        container.startDeferredLibraryScan()
    }

    // Skip the status flash when the incremental scan finishes quickly.
    LaunchedEffect(libraryScanInProgress) {
        if (!libraryScanInProgress) {
            showLibraryScanStatus = false
            return@LaunchedEffect
        }
        delay(LIBRARY_SCAN_STATUS_DELAY_MS)
        if (container.libraryScanInProgress.value) {
            showLibraryScanStatus = true
        }
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
        // Mini player everywhere except Home (has its own now-playing card) and full Player.
        val miniPlayerAllowed = currentRoute != null &&
            currentRoute != ReverieDestination.Player.route &&
            currentRoute != ReverieDestination.Home.route

        // Keep a tab highlighted on detail / Quick Access screens so the bottom bar
        // never sits in an "nothing selected" state.
        val selectedTabRoute = when (currentRoute) {
            ReverieDestination.Home.route,
            ReverieDestination.Library.route,
            ReverieDestination.Player.route,
            ReverieDestination.Settings.route,
            -> currentRoute
            else -> {
                val previous = navController.previousBackStackEntry?.destination?.route
                when {
                    ReverieDestination.isMainTabRoute(previous) -> previous
                    else -> ReverieDestination.Home.route
                }
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(playbackSnackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        if (showLibraryScanStatus) {
                            LibraryScanStatusBar()
                        }
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
                                // Always navigate — even when the tab looks selected while a
                                // Quick Access overlay (Import, Audio FX, etc.) is on top.
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
                } else if (showLibraryScanStatus) {
                    LibraryScanStatusBar()
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

/** Only show status if indexing is still going after this delay. */
private const val LIBRARY_SCAN_STATUS_DELAY_MS = 350L

/** Snackbar-style status strip — always visible while startup indexing runs. */
@Composable
private fun LibraryScanStatusBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = "Updating library…",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            textAlign = TextAlign.Center,
        )
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
    val isPlaying by remember(playbackRepository) {
        playbackRepository.playerProgress
            .map { it.isPlaying }
            .distinctUntilChanged()
    }.collectAsState(initial = playbackRepository.playerProgress.value.isPlaying)

    val current = track ?: return
    MiniPlayerBar(
        track = current,
        isPlaying = isPlaying,
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
        tonalElevation = 0.dp,
    ) {
        ReverieDestination.bottomNavItems.forEach { destination ->
            val selected = selectedRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Box(modifier = Modifier.navUnderGlow(visible = selected)) {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label,
                        )
                    }
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
