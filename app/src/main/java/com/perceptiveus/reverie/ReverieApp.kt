package com.perceptiveus.reverie

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.perceptiveus.reverie.core.design.ReverieTheme
import com.perceptiveus.reverie.core.design.components.MiniPlayerBar
import com.perceptiveus.reverie.core.navigation.ReverieDestination
import com.perceptiveus.reverie.core.navigation.ReverieNavGraph

@Composable
fun ReverieApp(container: AppContainer) {
    val themePreference by container.settingsRepository.themePreference.collectAsState()
    val playbackState by container.playbackRepository.playbackState.collectAsState()

    ReverieTheme(themePreference = themePreference) {
        val navController = rememberNavController()
        val factory = ReverieViewModelFactory(container)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val showBottomBar = currentDestination?.route in ReverieDestination.bottomNavItems.map { it.route }
        val showMiniPlayer = showBottomBar &&
            currentDestination?.route != ReverieDestination.Player.route &&
            playbackState.currentTrack != null

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        if (showMiniPlayer) {
                            MiniPlayerBar(
                                track = playbackState.currentTrack,
                                isPlaying = playbackState.isPlaying,
                                onPlayPause = container.playbackRepository::togglePlayPause,
                                onNext = container.playbackRepository::skipToNext,
                                onPrevious = container.playbackRepository::skipToPrevious,
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
                            currentRoute = currentDestination?.route,
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
            ReverieNavGraph(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                container = container,
                factory = factory,
                startDestination = ReverieDestination.Home.route,
            )
        }
    }
}

@Composable
private fun ReverieBottomBar(
    currentRoute: String?,
    onNavigate: (ReverieDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ReverieDestination.bottomNavItems.forEach { destination ->
            val selected = currentRoute == destination.route
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
