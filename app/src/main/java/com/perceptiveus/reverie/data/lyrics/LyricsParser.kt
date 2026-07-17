package com.perceptiveus.reverie.data.lyrics

import com.perceptiveus.reverie.domain.model.LyricLine
import com.perceptiveus.reverie.domain.model.LyricsDocument

/**
 * Parses standard LRC synced lyrics and plain-text lyric files.
 */
object LyricsParser {

    private val timestampRegex =
        Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offsetRegex =
        Regex("""\[offset\s*:\s*(-?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val metaRegex =
        Regex("""\[\s*(ti|ar|al|by|offset)\s*:[^\]]*]\s*""", RegexOption.IGNORE_CASE)

    fun parse(content: String, sourcePath: String = ""): LyricsDocument {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return LyricsDocument.EMPTY.copy(sourcePath = sourcePath)

        val hasTimestamps = timestampRegex.containsMatchIn(trimmed)
        return if (hasTimestamps) {
            parseLrc(trimmed, sourcePath)
        } else {
            parsePlain(trimmed, sourcePath)
        }
    }

    private fun parseLrc(content: String, sourcePath: String): LyricsDocument {
        var offsetMs = 0L
        offsetRegex.find(content)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { offsetMs = it }

        val lines = mutableListOf<LyricLine>()
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach

            val timestamps = timestampRegex.findAll(line).toList()
            if (timestamps.isEmpty()) return@forEach

            // Skip pure metadata lines like [ti:Song]
            val text = timestampRegex.replace(line, "").trim()
            if (text.isEmpty() && metaRegex.containsMatchIn(line)) return@forEach
            if (text.isEmpty()) return@forEach

            timestamps.forEach { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3]
                val fracMs = when {
                    fraction.isEmpty() -> 0L
                    fraction.length <= 2 -> fraction.toLong() * 10L // centiseconds → ms
                    else -> fraction.padEnd(3, '0').take(3).toLong()
                }
                val timeMs = (minutes * 60_000L + seconds * 1_000L + fracMs + offsetMs).coerceAtLeast(0L)
                lines += LyricLine(timeMs = timeMs, text = text)
            }
        }

        val sorted = lines.sortedBy { it.timeMs ?: 0L }
        return LyricsDocument(
            lines = sorted,
            isSynced = sorted.isNotEmpty(),
            sourcePath = sourcePath,
        )
    }

    private fun parsePlain(content: String, sourcePath: String): LyricsDocument {
        val lines = content.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .map { LyricLine(timeMs = null, text = it) }
            .toList()
        return LyricsDocument(
            lines = lines,
            isSynced = false,
            sourcePath = sourcePath,
        )
    }
}
