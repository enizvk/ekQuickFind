package com.ekquickfind.app.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap
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

/** Loads launcher icons (badged for work profiles) straight from LauncherApps. */
class AppIconFetcher(
    private val context: Context,
    private val packageName: String,
    private val activityName: String?,
    private val userSerial: Long,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        // A negative serial means "this app's own profile" (handler icons don't know a serial).
        val user =
            if (userSerial < 0) {
                android.os.Process.myUserHandle()
            } else {
                context.getSystemService(UserManager::class.java).getUserForSerialNumber(userSerial) ?: return null
            }
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val info =
            activityName?.let {
                launcherApps.resolveActivity(Intent().setComponent(ComponentName(packageName, it)), user)
            } ?: launcherApps.getActivityList(packageName, user).firstOrNull() ?: return null
        val drawable: Drawable = info.getBadgedIcon(context.resources.displayMetrics.densityDpi)
        // Adaptive icons aren't bitmaps; render once so the memory cache can hold them.
        val px = (options.size.width as? Dimension.Pixels)?.px ?: DEFAULT_SIZE_PX
        return ImageFetchResult(drawable.toBitmap(px, px).asImage(), isSampled = false, dataSource = DataSource.DISK)
    }

    class PackageFactory(
        private val context: Context,
    ) : Fetcher.Factory<ResultIcon.PackageIcon> {
        override fun create(
            data: ResultIcon.PackageIcon,
            options: Options,
            imageLoader: ImageLoader,
        ) = AppIconFetcher(context, data.packageName, data.activityName, data.userSerial, options)
    }

    /** Shortcut icons need launcher-only access, so fall back to the owning app's icon. */
    class ShortcutFactory(
        private val context: Context,
    ) : Fetcher.Factory<ResultIcon.ShortcutIcon> {
        override fun create(
            data: ResultIcon.ShortcutIcon,
            options: Options,
            imageLoader: ImageLoader,
        ) = AppIconFetcher(context, data.packageName, null, data.userSerial, options)
    }

    private companion object {
        const val DEFAULT_SIZE_PX = 192
    }
}

class PackageIconKeyer : Keyer<ResultIcon.PackageIcon> {
    override fun key(
        data: ResultIcon.PackageIcon,
        options: Options,
    ) = "app:${data.packageName}/${data.activityName}@${data.userSerial}"
}

class ShortcutIconKeyer : Keyer<ResultIcon.ShortcutIcon> {
    override fun key(
        data: ResultIcon.ShortcutIcon,
        options: Options,
    ) = "shortcut:${data.packageName}/${data.shortcutId}@${data.userSerial}"
}
