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
    /** User rating 0–5; 0 means unrated. Premium feature. */
    val rating: Int = 0,
    /** Parent folder id in the library tree; null when unknown. */
    val folderId: String? = null,
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
    /** Path relative to the Reverie library root. Empty string = library root. */
    val relativePath: String = "",
)

data class Playlist(
    val id: String,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val description: String = "",
    /** Absolute path to playlist cover image; empty when unset. */
    val coverPath: String = "",
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

/**
 * Describes where the current playback queue came from
 * (library, playlist, album, artist, etc.).
 */
sealed class QueueSource {
    data object Library : QueueSource()
    data class Playlist(
        val name: String,
        val description: String = "",
        val coverPath: String = "",
    ) : QueueSource()
    data class Album(
        val title: String,
        val artist: String,
        val year: Int = 0,
        val artworkPath: String = "",
    ) : QueueSource()
    data class Artist(
        val name: String,
        val artworkPath: String = "",
    ) : QueueSource()
    data class Folder(val name: String) : QueueSource()
    data class SmartPlaylist(val name: String) : QueueSource()
    data object RecentlyPlayed : QueueSource()
    data object Unknown : QueueSource()
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queueSize: Int = 0,
    val nextTrack: Track? = null,
    /** Current playlist in play order. */
    val queue: List<Track> = emptyList(),
    /** Index of [currentTrack] within [queue], or -1 when empty. */
    val queueIndex: Int = -1,
    /** Origin of the active queue. */
    val queueSource: QueueSource = QueueSource.Unknown,
    /**
     * Track ids muted for this playback session only.
     * Cleared when a new queue is started.
     */
    val disabledTrackIds: Set<String> = emptySet(),
    /** Last player error message, cleared when playback recovers. */
    val errorMessage: String? = null,
)

/**
 * High-frequency playback clock. Updated independently from [PlaybackState]
 * so seek-bar ticks do not recompose queue / Now Playing metadata.
 */
data class PlayerProgress(
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
)
