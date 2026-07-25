package com.perceptiveus.reverie.data.import

import androidx.room.withTransaction
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.ReverieDatabase
import com.perceptiveus.reverie.data.local.dao.MusicFolderDao
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.entity.MusicFolderEntity
import com.perceptiveus.reverie.data.local.entity.TrackEntity
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

/**
 * Walks the Reverie on-disk library and syncs folders/tracks into Room.
 * Removes database rows for files that no longer exist under the library root.
 *
 * Incremental: files whose size + lastModified match the stored fingerprint skip
 * metadata re-reads, but every scan re-stamps folderId from the on-disk path.
 */
class MusicIndexer(
    private val storage: MusicLibraryStorage,
    private val database: ReverieDatabase,
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
        var tracksUnchanged = 0

        // path → folderId derived from disk layout (source of truth for placement).
        val folderIdByPath = LinkedHashMap<String, String>(filesToIndex.size)

        val existingByPath = trackDao.getAllTracks()
            .filter { it.filePath.isNotBlank() }
            .associateBy { it.filePath }

        val toUpsert = ArrayList<TrackEntity>(filesToIndex.size)

        for ((index, file) in filesToIndex.withIndex()) {
            if (index > 0 && index % 24 == 0) {
                yield()
            }
            try {
                val absolutePath = file.canonicalPath
                val fileSize = file.length()
                val fileModified = file.lastModified()
                val existing = existingByPath[absolutePath]
                val relativeFolderPath = parentRelativePath(file, libraryRoot)
                val folderId = LibraryIds.folderId(relativeFolderPath)
                folderIdByPath[absolutePath] = folderId

                if (existing != null && isUnchanged(existing, fileSize, fileModified)) {
                    tracksUnchanged++
                    continue
                }

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

                toUpsert += TrackEntity(
                    id = existing?.id ?: LibraryIds.trackId(absolutePath),
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    durationMs = metadata.durationMs,
                    filePath = absolutePath,
                    artworkPath = artworkPath,
                    year = metadata.year,
                    genre = metadata.genre,
                    folderId = folderId,
                    dateAdded = existing?.dateAdded ?: now,
                    rating = existing?.rating ?: 0,
                    fileSizeBytes = fileSize,
                    fileModifiedAt = fileModified,
                )
            } catch (_: Exception) {
                skippedUnreadable++
            }
        }

        val scannedPaths = folderIdByPath.keys
        val libraryRootPath = libraryRoot.canonicalPath
        val indexedFolderIds = folderEntities.map { it.id }.toSet()

        val tracksToRemove = trackDao.getAllTracks().filter { track ->
            track.filePath.isBlank() ||
                (track.filePath.startsWith(libraryRootPath) && track.filePath !in scannedPaths)
        }

        database.withTransaction {
            // Upsert folders in place — never REPLACE (REPLACE deletes + SET NULL).
            folderDao.insertAll(folderEntities)

            if (toUpsert.isNotEmpty()) {
                trackDao.insertAll(toUpsert)
            }

            if (tracksToRemove.isNotEmpty()) {
                val removedIds = tracksToRemove.map { it.id }
                trackDao.deleteByIds(removedIds)
                playHistoryDao.deleteByTrackIds(removedIds)
            }

            // Always re-stamp folderId from disk path after all writes/deletes.
            // Heals null folderIds left by older builds and keeps assignments stable.
            for ((path, folderId) in folderIdByPath) {
                trackDao.updateFolderIdByFilePath(path, folderId)
            }

            val staleFolders = folderDao.getAllFolders().filter { it.id !in indexedFolderIds }
            if (staleFolders.isNotEmpty()) {
                val staleIds = staleFolders.map { it.id }
                // NO_ACTION FK: clear any leftover refs before deleting folders.
                trackDao.clearFolderIds(staleIds)
                folderDao.deleteByIds(staleIds)
                // Restore assignments for files that are still on disk.
                for ((path, folderId) in folderIdByPath) {
                    trackDao.updateFolderIdByFilePath(path, folderId)
                }
            }
        }

        albumArtCache.deleteOrphans(
            keepPaths = trackDao.getAllTracks().map { it.artworkPath }.toSet(),
        )

        LibraryScanResult(
            tracksFound = audioFiles.size,
            tracksIndexed = toUpsert.size + tracksUnchanged,
            tracksRemoved = tracksToRemove.size,
            foldersIndexed = folderEntities.size,
            truncatedBySongLimit = truncated,
            skippedUnreadable = skippedUnreadable,
            tracksUnchanged = tracksUnchanged,
        )
    }

    private fun isUnchanged(existing: TrackEntity, fileSize: Long, fileModified: Long): Boolean {
        if (existing.fileSizeBytes <= 0L || existing.fileModifiedAt <= 0L) return false
        return existing.fileSizeBytes == fileSize && existing.fileModifiedAt == fileModified
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
        val parentPath = parent.path
        val rootPath = root.path
        if (parentPath == rootPath) return ""
        val prefix = rootPath.trimEnd(File.separatorChar) + File.separator
        if (!parentPath.startsWith(prefix)) return ""
        return parentPath.removePrefix(prefix).replace('\\', '/')
    }

    companion object {
        private const val README_FILE_NAME = "README.txt"

        val AUDIO_EXTENSIONS = SupportedAudioFormats.IMPORTABLE_EXTENSIONS
    }
}
