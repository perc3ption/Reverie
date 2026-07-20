package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.domain.model.SmartPlaylist
import com.perceptiveus.reverie.domain.model.SmartPlaylistRule
import com.perceptiveus.reverie.domain.model.SmartPlaylistSort
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface SmartPlaylistRepository {
    val playlists: Flow<List<SmartPlaylist>>

    fun observePlaylist(id: String): Flow<SmartPlaylist?>
    fun observeRules(playlistId: String): Flow<List<SmartPlaylistRule>>

    suspend fun getPlaylist(id: String): SmartPlaylist?
    suspend fun getRules(playlistId: String): List<SmartPlaylistRule>
    suspend fun evaluateTracks(playlistId: String): List<Track>
    suspend fun evaluateTracks(
        rules: List<SmartPlaylistRule>,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
    ): List<Track>

    suspend fun createPlaylist(
        name: String,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
        rules: List<SmartPlaylistRule>,
    ): Result<SmartPlaylist>

    suspend fun updatePlaylist(
        id: String,
        name: String,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
        rules: List<SmartPlaylistRule>,
    ): Result<SmartPlaylist>

    suspend fun deletePlaylist(id: String)
}
