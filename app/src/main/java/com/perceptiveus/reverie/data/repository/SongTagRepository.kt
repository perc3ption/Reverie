package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.dao.SongTagDao
import com.perceptiveus.reverie.data.local.entity.TagEntity
import com.perceptiveus.reverie.data.local.entity.TrackTagCrossRef
import com.perceptiveus.reverie.data.local.mapper.toDomain
import com.perceptiveus.reverie.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface SongTagRepository {
    fun observeAllTags(): Flow<List<Tag>>
    fun observeTagsForTrack(trackId: String): Flow<List<Tag>>
    /** Creates the tag if needed, then links it to the track. */
    suspend fun addTagToTrack(trackId: String, name: String): Result<Tag>
    suspend fun addExistingTagToTrack(trackId: String, tagId: String): Result<Unit>
    suspend fun removeTagFromTrack(trackId: String, tagId: String)
}

class RoomSongTagRepository(
    private val songTagDao: SongTagDao,
    private val featureAccessChecker: FeatureAccessChecker,
) : SongTagRepository {

    override fun observeAllTags(): Flow<List<Tag>> =
        songTagDao.observeAllTags().map { rows -> rows.map { it.toDomain() } }

    override fun observeTagsForTrack(trackId: String): Flow<List<Tag>> =
        songTagDao.observeTagsForTrack(trackId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addTagToTrack(trackId: String, name: String): Result<Tag> {
        if (!featureAccessChecker.canAccess(AppFeature.TAGS)) {
            return Result.failure(TagAccessException)
        }
        val normalized = name.trim()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag cannot be empty."))
        }

        val existing = songTagDao.getByName(normalized)
        val tag = if (existing != null) {
            existing
        } else {
            val created = TagEntity(
                id = UUID.randomUUID().toString(),
                name = normalized,
            )
            songTagDao.insertTag(created)
            // Re-read in case of a concurrent insert with the same name.
            songTagDao.getByName(normalized) ?: created
        }
        songTagDao.insertTrackTag(TrackTagCrossRef(trackId = trackId, tagId = tag.id))
        return Result.success(tag.toDomain())
    }

    override suspend fun addExistingTagToTrack(trackId: String, tagId: String): Result<Unit> {
        if (!featureAccessChecker.canAccess(AppFeature.TAGS)) {
            return Result.failure(TagAccessException)
        }
        songTagDao.insertTrackTag(TrackTagCrossRef(trackId = trackId, tagId = tagId))
        return Result.success(Unit)
    }

    override suspend fun removeTagFromTrack(trackId: String, tagId: String) {
        songTagDao.deleteTrackTag(trackId, tagId)
    }
}

object TagAccessException : Exception("Tags are a Premium feature.")
