package com.perceptiveus.reverie.domain.model

/**
 * A single lyric line. [timeMs] is null for unsynced plain-text lyrics.
 */
data class LyricLine(
    val timeMs: Long?,
    val text: String,
)

data class LyricsDocument(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    /** Source file absolute path, for debugging / empty-state messaging. */
    val sourcePath: String = "",
) {
    fun activeLineIndex(positionMs: Long): Int {
        if (!isSynced || lines.isEmpty()) return -1
        var active = -1
        for (i in lines.indices) {
            val t = lines[i].timeMs ?: continue
            if (t <= positionMs) active = i else break
        }
        return active
    }

    companion object {
        val EMPTY = LyricsDocument(lines = emptyList(), isSynced = false)
    }
}
