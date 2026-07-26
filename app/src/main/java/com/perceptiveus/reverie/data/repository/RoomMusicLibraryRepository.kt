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
import com.perceptiveus.reverie.data.import.SafImportHelper
import com.perceptiveus.reverie.data.import.SupportedAudioFormats
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
import java.io.IOException

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

    override suspend fun moveTracksAndFolders(
        trackIds: Collection<String>,
        folderRelativePaths: Collection<String>,
        destinationRelativePath: String,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val destRelative = destinationRelativePath.trim().replace('\\', '/').trim('/')
            val rootPath = storage.libraryRoot.canonicalPath.trimEnd(File.separatorChar)
            val destinationDir = storage.createSubdirectory(destRelative)
            val destCanonical = destinationDir.canonicalPath.trimEnd(File.separatorChar)
            if (destCanonical != rootPath && !destCanonical.startsWith(rootPath + File.separator)) {
                error("Destination must be inside the Reverie library folder.")
            }

            val foldersToMove = folderRelativePaths
                .map { it.trim().replace('\\', '/').trim('/') }
                .filter { it.isNotEmpty() }
                .distinct()
                .sortedBy { it.count { ch -> ch == '/' } }

            for (folderPath in foldersToMove) {
                if (destRelative == folderPath || destRelative.startsWith("$folderPath/")) {
                    error("Can't move a folder into itself or one of its subfolders.")
                }
            }

            var movedFiles = 0
            val touchedParents = LinkedHashSet<File>()

            // Move whole folders first (shallowest path first).
            for (folderPath in foldersToMove) {
                val sourceDir = storage.resolveFile(folderPath)
                if (!sourceDir.isDirectory) continue
                val sourceCanonical = sourceDir.canonicalPath.trimEnd(File.separatorChar)
                if (sourceCanonical == rootPath ||
                    !sourceCanonical.startsWith(rootPath + File.separator)
                ) {
                    error("Can only move folders inside the Reverie library.")
                }
                // Already directly under destination with same name → nothing to do.
                val sourceParent = sourceDir.parentFile?.canonicalPath
                    ?.trimEnd(File.separatorChar)
                if (sourceParent == destCanonical && sourceDir.name == File(folderPath).name) {
                    continue
                }
                val targetDir = uniqueDirectory(destinationDir, sourceDir.name)
                moveDirectory(sourceDir, targetDir)
                movedFiles += countAudioFiles(targetDir)
                sourceDir.parentFile?.let { touchedParents += it }
            }

            // Move individually selected tracks that weren't covered by a folder move.
            val folderPrefixes = foldersToMove
            for (trackId in trackIds.distinct()) {
                val existing = trackDao.getById(trackId) ?: continue
                if (existing.filePath.isBlank()) continue
                val source = File(existing.filePath)
                val sourceCanonical = runCatching { source.canonicalPath }.getOrDefault(existing.filePath)
                    .trimEnd(File.separatorChar)
                if (!sourceCanonical.startsWith(rootPath + File.separator) &&
                    sourceCanonical != rootPath
                ) {
                    error("Can only move files inside the Reverie library folder.")
                }
                if (!source.isFile) continue

                val parentRelative = parentRelativePath(source, storage.libraryRoot)
                if (folderPrefixes.any { parentRelative == it || parentRelative.startsWith("$it/") }) {
                    continue
                }

                val sourceParentCanonical = source.parentFile?.canonicalPath
                    ?.trimEnd(File.separatorChar)
                if (sourceParentCanonical == destCanonical) {
                    continue
                }

                val target = SafImportHelper.uniqueDestinationFile(destinationDir, source.name)
                moveFileWithSidecars(source, target)
                movedFiles++
                source.parentFile?.let { touchedParents += it }
            }

            pruneEmptyAncestors(touchedParents, rootPath)
            musicIndexer.scanLibrary()
            movedFiles
        }
    }

    override suspend fun createLibraryFolder(relativePath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sanitized = relativePath.trim().replace('\\', '/').trim('/')
                if (sanitized.isEmpty()) error("Folder name is required.")
                if (sanitized.split('/').any { it.isBlank() || it == "." || it == ".." }) {
                    error("Invalid folder name.")
                }
                storage.createSubdirectory(sanitized)
                musicIndexer.scanLibrary()
                Unit
            }
        }

    private fun parentRelativePath(file: File, libraryRoot: File): String {
        val parent = file.parentFile?.canonicalFile ?: return ""
        val root = libraryRoot.canonicalFile
        val parentPath = parent.path
        val rootPath = root.path
        if (parentPath == rootPath) return ""
        val prefix = rootPath.trimEnd(File.separatorChar) + File.separator
        if (!parentPath.startsWith(prefix)) return ""
        return parentPath.removePrefix(prefix).replace('\\', '/')
    }

    private fun countAudioFiles(directory: File): Int {
        if (!directory.isDirectory) return 0
        return directory.walkTopDown().count { file ->
            file.isFile && file.extension.lowercase() in SupportedAudioFormats.IMPORTABLE_EXTENSIONS
        }
    }

    private fun uniqueDirectory(parent: File, desiredName: String): File {
        var candidate = File(parent, desiredName)
        if (!candidate.exists()) return candidate
        var index = 1
        while (candidate.exists()) {
            candidate = File(parent, "$desiredName ($index)")
            index++
        }
        return candidate
    }

    private fun moveDirectory(source: File, target: File) {
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) return
        // Cross-filesystem fallback.
        source.copyRecursively(target, overwrite = false)
        if (!source.deleteRecursively()) {
            throw IOException("Moved folder contents but could not remove \"${source.name}\".")
        }
    }

    private fun moveFileWithSidecars(source: File, target: File) {
        target.parentFile?.mkdirs()
        val sidecars = sidecarLyricsFiles(source)
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = false)
            if (!source.delete()) {
                target.delete()
                throw IOException("Could not move \"${source.name}\".")
            }
        }
        for (sidecar in sidecars) {
            if (!sidecar.isFile) continue
            val destSidecar = File(target.parentFile, sidecar.name)
            if (sidecar.renameTo(destSidecar)) continue
            sidecar.copyTo(destSidecar, overwrite = true)
            sidecar.delete()
        }
    }

    private fun sidecarLyricsFiles(audio: File): List<File> {
        val parent = audio.parentFile ?: return emptyList()
        val base = audio.nameWithoutExtension
        return listOf(
            File(parent, "$base.lrc"),
            File(parent, "$base.LRC"),
            File(parent, "$base.txt"),
            File(parent, "$base.TXT"),
        )
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
        sidecarLyricsFiles(audio).forEach { sidecar ->
            if (sidecar.isFile) sidecar.delete()
        }
    }
}

object RatingAccessException : Exception("Ratings are a Premium feature.")

object AlbumArtAccessException : Exception("Album art editing is a Premium feature.")
