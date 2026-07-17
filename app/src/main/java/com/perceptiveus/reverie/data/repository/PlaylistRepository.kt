package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Playlist persistence backed by Room. */
interface PlaylistRepository {
    val playlists: StateFlow<List<Playlist>>
    val playlistCount: StateFlow<Int>

    fun observePlaylistsForTrack(trackId: String): Flow<List<Playlist>>
    fun observePlaylist(playlistId: String): Flow<Playlist?>
    fun observePlaylistTracks(playlistId: String): Flow<List<Track>>
    suspend fun createPlaylist(name: String): Result<Playlist>
    suspend fun deletePlaylist(id: String)
    suspend fun addTrackToPlaylist(playlistId: String, trackId: String)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
}
