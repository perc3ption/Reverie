package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.dao.PlaylistDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.entity.PlaylistEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistTrackCrossRef
import com.perceptiveus.reverie.data.local.mapper.toDomain
import com.perceptiveus.reverie.domain.model.Playlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class RoomPlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
    private val featureAccessChecker: FeatureAccessChecker,
    scope: CoroutineScope,
) : PlaylistRepository {

    override val playlists: StateFlow<List<Playlist>> = playlistDao.observePlaylistsWithCounts()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val playlistCount: StateFlow<Int> = playlistDao.observePlaylistCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), 0)

    override suspend fun createPlaylist(name: String): Result<Playlist> {
        val currentCount = playlistDao.count()
        if (!featureAccessChecker.canCreatePlaylist(currentCount)) {
            return Result.failure(PlaylistLimitException(currentCount))
        }
        val playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
        )
        playlistDao.insert(playlist)
        return Result.success(
            Playlist(
                id = playlist.id,
                name = playlist.name,
                trackCount = 0,
                createdAt = playlist.createdAt,
            ),
        )
    }

    override suspend fun deletePlaylist(id: String) {
        playlistDao.deleteById(id)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        if (trackDao.getById(trackId) == null) return
        val nextPosition = playlistDao.maxPositionForPlaylist(playlistId) + 1
        playlistDao.insertPlaylistTracks(
            listOf(PlaylistTrackCrossRef(playlistId, trackId, nextPosition)),
        )
    }
}

class PlaylistLimitException(val currentCount: Int) :
    Exception("Free tier allows ${FeatureAccessChecker.FREE_MAX_PLAYLISTS} playlists (currently $currentCount).")
