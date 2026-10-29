package com.ekquickfind.app.icons

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.SystemClock
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.ekquickfind.core.search.ResultIcon
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconFetcherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun loadsARealIconAndServesTheSecondRequestFromMemoryWithin50ms() =
        runBlocking {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val settings =
                launcherApps.getActivityList("com.android.settings", Process.myUserHandle()).first().componentName
            val serial = context.getSystemService(UserManager::class.java).getSerialNumberForUser(Process.myUserHandle())
            val icon = ResultIcon.PackageIcon(settings.packageName, settings.className, serial)
            val loader = EkImageLoader.create(context)
            val request =
                ImageRequest
                    .Builder(context)
                    .data(icon)
                    .size(144)
                    .build()

            val first = loader.execute(request)
            assertTrue("first load failed: $first", first is SuccessResult)

            val start = SystemClock.elapsedRealtime()
            val second = loader.execute(request) as SuccessResult
            val elapsed = SystemClock.elapsedRealtime() - start

            assertEquals(DataSource.MEMORY_CACHE, second.dataSource)
            assertTrue("second load took ${elapsed}ms", elapsed < 50)
        }
}
