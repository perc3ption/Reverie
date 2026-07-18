package com.perceptiveus.reverie.data.storage

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Manages the on-disk Reverie music library rooted at:
 *
 * `Android/media/<package>/Reverie/`
 *
 * This location is user-visible in many file managers and over USB/MTP, while
 * remaining app-scoped and Play-Store-friendly. Falls back to app-private
 * external storage if media dirs are unavailable.
 */
class MusicLibraryStorage(
    private val context: Context,
) {

    /** Absolute path to the Reverie library root directory. */
    val libraryRoot: File by lazy { resolveLibraryRoot() }

    /**
     * Creates the library root on first launch (and on every startup, idempotently).
     * Also writes a short README for power users who browse the folder directly.
     */
    @Throws(IOException::class)
    fun initialize(): File {
        val root = ensureLibraryRootExists()
        ensureWelcomeReadme(root)
        return root
    }

    /** Creates [libraryRoot] if it does not exist. */
    @Throws(IOException::class)
    fun ensureLibraryRootExists(): File {
        val root = libraryRoot
        if (root.exists() && !root.isDirectory) {
            throw IOException("Library path exists but is not a directory: ${root.absolutePath}")
        }
        if (!root.exists() && !root.mkdirs()) {
            throw IOException("Failed to create library root: ${root.absolutePath}")
        }
        return root
    }

    /**
     * Resolves [relativePath] under [libraryRoot]. Rejects paths that escape the library.
     * Used by import and indexing to safely target subfolders.
     */
    fun resolveFile(relativePath: String): File {
        val sanitized = relativePath.trim().replace('\\', '/').trim('/')
        val target = if (sanitized.isEmpty()) {
            libraryRoot
        } else {
            File(libraryRoot, sanitized)
        }
        return target.canonicalFile.also { canonical ->
            val rootPath = libraryRoot.canonicalPath
            if (!canonical.path.startsWith(rootPath)) {
                throw IllegalArgumentException("Path escapes library root: $relativePath")
            }
        }
    }

    /** Creates a subdirectory under [libraryRoot], including any missing parents. */
    @Throws(IOException::class)
    fun createSubdirectory(relativePath: String): File {
        val dir = resolveFile(relativePath)
        if (dir.exists() && !dir.isDirectory) {
            throw IOException("Path exists but is not a directory: ${dir.absolutePath}")
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Failed to create directory: ${dir.absolutePath}")
        }
        return dir
    }

    /** Lists immediate child directories of [libraryRoot] or of [relativePath] if provided. */
    fun listSubdirectories(relativePath: String = ""): List<File> {
        val parent = resolveFile(relativePath)
        if (!parent.isDirectory) return emptyList()
        return parent.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith('.') }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    /** Human-readable location label for settings / about screens. */
    fun


























































































































































































































































































































































































































































































































































































            displayPath(): String = libraryRoot.absolutePath

    /** All subdirectory paths relative to [libraryRoot], including the root (empty string). */
    fun listAllSubdirectoryPaths(): List<String> {
        val paths = mutableListOf("")
        fun walk(relativePath: String) {
            for (directory in listSubdirectories(relativePath)) {
                val childPath = if (relativePath.isEmpty()) {
                    directory.name
                } else {
                    "$relativePath/${directory.name}"
                }
                paths.add(childPath)
                walk(childPath)
            }
        }
        walk("")
        return paths
    }

    fun destinationLabel(relativePath: String): String {
        return if (relativePath.isEmpty()) {
            "$LIBRARY_FOLDER_NAME (root)"
        } else {
            "$LIBRARY_FOLDER_NAME/$relativePath"
        }
    }

    private fun resolveLibraryRoot(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()
            ?: context.getExternalFilesDir(null)
            ?: context.filesDir
        return File(mediaDir, LIBRARY_FOLDER_NAME)
    }

    private fun ensureWelcomeReadme(root: File) {
        val readme = File(root, README_FILE_NAME)
        if (readme.exists()) return
        readme.writeText(
            """
            Reverie Music Library
            =====================

            This folder is Reverie's home for your music collection.

            Power users:
            - Copy folders and audio files here via USB or a file manager.
            - Keep your folder structure — Reverie will scan it.
            - Then open Reverie and tap "Scan Library" on the Import screen.

            In-app import:
            - Use Import Music to copy or move files from elsewhere on your device
              into this folder.

            Supported formats include common audio types (MP3, FLAC, AAC, WAV, OGG, etc.).
            """.trimIndent() + "\n",
        )
    }

    companion object {
        const val LIBRARY_FOLDER_NAME = "Reverie"
        private const val README_FILE_NAME = "README.txt"
    }
}
