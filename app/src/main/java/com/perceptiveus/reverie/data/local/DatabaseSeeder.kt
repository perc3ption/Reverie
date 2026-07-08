package com.perceptiveus.reverie.data.local

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

/**
 * Seeds starter data on first database creation so the UI is usable before import is built.
 * Real imports will insert rows via SAF + metadata extraction.
 */
object DatabaseSeeder {

    suspend fun seedIfEmpty(
        folderDao: MusicFolderDao,
        trackDao: TrackDao,
        playlistDao: PlaylistDao,
        playHistoryDao: PlayHistoryDao,
        userSettingsDao: UserSettingsDao,
    ) {
        if (folderDao.count() > 0) return

        val folders = listOf(
            MusicFolderEntity("f1", "Downloaded Music"),
            MusicFolderEntity("f2", "FLAC Collection"),
            MusicFolderEntity("f3", "Anime & OST"),
            MusicFolderEntity("f4", "Rock Classics"),
            MusicFolderEntity("f5", "Chill & Lo-Fi"),
        )
        folderDao.insertAll(folders)

        val now = System.currentTimeMillis()
        val tracks = listOf(
            TrackEntity("1", "Afterglow", "Echos", "Silent Skies", 293_000, folderId = "f1", dateAdded = now),
            TrackEntity("2", "Nocturne", "Lune", "Nightfall", 245_000, folderId = "f2", dateAdded = now),
            TrackEntity("3", "Midnight Drive", "Tokyo Wanderer", "Neon Streets", 312_000, folderId = "f3", dateAdded = now),
            TrackEntity("4", "Starlight", "Nova", "Cosmos", 198_000, folderId = "f4", dateAdded = now),
            TrackEntity("5", "Echoes", "Reverie", "Dreamscape", 267_000, folderId = "f5", dateAdded = now),
        )
        trackDao.insertAll(tracks)

        tracks.forEachIndexed { index, track ->
            playHistoryDao.insert(
                PlayHistoryEntity(trackId = track.id, playedAt = now - index * 60_000L),
            )
        }

        val playlists = listOf(
            PlaylistEntity("p1", "Late Night", createdAt = now - 86_400_000),
            PlaylistEntity("p2", "Focus Flow", createdAt = now - 172_800_000),
        )
        playlistDao.insert(playlists[0])
        playlistDao.insert(playlists[1])
        playlistDao.insertPlaylistTracks(
            listOf(
                PlaylistTrackCrossRef("p1", "1", 0),
                PlaylistTrackCrossRef("p1", "2", 1),
                PlaylistTrackCrossRef("p1", "3", 2),
                PlaylistTrackCrossRef("p2", "4", 0),
                PlaylistTrackCrossRef("p2", "5", 1),
            ),
        )

        userSettingsDao.upsert(UserSettingsEntity())
    }
}
