package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.data.import.EditableTrackMetadata
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.PlayerProgress
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import com.perceptiveus.reverie.playback.PlaybackAudioAnalyzer
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

    /**
     * Sets the track rating (0–5). 0 clears the rating.
     * Premium-gated via [com.perceptiveus.reverie.core.entitlement.AppFeature.RATINGS].
     */
    suspend fun updateTrackRating(trackId: String, rating: Int): Result<Unit>

    /**
     * Imports album art from [sourceUri] into the art cache and updates Room.
     * Premium-gated via [com.perceptiveus.reverie.core.entitlement.AppFeature.ALBUM_ART_EDITING].
     * @return absolute path of the saved artwork on success
     */
    suspend fun updateTrackArtwork(trackId: String, sourceUri: android.net.Uri): Result<String>
}

/** Playback state backed by Media3 ExoPlayer via [com.perceptiveus.reverie.playback.PlaybackService]. */
interface PlaybackRepository {
    /**
     * Structural session state (track, queue, shuffle/repeat).
     * Does not tick every position update — use [playerProgress] for that.
     */
    val playbackState: StateFlow<PlaybackState>
    /** High-frequency position / isPlaying for seek bars and play buttons. */
    val playerProgress: StateFlow<PlayerProgress>
    /** Live spectrum/waveform from decoded PCM (no mic permission). */
    val visualizerFrame: StateFlow<PlaybackAudioAnalyzer.Frame>

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
    /**
     * Updates [artworkPath] on matching queue items after album-art import
     * so Now Playing reflects the change without restarting playback.
     */
    fun updateQueueArtwork(artist: String, album: String, artworkPath: String)
    /**
     * Updates title/artist/album on a queue item after metadata edit
     * so Home / Player / Queue stay in sync without restarting playback.
     */
    fun updateQueueTrackMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String,
    )
    fun togglePlayPause()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeatMode()
}
