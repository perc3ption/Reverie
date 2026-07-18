package com.perceptiveus.reverie.data.import

import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.dao.MusicFolderDao
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.entity.MusicFolderEntity
import com.perceptiveus.reverie.data.local.entity.TrackEntity
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Walks the Reverie on-disk library and syncs folders/tracks into Room.
 * Removes database rows for files that no longer exist under the library root.
 */
class MusicIndexer(
    private val storage: MusicLibraryStorage,
    private val folderDao: MusicFolderDao,
    private val trackDao: TrackDao,
    private val playHistoryDao: PlayHistoryDao,
    private val metadataReader: AudioMetadataReader,
    private val albumArtCache: AlbumArtCache,
    private val featureAccessChecker: FeatureAccessChecker,
) {

    suspend fun scanLibrary(): LibraryScanResult = withContext(Dispatchers.IO) {
        val libraryRoot = storage.libraryRoot
        if (!libraryRoot.exists()) {
            storage.ensureLibraryRootExists()
        }

        val audioFiles = discoverAudioFiles(libraryRoot).sortedBy { it.canonicalPath }
        val maxSongs = featureAccessChecker.maxSongs()
        val truncated = audioFiles.size > maxSongs
        val filesToIndex = if (truncated) audioFiles.take(maxSongs) else audioFiles

        val folderEntities = buildFolderEntities(filesToIndex, libraryRoot)
        val now = System.currentTimeMillis()
        var skippedUnreadable = 0

        val trackEntities = filesToIndex.mapNotNull { file ->
            try {
                val absolutePath = file.canonicalPath
                val relativeFolderPath = parentRelativePath(file, libraryRoot)
                val existing = trackDao.getByFilePath(absolutePath)
                val metadata = metadataReader.read(file)
                val artworkPath = when {
                    existing != null &&
                        existing.artist == metadata.artist &&
                        existing.album == metadata.album &&
                        existing.artworkPath.isNotBlank() &&
                        File(existing.artworkPath).exists() -> existing.artworkPath
                    else -> albumArtCache.resolveOrCache(
                        artist = metadata.artist,
                        album = metadata.album,
                        audioFile = file,
                        embeddedBytes = metadata.artworkBytes,
                    )
                }

                TrackEntity(
                    id = existing?.id ?: LibraryIds.trackId(absolutePath),
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    durationMs = metadata.durationMs,
                    filePath = absolutePath,
                    artworkPath = artworkPath,
                    year = metadata.year,
                    genre = metadata.genre,
                    folderId = LibraryIds.folderId(relativeFolderPath),
                    dateAdded = existing?.dateAdded ?: now,
                    rating = existing?.rating ?: 0,
                )
            } catch (_: Exception) {
                skippedUnreadable++
                null
            }
        }

        folderDao.insertAll(folderEntities)
        if (trackEntities.isNotEmpty()) {
            trackDao.insertAll(trackEntities)
        }

        val scannedPaths = filesToIndex.map { it.canonicalPath }.toSet()
        val libraryRootPath = libraryRoot.canonicalPath
        val existingTracks = trackDao.getAllTracks()
        val removedTracks = existingTracks.filter { track ->
            track.filePath.isBlank() ||
                (track.filePath.startsWith(libraryRootPath) && track.filePath !in scannedPaths)
        }
        if (removedTracks.isNotEmpty()) {
            val removedIds = removedTracks.map { it.id }
            trackDao.deleteByIds(removedIds)
            playHistoryDao.deleteByTrackIds(removedIds)
        }

        val indexedFolderIds = folderEntities.map { it.id }.toSet()
        val staleFolders = folderDao.getAllFolders().filter { it.id !in indexedFolderIds }
        if (staleFolders.isNotEmpty()) {
            folderDao.deleteByIds(staleFolders.map { it.id })
        }

        albumArtCache.deleteOrphans(
            keepPaths = trackDao.getAllTracks().map { it.artworkPath }.toSet(),
        )

        LibraryScanResult(
            tracksFound = audioFiles.size,
            tracksIndexed = trackEntities.size,
            tracksRemoved = removedTracks.size,
            foldersIndexed = folderEntities.size,
            truncatedBySongLimit = truncated,
            skippedUnreadable = skippedUnreadable,
        )
    }

    private fun discoverAudioFiles(libraryRoot: File): List<File> {
        if (!libraryRoot.isDirectory) return emptyList()
        return libraryRoot.walkTopDown()
            .filter { file ->
                file.isFile &&
                    !file.isHidden &&
                    !file.name.startsWith('.') &&
                    !file.name.equals(README_FILE_NAME, ignoreCase = true) &&
                    file.extension.lowercase() in AUDIO_EXTENSIONS
            }
            .toList()
    }

    private fun buildFolderEntities(audioFiles: List<File>, libraryRoot: File): List<MusicFolderEntity> {
        val relativePaths = mutableSetOf("")
        for (file in audioFiles) {
            var path = parentRelativePath(file, libraryRoot)
            while (true) {
                relativePaths.add(path)
                if (path.isEmpty()) break
                path = if ('/' in path) path.substringBeforeLast('/') else ""
            }
        }

        return relativePaths.sorted().map { relativePath ->
            MusicFolderEntity(
                id = LibraryIds.folderId(relativePath),
                name = LibraryIds.folderDisplayName(relativePath),
                relativePath = relativePath,
            )
        }
    }

    private fun parentRelativePath(file: File, libraryRoot: File): String {
        val parent = file.parentFile?.canonicalFile ?: return ""
        val root = libraryRoot.canonicalFile
        if (parent.path == root.path) return ""
        return parent.path.removePrefix(root.path).trimStart(File.separatorChar)
            .replace('\\', '/')
    }

    companion object {
        private const val README_FILE_NAME = "README.txt"

        val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "alac", "aiff", "aif",
        )
    }
}
