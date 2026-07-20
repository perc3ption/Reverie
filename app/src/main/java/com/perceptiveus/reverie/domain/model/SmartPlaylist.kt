package com.perceptiveus.reverie.domain.model

data class SmartPlaylist(
    val id: String,
    val name: String,
    val sortOrder: SmartPlaylistSort = SmartPlaylistSort.TITLE,
    val trackLimit: Int = 100,
    val ruleCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class SmartPlaylistRule(
    val id: Long = 0L,
    val field: SmartPlaylistField,
    val operator: SmartPlaylistOperator,
    val value: String = "",
    val valueSecondary: String = "",
)

enum class SmartPlaylistField {
    ARTIST,
    ALBUM,
    GENRE,
    YEAR,
    RATING,
    TAG,
    DATE_ADDED,
    PLAY_COUNT,
    RECENTLY_PLAYED,
}

enum class SmartPlaylistOperator {
    IS,
    IS_NOT,
    CONTAINS,
    EQUALS,
    GREATER_EQUAL,
    LESS_EQUAL,
    BETWEEN,
    HAS,
    IN_LAST_DAYS,
}

enum class SmartPlaylistSort {
    TITLE,
    ARTIST,
    DATE_ADDED,
    MOST_PLAYED,
    RECENTLY_PLAYED,
}

fun SmartPlaylistField.displayName(): String = when (this) {
    SmartPlaylistField.ARTIST -> "Artist"
    SmartPlaylistField.ALBUM -> "Album"
    SmartPlaylistField.GENRE -> "Genre"
    SmartPlaylistField.YEAR -> "Year"
    SmartPlaylistField.RATING -> "Rating"
    SmartPlaylistField.TAG -> "Tag"
    SmartPlaylistField.DATE_ADDED -> "Date added"
    SmartPlaylistField.PLAY_COUNT -> "Play count"
    SmartPlaylistField.RECENTLY_PLAYED -> "Recently played"
}

fun SmartPlaylistOperator.displayName(): String = when (this) {
    SmartPlaylistOperator.IS -> "is"
    SmartPlaylistOperator.IS_NOT -> "is not"
    SmartPlaylistOperator.CONTAINS -> "contains"
    SmartPlaylistOperator.EQUALS -> "equals"
    SmartPlaylistOperator.GREATER_EQUAL -> "at least"
    SmartPlaylistOperator.LESS_EQUAL -> "at most"
    SmartPlaylistOperator.BETWEEN -> "between"
    SmartPlaylistOperator.HAS -> "has"
    SmartPlaylistOperator.IN_LAST_DAYS -> "in last N days"
}

fun SmartPlaylistSort.displayName(): String = when (this) {
    SmartPlaylistSort.TITLE -> "Title"
    SmartPlaylistSort.ARTIST -> "Artist"
    SmartPlaylistSort.DATE_ADDED -> "Date added"
    SmartPlaylistSort.MOST_PLAYED -> "Most played"
    SmartPlaylistSort.RECENTLY_PLAYED -> "Recently played"
}

fun operatorsFor(field: SmartPlaylistField): List<SmartPlaylistOperator> = when (field) {
    SmartPlaylistField.ARTIST,
    SmartPlaylistField.ALBUM,
    SmartPlaylistField.GENRE,
    -> listOf(
        SmartPlaylistOperator.IS,
        SmartPlaylistOperator.IS_NOT,
        SmartPlaylistOperator.CONTAINS,
    )
    SmartPlaylistField.YEAR -> listOf(
        SmartPlaylistOperator.EQUALS,
        SmartPlaylistOperator.GREATER_EQUAL,
        SmartPlaylistOperator.LESS_EQUAL,
        SmartPlaylistOperator.BETWEEN,
    )
    SmartPlaylistField.RATING -> listOf(
        SmartPlaylistOperator.EQUALS,
        SmartPlaylistOperator.GREATER_EQUAL,
    )
    SmartPlaylistField.TAG -> listOf(SmartPlaylistOperator.HAS)
    SmartPlaylistField.DATE_ADDED,
    SmartPlaylistField.RECENTLY_PLAYED,
    -> listOf(SmartPlaylistOperator.IN_LAST_DAYS)
    SmartPlaylistField.PLAY_COUNT -> listOf(SmartPlaylistOperator.GREATER_EQUAL)
}
