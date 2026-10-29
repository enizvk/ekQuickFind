package com.ekquickfind.app.icons

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade

object EkImageLoader {
    fun create(context: Context): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(AppIconFetcher.PackageFactory(context))
                add(PackageIconKeyer())
                add(AppIconFetcher.ShortcutFactory(context))
                add(ShortcutIconKeyer())
                add(ThumbnailFetcher.Factory(context))
                add(ThumbnailKeyer())
            }.crossfade(150)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
            .build()
}
