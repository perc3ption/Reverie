package com.perceptiveus.reverie.data.smartplaylist

import com.perceptiveus.reverie.domain.model.SmartPlaylistField
import com.perceptiveus.reverie.domain.model.SmartPlaylistOperator
import com.perceptiveus.reverie.domain.model.SmartPlaylistRule
import com.perceptiveus.reverie.domain.model.SmartPlaylistSort
import com.perceptiveus.reverie.domain.model.Track
import java.util.concurrent.TimeUnit

/**
 * Evaluates smart playlist rules in memory (AND across all rules).
 * Kept off the playback path — call only when opening/refreshing a smart playlist.
 */
object SmartPlaylistEvaluator {

    data class Context(
        val playCounts: Map<String, Int> = emptyMap(),
        val lastPlayedAt: Map<String, Long> = emptyMap(),
        val tagIdsByTrack: Map<String, Set<String>> = emptyMap(),
        val tagNamesByTrack: Map<String, Set<String>> = emptyMap(),
        val nowMs: Long = System.currentTimeMillis(),
    )

    fun evaluate(
        tracks: List<Track>,
        rules: List<SmartPlaylistRule>,
        sortOrder: SmartPlaylistSort,
        trackLimit: Int,
        context: Context,
    ): List<Track> {
        if (rules.isEmpty()) return emptyList()

        val matched = tracks.filter { track ->
            rules.all { rule -> matches(track, rule, context) }
        }

        val sorted = when (sortOrder) {
            SmartPlaylistSort.TITLE -> matched.sortedBy { it.title.lowercase() }
            SmartPlaylistSort.ARTIST -> matched.sortedWith(
                compareBy({ it.artist.lowercase() }, { it.title.lowercase() }),
            )
            SmartPlaylistSort.DATE_ADDED -> matched.sortedByDescending { it.dateAdded }
            SmartPlaylistSort.MOST_PLAYED -> matched.sortedWith(
                compareByDescending<Track> { context.playCounts[it.id] ?: 0 }
                    .thenBy { it.title.lowercase() },
            )
            SmartPlaylistSort.RECENTLY_PLAYED -> matched.sortedWith(
                compareByDescending<Track> { context.lastPlayedAt[it.id] ?: 0L }
                    .thenBy { it.title.lowercase() },
            )
        }

        val limit = trackLimit.coerceIn(1, 500)
        return sorted.take(limit)
    }

    private fun matches(
        track: Track,
        rule: SmartPlaylistRule,
        context: Context,
    ): Boolean {
        val value = rule.value.trim()
        return when (rule.field) {
            SmartPlaylistField.ARTIST -> matchText(track.artist, rule.operator, value)
            SmartPlaylistField.ALBUM -> matchText(track.album, rule.operator, value)
            SmartPlaylistField.GENRE -> matchText(track.genre, rule.operator, value)
            SmartPlaylistField.YEAR -> matchYear(track.year, rule)
            SmartPlaylistField.RATING -> matchInt(track.rating, rule.operator, value.toIntOrNull())
            SmartPlaylistField.TAG -> {
                if (rule.operator != SmartPlaylistOperator.HAS || value.isBlank()) return false
                val byId = context.tagIdsByTrack[track.id].orEmpty()
                val byName = context.tagNamesByTrack[track.id].orEmpty()
                value in byId || byName.any { it.equals(value, ignoreCase = true) }
            }
            SmartPlaylistField.DATE_ADDED -> {
                val days = value.toIntOrNull() ?: return false
                if (rule.operator != SmartPlaylistOperator.IN_LAST_DAYS) return false
                val cutoff = context.nowMs - TimeUnit.DAYS.toMillis(days.toLong().coerceAtLeast(0))
                track.dateAdded >= cutoff
            }
            SmartPlaylistField.PLAY_COUNT -> {
                val min = value.toIntOrNull() ?: return false
                if (rule.operator != SmartPlaylistOperator.GREATER_EQUAL) return false
                (context.playCounts[track.id] ?: 0) >= min
            }
            SmartPlaylistField.RECENTLY_PLAYED -> {
                val days = value.toIntOrNull() ?: return false
                if (rule.operator != SmartPlaylistOperator.IN_LAST_DAYS) return false
                val lastPlayed = context.lastPlayedAt[track.id] ?: return false
                val cutoff = context.nowMs - TimeUnit.DAYS.toMillis(days.toLong().coerceAtLeast(0))
                lastPlayed >= cutoff
            }
        }
    }

    private fun matchText(
        actual: String,
        operator: SmartPlaylistOperator,
        expected: String,
    ): Boolean {
        if (expected.isBlank()) return false
        return when (operator) {
            SmartPlaylistOperator.IS -> actual.equals(expected, ignoreCase = true)
            SmartPlaylistOperator.IS_NOT -> !actual.equals(expected, ignoreCase = true)
            SmartPlaylistOperator.CONTAINS -> actual.contains(expected, ignoreCase = true)
            else -> false
        }
    }

    private fun matchYear(year: Int, rule: SmartPlaylistRule): Boolean {
        val primary = rule.value.trim().toIntOrNull() ?: return false
        return when (rule.operator) {
            SmartPlaylistOperator.EQUALS -> year == primary
            SmartPlaylistOperator.GREATER_EQUAL -> year >= primary
            SmartPlaylistOperator.LESS_EQUAL -> year > 0 && year <= primary
            SmartPlaylistOperator.BETWEEN -> {
                val secondary = rule.valueSecondary.trim().toIntOrNull() ?: return false
                val low = minOf(primary, secondary)
                val high = maxOf(primary, secondary)
                year in low..high
            }
            else -> false
        }
    }

    private fun matchInt(
        actual: Int,
        operator: SmartPlaylistOperator,
        expected: Int?,
    ): Boolean {
        if (expected == null) return false
        return when (operator) {
            SmartPlaylistOperator.EQUALS -> actual == expected
            SmartPlaylistOperator.GREATER_EQUAL -> actual >= expected
            else -> false
        }
    }
}
