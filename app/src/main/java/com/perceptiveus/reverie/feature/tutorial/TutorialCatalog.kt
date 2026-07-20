package com.perceptiveus.reverie.feature.tutorial

/**
 * Static catalog for Discover Reverie.
 * Screenshots can be wired later via [TutorialChapter.imageDrawableRes].
 */
enum class TutorialTryIt {
    NONE,
    IMPORT,
    LIBRARY,
    PLAYER,
    SEARCH,
    AUDIO_FX,
    SMART_PLAYLISTS,
    STATS,
    SETTINGS,
}

data class TutorialChapter(
    val id: String,
    val section: String,
    val title: String,
    val summary: String,
    val bullets: List<String>,
    val tryIt: TutorialTryIt = TutorialTryIt.NONE,
    val tryItLabel: String? = null,
    val isPremium: Boolean = false,
    /** Optional drawable id for a future screenshot; null shows an icon placeholder. */
    val imageDrawableRes: Int? = null,
)

data class TutorialProgress(
    val firstRunDismissed: Boolean = false,
    val completedChapterIds: Set<String> = emptySet(),
) {
    fun isCompleted(chapterId: String): Boolean = chapterId in completedChapterIds

    fun withCompleted(chapterId: String): TutorialProgress =
        copy(completedChapterIds = completedChapterIds + chapterId)

    companion object {
        val Default = TutorialProgress()
    }
}

object TutorialCatalog {
    val chapters: List<TutorialChapter> = listOf(
        TutorialChapter(
            id = "import",
            section = "Getting started",
            title = "Import your music",
            summary = "Bring songs or whole folders into Reverie’s local library.",
            bullets = listOf(
                "Use Import Music from Home or Library Quick Access.",
                "Choose individual files or an entire folder.",
                "Files are stored under the app’s Reverie media folder on your device.",
                "Supported formats include MP3, FLAC, M4A, AAC, OGG, Opus, WAV, and more.",
            ),
            tryIt = TutorialTryIt.IMPORT,
            tryItLabel = "Open Import",
        ),
        TutorialChapter(
            id = "browse",
            section = "Getting started",
            title = "Browse the library",
            summary = "Explore Folders, Artists, Albums, Songs, and Playlists.",
            bullets = listOf(
                "Open the Library tab to switch between browse modes.",
                "Folders mirror how you organized imports on disk.",
                "Play a single song or use Play All from an album, artist, or folder.",
            ),
            tryIt = TutorialTryIt.LIBRARY,
            tryItLabel = "Open Library",
        ),
        TutorialChapter(
            id = "playback",
            section = "Getting started",
            title = "Play & Now Playing",
            summary = "Control playback from Home, the mini player, or the full Player.",
            bullets = listOf(
                "Tap a track on Home or in Library to start listening.",
                "The mini player stays on main tabs while a song is active.",
                "Open the Player tab for seek, shuffle, repeat, and media views.",
                "Playback continues in the background with a system media notification.",
            ),
            tryIt = TutorialTryIt.PLAYER,
            tryItLabel = "Open Player",
        ),
        TutorialChapter(
            id = "queue",
            section = "Getting started",
            title = "Manage the queue",
            summary = "See what’s next and shape the listening session.",
            bullets = listOf(
                "Open the queue from Home or Player (⋮ → View queue).",
                "Tap a row to jump to that track.",
                "Reorder with up/down, or mute a track for this session only.",
                "Add to Queue from Library, Search, or Song Detail.",
            ),
            tryIt = TutorialTryIt.PLAYER,
            tryItLabel = "Open Player",
        ),
        TutorialChapter(
            id = "search",
            section = "Getting started",
            title = "Search everything",
            summary = "Find songs, artists, albums, playlists, and folders quickly.",
            bullets = listOf(
                "Tap Search on Home or Library.",
                "Results cover your whole local library in one place.",
            ),
            tryIt = TutorialTryIt.SEARCH,
            tryItLabel = "Open Search",
        ),
        TutorialChapter(
            id = "playlists",
            section = "Make it yours",
            title = "Playlists",
            summary = "Build mixes you can return to anytime.",
            bullets = listOf(
                "Create playlists from the Library → Playlists tab.",
                "Add or remove songs on a playlist’s detail screen.",
                "Optional description and cover help you tell mixes apart.",
                "Free tier includes up to 3 playlists; Premium unlocks more.",
            ),
            tryIt = TutorialTryIt.LIBRARY,
            tryItLabel = "Open Library",
        ),
        TutorialChapter(
            id = "song_detail",
            section = "Make it yours",
            title = "Song details",
            summary = "Inspect a track and polish how it looks and feels.",
            bullets = listOf(
                "Open Song details from a track menu or the Player.",
                "Edit metadata so titles and albums stay accurate.",
                "Import or refresh album art when a cover is missing.",
                "Premium: rate tracks and organize with tags.",
            ),
            tryIt = TutorialTryIt.LIBRARY,
            tryItLabel = "Open Library",
        ),
        TutorialChapter(
            id = "player_media",
            section = "Make it yours",
            title = "Visualizer & lyrics",
            summary = "Switch the Player media area between art, spectrum, and lyrics.",
            bullets = listOf(
                "On Player, toggle Album Art / Visualizer / Lyrics.",
                "The free visualizer shows a classic spectrum; more skins are Premium.",
                "Import a sidecar .lrc or .txt for synced lyrics (Premium).",
            ),
            tryIt = TutorialTryIt.PLAYER,
            tryItLabel = "Open Player",
            isPremium = true,
        ),
        TutorialChapter(
            id = "audio_fx",
            section = "Sound & smart listening",
            title = "Audio FX",
            summary = "Shape tone and playback feel to match your headphones.",
            bullets = listOf(
                "Enable the 10-band EQ, preamp, and bass boost.",
                "Try presets, then fine-tune bands yourself.",
                "Loudness leveling soft-matches volume across tracks.",
                "Crossfade overlaps songs; gapless keeps transitions tight.",
                "Use Restore in the top bar anytime you want factory defaults.",
            ),
            tryIt = TutorialTryIt.AUDIO_FX,
            tryItLabel = "Open Audio FX",
            isPremium = true,
        ),
        TutorialChapter(
            id = "smart_playlists",
            section = "Sound & smart listening",
            title = "Smart Playlists",
            summary = "Rule-based playlists that refresh when you open them.",
            bullets = listOf(
                "Create rules on artist, album, genre, year, rating, tags, and more.",
                "Choose sort order and a track limit.",
                "Matching songs are evaluated on open — no manual upkeep.",
            ),
            tryIt = TutorialTryIt.SMART_PLAYLISTS,
            tryItLabel = "Open Smart Playlists",
            isPremium = true,
        ),
        TutorialChapter(
            id = "stats",
            section = "Sound & smart listening",
            title = "Library Stats",
            summary = "See what’s in your collection and what you’ve played most.",
            bullets = listOf(
                "Open Stats from Home or Library Quick Access.",
                "Totals and top tracks/artists come from your play history.",
            ),
            tryIt = TutorialTryIt.STATS,
            tryItLabel = "Open Stats",
            isPremium = true,
        ),
        TutorialChapter(
            id = "settings_premium",
            section = "Settings & Premium",
            title = "Settings & Premium",
            summary = "Theme, profile, and what’s included with Premium.",
            bullets = listOf(
                "Change display name and light / dark / system theme in Settings.",
                "Premium unlocks Audio FX, Smart Playlists, Stats, lyrics, ratings, and more.",
                "Use View Premium Features for the full list.",
                "Restore Purchases is ready for when Play Billing is wired.",
            ),
            tryIt = TutorialTryIt.SETTINGS,
            tryItLabel = "Open Settings",
        ),
    )

    fun byId(id: String): TutorialChapter? = chapters.find { it.id == id }

    val sections: List<String> = chapters.map { it.section }.distinct()
}
