package com.perceptiveus.reverie.data.local.entity

/** Query result for folder list with aggregated track counts. */
data class FolderWithCounts(
    val id: String,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
)

/** Query result for artist grouping. */
data class ArtistAggregate(
    val artist: String,
    val trackCount: Int,
    val albumCount: Int,
)

/** Query result for album grouping. */
data class AlbumAggregate(
    val album: String,
    val artist: String,
    val trackCount: Int,
)

/** Query result for playlist list with track count. */
data class PlaylistWithCount(
    val id: String,
    val name: String,
    val createdAt: Long,
    val trackCount: Int,
)
