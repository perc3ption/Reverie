package com.perceptiveus.reverie.data.import

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

internal data class AudioDocument(
    val uri: Uri,
    val displayName: String,
    /** Relative path within the picked tree, including filename. Empty for single-file picks. */
    val relativePath: String,
)

internal object SafImportHelper {

    fun takePersistableReadPermission(contentResolver: ContentResolver, uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Some providers do not allow persistable permissions.
        }
    }

    fun takePersistableReadWritePermission(contentResolver: ContentResolver, uri: Uri) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            takePersistableReadPermission(contentResolver, uri)
        }
    }

    fun resolveDisplayName(context: Context, uri: Uri): String {
        DocumentFile.fromSingleUri(context, uri)?.name?.let { return sanitizeFileName(it) }
        val lastSegment = uri.lastPathSegment ?: "audio"
        return sanitizeFileName(lastSegment.substringAfterLast(':'))
    }

    /**
     * Collects audio files under a SAF tree, preserving the picked folder name and
     * relative subfolder paths (e.g. `Album/Disc 1/track.mp3`).
     */
    fun collectAudioFromTree(context: Context, treeUri: Uri): List<AudioDocument> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val rootFolderName = sanitizeFileName(
            root.name?.takeIf { it.isNotBlank() } ?: "Imported Folder",
        )
        val results = mutableListOf<AudioDocument>()
        // Start relative paths with the picked folder so it is created under the destination.
        walkTree(root, relativeDirectory = rootFolderName, results = results)
        return results
    }

    private fun walkTree(
        node: DocumentFile,
        relativeDirectory: String,
        results: MutableList<AudioDocument>,
    ) {
        val children = node.listFiles()
        for (child in children) {
            val childName = sanitizeFileName(child.name ?: continue)
            if (childName.isEmpty()) continue
            if (child.isDirectory) {
                val nextDirectory = if (relativeDirectory.isEmpty()) {
                    childName
                } else {
                    "$relativeDirectory/$childName"
                }
                walkTree(child, nextDirectory, results)
            } else if (child.isFile && isAudioFileName(childName)) {
                val relativePath = if (relativeDirectory.isEmpty()) {
                    childName
                } else {
                    "$relativeDirectory/$childName"
                }
                results.add(
                    AudioDocument(
                        uri = child.uri,
                        displayName = childName,
                        relativePath = relativePath,
                    ),
                )
            }
        }
    }

    fun copyUriToFile(contentResolver: ContentResolver, sourceUri: Uri, destination: File) {
        destination.parentFile?.mkdirs()
        contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Unable to read $sourceUri")
    }

    fun deleteSourceUri(contentResolver: ContentResolver, uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (_: Exception) {
            false
        }
    }

    fun uniqueDestinationFile(directory: File, desiredName: String): File {
        val safeName = sanitizeFileName(desiredName)
        if (safeName.isEmpty()) {
            return uniqueDestinationFile(directory, "audio.mp3")
        }
        var candidate = File(directory, safeName)
        if (!candidate.exists()) return candidate

        val extension = safeName.substringAfterLast('.', "")
        val base = safeName.substringBeforeLast('.')
        var index = 1
        while (candidate.exists()) {
            val nextName = if (extension.isNotEmpty()) {
                "$base ($index).$extension"
            } else {
                "$base ($index)"
            }
            candidate = File(directory, nextName)
            index++
        }
        return candidate
    }

    fun isAudioFileName(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in MusicIndexer.AUDIO_EXTENSIONS
    }

    fun isAudioUri(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)
        if (mime != null && (mime.startsWith("audio/") || mime == "application/ogg")) {
            return true
        }
        return isAudioFileName(resolveDisplayName(context, uri))
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.replace('\\', '_').replace('/', '_').trim()
    }
}
