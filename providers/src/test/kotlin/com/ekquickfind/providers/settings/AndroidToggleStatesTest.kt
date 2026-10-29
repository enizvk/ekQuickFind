package com.ekquickfind.providers.settings

import android.app.NotificationManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidToggleStatesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val states = AndroidToggleStates(context)

    @Test
    fun readsWifi() {
        context.getSystemService(WifiManager::class.java).isWifiEnabled = true
        assertEquals(true, states.state(QuickToggle.Wifi))
    }

    @Test
    fun readsBatterySaver() {
        shadowOf(context.getSystemService(PowerManager::class.java)).setIsPowerSaveMode(true)
        assertEquals(true, states.state(QuickToggle.BatterySaver))
    }

    @Test
    fun readsAutoRotate() {
        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        assertEquals(false, states.state(QuickToggle.AutoRotate))
        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        assertEquals(true, states.state(QuickToggle.AutoRotate))
    }

    @Test
    fun bluetoothNeedsNearbyDevicesThenUsesTheSystemDialogs() {
        val app = shadowOf(context as android.app.Application)
        app.denyPermissions(android.Manifest.permission.BLUETOOTH_CONNECT)
        assertEquals(ToggleControl.NeedsPermission, states.control(QuickToggle.Bluetooth))
        assertEquals(listOf(android.Manifest.permission.BLUETOOTH_CONNECT), states.permissions(QuickToggle.Bluetooth))

        app.grantPermissions(android.Manifest.permission.BLUETOOTH_CONNECT)
        assertEquals(ToggleControl.Confirm, states.control(QuickToggle.Bluetooth))
        assertEquals(
            android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE,
            states.confirmIntent(QuickToggle.Bluetooth, on = true)?.action,
        )
        assertEquals("android.bluetooth.adapter.action.REQUEST_DISABLE", states.confirmIntent(QuickToggle.Bluetooth, on = false)?.action)
    }

    @Test
    fun systemOnlyTogglesCannotBeSetDirectly() {
        listOf(QuickToggle.Wifi, QuickToggle.BatterySaver).forEach {
            assertEquals(it.name, ToggleControl.SystemOnly, states.control(it))
        }
    }

    @Test
    fun doNotDisturbNeedsPolicyAccessThenSetsDirectly() {
        val nm = context.getSystemService(NotificationManager::class.java)
        shadowOf(nm).setNotificationPolicyAccessGranted(false)
        assertEquals(ToggleControl.NeedsAccess, states.control(QuickToggle.DoNotDisturb))
        assertEquals(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, states.accessIntent(QuickToggle.DoNotDisturb)?.action)

        shadowOf(nm).setNotificationPolicyAccessGranted(true)
        assertEquals(ToggleControl.Direct, states.control(QuickToggle.DoNotDisturb))
        assertTrue(states.set(QuickToggle.DoNotDisturb, true))
        assertEquals(true, states.state(QuickToggle.DoNotDisturb))
        assertTrue(states.set(QuickToggle.DoNotDisturb, false))
        assertEquals(false, states.state(QuickToggle.DoNotDisturb))
    }

    @Test
    fun autoRotateNeedsWriteSettingsAccessThenSetsDirectly() {
        var canWrite = false
        val states = AndroidToggleStates(context) { canWrite }
        assertEquals(ToggleControl.NeedsAccess, states.control(QuickToggle.AutoRotate))
        assertEquals(Settings.ACTION_MANAGE_WRITE_SETTINGS, states.accessIntent(QuickToggle.AutoRotate)?.action)

        canWrite = true
        assertEquals(ToggleControl.Direct, states.control(QuickToggle.AutoRotate))
        assertTrue(states.set(QuickToggle.AutoRotate, true))
        assertEquals(true, states.state(QuickToggle.AutoRotate))
    }

    @Test
    fun readsDoNotDisturb() {
        val nm = context.getSystemService(NotificationManager::class.java)
        shadowOf(nm).setNotificationPolicyAccessGranted(true)
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        assertEquals(true, states.state(QuickToggle.DoNotDisturb))
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        assertEquals(false, states.state(QuickToggle.DoNotDisturb))
    }
}
