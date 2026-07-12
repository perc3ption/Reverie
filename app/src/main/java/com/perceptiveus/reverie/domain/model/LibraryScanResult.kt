package com.perceptiveus.reverie.domain.model

data class LibraryScanResult(
    val tracksFound: Int,
    val tracksIndexed: Int,
    val tracksRemoved: Int,
    val foldersIndexed: Int,
    val truncatedBySongLimit: Boolean = false,
    val skippedUnreadable: Int = 0,
)
