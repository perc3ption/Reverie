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
