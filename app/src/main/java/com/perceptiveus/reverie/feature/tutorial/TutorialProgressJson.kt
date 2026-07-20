package com.perceptiveus.reverie.feature.tutorial

import org.json.JSONArray
import org.json.JSONObject

fun TutorialProgress.toJson(): String = JSONObject().apply {
    put("firstRunDismissed", firstRunDismissed)
    put("completedChapterIds", JSONArray(completedChapterIds.toList()))
}.toString()

fun parseTutorialProgress(json: String?): TutorialProgress {
    if (json.isNullOrBlank()) return TutorialProgress.Default
    return try {
        val obj = JSONObject(json)
        val ids = buildSet {
            val arr = obj.optJSONArray("completedChapterIds") ?: JSONArray()
            for (i in 0 until arr.length()) {
                arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
        TutorialProgress(
            firstRunDismissed = obj.optBoolean("firstRunDismissed", false),
            completedChapterIds = ids,
        )
    } catch (_: Exception) {
        TutorialProgress.Default
    }
}
