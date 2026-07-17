package com.perceptiveus.reverie.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "music_folders",
    indices = [Index(value = ["relativePath"], unique = true)],
)
data class MusicFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Path relative to the Reverie library root, e.g. "Rock/Album 1". Empty for root. */
    val relativePath: String = "",
    /** Reserved for SAF import source URI. */
    val sourceUri: String = "",
    val importedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = MusicFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("folderId"), Index("artist"), Index("album"), Index("filePath")],
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long = 0L,
    /** Content URI or file path; populated when import is implemented. */
    val filePath: String = "",
    /** Absolute path to cached album art JPEG/PNG; empty if none. */
    val artworkPath: String = "",
    /** Release year from tags; 0 when unknown. */
    val year: Int = 0,
    val genre: String = "",
    val folderId: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    /** Absolute path to playlist cover image; empty when unset. */
    val coverPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackId")],
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val position: Int,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "track_tags",
    primaryKeys = ["trackId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class TrackTagCrossRef(
    val trackId: String,
    val tagId: String,
)

@Entity(
    tableName = "play_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackId"), Index("playedAt")],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis(),
)

/** Single-row user preferences table (id is always [SETTINGS_ROW_ID]). */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = SETTINGS_ROW_ID,
    val displayName: String = "Listener",
    val themePreference: String = "SYSTEM",
) {
    companion object {
        const val SETTINGS_ROW_ID = 1
    }
}
