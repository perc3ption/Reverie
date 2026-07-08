package com.perceptiveus.reverie.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long = 0L,
    val filePath: String = "",
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
)

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
)

data class MusicFolder(
    val id: String,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
)

data class Playlist(
    val id: String,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queueSize: Int = 0,
)
