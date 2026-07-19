package com.perceptiveus.reverie.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ReverieDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : ReverieDestination(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object Library : ReverieDestination(
        route = "library",
        label = "Library",
        selectedIcon = Icons.Filled.Album,
        unselectedIcon = Icons.Outlined.Album,
    )

    data object Player : ReverieDestination(
        route = "player",
        label = "Player",
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq,
    )

    data object Settings : ReverieDestination(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )

    data object ImportMusic : ReverieDestination(
        route = "import_music",
        label = "Import Music",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object PremiumFeatures : ReverieDestination(
        route = "premium_features",
        label = "Premium",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object Search : ReverieDestination(
        route = "search",
        label = "Search",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object LibraryStats : ReverieDestination(
        route = "library_stats",
        label = "Stats",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object SongDetail : ReverieDestination(
        route = "song/{trackId}",
        label = "Song",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ) {
        fun createRoute(trackId: String): String = "song/$trackId"
    }

    data object PlaylistDetail : ReverieDestination(
        route = "playlist/{playlistId}",
        label = "Playlist",
        selectedIcon = Icons.Filled.Album,
        unselectedIcon = Icons.Outlined.Album,
    ) {
        fun createRoute(playlistId: String): String = "playlist/$playlistId"
    }

    companion object {
        const val SONG_TRACK_ID_ARG = "trackId"
        const val PLAYLIST_ID_ARG = "playlistId"
        val bottomNavItems = listOf(Home, Library, Player, Settings)

        /** Routes where the bottom bar stays mounted (avoids tearing down nav chrome). */
        val bottomBarVisibleRoutes = setOf(
            Home.route,
            Library.route,
            Player.route,
            Settings.route,
            Search.route,
            LibraryStats.route,
            SongDetail.route,
            PlaylistDetail.route,
        )
    }
}
