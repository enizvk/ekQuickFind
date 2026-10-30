package com.ekquickfind.providers.files

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment

enum class Access { Full, Partial, None }

data class FileAccessState(
    val images: Access,
    val video: Access,
    val audio: Access,
    /** All files access; needed to see other apps' documents and downloads. */
    val documents: Boolean,
) {
    val anyMedia: Boolean get() = images != Access.None || video != Access.None || audio != Access.None

    val searchableKinds: Set<MediaKind>
        get() =
            buildSet {
                if (images != Access.None) add(MediaKind.Image)
                if (video != Access.None) add(MediaKind.Video)
                if (audio != Access.None) add(MediaKind.Audio)
                if (documents) add(MediaKind.Document)
            }

    companion object {
        val None = FileAccessState(Access.None, Access.None, Access.None, documents = false)
    }
}

fun interface FileAccess {
    fun current(): FileAccessState

    companion object {
        fun of(context: Context) =
            FileAccess {
                fun granted(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
                // Partial access ("Select photos") is Android 14+.
                val selected =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

                fun visual(p: String) =
                    when {
                        granted(p) -> Access.Full
                        selected -> Access.Partial
                        else -> Access.None
                    }
                FileAccessState(
                    images = visual(Manifest.permission.READ_MEDIA_IMAGES),
                    video = visual(Manifest.permission.READ_MEDIA_VIDEO),
                    audio = if (granted(Manifest.permission.READ_MEDIA_AUDIO)) Access.Full else Access.None,
                    documents = Environment.isExternalStorageManager(),
                )
            }
    }
}
