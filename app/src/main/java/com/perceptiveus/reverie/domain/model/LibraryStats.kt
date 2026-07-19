package com.perceptiveus.reverie.domain.model

data class LibraryStats(
    val songCount: Int = 0,
    val artistCount: Int = 0,
    val albumCount: Int = 0,
    val playlistCount: Int = 0,
    val totalPlays: Int = 0,
    val playsLast7Days: Int = 0,
    val playsLast30Days: Int = 0,
    val topTracks: List<PlayedItemStat> = emptyList(),
    val topArtists: List<PlayedItemStat> = emptyList(),
)

data class PlayedItemStat(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val artworkPath: String = "",
    val playCount: Int,
)
