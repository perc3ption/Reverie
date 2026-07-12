package com.perceptiveus.reverie.data.import

import android.media.MediaMetadataRetriever
import java.io.File

data class AudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

/**
 * Reads embedded audio tags from local files.
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

            AudioMetadata(
                title = title.ifBlank { file.nameWithoutExtension },
                artist = artist.ifBlank { UNKNOWN_ARTIST },
                album = album.ifBlank { UNKNOWN_ALBUM },
                durationMs = duration.coerceAtLeast(0L),
            )
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val UNKNOWN_ARTIST = "Unknown Artist"
        private const val UNKNOWN_ALBUM = "Unknown Album"
    }
}
