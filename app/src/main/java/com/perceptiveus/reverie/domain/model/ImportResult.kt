package com.perceptiveus.reverie.domain.model

data class ImportResult(
    val filesAttempted: Int,
    val filesImported: Int,
    val filesFailed: Int,
    val moveDeleteFailures: Int,
    val truncatedBySongLimit: Boolean,
    val scanResult: LibraryScanResult? = null,
)
