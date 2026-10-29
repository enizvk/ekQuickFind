package com.ekquickfind.providers.settings

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings

/** Toggles whose live state is shown inline. */
enum class QuickToggle(
    val title: String,
    val keywords: List<String>,
    /** Screen or panel opened on tap; null for toggles that flip in place. */
    val action: String?,
) {
    Wifi("Wi‑Fi", listOf("wifi", "wi-fi", "wireless", "wlan"), Settings.Panel.ACTION_WIFI),
    Bluetooth("Bluetooth", listOf("bluetooth", "bt"), Settings.ACTION_BLUETOOTH_SETTINGS),
    DoNotDisturb("Do Not Disturb", listOf("do not disturb", "dnd", "silence"), Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS),
    AutoRotate("Auto-rotate", listOf("auto rotate", "rotation", "rotate", "rotation lock"), Settings.ACTION_DISPLAY_SETTINGS),
    BatterySaver(
        "Battery saver",
        listOf("battery saver", "power saving", "low power", "power saver"),
        Settings.ACTION_BATTERY_SAVER_SETTINGS,
    ),
    Flashlight("Flashlight", listOf("flashlight", "torch", "flash light", "lamp"), null),
}

/** How ekQuickFind may change a toggle. */
enum class ToggleControl {
    /** Change it in place. */
    Direct,

    /** Allowed after the user grants a special access once (Do Not Disturb, modify system settings). */
    NeedsAccess,

    /** Needs a runtime permission first (Bluetooth: Nearby devices), then works like [Confirm]. */
    NeedsPermission,

    /** The system switches it after a small confirmation dialog (Bluetooth). */
    Confirm,

    /** Only the system may change it (Wi-Fi, Bluetooth, battery saver since Android 10/13); open its panel. */
    SystemOnly,
}

interface ToggleStates {
    /** Current state, or null when this device can't report or doesn't have it (for example no flash). */
    fun state(toggle: QuickToggle): Boolean?

    fun control(toggle: QuickToggle): ToggleControl

    /** Changes a Direct toggle; returns false if the system refused. */
    fun set(
        toggle: QuickToggle,
        on: Boolean,
    ): Boolean

    /** The system page that grants access for a NeedsAccess toggle. */
    fun accessIntent(toggle: QuickToggle): Intent?

    /** Runtime permissions a NeedsPermission toggle asks for. */
    fun permissions(toggle: QuickToggle): List<String> = emptyList()

    /** The system dialog that switches a Confirm toggle to [on]. */
    fun confirmIntent(
        toggle: QuickToggle,
        on: Boolean,
    ): Intent? = null
}

private const val REQUEST_DISABLE = "android.bluetooth.adapter.action.REQUEST_DISABLE"

class AndroidToggleStates(
    private val context: Context,
    private val canWriteSettings: () -> Boolean = { Settings.System.canWrite(context) },
) : ToggleStates {
    private val camera by lazy { context.getSystemService(CameraManager::class.java) }
    private val torchCameraId: String? by lazy {
        runCatching {
            camera.cameraIdList.firstOrNull { id ->
                camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    @Volatile private var torchOn = false
    private var torchCallbackRegistered = false

    override fun state(toggle: QuickToggle): Boolean? =
        runCatching {
            when (toggle) {
                QuickToggle.Wifi -> context.getSystemService(WifiManager::class.java).isWifiEnabled
                QuickToggle.Bluetooth -> context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
                QuickToggle.DoNotDisturb ->
                    context.getSystemService(NotificationManager::class.java).currentInterruptionFilter !=
                        NotificationManager.INTERRUPTION_FILTER_ALL
                QuickToggle.AutoRotate -> Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                QuickToggle.BatterySaver -> context.getSystemService(PowerManager::class.java).isPowerSaveMode
                QuickToggle.Flashlight -> torchCameraId?.let { watchTorch(it) }
            }
        }.getOrNull()

    private val notifications by lazy { context.getSystemService(NotificationManager::class.java) }

    override fun control(toggle: QuickToggle): ToggleControl =
        when (toggle) {
            QuickToggle.Flashlight -> ToggleControl.Direct
            QuickToggle.DoNotDisturb -> directIf(notifications.isNotificationPolicyAccessGranted)
            QuickToggle.AutoRotate -> directIf(canWriteSettings())
            QuickToggle.Bluetooth ->
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    ToggleControl.Confirm
                } else {
                    ToggleControl.NeedsPermission
                }
            QuickToggle.Wifi, QuickToggle.BatterySaver -> ToggleControl.SystemOnly
        }

    private fun directIf(granted: Boolean) = if (granted) ToggleControl.Direct else ToggleControl.NeedsAccess

    override fun set(
        toggle: QuickToggle,
        on: Boolean,
    ): Boolean =
        runCatching {
            when (toggle) {
                QuickToggle.Flashlight -> {
                    val id = torchCameraId ?: return false
                    camera.setTorchMode(id, on)
                    torchOn = on
                }
                QuickToggle.DoNotDisturb ->
                    notifications.setInterruptionFilter(
                        if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL,
                    )
                QuickToggle.AutoRotate ->
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        if (on) 1 else 0,
                    )
                else -> return false
            }
            true
        }.getOrDefault(false)

    override fun permissions(toggle: QuickToggle) =
        if (toggle ==
            QuickToggle.Bluetooth
        ) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList()
        }

    // REQUEST_DISABLE isn't public API, but Settings handles it on AOSP and One UI; FlipToggleAction falls back if refused.
    override fun confirmIntent(
        toggle: QuickToggle,
        on: Boolean,
    ): Intent? =
        if (toggle == QuickToggle.Bluetooth) {
            Intent(if (on) BluetoothAdapter.ACTION_REQUEST_ENABLE else REQUEST_DISABLE)
        } else {
            null
        }

    override fun accessIntent(toggle: QuickToggle): Intent? =
        when (toggle) {
            QuickToggle.DoNotDisturb -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            QuickToggle.AutoRotate -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", context.packageName, null))
            else -> null
        }

    // The camera service only reports torch state through a callback, so track it from first use.
    private fun watchTorch(id: String): Boolean {
        if (!torchCallbackRegistered) {
            torchCallbackRegistered = true
            camera.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(
                        cameraId: String,
                        enabled: Boolean,
                    ) {
                        if (cameraId == id) torchOn = enabled
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
        return torchOn
    }
}
