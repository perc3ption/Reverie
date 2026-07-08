package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.domain.model.Playlist
import kotlinx.coroutines.flow.StateFlow

/** Playlist persistence backed by Room. */
interface PlaylistRepository {
    val playlists: StateFlow<List<Playlist>>
    val playlistCount: StateFlow<Int>

    suspend fun createPlaylist(name: String): Result<Playlist>
    suspend fun deletePlaylist(id: String)
    suspend fun addTrackToPlaylist(playlistId: String, trackId: String)
}
