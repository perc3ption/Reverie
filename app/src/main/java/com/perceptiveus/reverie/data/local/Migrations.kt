package com.perceptiveus.reverie.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE music_folders ADD COLUMN relativePath TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
            "UPDATE music_folders SET relativePath = id WHERE relativePath = ''",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_music_folders_relativePath " +
                "ON music_folders(relativePath)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_tracks_filePath ON tracks(filePath)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE tracks ADD COLUMN artworkPath TEXT NOT NULL DEFAULT ''",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN year INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tracks ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS song_tags (
                trackId TEXT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(trackId, name),
                FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_song_tags_trackId ON song_tags(trackId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_song_tags_name ON song_tags(name)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tags (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS track_tags (
                trackId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                PRIMARY KEY(trackId, tagId),
                FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_track_tags_tagId ON track_tags(tagId)")

        // Move any legacy song_tags rows into the shared tags model.
        db.execSQL(
            """
            INSERT OR IGNORE INTO tags (id, name, createdAt)
            SELECT lower(hex(randomblob(16))), name, MIN(createdAt)
            FROM song_tags
            GROUP BY name COLLATE NOCASE
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO track_tags (trackId, tagId)
            SELECT st.trackId, t.id
            FROM song_tags st
            INNER JOIN tags t ON t.name = st.name COLLATE NOCASE
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE IF EXISTS song_tags")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE playlists ADD COLUMN description TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
            "ALTER TABLE playlists ADD COLUMN coverPath TEXT NOT NULL DEFAULT ''",
        )
    }
}

/** Replace isFavorite with rating (0–5); former favorites become 5 stars. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tracks_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT NOT NULL,
                durationMs INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                artworkPath TEXT NOT NULL,
                year INTEGER NOT NULL,
                genre TEXT NOT NULL,
                folderId TEXT,
                dateAdded INTEGER NOT NULL,
                rating INTEGER NOT NULL,
                FOREIGN KEY(folderId) REFERENCES music_folders(id) ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO tracks_new (
                id, title, artist, album, durationMs, filePath, artworkPath,
                year, genre, folderId, dateAdded, rating
            )
            SELECT
                id, title, artist, album, durationMs, filePath, artworkPath,
                year, genre, folderId, dateAdded,
                CASE WHEN isFavorite = 1 THEN 5 ELSE 0 END
            FROM tracks
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE tracks")
        db.execSQL("ALTER TABLE tracks_new RENAME TO tracks")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_folderId ON tracks(folderId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_artist ON tracks(artist)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_album ON tracks(album)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_filePath ON tracks(filePath)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS smart_playlists (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                sortOrder TEXT NOT NULL,
                trackLimit INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS smart_playlist_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                playlistId TEXT NOT NULL,
                field TEXT NOT NULL,
                operator TEXT NOT NULL,
                value TEXT NOT NULL,
                valueSecondary TEXT NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(playlistId) REFERENCES smart_playlists(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_smart_playlist_rules_playlistId ON smart_playlist_rules(playlistId)",
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_settings ADD COLUMN audioFxJson TEXT NOT NULL DEFAULT ''",
        )
    }
}
