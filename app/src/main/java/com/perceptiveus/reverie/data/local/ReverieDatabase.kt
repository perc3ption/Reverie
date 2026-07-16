package com.perceptiveus.reverie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.perceptiveus.reverie.data.local.dao.MusicFolderDao
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.dao.PlaylistDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.dao.UserSettingsDao
import com.perceptiveus.reverie.data.local.entity.MusicFolderEntity
import com.perceptiveus.reverie.data.local.entity.PlayHistoryEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistTrackCrossRef
import com.perceptiveus.reverie.data.local.entity.TrackEntity
import com.perceptiveus.reverie.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        MusicFolderEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlayHistoryEntity::class,
        UserSettingsEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ReverieDatabase : RoomDatabase() {

    abstract fun musicFolderDao(): MusicFolderDao
    abstract fun trackDao(): TrackDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun userSettingsDao(): UserSettingsDao

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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
        }
    }
}
