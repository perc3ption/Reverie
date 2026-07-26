package com.perceptiveus.reverie.data.repository

import android.content.Context
import android.net.Uri
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.import.AlbumArtCache
import com.perceptiveus.reverie.data.import.AlbumArtImportResult
import com.perceptiveus.reverie.data.import.AlbumArtImporter
import com.perceptiveus.reverie.data.import.AudioMetadataWriter
import com.perceptiveus.reverie.data.import.EditableTrackMetadata
import com.perceptiveus.reverie.data.import.MusicIndexer
import com.perceptiveus.reverie.data.local.dao.MusicFolderDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.mapper.toDomain
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
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
    private val appContext: Context,
    folderDao: MusicFolderDao,
    private val trackDao: TrackDao,
    private val musicIndexer: MusicIndexer,
    private val metadataWriter: AudioMetadataWriter,
    private val albumArtCache: AlbumArtCache,
    private val featureAccessChecker: FeatureAccessChecker,
    private val storage: MusicLibraryStorage,
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

    override val homeLibraryPreview: StateFlow<List<Track>> =
        trackDao.observeHomeLibraryPreview(limit = 12)
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

    override suspend fun updateTrackArtwork(
        trackId: String,
        sourceUri: Uri,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!featureAccessChecker.canAccess(AppFeature.ALBUM_ART_EDITING)) {
            return@withContext Result.failure(AlbumArtAccessException)
        }
        runCatching {
            val existing = trackDao.getById(trackId)
                ?: error("Track not found.")
            when (
                val result = AlbumArtImporter.importForAlbum(
                    context = appContext,
                    artist = existing.artist,
                    album = existing.album,
                    sourceUri = sourceUri,
                    albumArtCache = albumArtCache,
                )
            ) {
                is AlbumArtImportResult.Success -> {
                    trackDao.updateArtworkPathForAlbum(
                        artist = existing.artist,
                        album = existing.album,
                        artworkPath = result.artworkPath,
                    )
                    result.artworkPath
                }
                is AlbumArtImportResult.Failure -> error(result.message)
            }
        }
    }

    override suspend fun deleteTrack(trackId: String): Result<Unit> =
        deleteTracksAndFolders(trackIds = listOf(trackId)).map { Unit }

    override suspend fun deleteTracksAndFolders(
        trackIds: Collection<String>,
        folderRelativePaths: Collection<String>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val distinctIds = trackIds.distinct()
            val rootPath = storage.libraryRoot.canonicalPath.trimEnd(File.separatorChar)
            val deletedIds = ArrayList<String>(distinctIds.size)
            val touchedParents = LinkedHashSet<File>()

            for (trackId in distinctIds) {
                val existing = trackDao.getById(trackId) ?: continue
                if (existing.filePath.isNotBlank()) {
                    val file = File(existing.filePath)
                    val canonical = runCatching { file.canonicalPath }.getOrDefault(existing.filePath)
                        .trimEnd(File.separatorChar)
                    val underLibrary = canonical == rootPath ||
                        canonical.startsWith(rootPath + File.separator)
                    if (!underLibrary) {
                        error("Can only delete files inside the Reverie library folder.")
                    }
                    if (file.exists() && !file.delete()) {
                        error("Could not delete \"${file.name}\".")
                    }
                    deleteSidecarLyrics(file)
                    file.parentFile?.let { touchedParents += it }
                }
                deletedIds += trackId
            }

            if (deletedIds.isNotEmpty()) {
                trackDao.deleteByIds(deletedIds)
            }

            val foldersToRemove = folderRelativePaths
                .map { it.trim().trim('/') }
                .filter { it.isNotEmpty() }
                .distinct()
                .sortedByDescending { it.count { ch -> ch == '/' } }

            for (relativePath in foldersToRemove) {
                val dir = storage.resolveFile(relativePath)
                val canonical = dir.canonicalPath.trimEnd(File.separatorChar)
                val underLibrary = canonical != rootPath &&
                    canonical.startsWith(rootPath + File.separator)
                if (!underLibrary) continue
                if (dir.exists()) {
                    if (!dir.deleteRecursively()) {
                        error("Could not delete folder \"$relativePath\".")
                    }
                }
            }

            pruneEmptyAncestors(touchedParents, rootPath)

            albumArtCache.deleteOrphans(
                keepPaths = trackDao.getAllTracks().map { it.artworkPath }.toSet(),
            )

            // Sync folder rows / counts after disk changes.
            if (deletedIds.isNotEmpty() || foldersToRemove.isNotEmpty()) {
                musicIndexer.scanLibrary()
            }

            deletedIds.size
        }
    }

    /** Removes empty directories up toward (but not including) the library root. */
    private fun pruneEmptyAncestors(starts: Set<File>, rootPath: String) {
        val queue = ArrayDeque(starts)
        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            val canonical = runCatching { dir.canonicalPath }.getOrNull()
                ?.trimEnd(File.separatorChar)
                ?: continue
            if (canonical == rootPath || !canonical.startsWith(rootPath + File.separator)) continue
            if (!dir.isDirectory) continue
            val children = dir.listFiles().orEmpty()
            if (children.isNotEmpty()) continue
            if (dir.delete()) {
                dir.parentFile?.let { queue += it }
            }
        }
    }

    private fun deleteSidecarLyrics(audio: File) {
        val parent = audio.parentFile ?: return
        val base = audio.nameWithoutExtension
        listOf(
            File(parent, "$base.lrc"),
            File(parent, "$base.LRC"),
            File(parent, "$base.txt"),
            File(parent, "$base.TXT"),
        ).forEach { sidecar ->
            if (sidecar.isFile) sidecar.delete()
        }
    }
}

object RatingAccessException : Exception("Ratings are a Premium feature.")

object AlbumArtAccessException : Exception("Album art editing is a Premium feature.")
