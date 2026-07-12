package com.perceptiveus.reverie.data.local

import com.perceptiveus.reverie.data.local.dao.UserSettingsDao
import com.perceptiveus.reverie.data.local.entity.UserSettingsEntity

/**
 * Ensures default settings exist on first launch.
 * Library content comes from [com.perceptiveus.reverie.data.import.MusicIndexer], not seed data.
 */
object DatabaseSeeder {

    suspend fun seedSettingsIfNeeded(userSettingsDao: UserSettingsDao) {
        if (userSettingsDao.getSettings() != null) return
        userSettingsDao.upsert(UserSettingsEntity())
    }
}
