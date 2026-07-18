package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.data.import.EditableTrackMetadata
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.flow.StateFlow

/** Library data sourced from Room. */
interface MusicLibraryRepository {
    val folders: StateFlow<List<MusicFolder>>
    val artists: StateFlow<List<Artist>>
    val albums: StateFlow<List<Album>>
    val songs: StateFlow<List<Track>>
    val recentlyPlayed: StateFlow<List<Track>>
    val songCount: StateFlow<Int>

    /** Scans the on-disk Reverie folder and syncs metadata into Room. */
    suspend fun scanLibrary(): LibraryScanResult

    /**
     * Writes [metadata] into the audio file on disk, then updates Room.
     * Changes persist if the file is later copied outside the app.
     */
    suspend fun updateTrackMetadata(trackId: String, metadata: EditableTrackMetadata): Result<Unit>
}

/** Playback state backed by Media3 ExoPlayer via [com.perceptiveus.reverie.playback.PlaybackService]. */
interface PlaybackRepository {
    val playbackState: StateFlow<PlaybackState>

    fun play(
        tracks: List<Track>,
        startIndex: Int = 0,
        source: QueueSource = QueueSource.Unknown,
    )
    /** Jump to an item already in the active queue and start playback. */
    fun playQueueIndex(index: Int)
    /**
     * Session-only mute/unmute for a track in the current queue.
     * Does not change playlists or library membership.
     */
    fun toggleQueueTrackEnabled(trackId: String)
    /** Append tracks to the end of the queue without interrupting playback. */
    fun addToQueue(tracks: List<Track>)
    /** Reorder an item within the active queue; keeps the current track playing. */
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun togglePlayPause()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeatMode()
}
