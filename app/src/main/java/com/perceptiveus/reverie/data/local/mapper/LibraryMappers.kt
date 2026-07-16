package com.perceptiveus.reverie.data.local.mapper

import com.perceptiveus.reverie.core.settings.AppThemePreference
import com.perceptiveus.reverie.data.local.entity.AlbumAggregate
import com.perceptiveus.reverie.data.local.entity.ArtistAggregate
import com.perceptiveus.reverie.data.local.entity.FolderWithCounts
import com.perceptiveus.reverie.data.local.entity.PlaylistWithCount
import com.perceptiveus.reverie.data.local.entity.TrackEntity
import com.perceptiveus.reverie.data.local.entity.UserSettingsEntity
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Track

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    filePath = filePath,
    artworkPath = artworkPath,
)

fun FolderWithCounts.toDomain(): MusicFolder = MusicFolder(
    id = id,
    name = name,
    songCount = songCount,
    albumCount = albumCount,
)

fun ArtistAggregate.toDomain(): Artist = Artist(
    id = artist.lowercase(),
    name = artist,
    albumCount = albumCount,
    trackCount = trackCount,
)

fun AlbumAggregate.toDomain(): Album = Album(
    id = "${artist.lowercase()}::${album.lowercase()}",
    title = album,
    artist = artist,
    trackCount = trackCount,
)

fun PlaylistWithCount.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    trackCount = trackCount,
    createdAt = createdAt,
)

fun UserSettingsEntity.toThemePreference(): AppThemePreference =
    runCatching { AppThemePreference.valueOf(themePreference) }
        .getOrDefault(AppThemePreference.SYSTEM)
