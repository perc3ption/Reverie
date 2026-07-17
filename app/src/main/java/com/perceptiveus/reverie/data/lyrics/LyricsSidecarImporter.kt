package com.perceptiveus.reverie.data.lyrics

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.perceptiveus.reverie.domain.model.LyricsDocument
import java.io.File
import java.io.IOException

sealed class LyricsImportResult {
    data class Success(val document: LyricsDocument) : LyricsImportResult()
    data class Failure(val message: String) : LyricsImportResult()
}

/**
 * Copies a user-picked `.lrc` / `.txt` next to a track and renames it to match
 * the audio basename (e.g. `Song.mp3` → `Song.lrc`).
 */
object LyricsSidecarImporter {

    private val ALLOWED_EXTENSIONS = setOf("lrc", "txt")
    private const val MAX_BYTES = 512L * 1024L

    fun importForTrack(
        context: Context,
        audioPath: String,
        sourceUri: Uri,
    ): LyricsImportResult {
        if (audioPath.isBlank()) {
            return LyricsImportResult.Failure("No song is playing. Play a track first.")
        }

        val audio = File(audioPath)
        if (!audio.isFile) {
            return LyricsImportResult.Failure("Could not find the audio file for this song.")
        }
        val parent = audio.parentFile
            ?: return LyricsImportResult.Failure("Could not find the song folder.")

        val displayName = resolveDisplayName(context, sourceUri)
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension !in ALLOWED_EXTENSIONS) {
            return LyricsImportResult.Failure(
                "Unsupported file type. Please choose a .lrc or .txt lyrics file.",
            )
        }

        val bytes = try {
            context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            return LyricsImportResult.Failure("Could not read the selected file.")
        } ?: return LyricsImportResult.Failure("Could not open the selected file.")

        if (bytes.isEmpty()) {
            return LyricsImportResult.Failure("The selected file is empty.")
        }
        if (bytes.size > MAX_BYTES) {
            return LyricsImportResult.Failure("Lyrics file is too large (max 512 KB).")
        }

        val content = decodeText(bytes)
            ?: return LyricsImportResult.Failure("Could not read the file as text.")

        val parsed = LyricsParser.parse(content)
        if (parsed.lines.isEmpty()) {
            return LyricsImportResult.Failure(
                "No lyrics found in that file. Check that it is a valid .lrc or .txt.",
            )
        }

        val destination = File(parent, "${audio.nameWithoutExtension}.$extension")
        return try {
            parent.mkdirs()
            destination.writeBytes(bytes)
            val document = LyricsLoader.loadForAudioFile(audioPath)
                ?: LyricsParser.parse(content, sourcePath = destination.absolutePath)
            if (document.lines.isEmpty()) {
                LyricsImportResult.Failure("Saved the file, but lyrics could not be loaded.")
            } else {
                LyricsImportResult.Success(document)
            }
        } catch (_: IOException) {
            LyricsImportResult.Failure("Could not save lyrics next to the song.")
        } catch (_: Exception) {
            LyricsImportResult.Failure("Could not save lyrics next to the song.")
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        DocumentFile.fromSingleUri(context, uri)?.name?.let { return it.trim() }
        val segment = uri.lastPathSegment ?: return ""
        return segment.substringAfterLast(':').substringAfterLast('/').trim()
    }

    private fun decodeText(bytes: ByteArray): String? {
        return runCatching { bytes.toString(Charsets.UTF_8) }
            .recoverCatching { bytes.toString(Charsets.ISO_8859_1) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
