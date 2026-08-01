package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.dao.SmartPlaylistDao
import com.perceptiveus.reverie.data.local.dao.SongTagDao
import com.perceptiveus.reverie.data.local.dao.TrackDao
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistEntity
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistRuleEntity
import com.perceptiveus.reverie.data.local.entity.SmartPlaylistWithRuleCount
import com.perceptiveus.reverie.data.local.mapper.toDomain
import com.perceptiveus.reverie.data.smartplaylist.SmartPlaylistEvaluator
import com.perceptiveus.reverie.domain.model.SmartPlaylist
import com.perceptiveus.reverie.domain.model.SmartPlaylistField
import com.perceptiveus.reverie.domain.model.SmartPlaylistOperator
import com.perceptiveus.reverie.domain.model.SmartPlaylistRule
import com.perceptiveus.reverie.domain.model.SmartPlaylistSort
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.util.UUID

class SmartPlaylistAccessException : Exception("Smart Playlists are a Premium feature.")

@OptIn(ExperimentalCoroutinesApi::class)
class RoomSmartPlaylistRepository(
    private val smartPlaylistDao: SmartPlaylistDao,
    private val trackDao: TrackDao,
    private val playHistoryDao: PlayHistoryDao,
    private val songTagDao: SongTagDao,
    private val featureAccessChecker: FeatureAccessChecker,
) : SmartPlaylistRepository {

    /**
     * Smart playlists with live [SmartPlaylist.matchCount].
     *
     * Performance: tracks + play/tag context are loaded once per change, then each
     * playlist is counted in memory on [Dispatchers.Default] (no per-row DB work,
     * no sort — only filter + early exit at trackLimit).
     */
    override val playlists: Flow<List<SmartPlaylist>> = combine(
        smartPlaylistDao.observeAllWithRuleCounts(),
        smartPlaylistDao.observeAllRules(),
        trackDao.observeAllTracks(),
        playHistoryDao.observeCount(),
        songTagDao.observeTrackTagCount(),
    ) { playlists, rules, tracks, _, _ ->
        Triple(playlists, rules, tracks)
    }.mapLatest { (playlists, rules, trackEntities) ->
        if (playlists.isEmpty()) return@mapLatest emptyList()

        val domainTracks = trackEntities.map { it.toDomain() }
        val rulesByPlaylist = rules.groupBy { it.playlistId }
        val needsPlayContext = rules.any { rule ->
            rule.field == SmartPlaylistField.PLAY_COUNT.name ||
                rule.field == SmartPlaylistField.RECENTLY_PLAYED.name
        }
        val needsTagContext = rules.any { rule ->
            rule.field == SmartPlaylistField.TAG.name
        }

        val playCounts = if (needsPlayContext) {
            playHistoryDao.playCountsByTrack().associate { it.trackId to it.playCount }
        } else {
            emptyMap()
        }
        val lastPlayed = if (needsPlayContext) {
            playHistoryDao.lastPlayedAtByTrack().associate { it.trackId to it.timestamp }
        } else {
            emptyMap()
        }
        val context = if (needsTagContext || needsPlayContext) {
            buildContext(playCounts, lastPlayed, includeTags = needsTagContext)
        } else {
            SmartPlaylistEvaluator.Context(nowMs = System.currentTimeMillis())
        }

        playlists.map { row ->
            val playlistRules = rulesByPlaylist[row.id].orEmpty().map { it.toDomain() }
            val matchCount = SmartPlaylistEvaluator.countMatches(
                tracks = domainTracks,
                rules = playlistRules,
                trackLimit = row.trackLimit,
                context = context,
            )
            row.toDomain(matchCount = matchCount)
        }
    }.flowOn(Dispatchers.Default)

    override fun observePlaylist(id: String): Flow<SmartPlaylist?> =
        combine(
            smartPlaylistDao.observeById(id),
            smartPlaylistDao.observeRules(id),
            trackDao.observeAllTracks(),
            playHistoryDao.observeCount(),
            songTagDao.observeTrackTagCount(),
        ) { entity, rules, tracks, _, _ ->
            Triple(entity, rules, tracks)
        }.mapLatest { (entity, rules, trackEntities) ->
            entity ?: return@mapLatest null
            val playlistRules = rules.map { it.toDomain() }
            val needsPlayContext = playlistRules.any {
                it.field == SmartPlaylistField.PLAY_COUNT ||
                    it.field == SmartPlaylistField.RECENTLY_PLAYED
            }
            val needsTagContext = playlistRules.any { it.field == SmartPlaylistField.TAG }
            val playCounts = if (needsPlayContext) {
                playHistoryDao.playCountsByTrack().associate { it.trackId to it.playCount }
            } else {
                emptyMap()
            }
            val lastPlayed = if (needsPlayContext) {
                playHistoryDao.lastPlayedAtByTrack().associate { it.trackId to it.timestamp }
            } else {
                emptyMap()
            }
            val context = if (needsTagContext || needsPlayContext) {
                buildContext(playCounts, lastPlayed, includeTags = needsTagContext)
            } else {
                SmartPlaylistEvaluator.Context(nowMs = System.currentTimeMillis())
            }
            val matchCount = SmartPlaylistEvaluator.countMatches(
                tracks = trackEntities.map { it.toDomain() },
                rules = playlistRules,
                trackLimit = entity.trackLimit,
                context = context,
            )
            entity.toDomain(ruleCount = playlistRules.size, matchCount = matchCount)
        }.flowOn(Dispatchers.Default).distinctUntilChanged()

    override fun observeRules(playlistId: String): Flow<List<SmartPlaylistRule>> =
        smartPlaylistDao.observeRules(playlistId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getPlaylist(id: String): SmartPlaylist? {
        val entity = smartPlaylistDao.getById(id) ?: return null
        return entity.toDomain(smartPlaylistDao.getRules(id).size)
    }

    override suspend fun getRules(playlistId: String): List<SmartPlaylistRule> =
        smartPlaylistDao.getRules(playlistId).map { it.toDomain() }

    override suspend fun evaluateTracks(playlistId: String): List<Track> {
        ensureAccess()
        val playlist = smartPlaylistDao.getById(playlistId) ?: return emptyList()
        val rules = smartPlaylistDao.getRules(playlistId).map { it.toDomain() }
        return evaluateTracks(
            rules = rules,
            sortOrder = playlist.sortOrder.toSort(),
            trackLimit = playlist.trackLimit,
        )
    }

    override suspend fun evaluateTracks(
        rules: List<SmartPlaylistRule>,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
    ): List<Track> {
        ensureAccess()
        val tracks = trackDao.getAllTracks().map { it.toDomain() }
        val playCounts = playHistoryDao.playCountsByTrack()
            .associate { it.trackId to it.playCount }
        val lastPlayed = playHistoryDao.lastPlayedAtByTrack()
            .associate { it.trackId to it.timestamp }
        return SmartPlaylistEvaluator.evaluate(
            tracks = tracks,
            rules = rules,
            sortOrder = sortOrder,
            trackLimit = trackLimit,
            context = buildContext(playCounts, lastPlayed, includeTags = true),
        )
    }

    private suspend fun buildContext(
        playCounts: Map<String, Int>,
        lastPlayed: Map<String, Long>,
        includeTags: Boolean,
    ): SmartPlaylistEvaluator.Context {
        if (!includeTags) {
            return SmartPlaylistEvaluator.Context(
                playCounts = playCounts,
                lastPlayedAt = lastPlayed,
                nowMs = System.currentTimeMillis(),
            )
        }
        val allTags = songTagDao.getAllTags()
        val tagNameById = allTags.associate { it.id to it.name }
        val refs = songTagDao.getAllTrackTags()
        val tagIdsByTrack = refs.groupBy({ it.trackId }, { it.tagId })
            .mapValues { (_, ids) -> ids.toSet() }
        val tagNamesByTrack = tagIdsByTrack.mapValues { (_, ids) ->
            ids.mapNotNull { tagNameById[it] }.toSet()
        }
        return SmartPlaylistEvaluator.Context(
            playCounts = playCounts,
            lastPlayedAt = lastPlayed,
            tagIdsByTrack = tagIdsByTrack,
            tagNamesByTrack = tagNamesByTrack,
            nowMs = System.currentTimeMillis(),
        )
    }

    override suspend fun createPlaylist(
        name: String,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
        rules: List<SmartPlaylistRule>,
    ): Result<SmartPlaylist> {
        ensureAccess()
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty."))
        if (rules.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one rule."))

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val entity = SmartPlaylistEntity(
            id = id,
            name = trimmed,
            sortOrder = sortOrder.name,
            trackLimit = trackLimit.coerceIn(1, 500),
            createdAt = now,
            updatedAt = now,
        )
        smartPlaylistDao.insertPlaylist(entity)
        smartPlaylistDao.replacePlaylistRules(id, rules.toEntities(id))
        return Result.success(entity.toDomain(rules.size))
    }

    override suspend fun updatePlaylist(
        id: String,
        name: String,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
        rules: List<SmartPlaylistRule>,
    ): Result<SmartPlaylist> {
        ensureAccess()
        val existing = smartPlaylistDao.getById(id)
            ?: return Result.failure(IllegalArgumentException("Smart playlist not found."))
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty."))
        if (rules.isEmpty()) return Result.failure(IllegalArgumentException("Add at least one rule."))

        val updated = existing.copy(
            name = trimmed,
            sortOrder = sortOrder.name,
            trackLimit = trackLimit.coerceIn(1, 500),
            updatedAt = System.currentTimeMillis(),
        )
        smartPlaylistDao.insertPlaylist(updated)
        smartPlaylistDao.replacePlaylistRules(id, rules.toEntities(id))
        return Result.success(updated.toDomain(rules.size))
    }

    override suspend fun deletePlaylist(id: String) {
        ensureAccess()
        smartPlaylistDao.deletePlaylist(id)
    }

    private fun ensureAccess() {
        if (!featureAccessChecker.canAccess(AppFeature.SMART_PLAYLISTS)) {
            throw SmartPlaylistAccessException()
        }
    }
}

private fun SmartPlaylistEntity.toDomain(ruleCount: Int, matchCount: Int = 0): SmartPlaylist =
    SmartPlaylist(
        id = id,
        name = name,
        sortOrder = sortOrder.toSort(),
        trackLimit = trackLimit,
        ruleCount = ruleCount,
        matchCount = matchCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun SmartPlaylistWithRuleCount.toDomain(matchCount: Int = 0): SmartPlaylist = SmartPlaylist(
    id = id,
    name = name,
    sortOrder = sortOrder.toSort(),
    trackLimit = trackLimit,
    ruleCount = ruleCount,
    matchCount = matchCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun SmartPlaylistRuleEntity.toDomain(): SmartPlaylistRule = SmartPlaylistRule(
    id = id,
    field = runCatching { SmartPlaylistField.valueOf(field) }.getOrDefault(SmartPlaylistField.ARTIST),
    operator = runCatching { SmartPlaylistOperator.valueOf(operator) }
        .getOrDefault(SmartPlaylistOperator.CONTAINS),
    value = value,
    valueSecondary = valueSecondary,
)

private fun List<SmartPlaylistRule>.toEntities(playlistId: String): List<SmartPlaylistRuleEntity> =
    mapIndexed { index, rule ->
        SmartPlaylistRuleEntity(
            id = 0,
            playlistId = playlistId,
            field = rule.field.name,
            operator = rule.operator.name,
            value = rule.value,
            valueSecondary = rule.valueSecondary,
            position = index,
        )
    }

private fun String.toSort(): SmartPlaylistSort =
    runCatching { SmartPlaylistSort.valueOf(this) }.getOrDefault(SmartPlaylistSort.TITLE)
