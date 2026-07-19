package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.domain.model.LibraryStats

interface LibraryStatsRepository {
    suspend fun loadStats(
        topTracksLimit: Int = 5,
        topArtistsLimit: Int = 5,
    ): LibraryStats
}
