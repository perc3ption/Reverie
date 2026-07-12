package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.flow.StateFlow

/** Library data sourced from Room. */
interface MusicLibraryRepository {
    val folders: StateFlow<List<MusicFolder>>
    val artists: StateFlow<List<Artist>>
    val albums: StateFlow<List<Album>>
    val recentlyPlayed: StateFlow<List<Track>>
    val songCount: StateFlow<Int>

    /** Scans the on-disk Reverie folder and syncs metadata into Room. */
    suspend fun scanLibrary(): LibraryScanResult
}

/** Playback state backed by Media3/ExoPlayer later. */
interface PlaybackRepository {
    val playbackState: StateFlow<PlaybackState>

    // TODO: Wire to Media3 ExoPlayer service.
    fun togglePlayPause()
    fun skipToNext()
    fun skipToPrevious()
    fun toggleShuffle()
    fun cycleRepeatMode()
}
