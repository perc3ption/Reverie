package com.perceptiveus.reverie.data.import

import android.media.MediaMetadataRetriever
import java.io.File

data class AudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** Release year when present in tags; 0 when unknown. */
    val year: Int = 0,
    val genre: String = "",
    /** Embedded cover bytes when present (JPEG/PNG/etc.). */
    val artworkBytes: ByteArray? = null,
)

/**
 * Reads embedded audio tags (and cover art) from local files.
 * Used by [MusicIndexer] when scanning the Reverie library folder.
 */
class AudioMetadataReader {

    fun read(file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty().trim()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val year = parseYear(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),
            )
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                .orEmpty()
                .trim()
            val artwork = runCatching { retriever.embeddedPicture }.getOrNull()?.takeIf { it.isNotEmpty() }

            AudioMetadata(
                title = title.ifBlank { file.nameWithoutExtension },
                artist = artist.ifBlank { UNKNOWN_ARTIST },
                album = album.ifBlank { UNKNOWN_ALBUM },
                durationMs = duration.coerceAtLeast(0L),
                year = year,
                genre = genre,
                artworkBytes = artwork,
            )
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val UNKNOWN_ARTIST = "Unknown Artist"
        private const val UNKNOWN_ALBUM = "Unknown Album"
        private val YEAR_PATTERN = Regex("""\b(19|20)\d{2}\b""")

        /** Parses a year tag or date string like "2020" / "2020-01-15" into a 4-digit year. */
        internal fun parseYear(raw: String?): Int {
            if (raw.isNullOrBlank()) return 0
            val trimmed = raw.trim()
            trimmed.toIntOrNull()?.takeIf { it in 1000..9999 }?.let { return it }
            return YEAR_PATTERN.find(trimmed)?.value?.toIntOrNull() ?: 0
        }
    }
}
