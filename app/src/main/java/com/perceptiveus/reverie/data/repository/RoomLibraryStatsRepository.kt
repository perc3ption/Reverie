package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.dao.PlaylistDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.domain.model.LibraryStats
import com.perceptiveus.reverie.domain.model.PlayedItemStat
import java.util.concurrent.TimeUnit

class RoomLibraryStatsRepository(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val playHistoryDao: PlayHistoryDao,
) : LibraryStatsRepository {

    override suspend fun loadStats(
        topTracksLimit: Int,
        topArtistsLimit: Int,
    ): LibraryStats {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)

        return LibraryStats(
            songCount = trackDao.countTracks(),
            artistCount = trackDao.countDistinctArtists(),
            albumCount = trackDao.countDistinctAlbums(),
            playlistCount = playlistDao.count(),
            totalPlays = playHistoryDao.countAll(),
            playsLast7Days = playHistoryDao.countSince(sevenDaysAgo),
            playsLast30Days = playHistoryDao.countSince(thirtyDaysAgo),
            topTracks = playHistoryDao.topPlayedTracks(topTracksLimit).map { row ->
                PlayedItemStat(
                    id = row.id,
                    title = row.title,
                    subtitle = listOf(row.artist, row.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    artworkPath = row.artworkPath,
                    playCount = row.playCount,
                )
            },
            topArtists = playHistoryDao.topPlayedArtists(topArtistsLimit).map { row ->
                PlayedItemStat(
                    id = row.name,
                    title = row.name,
                    playCount = row.playCount,
                )
            },
        )
    }
}
