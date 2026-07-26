package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.data.import.EditableTrackMetadata
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake library data for UI development.
 * Replace with Room-backed repository after import pipeline exists.
 */
class FakeMusicLibraryRepository : MusicLibraryRepository {

    private val sampleTracks = listOf(
        Track("1", "Afterglow", "Echos", "Silent Skies", 293_000),
        Track("2", "Nocturne", "Lune", "Nightfall", 245_000),
        Track("3", "Midnight Drive", "Tokyo Wanderer", "Neon Streets", 312_000),
        Track("4", "Starlight", "Nova", "Cosmos", 198_000),
        Track("5", "Echoes", "Reverie", "Dreamscape", 267_000),
    )

    private val _folders = MutableStateFlow(
        listOf(
            MusicFolder("f0", "Library Root", 5, 4, relativePath = ""),
            MusicFolder("f1", "Downloaded Music", 3, 2, relativePath = "Downloaded Music"),
            MusicFolder("f2", "FLAC Collection", 2, 1, relativePath = "FLAC Collection"),
        ),
    )
    override val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private val _artists = MutableStateFlow(
        listOf(
            Artist("a1", "Echos", 3, 24),
            Artist("a2", "Lune", 2, 18),
            Artist("a3", "Tokyo Wanderer", 4, 31),
            Artist("a4", "Nova", 1, 12),
        ),
    )
    override val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _albums = MutableStateFlow(
        listOf(
            Album("al1", "Silent Skies", "Echos", 12),
            Album("al2", "Nightfall", "Lune", 10),
            Album("al3", "Neon Streets", "Tokyo Wanderer", 14),
            Album("al4", "Cosmos", "Nova", 8),
        ),
    )
    override val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _songs = MutableStateFlow(sampleTracks)
    override val songs: StateFlow<List<Track>> = _songs.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow(sampleTracks)
    override val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    private val _homeLibraryPreview = MutableStateFlow(sampleTracks.take(12))
    override val homeLibraryPreview: StateFlow<List<Track>> = _homeLibraryPreview.asStateFlow()

    private val _songCount = MutableStateFlow(42)
    override val songCount: StateFlow<Int> = _songCount.asStateFlow()

    override suspend fun scanLibrary(): LibraryScanResult = LibraryScanResult(
        tracksFound = 0,
        tracksIndexed = 0,
        tracksRemoved = 0,
        foldersIndexed = 0,
    )

    override suspend fun updateTrackMetadata(
        trackId: String,
        metadata: EditableTrackMetadata,
    ): Result<Unit> {
        val current = _songs.value.toMutableList()
        val index = current.indexOfFirst { it.id == trackId }
        if (index < 0) return Result.failure(IllegalArgumentException("Track not found."))
        current[index] = current[index].copy(
            title = metadata.title,
            artist = metadata.artist,
            album = metadata.album,
            year = metadata.year,
            genre = metadata.genre,
        )
        _songs.value = current
        return Result.success(Unit)
    }

    override suspend fun updateTrackRating(trackId: String, rating: Int): Result<Unit> {
        val current = _songs.value.toMutableList()
        val index = current.indexOfFirst { it.id == trackId }
        if (index < 0) return Result.failure(IllegalArgumentException("Track not found."))
        current[index] = current[index].copy(rating = rating.coerceIn(0, 5))
        _songs.value = current
        return Result.success(Unit)
    }

    override suspend fun updateTrackArtwork(
        trackId: String,
        sourceUri: android.net.Uri,
    ): Result<String> {
        val current = _songs.value.toMutableList()
        val index = current.indexOfFirst { it.id == trackId }
        if (index < 0) return Result.failure(IllegalArgumentException("Track not found."))
        val path = "fake://artwork/$trackId"
        val track = current[index]
        _songs.value = current.map {
            if (it.artist == track.artist && it.album == track.album) {
                it.copy(artworkPath = path)
            } else {
                it
            }
        }
        return Result.success(path)
    }

    override suspend fun deleteTrack(trackId: String): Result<Unit> {
        val current = _songs.value
        if (current.none { it.id == trackId }) {
            return Result.failure(IllegalArgumentException("Track not found."))
        }
        _songs.value = current.filterNot { it.id == trackId }
        _recentlyPlayed.value = _recentlyPlayed.value.filterNot { it.id == trackId }
        _songCount.value = (_songCount.value - 1).coerceAtLeast(0)
        return Result.success(Unit)
    }

    override suspend fun deleteTracksAndFolders(
        trackIds: Collection<String>,
        folderRelativePaths: Collection<String>,
    ): Result<Int> {
        val idSet = trackIds.toSet()
        val before = _songs.value.size
        _songs.value = _songs.value.filterNot { it.id in idSet }
        _recentlyPlayed.value = _recentlyPlayed.value.filterNot { it.id in idSet }
        val removed = before - _songs.value.size
        _songCount.value = (_songCount.value - removed).coerceAtLeast(0)
        if (folderRelativePaths.isNotEmpty()) {
            val removePaths = folderRelativePaths.toSet()
            _folders.value = _folders.value.filterNot { it.relativePath in removePaths }
        }
        return Result.success(removed)
    }

    override suspend fun moveTracksAndFolders(
        trackIds: Collection<String>,
        folderRelativePaths: Collection<String>,
        destinationRelativePath: String,
    ): Result<Int> {
        // In-memory fake: just report success without reshuffling sample paths.
        return Result.success(trackIds.distinct().size)
    }

    override suspend fun createLibraryFolder(relativePath: String): Result<Unit> {
        val name = relativePath.substringAfterLast('/').ifBlank { relativePath }
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Folder name is required."))
        if (_folders.value.any { it.relativePath.equals(relativePath, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Folder already exists."))
        }
        _folders.value = _folders.value + MusicFolder(
            id = "fake-$relativePath",
            name = name,
            songCount = 0,
            albumCount = 0,
            relativePath = relativePath,
        )
        return Result.success(Unit)
    }
}
