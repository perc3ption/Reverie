package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.dao.PlaylistDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.entity.PlaylistEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistTrackCrossRef
import com.perceptiveus.reverie.data.local.mapper.toDomain
import com.perceptiveus.reverie.data.playlist.PlaylistCoverStore
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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

    override fun observePlaylistsForTrack(trackId: String): Flow<List<Playlist>> =
        playlistDao.observePlaylistsForTrack(trackId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observePlaylist(playlistId: String): Flow<Playlist?> =
        playlistDao.observePlaylist(playlistId)
            .map { it?.toDomain() }

    override fun observePlaylistTracks(playlistId: String): Flow<List<Track>> =
        playlistDao.observeTracksForPlaylist(playlistId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun createPlaylist(name: String): Result<Playlist> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Playlist name cannot be empty."))
        }
        val currentCount = playlistDao.count()
        if (!featureAccessChecker.canCreatePlaylist(currentCount)) {
            return Result.failure(PlaylistLimitException(currentCount))
        }
        val playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = trimmed,
        )
        playlistDao.insert(playlist)
        return Result.success(
            Playlist(
                id = playlist.id,
                name = playlist.name,
                trackCount = 0,
                createdAt = playlist.createdAt,
                description = playlist.description,
                coverPath = playlist.coverPath,
            ),
        )
    }

    override suspend fun deletePlaylist(id: String) {
        val existing = playlistDao.getById(id)
        playlistDao.deleteById(id)
        existing?.coverPath?.let { PlaylistCoverStore.deleteCover(it) }
    }

    override suspend fun updatePlaylist(
        id: String,
        name: String?,
        description: String?,
        coverPath: String?,
    ): Result<Unit> {
        val existing = playlistDao.getById(id)
            ?: return Result.failure(IllegalArgumentException("Playlist not found."))
        val nextName = name?.trim()?.takeIf { it.isNotEmpty() } ?: existing.name
        val nextDescription = description ?: existing.description
        val nextCover = coverPath ?: existing.coverPath
        if (coverPath != null &&
            existing.coverPath.isNotBlank() &&
            existing.coverPath != coverPath
        ) {
            PlaylistCoverStore.deleteCover(existing.coverPath)
        }
        playlistDao.insert(
            existing.copy(
                name = nextName,
                description = nextDescription,
                coverPath = nextCover,
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        if (trackDao.getById(trackId) == null) return
        if (playlistDao.trackCountInPlaylist(playlistId, trackId) > 0) return
        val nextPosition = playlistDao.maxPositionForPlaylist(playlistId) + 1
        playlistDao.insertPlaylistTracks(
            listOf(PlaylistTrackCrossRef(playlistId, trackId, nextPosition)),
        )
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.deletePlaylistTrack(playlistId, trackId)
    }
}

class PlaylistLimitException(val currentCount: Int) :
    Exception("Free tier allows ${FeatureAccessChecker.FREE_MAX_PLAYLISTS} playlists (currently $currentCount).")
