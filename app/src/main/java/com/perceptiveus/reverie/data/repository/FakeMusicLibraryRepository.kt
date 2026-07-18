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
}
