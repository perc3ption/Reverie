package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.data.import.MusicIndexer
import com.perceptiveus.reverie.data.local.dao.MusicFolderDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.mapper.toDomain
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoomMusicLibraryRepository(
    folderDao: MusicFolderDao,
    private val trackDao: TrackDao,
    private val musicIndexer: MusicIndexer,
    scope: CoroutineScope,
) : MusicLibraryRepository {

    override val folders: StateFlow<List<MusicFolder>> = folderDao.observeFoldersWithCounts()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val artists: StateFlow<List<Artist>> = trackDao.observeArtists()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val albums: StateFlow<List<Album>> = trackDao.observeAlbums()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val recentlyPlayed: StateFlow<List<Track>> = trackDao.observeRecentlyPlayed()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val songCount: StateFlow<Int> = trackDao.observeSongCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), 0)

    override suspend fun scanLibrary(): LibraryScanResult = musicIndexer.scanLibrary()
}
