package com.ekquickfind.app.icons

import android.content.Context
import android.util.Size
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.Dimension
import com.ekquickfind.core.search.ResultIcon

/** Image and video thumbnails via ContentResolver.loadThumbnail (uses MediaStore's own thumbnail cache). */
class ThumbnailFetcher(
    private val context: Context,
    private val data: ResultIcon.Thumbnail,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val px = (options.size.width as? Dimension.Pixels)?.px ?: 192
        val bitmap = runCatching { context.contentResolver.loadThumbnail(data.uri.toUri(), Size(px, px), null) }.getOrNull() ?: return null
        return ImageFetchResult(bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(
        private val context: Context,
    ) : Fetcher.Factory<ResultIcon.Thumbnail> {
        override fun create(
            data: ResultIcon.Thumbnail,
            options: Options,
            imageLoader: ImageLoader,
        ) = ThumbnailFetcher(context, data, options)
    }
}

class ThumbnailKeyer : Keyer<ResultIcon.Thumbnail> {
    override fun key(
        data: ResultIcon.Thumbnail,
        options: Options,
    ) = "thumb:${data.uri}"
}
