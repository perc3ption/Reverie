package com.perceptiveus.reverie.data.import

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

sealed class AlbumArtImportResult {
    data class Success(val artworkPath: String) : AlbumArtImportResult()
    data class Failure(val message: String) : AlbumArtImportResult()
}

/**
 * Imports a user-picked image as album art into [AlbumArtCache]
 * (same pick-and-copy idea as lyrics / playlist covers).
 */
object AlbumArtImporter {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private const val MAX_BYTES = 8L * 1024L * 1024L

    fun importForAlbum(
        context: Context,
        artist: String,
        album: String,
        sourceUri: Uri,
        albumArtCache: AlbumArtCache,
    ): AlbumArtImportResult {
        val displayName = DocumentFile.fromSingleUri(context, sourceUri)?.name.orEmpty()
        val extension = displayName.substringAfterLast('.', "").lowercase().ifBlank {
            when (context.contentResolver.getType(sourceUri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                "image/bmp" -> "bmp"
                else -> "jpg"
            }
        }
        if (extension !in IMAGE_EXTENSIONS) {
            return AlbumArtImportResult.Failure("Unsupported image type. Choose a JPG, PNG, or WEBP file.")
        }

        val bytes = try {
            context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
        if (bytes == null || bytes.isEmpty()) {
            return AlbumArtImportResult.Failure("Could not read the selected image.")
        }
        if (bytes.size > MAX_BYTES) {
            return AlbumArtImportResult.Failure("Image is too large (max 8 MB).")
        }

        val path = albumArtCache.putOrReplace(artist = artist, album = album, bytes = bytes)
        if (path.isBlank()) {
            return AlbumArtImportResult.Failure("Could not save album art.")
        }
        return AlbumArtImportResult.Success(path)
    }
}
