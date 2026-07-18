package com.perceptiveus.reverie.data.import

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import java.io.File

/**
 * Editable audio tag fields that are written into the file itself
 * (so they survive copying the file outside Reverie).
 */
data class EditableTrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val year: Int,
    val genre: String,
)

/**
 * Writes embedded tags into local audio files under the Reverie library.
 */
class AudioMetadataWriter {

    init {
        // Required for jaudiotagger on Android's Java runtime.
        TagOptionSingleton.getInstance().isAndroid = true
    }

    fun write(file: File, metadata: EditableTrackMetadata) {
        require(file.isFile && file.canWrite()) {
            "File is not writable: ${file.absolutePath}"
        }
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        tag.setField(FieldKey.TITLE, metadata.title.trim())
        tag.setField(FieldKey.ARTIST, metadata.artist.trim())
        tag.setField(FieldKey.ALBUM, metadata.album.trim())
        if (metadata.year > 0) {
            tag.setField(FieldKey.YEAR, metadata.year.toString())
        } else {
            tag.deleteField(FieldKey.YEAR)
        }
        val genre = metadata.genre.trim()
        if (genre.isNotEmpty()) {
            tag.setField(FieldKey.GENRE, genre)
        } else {
            tag.deleteField(FieldKey.GENRE)
        }
        audioFile.commit()
    }
}
