package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.import.AudioMetadataWriter
import com.perceptiveus.reverie.data.import.EditableTrackMetadata
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File

class RoomMusicLibraryRepository(
    folderDao: MusicFolderDao,
    private val trackDao: TrackDao,
    private val musicIndexer: MusicIndexer,
    private val metadataWriter: AudioMetadataWriter,
    private val featureAccessChecker: FeatureAccessChecker,
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

    override val songs: StateFlow<List<Track>> = trackDao.observeAllTracks()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val recentlyPlayed: StateFlow<List<Track>> = trackDao.observeRecentlyPlayed()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override val songCount: StateFlow<Int> = trackDao.observeSongCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), 0)

    override suspend fun scanLibrary(): LibraryScanResult = musicIndexer.scanLibrary()

    override suspend fun updateTrackMetadata(
        trackId: String,
        metadata: EditableTrackMetadata,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = trackDao.getById(trackId)
                ?: error("Track not found.")
            val file = File(existing.filePath)
            if (!file.isFile) error("Audio file is missing.")
            if (!file.canWrite()) error("Audio file is not writable.")

            val cleaned = EditableTrackMetadata(
                title = metadata.title.trim().ifBlank { existing.title },
                artist = metadata.artist.trim().ifBlank { existing.artist },
                album = metadata.album.trim().ifBlank { existing.album },
                year = metadata.year.coerceIn(0, 9999),
                genre = metadata.genre.trim(),
            )
            metadataWriter.write(file, cleaned)
            trackDao.insert(
                existing.copy(
                    title = cleaned.title,
                    artist = cleaned.artist,
                    album = cleaned.album,
                    year = cleaned.year,
                    genre = cleaned.genre,
                ),
            )
        }
    }

    override suspend fun updateTrackRating(
        trackId: String,
        rating: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!featureAccessChecker.canAccess(AppFeature.RATINGS)) {
            return@withContext Result.failure(RatingAccessException)
        }
        runCatching {
            trackDao.getById(trackId) ?: error("Track not found.")
            trackDao.updateRating(trackId, rating.coerceIn(0, 5))
        }
    }
}

object RatingAccessException : Exception("Ratings are a Premium feature.")
