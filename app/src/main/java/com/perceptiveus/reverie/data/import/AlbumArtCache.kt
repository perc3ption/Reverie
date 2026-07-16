package com.perceptiveus.reverie.data.import

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Caches album cover art on disk, keyed by artist+album so tracks in the same
 * album share one file instead of duplicating embedded images.
 */
class AlbumArtCache(context: Context) {

    private val cacheDir: File = File(context.applicationContext.filesDir, ARTWORK_DIR).also { it.mkdirs() }

    /**
     * Returns an absolute path to cached art, or empty if none could be resolved.
     * Reuses an existing cache file when present.
     */
    fun resolveOrCache(
        artist: String,
        album: String,
        audioFile: File,
        embeddedBytes: ByteArray?,
    ): String {
        val key = cacheKey(artist, album)
        val existing = findExisting(key)
        if (existing != null) return existing.absolutePath

        val bytes = embeddedBytes?.takeIf { it.isNotEmpty() }
            ?: readSidecarCover(audioFile.parentFile)
            ?: return ""

        val out = File(cacheDir, "$key.${extensionFor(bytes)}")
        return try {
            out.writeBytes(bytes)
            out.absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    fun deleteOrphans(keepPaths: Set<String>) {
        val keep = keepPaths.filter { it.isNotBlank() }.toSet()
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in keep) {
                file.delete()
            }
        }
    }

    private fun findExisting(key: String): File? {
        EXTENSIONS.forEach { ext ->
            val candidate = File(cacheDir, "$key.$ext")
            if (candidate.exists() && candidate.length() > 0L) return candidate
        }
        return null
    }

    private fun readSidecarCover(folder: File?): ByteArray? {
        if (folder == null || !folder.isDirectory) return null
        for (name in SIDECAR_NAMES) {
            val file = File(folder, name)
            if (file.isFile && file.length() in 1..(MAX_SIDECAR_BYTES)) {
                return runCatching { file.readBytes() }.getOrNull()
            }
        }
        // Case-insensitive pass for mixed folders
        val lowerNames = SIDECAR_NAMES.map { it.lowercase() }.toSet()
        folder.listFiles()?.forEach { file ->
            if (file.isFile && file.name.lowercase() in lowerNames && file.length() in 1..(MAX_SIDECAR_BYTES)) {
                return runCatching { file.readBytes() }.getOrNull()
            }
        }
        return null
    }

    private fun cacheKey(artist: String, album: String): String {
        val raw = "${artist.trim().lowercase()}|${album.trim().lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun extensionFor(bytes: ByteArray): String = when {
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() -> "jpg"
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() -> "png"
        else -> "jpg"
    }

    companion object {
        private const val ARTWORK_DIR = "album_art"
        private const val MAX_SIDECAR_BYTES = 8L * 1024L * 1024L
        private val EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
        private val SIDECAR_NAMES = listOf(
            "cover.jpg",
            "cover.jpeg",
            "cover.png",
            "folder.jpg",
            "folder.jpeg",
            "folder.png",
            "AlbumArt.jpg",
            "AlbumArt.jpeg",
            "front.jpg",
            "front.jpeg",
            "front.png",
        )
    }
}
