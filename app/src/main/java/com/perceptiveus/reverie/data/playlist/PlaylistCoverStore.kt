package com.perceptiveus.reverie.data.playlist

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

/** Copies a user-picked image into app-private storage for playlist covers. */
object PlaylistCoverStore {

    private const val DIR_NAME = "playlist_covers"
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    fun importCover(context: Context, playlistId: String, sourceUri: Uri): String {
        val displayName = DocumentFile.fromSingleUri(context, sourceUri)?.name.orEmpty()
        val extension = displayName.substringAfterLast('.', "").lowercase().ifBlank {
            when (context.contentResolver.getType(sourceUri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }
        }
        if (extension !in IMAGE_EXTENSIONS) {
            throw IOException("Unsupported image type. Choose a JPG, PNG, or WEBP file.")
        }

        val dir = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
        // Clear prior covers for this playlist (any extension).
        dir.listFiles()
            ?.filter { it.nameWithoutExtension == playlistId }
            ?.forEach { it.delete() }

        val destination = File(dir, "$playlistId.$extension")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Could not read the selected image.")

        if (!destination.exists() || destination.length() == 0L) {
            throw IOException("Could not save playlist cover.")
        }
        return destination.absolutePath
    }

    fun deleteCover(coverPath: String) {
        if (coverPath.isBlank()) return
        runCatching { File(coverPath).takeIf { it.exists() }?.delete() }
    }
}
