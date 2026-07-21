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

    data object AudioFx : ReverieDestination(
        route = "audio_fx",
        label = "Audio FX",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object Tutorial : ReverieDestination(
        route = "tutorial",
        label = "Tutorial",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object TutorialChapter : ReverieDestination(
        route = "tutorial/{chapterId}",
        label = "Tutorial",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ) {
        fun createRoute(chapterId: String): String = "tutorial/$chapterId"
    }

    data object SmartPlaylists : ReverieDestination(
        route = "smart_playlists",
        label = "Smart Playlists",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    data object SmartPlaylistDetail : ReverieDestination(
        route = "smart_playlist/{smartPlaylistId}",
        label = "Smart Playlist",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ) {
        fun createRoute(id: String): String = "smart_playlist/$id"
    }

    data object SmartPlaylistEditor : ReverieDestination(
        route = "smart_playlist_edit/{smartPlaylistId}",
        label = "Edit Smart Playlist",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ) {
        const val NEW_ID = "new"
        fun createRoute(id: String = NEW_ID): String = "smart_playlist_edit/$id"
    }

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
        const val SMART_PLAYLIST_ID_ARG = "smartPlaylistId"
        const val TUTORIAL_CHAPTER_ID_ARG = "chapterId"
        val bottomNavItems = listOf(Home, Library, Player, Settings)

        /** Routes where the bottom bar stays mounted (avoids tearing down nav chrome). */
        val bottomBarVisibleRoutes = setOf(
            Home.route,
            Library.route,
            Player.route,
            Settings.route,
            Search.route,
            ImportMusic.route,
            PremiumFeatures.route,
            LibraryStats.route,
            AudioFx.route,
            Tutorial.route,
            TutorialChapter.route,
            SmartPlaylists.route,
            SmartPlaylistDetail.route,
            SmartPlaylistEditor.route,
            SongDetail.route,
            PlaylistDetail.route,
        )

        fun isMainTabRoute(route: String?): Boolean =
            route == Home.route ||
                route == Library.route ||
                route == Player.route ||
                route == Settings.route
    }
}
