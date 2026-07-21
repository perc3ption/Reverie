package com.perceptiveus.reverie.playback

import com.perceptiveus.reverie.data.import.SupportedAudioFormats
import java.io.File

/**
 * Helpers for deciding whether a library file can be queued for Media3 ExoPlayer.
 * Extension sets live in [SupportedAudioFormats].
 */
object AudioPlaybackSupport {

    val PLAYABLE_EXTENSIONS: Set<String> = SupportedAudioFormats.PLAYABLE_EXTENSIONS

    val UNSUPPORTED_EXTENSIONS: Set<String> = SupportedAudioFormats.UNSUPPORTED_FOR_PLAYBACK

    fun extensionOf(filePath: String): String =
        filePath.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    fun isExtensionPlayable(filePath: String): Boolean =
        extensionOf(filePath) in PLAYABLE_EXTENSIONS

    fun isUnsupportedCodec(filePath: String): Boolean =
        extensionOf(filePath) in UNSUPPORTED_EXTENSIONS

    fun isPlayableFile(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        if (!isExtensionPlayable(filePath)) return false
        return File(filePath).exists()
    }
}
