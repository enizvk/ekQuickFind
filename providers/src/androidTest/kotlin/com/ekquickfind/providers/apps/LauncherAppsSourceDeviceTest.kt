package com.ekquickfind.providers.apps

import android.content.Context
import android.content.pm.LauncherApps
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the real device: real labels, real profiles. */
@RunWith(AndroidJUnit4::class)
class LauncherAppsSourceDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val source = LauncherAppsSource(context)

    @Test
    fun loadsRealLabelsForLauncherApps() {
        val apps = source.loadApps()
        assertTrue("expected some apps, got ${apps.size}", apps.size > 5)
        assertTrue("labels must not be package names", apps.count { it.label == it.packageName } < apps.size / 4)
        assertTrue(apps.any { it.packageName == "com.android.settings" && it.label.isNotBlank() })
    }

    @Test
    fun includesWorkProfileAppsWhenAWorkProfileExists() {
        val profiles = context.getSystemService(LauncherApps::class.java).profiles
        assumeTrue("device has no second profile", profiles.size > 1)
        val apps = source.loadApps()
        assertTrue("no badged apps among ${apps.size}", apps.any { it.profileBadge != null })
        assertTrue("personal apps missing", apps.any { it.profileBadge == null })
    }
}
