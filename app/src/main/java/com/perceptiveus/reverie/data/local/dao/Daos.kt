package com.perceptiveus.reverie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.perceptiveus.reverie.data.local.entity.AlbumAggregate
import com.perceptiveus.reverie.data.local.entity.ArtistAggregate
import com.perceptiveus.reverie.data.local.entity.FolderWithCounts
import com.perceptiveus.reverie.data.local.entity.MusicFolderEntity
import com.perceptiveus.reverie.data.local.entity.NamedPlayStatRow
import com.perceptiveus.reverie.data.local.entity.PlayHistoryEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistEntity
import com.perceptiveus.reverie.data.local.entity.PlaylistTrackCrossRef
import com.perceptiveus.reverie.data.local.entity.PlaylistWithCount
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistEntity
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistRuleEntity
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistWithRuleCount
import com.perceptiveus.reverie.data.local.entity.TagEntity
import com.perceptiveus.reverie.data.local.entity.TrackEntity
import com.perceptiveus.reverie.data.local.entity.TrackPlayCountRow
import com.perceptiveus.reverie.data.local.entity.TrackPlayStatRow
import com.perceptiveus.reverie.data.local.entity.TrackTagCrossRef
import com.perceptiveus.reverie.data.local.entity.TrackTimestampRow
import com.perceptiveus.reverie.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicFolderDao {

    @Query(
        """
        SELECT f.id, f.name, f.relativePath,
               COUNT(t.id) AS songCount,
               COUNT(DISTINCT t.album) AS albumCount
        FROM music_folders f
        LEFT JOIN tracks t ON t.folderId = f.id
        GROUP BY f.id
        ORDER BY f.relativePath ASC
        """,
    )
    fun observeFoldersWithCounts(): Flow<List<FolderWithCounts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<MusicFolderEntity>)

    @Query("SELECT * FROM music_folders")
    suspend fun getAllFolders(): List<MusicFolderEntity>

    @Query("DELETE FROM music_folders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM music_folders")
    suspend fun count(): Int
}

@Dao
interface TrackDao {

    @Query("SELECT COUNT(*) FROM tracks")
    fun observeSongCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun countTracks(): Int

    @Query("SELECT COUNT(DISTINCT artist) FROM tracks WHERE artist != ''")
    suspend fun countDistinctArtists(): Int

    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT album, artist FROM tracks GROUP BY album, artist
        ) AS album_groups
        """,
    )
    suspend fun countDistinctAlbums(): Int

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT artist,
               COUNT(*) AS trackCount,
               COUNT(DISTINCT album) AS albumCount
        FROM tracks
        GROUP BY artist
        ORDER BY artist ASC
        """,
    )
    fun observeArtists(): Flow<List<ArtistAggregate>>

    @Query(
        """
        SELECT album, artist,
               COUNT(*) AS trackCount
        FROM tracks
        GROUP BY album, artist
        ORDER BY album ASC
        """,
    )
    fun observeAlbums(): Flow<List<AlbumAggregate>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, MAX(playedAt) AS latestPlayed
            FROM play_history
            GROUP BY trackId
        ) recent ON recent.trackId = t.id
        ORDER BY recent.latestPlayed DESC
        LIMIT :limit
        """,
    )
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<TrackEntity>>

    @Upsert
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Upsert
    suspend fun insert(track: TrackEntity)

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE filePath = :path LIMIT 1")
    suspend fun getByFilePath(path: String): TrackEntity?

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TrackEntity?

    @Query("UPDATE tracks SET rating = :rating WHERE id = :trackId")
    suspend fun updateRating(trackId: String, rating: Int)

    @Query("UPDATE tracks SET artworkPath = :artworkPath WHERE id = :trackId")
    suspend fun updateArtworkPath(trackId: String, artworkPath: String)

    @Query(
        """
        UPDATE tracks SET artworkPath = :artworkPath
        WHERE artist = :artist AND album = :album
        """,
    )
    suspend fun updateArtworkPathForAlbum(artist: String, album: String, artworkPath: String)
}

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insert(entry: PlayHistoryEntity)

    @Query("DELETE FROM play_history WHERE trackId IN (:trackIds)")
    suspend fun deleteByTrackIds(trackIds: List<String>)

    @Query("DELETE FROM play_history WHERE playedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM play_history")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM play_history WHERE playedAt >= :sinceMs")
    suspend fun countSince(sinceMs: Long): Int

    @Query(
        """
        SELECT t.id, t.title, t.artist, t.album, t.artworkPath,
               COUNT(h.id) AS playCount
        FROM play_history h
        INNER JOIN tracks t ON t.id = h.trackId
        GROUP BY t.id
        ORDER BY playCount DESC, t.title ASC
        LIMIT :limit
        """,
    )
    suspend fun topPlayedTracks(limit: Int): List<TrackPlayStatRow>

    @Query(
        """
        SELECT t.artist AS name, COUNT(h.id) AS playCount
        FROM play_history h
        INNER JOIN tracks t ON t.id = h.trackId
        WHERE t.artist != ''
        GROUP BY t.artist
        ORDER BY playCount DESC, t.artist ASC
        LIMIT :limit
        """,
    )
    suspend fun topPlayedArtists(limit: Int): List<NamedPlayStatRow>

    @Query(
        """
        SELECT trackId, COUNT(*) AS playCount
        FROM play_history
        GROUP BY trackId
        """,
    )
    suspend fun playCountsByTrack(): List<TrackPlayCountRow>

    @Query(
        """
        SELECT trackId, MAX(playedAt) AS timestamp
        FROM play_history
        GROUP BY trackId
        """,
    )
    suspend fun lastPlayedAtByTrack(): List<TrackTimestampRow>
}

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, p.description, p.coverPath, p.createdAt,
               COUNT(pt.trackId) AS trackCount
        FROM playlists p
        LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """,
    )
    fun observePlaylistsWithCounts(): Flow<List<PlaylistWithCount>>

    @Query(
        """
        SELECT p.id, p.name, p.description, p.coverPath, p.createdAt,
               COUNT(all_pt.trackId) AS trackCount
        FROM playlists p
        INNER JOIN playlist_tracks selected_pt ON selected_pt.playlistId = p.id
        LEFT JOIN playlist_tracks all_pt ON all_pt.playlistId = p.id
        WHERE selected_pt.trackId = :trackId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """,
    )
    fun observePlaylistsForTrack(trackId: String): Flow<List<PlaylistWithCount>>

    @Query(
        """
        SELECT p.id, p.name, p.description, p.coverPath, p.createdAt,
               (SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = p.id) AS trackCount
        FROM playlists p
        WHERE p.id = :playlistId
        LIMIT 1
        """,
    )
    fun observePlaylist(playlistId: String): Flow<PlaylistWithCount?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlaylistEntity?

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """,
    )
    fun observeTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrack(playlistId: String, trackId: String)

    @Query("SELECT COUNT(*) FROM playlists")
    fun observePlaylistCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPositionForPlaylist(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun trackCountInPlaylist(playlistId: String, trackId: String): Int

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(refs: List<PlaylistTrackCrossRef>)

    @Transaction
    suspend fun insertPlaylistWithTracks(playlist: PlaylistEntity, tracks: List<PlaylistTrackCrossRef>) {
        insert(playlist)
        if (tracks.isNotEmpty()) {
            insertPlaylistTracks(tracks)
        }
    }

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun count(): Int
}

@Dao
interface SongTagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN track_tags tt ON tt.tagId = t.id
        WHERE tt.trackId = :trackId
        ORDER BY t.name COLLATE NOCASE ASC
        """,
    )
    fun observeTagsForTrack(trackId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM track_tags")
    suspend fun getAllTrackTags(): List<TrackTagCrossRef>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackTag(ref: TrackTagCrossRef)

    @Query("DELETE FROM track_tags WHERE trackId = :trackId AND tagId = :tagId")
    suspend fun deleteTrackTag(trackId: String, tagId: String)
}

@Dao
interface SmartPlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, p.sortOrder, p.trackLimit, p.createdAt, p.updatedAt,
               COUNT(r.id) AS ruleCount
        FROM smart_playlists p
        LEFT JOIN smart_playlist_rules r ON r.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
        """,
    )
    fun observeAllWithRuleCounts(): Flow<List<SmartPlaylistWithRuleCount>>

    @Query("SELECT * FROM smart_playlists WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<SmartPlaylistEntity?>

    @Query("SELECT * FROM smart_playlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SmartPlaylistEntity?

    @Query(
        """
        SELECT * FROM smart_playlist_rules
        WHERE playlistId = :playlistId
        ORDER BY position ASC, id ASC
        """,
    )
    fun observeRules(playlistId: String): Flow<List<SmartPlaylistRuleEntity>>

    @Query(
        """
        SELECT * FROM smart_playlist_rules
        WHERE playlistId = :playlistId
        ORDER BY position ASC, id ASC
        """,
    )
    suspend fun getRules(playlistId: String): List<SmartPlaylistRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: SmartPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<SmartPlaylistRuleEntity>)

    @Query("DELETE FROM smart_playlist_rules WHERE playlistId = :playlistId")
    suspend fun deleteRulesForPlaylist(playlistId: String)

    @Query("DELETE FROM smart_playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Transaction
    suspend fun replacePlaylistRules(playlistId: String, rules: List<SmartPlaylistRuleEntity>) {
        deleteRulesForPlaylist(playlistId)
        if (rules.isNotEmpty()) {
            insertRules(rules)
        }
    }
}

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: Int = UserSettingsEntity.SETTINGS_ROW_ID): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    suspend fun getSettings(id: Int = UserSettingsEntity.SETTINGS_ROW_ID): UserSettingsEntity?
}
