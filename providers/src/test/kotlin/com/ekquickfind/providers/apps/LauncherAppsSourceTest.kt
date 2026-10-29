package com.ekquickfind.providers.apps

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageInfo
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
class LauncherAppsSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val personal = Process.myUserHandle()
    private val work = UserHandle.getUserHandleForUid(10 * 100_000)

    @Before
    fun setUp() {
        shadowOf(userManager).addUserProfile(work)
        shadowOf(userManager).setSerialNumberForUser(work, 10L)
        shadowOf(launcherApps).addActivity(personal, activity("com.android.chrome", "Main", "Chrome", personal))
        shadowOf(launcherApps).addActivity(personal, activity("com.android.settings", "Settings", "Settings", personal, system = true))
        shadowOf(launcherApps).addActivity(work, activity("com.slack", "Main", "Slack", work))
    }

    @Test
    fun enumeratesAppsAcrossPersonalAndWorkProfiles() {
        val apps = LauncherAppsSource(context).loadApps()

        // Robolectric resolves labels to package names; real labels are covered by the instrumented test.
        assertEquals(setOf("com.android.chrome", "com.android.settings", "com.slack"), apps.map { it.packageName }.toSet())
        val slack = apps.single { it.packageName == "com.slack" }
        assertEquals("Work", slack.profileBadge)
        assertEquals(10L, slack.userSerial)
        assertEquals("com.slack.Main", slack.activityName)
        assertEquals(null, apps.single { it.packageName == "com.android.chrome" }.profileBadge)
    }

    @Test
    fun flagsNonUpdatedSystemApps() {
        val apps = LauncherAppsSource(context).loadApps()
        assertTrue(apps.single { it.packageName == "com.android.settings" }.isSystemApp)
        assertFalse(apps.single { it.packageName == "com.android.chrome" }.isSystemApp)
    }

    @Test
    fun hidesShortcutsWithoutHostPermission() {
        shadowOf(launcherApps).setHasShortcutHostPermission(false)
        val source = LauncherAppsSource(context)
        assertFalse(source.hasShortcutAccess())
        assertTrue(source.loadShortcuts().isEmpty())
    }

    private fun activity(
        pkg: String,
        cls: String,
        label: String,
        user: UserHandle,
        system: Boolean = false,
    ): LauncherActivityInfo {
        val info =
            ActivityInfo().apply {
                packageName = pkg
                name = "$pkg.$cls"
                nonLocalizedLabel = label
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = pkg
                        flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
                    }
            }
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = pkg
                applicationInfo = info.applicationInfo.apply { nonLocalizedLabel = label }
                activities = arrayOf(info)
            },
        )
        // API 37 builds LauncherActivityInfo from a hidden internal wrapper.
        val internalClass = Class.forName("android.content.pm.LauncherActivityInfoInternal")
        val statesClass = Class.forName("android.content.pm.IncrementalStatesInfo")
        val internal =
            ReflectionHelpers.callConstructor(
                internalClass,
                ClassParameter.from(ActivityInfo::class.java, info),
                ClassParameter(statesClass, null),
                ClassParameter.from(UserHandle::class.java, user),
                ClassParameter.from(Boolean::class.javaPrimitiveType, false),
            )
        return ReflectionHelpers.callConstructor(
            LauncherActivityInfo::class.java,
            ClassParameter.from(Context::class.java, context),
            ClassParameter(internalClass, internal),
        )
    }
}
