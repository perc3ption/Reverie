package com.perceptiveus.reverie.domain.model

data class LibraryScanResult(
    val tracksFound: Int,
    val tracksIndexed: Int,
    val tracksRemoved: Int,
    val foldersIndexed: Int,
    val truncatedBySongLimit: Boolean = false,
    val skippedUnreadable: Int = 0,
    /** Files whose size+mtime matched the DB row — metadata re-read skipped. */
    val tracksUnchanged: Int = 0,
)
