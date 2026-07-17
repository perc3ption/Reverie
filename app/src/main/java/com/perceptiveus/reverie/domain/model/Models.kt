package com.perceptiveus.reverie.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long = 0L,
    val filePath: String = "",
    /** Absolute path to cached cover art; empty when unavailable. */
    val artworkPath: String = "",
    /** Release year from tags; 0 when unknown. */
    val year: Int = 0,
    val genre: String = "",
    val dateAdded: Long = 0L,
    val isFavorite: Boolean = false,
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

data class Tag(
    val id: String,
    val name: String,
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
    val nextTrack: Track? = null,
    /** Current playlist in play order. */
    val queue: List<Track> = emptyList(),
    /** Index of [currentTrack] within [queue], or -1 when empty. */
    val queueIndex: Int = -1,
    /** ExoPlayer audio session id for [android.media.audiofx.Visualizer]; 0 when unset. */
    val audioSessionId: Int = 0,
)
