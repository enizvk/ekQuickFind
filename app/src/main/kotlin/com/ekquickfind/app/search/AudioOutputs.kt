package com.ekquickfind.app.search

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * The headphones or car the phone is playing through, by name ("Galaxy Buds", "My Car"); null for its own speaker.
 * Audio devices are readable without the Nearby devices permission.
 */
class AudioOutputs(
    private val context: Context,
) {
    fun current(): String? {
        val device =
            context
                .getSystemService(AudioManager::class.java)
                .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type in EXTERNAL } ?: return null
        return device.productName
            ?.toString()
            ?.trim()
            ?.ifEmpty { null } ?: GENERIC
    }

    private companion object {
        const val GENERIC = "Headphones"
        val EXTERNAL =
            setOf(
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
            )
    }
}
