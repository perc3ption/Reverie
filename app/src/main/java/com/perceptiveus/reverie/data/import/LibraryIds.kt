package com.perceptiveus.reverie.data.import

/** Stable IDs derived from on-disk library layout. */
internal object LibraryIds {
    const val ROOT_FOLDER_ID = "_library_root"

    fun folderId(relativePath: String): String {
        if (relativePath.isEmpty()) return ROOT_FOLDER_ID
        return "folder_${relativePath.replace('/', '_')}"
    }

    fun folderDisplayName(relativePath: String): String {
        if (relativePath.isEmpty()) return "Library Root"
        return relativePath.substringAfterLast('/')
    }

    fun trackId(absolutePath: String): String {
        return "track_${absolutePath.hashCode().toLong().toULong().toString(16)}"
    }
}
