package com.perceptiveus.reverie.data.import

/**
 * Single source of truth for audio extensions Reverie imports and can play.
 *
 * Keep UI copy (Import, Tutorial, README) in sync via the display helpers below.
 */
object SupportedAudioFormats {

    /**
     * Extensions accepted by import / library scan.
     * Includes WMA (indexed for completeness; not playable with stock ExoPlayer).
     */
    val IMPORTABLE_EXTENSIONS: Set<String> = setOf(
        "mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "alac", "aiff", "aif",
    )

    /** Indexed but not playable with the stock Media3 decoder set. */
    val UNSUPPORTED_FOR_PLAYBACK: Set<String> = setOf("wma")

    /** Extensions ExoPlayer is expected to decode without extra extensions. */
    val PLAYABLE_EXTENSIONS: Set<String> = IMPORTABLE_EXTENSIONS - UNSUPPORTED_FOR_PLAYBACK

    /** Chip labels for Import UI (AIFF covers `.aiff` / `.aif`). */
    val IMPORT_FORMAT_LABELS: List<String> = listOf(
        "MP3", "FLAC", "M4A", "AAC", "OGG", "Opus", "WAV", "WMA", "ALAC", "AIFF",
    )

    /** Compact list for Import screen and tutorial (AIFF covers `.aiff` / `.aif`). */
    const val IMPORT_FORMATS_LINE: String =
        "MP3, FLAC, M4A, AAC, OGG, Opus, WAV, WMA, ALAC, AIFF"

    const val PLAYABLE_FORMATS_LINE: String =
        "MP3, FLAC, M4A, AAC, OGG, Opus, WAV, ALAC, AIFF"

    /** Short note shown under format lists. */
    const val WMA_PLAYBACK_NOTE: String =
        "WMA can be imported into your library but isn’t playable yet."

    /** Full blurb for the Import screen body. */
    const val IMPORT_SCREEN_BLURB: String =
        "Supported formats: $IMPORT_FORMATS_LINE. $WMA_PLAYBACK_NOTE"

    /** Tutorial / options sheet one-liner. */
    const val IMPORT_SUPPORT_SUMMARY: String =
        "Imports $IMPORT_FORMATS_LINE. $WMA_PLAYBACK_NOTE"
}
