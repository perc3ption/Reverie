package com.perceptiveus.reverie.data.import

import android.content.Context
import android.net.Uri
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.storage.ImportMode
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
import com.perceptiveus.reverie.domain.model.ImportResult
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MusicImportRepository(
    private val context: Context,
    private val storage: MusicLibraryStorage,
    private val trackDao: TrackDao,
    private val featureAccessChecker: FeatureAccessChecker,
    private val scanLibrary: suspend () -> LibraryScanResult,
) {

    suspend fun importFiles(
        uris: List<Uri>,
        mode: ImportMode,
        destinationRelativePath: String,
    ): ImportResult = withContext(Dispatchers.IO) {
        storage.ensureLibraryRootExists()
        val destinationRoot = storage.createSubdirectory(destinationRelativePath)

        val audioUris = uris.filter { SafImportHelper.isAudioUri(context, it) }
        val capacity = remainingCapacity()
        val allowed = if (capacity == Int.MAX_VALUE) audioUris else audioUris.take(capacity)
        val truncated = audioUris.size > allowed.size

        var imported = 0
        var failed = 0
        var moveFailures = 0

        for (uri in allowed) {
            try {
                SafImportHelper.takePersistableReadPermission(context.contentResolver, uri)
                val displayName = SafImportHelper.resolveDisplayName(context, uri)
                val targetFile = SafImportHelper.uniqueDestinationFile(destinationRoot, displayName)
                SafImportHelper.copyUriToFile(context.contentResolver, uri, targetFile)
                imported++

                if (mode == ImportMode.MOVE) {
                    val deleted = SafImportHelper.deleteSourceUri(context.contentResolver, uri)
                    if (!deleted) moveFailures++
                }
            } catch (_: Exception) {
                failed++
            }
        }

        val scanResult = if (imported > 0) scanLibrary() else null
        ImportResult(
            filesAttempted = allowed.size,
            filesImported = imported,
            filesFailed = failed,
            moveDeleteFailures = moveFailures,
            truncatedBySongLimit = truncated,
            scanResult = scanResult,
        )
    }

    suspend fun importFolderTree(
        treeUri: Uri,
        mode: ImportMode,
        destinationRelativePath: String,
    ): ImportResult = withContext(Dispatchers.IO) {
        storage.ensureLibraryRootExists()
        SafImportHelper.takePersistableReadWritePermission(context.contentResolver, treeUri)

        val destinationRoot = storage.createSubdirectory(destinationRelativePath)
        val audioDocuments = SafImportHelper.collectAudioFromTree(context, treeUri)

        val capacity = remainingCapacity()
        val allowed = if (capacity == Int.MAX_VALUE) audioDocuments else audioDocuments.take(capacity)
        val truncated = audioDocuments.size > allowed.size

        var imported = 0
        var failed = 0
        var moveFailures = 0

        for (document in allowed) {
            try {
                val targetFile = resolveTreeTargetFile(destinationRoot, document.relativePath)
                targetFile.parentFile?.mkdirs()
                SafImportHelper.copyUriToFile(context.contentResolver, document.uri, targetFile)
                imported++

                if (mode == ImportMode.MOVE) {
                    val deleted = SafImportHelper.deleteSourceUri(context.contentResolver, document.uri)
                    if (!deleted) moveFailures++
                }
            } catch (_: Exception) {
                failed++
            }
        }

        val scanResult = if (imported > 0) scanLibrary() else null
        ImportResult(
            filesAttempted = allowed.size,
            filesImported = imported,
            filesFailed = failed,
            moveDeleteFailures = moveFailures,
            truncatedBySongLimit = truncated,
            scanResult = scanResult,
        )
    }

    private suspend fun remainingCapacity(): Int {
        val maxSongs = featureAccessChecker.maxSongs()
        if (maxSongs == Int.MAX_VALUE) return Int.MAX_VALUE
        val current = trackDao.countTracks()
        return (maxSongs - current).coerceAtLeast(0)
    }

    private fun resolveTreeTargetFile(destinationRoot: File, relativePath: String): File {
        val segments = relativePath.split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) {
            throw IOException("Invalid audio path")
        }
        var current = destinationRoot
        for (segment in segments.dropLast(1)) {
            current = File(current, segment)
        }
        return SafImportHelper.uniqueDestinationFile(current, segments.last())
    }
}
