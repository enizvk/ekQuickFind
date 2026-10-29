package com.ekquickfind.providers.photos

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaStorePhotoLibrary(
    private val resolver: ContentResolver,
) : PhotoLibrary {
    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override fun photos(): List<PhotoItem> {
        val out = mutableListOf<PhotoItem>()
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_MODIFIED)
        resolver.query(images, projection, null, null, "${MediaStore.Images.Media.DATE_MODIFIED} DESC")?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                out += PhotoItem(id, ContentUris.withAppendedId(images, id), c.getLong(1))
            }
        }
        return out
    }

    override fun xmp(item: PhotoItem): String? =
        resolver.query(item.uri, arrayOf(MediaStore.MediaColumns.XMP), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getBlob(0)?.toString(Charsets.UTF_8) else null
        }

    override fun count(): Int = resolver.query(images, arrayOf(MediaStore.Images.Media._ID), null, null, null)?.use { it.count } ?: 0
}

/** Google ML Kit's bundled image labeller: about 400 everyday labels, runs fully on the phone. */
class MlKitPhotoLabeler(
    private val context: Context,
    minConfidence: Float = 0.7f,
) : PhotoLabeler {
    private val labeler by lazy { ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(minConfidence).build()) }

    override suspend fun labels(uri: Uri): List<Label> {
        // A small thumbnail is plenty for labelling and far cheaper than decoding the full photo.
        val bitmap = withContext(Dispatchers.IO) { context.contentResolver.loadThumbnail(uri, Size(THUMB, THUMB), null) }
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            return suspendCancellableCoroutine { cont ->
                labeler
                    .process(image)
                    .addOnSuccessListener { found -> cont.resume(found.map { Label(it.text, it.confidence) }) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        const val THUMB = 384
    }
}
