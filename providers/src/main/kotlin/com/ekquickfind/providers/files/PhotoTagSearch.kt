package com.ekquickfind.providers.files

import com.ekquickfind.data.photos.TagHit

/** Photos whose tags match a query; [None] when photo tags are off. */
fun interface PhotoTagSearch {
    suspend fun matches(normalizedQuery: String): List<TagHit>

    companion object {
        val None = PhotoTagSearch { emptyList() }
    }
}
