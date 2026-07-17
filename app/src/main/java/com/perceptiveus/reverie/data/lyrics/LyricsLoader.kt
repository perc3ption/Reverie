package com.perceptiveus.reverie.data.lyrics

import com.perceptiveus.reverie.domain.model.LyricsDocument
import java.io.File

/**
 * Loads lyrics from sidecar files next to an audio track.
 *
 * Looks for (in order):
 * - same basename `.lrc` (synced)
 * - same basename `.txt`
 * - `lyrics.lrc` / `lyrics.txt` in the same folder
 */
object LyricsLoader {

    fun loadForAudioFile(audioPath: String): LyricsDocument? {
        if (audioPath.isBlank()) return null
        val audio = File(audioPath)
        if (!audio.isFile) return null

        val candidates = buildList {
            val base = audio.nameWithoutExtension
            val parent = audio.parentFile
            add(File(audio.parent, "$base.lrc"))
            add(File(audio.parent, "$base.LRC"))
            add(File(audio.parent, "$base.txt"))
            add(File(audio.parent, "$base.TXT"))
            if (parent != null) {
                add(File(parent, "lyrics.lrc"))
                add(File(parent, "lyrics.txt"))
            }
        }

        val file = candidates.firstOrNull { it.isFile && it.length() in 1..MAX_BYTES } ?: return null
        val content = runCatching { file.readText(Charsets.UTF_8) }
            .recoverCatching { file.readText(Charsets.ISO_8859_1) }
            .getOrNull()
            ?: return null

        val doc = LyricsParser.parse(content, sourcePath = file.absolutePath)
        return doc.takeIf { it.lines.isNotEmpty() }
    }

    private const val MAX_BYTES = 512L * 1024L
}
