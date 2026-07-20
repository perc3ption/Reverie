package com.perceptiveus.reverie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.perceptiveus.reverie.data.local.dao.MusicFolderDao
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.dao.PlaylistDao
import com.perceptiveus.reverie.data.local.dao.SmartPlaylistDao
import com.perceptiveus.reverie.data.local.dao.SongTagDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.dao.UserSettingsDao
import com.perceptiveus.reverie.data.local.entity.MusicFolderEntity
import com.perceptiveus.reverie.data.local.entity.PlayHistoryEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistTrackCrossRef
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistEntity
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistRuleEntity
import com.perceptiveus.reverie.data.local.entity.TagEntity
import com.perceptiveus.reverie.data.local.entity.TrackEntity
import com.perceptiveus.reverie.data.local.entity.TrackTagCrossRef
import com.perceptiveus.reverie.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        MusicFolderEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        TagEntity::class,
        TrackTagCrossRef::class,
        PlayHistoryEntity::class,
        UserSettingsEntity::class,
        SmartPlaylistEntity::class,
        SmartPlaylistRuleEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class ReverieDatabase : RoomDatabase() {

    abstract fun musicFolderDao(): MusicFolderDao
    abstract fun trackDao(): TrackDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songTagDao(): SongTagDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun smartPlaylistDao(): SmartPlaylistDao

    companion object {
        private const val DATABASE_NAME = "reverie.db"

        @Volatile
        private var instance: ReverieDatabase? = null

        fun getInstance(context: Context): ReverieDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReverieDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                )
                    .build().also { instance = it }
            }
        }
    }
}
