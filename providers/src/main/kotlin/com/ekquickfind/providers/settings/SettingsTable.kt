package com.ekquickfind.providers.settings

import android.content.Intent
import android.provider.Settings
import android.provider.Settings.Panel

/** A device Settings screen users can jump to. Data only; matching lives in the provider. */
data class SettingsEntry(
    val key: String,
    val title: String,
    val breadcrumb: String,
    val keywords: List<String>,
    val action: String,
    /** Opened when [action] doesn't exist on this device; usually the parent screen. */
    val fallbackAction: String,
)

object SettingsTable {
    private const val ROOT = Settings.ACTION_SETTINGS
    private const val NETWORK = Settings.ACTION_WIRELESS_SETTINGS
    private const val DISPLAY = Settings.ACTION_DISPLAY_SETTINGS
    private const val SOUND = Settings.ACTION_SOUND_SETTINGS
    private const val APPS = Settings.ACTION_APPLICATION_SETTINGS
    private const val SECURITY = Settings.ACTION_SECURITY_SETTINGS
    private const val PRIVACY = Settings.ACTION_PRIVACY_SETTINGS
    private const val CONNECTED = Settings.ACTION_BLUETOOTH_SETTINGS
    private const val BATTERY = Intent.ACTION_POWER_USAGE_SUMMARY
    private const val ACCESSIBILITY = Settings.ACTION_ACCESSIBILITY_SETTINGS
    private const val ABOUT = Settings.ACTION_DEVICE_INFO_SETTINGS

    // Not in the public SDK but exported by AOSP Settings; the device check falls back when missing.
    private const val TETHER = "android.settings.TETHER_SETTINGS"
    private const val SYSTEM_UPDATE = "android.settings.SYSTEM_UPDATE_SETTINGS"
    private const val BACKUP = "android.settings.BACKUP_AND_RESTORE_SETTINGS"
    private const val GESTURES = "android.settings.GESTURE_SETTINGS"
    private const val TEXT_READING = "android.settings.TEXT_READING_SETTINGS"
    private const val PRIVATE_DNS = "android.settings.NETWORK_AND_INTERNET_SETTINGS"
    private const val MOBILE_NETWORK = "android.settings.NETWORK_OPERATOR_SETTINGS"
    private const val EMERGENCY = "android.settings.EMERGENCY_SETTINGS"
    private const val WORK_PROFILE = "android.settings.MANAGED_PROFILE_SETTINGS"
    private const val LOCK_SCREEN = "android.settings.LOCK_SCREEN_SETTINGS"
    private const val NOTIFICATION_HISTORY = "android.settings.NOTIFICATION_HISTORY"
    private const val MODES = "android.settings.AUTOMATIC_ZEN_RULE_SETTINGS"
    private const val DARK_THEME = "android.settings.DARK_THEME_SETTINGS"
    private const val HEARING_DEVICES = "android.settings.HEARING_DEVICES_SETTINGS"
    private const val USER_SETTINGS = "android.settings.USER_SETTINGS"

    private fun e(
        key: String,
        title: String,
        breadcrumb: String,
        keywords: String,
        action: String,
        fallback: String,
    ) = SettingsEntry(key, title, breadcrumb, keywords.split(',').map { it.trim() }, action, fallback)

    val entries: List<SettingsEntry> =
        listOf(
            // Network & internet
            e("network", "Network & internet", "Settings", "network, internet, connections, connectivity", NETWORK, ROOT),
            e("wifi", "Wi‑Fi", "Network & internet", "wifi, wi-fi, wireless, wlan, internet", Settings.ACTION_WIFI_SETTINGS, NETWORK),
            e(
                "wifi-panel",
                "Wi‑Fi quick panel",
                "Network & internet",
                "wifi panel, join wifi, connect wifi",
                Panel.ACTION_WIFI,
                Settings.ACTION_WIFI_SETTINGS,
            ),
            e(
                "internet-panel",
                "Internet",
                "Network & internet",
                "internet panel, connectivity, online, offline",
                Panel.ACTION_INTERNET_CONNECTIVITY,
                NETWORK,
            ),
            e(
                "mobile-network",
                "Mobile network",
                "Network & internet",
                "mobile network, cellular, sim, carrier, operator, 5g, 4g",
                MOBILE_NETWORK,
                NETWORK,
            ),
            e(
                "roaming",
                "Roaming",
                "Network & internet",
                "roaming, data roaming, abroad, travel",
                Settings.ACTION_DATA_ROAMING_SETTINGS,
                NETWORK,
            ),
            e(
                "data-usage",
                "Data usage",
                "Network & internet",
                "data usage, data saver, data limit, mobile data",
                Settings.ACTION_DATA_USAGE_SETTINGS,
                NETWORK,
            ),
            e(
                "hotspot",
                "Hotspot & tethering",
                "Network & internet",
                "hotspot, tethering, share internet, mobile hotspot, usb tethering",
                TETHER,
                NETWORK,
            ),
            e(
                "airplane",
                "Airplane mode",
                "Network & internet",
                "airplane, aeroplane, flight mode, offline mode",
                Settings.ACTION_AIRPLANE_MODE_SETTINGS,
                NETWORK,
            ),
            e("vpn", "VPN", "Network & internet", "vpn, virtual private network, proxy", Settings.ACTION_VPN_SETTINGS, NETWORK),
            e("private-dns", "Private DNS", "Network & internet", "private dns, dns, ad blocking dns", PRIVATE_DNS, NETWORK),
            e(
                "wifi-advanced",
                "Wi‑Fi preferences",
                "Network & internet",
                "wifi preferences, wifi advanced, auto connect",
                Settings.ACTION_WIFI_IP_SETTINGS,
                Settings.ACTION_WIFI_SETTINGS,
            ),
            // Connected devices
            e(
                "bluetooth",
                "Bluetooth",
                "Connected devices",
                "bluetooth, bt, headphones, earbuds, pair, pairing",
                Settings.ACTION_BLUETOOTH_SETTINGS,
                ROOT,
            ),
            e("nfc", "NFC", "Connected devices", "nfc, contactless, tap to pay, near field", Settings.ACTION_NFC_SETTINGS, CONNECTED),
            e(
                "nfc-payment",
                "Contactless payments",
                "Connected devices",
                "payment, google wallet, tap and pay, default payment",
                Settings.ACTION_NFC_PAYMENT_SETTINGS,
                Settings.ACTION_NFC_SETTINGS,
            ),
            e(
                "cast",
                "Cast",
                "Connected devices",
                "cast, screen mirroring, smart view, chromecast, mirror screen",
                Settings.ACTION_CAST_SETTINGS,
                CONNECTED,
            ),
            e("print", "Printing", "Connected devices", "print, printer, printing", Settings.ACTION_PRINT_SETTINGS, CONNECTED),
            // Display
            e("display", "Display", "Settings", "display, screen", DISPLAY, ROOT),
            e("dark-theme", "Dark theme", "Display", "dark mode, dark theme, night mode, black theme", DARK_THEME, DISPLAY),
            e("brightness", "Brightness", "Display", "brightness, adaptive brightness, auto brightness, dim", DISPLAY, ROOT),
            e(
                "night-light",
                "Night light",
                "Display",
                "night light, eye comfort, blue light, night shift, warm screen",
                Settings.ACTION_NIGHT_DISPLAY_SETTINGS,
                DISPLAY,
            ),
            e("screen-timeout", "Screen timeout", "Display", "screen timeout, sleep, screen off, auto lock", DISPLAY, ROOT),
            e(
                "font-size",
                "Font size and style",
                "Display",
                "font size, text size, bigger text, display size, zoom",
                TEXT_READING,
                DISPLAY,
            ),
            e("auto-rotate", "Auto-rotate", "Display", "rotate, rotation, auto rotate, orientation, landscape", DISPLAY, ROOT),
            e("screensaver", "Screen saver", "Display", "screensaver, screen saver, daydream", Settings.ACTION_DREAM_SETTINGS, DISPLAY),
            e(
                "wallpaper",
                "Wallpaper & style",
                "Settings",
                "wallpaper, background, theme, style, colors",
                Intent.ACTION_SET_WALLPAPER,
                ROOT,
            ),
            // Sound & notifications
            e("sound", "Sound & vibration", "Settings", "sound, volume, vibration, audio", SOUND, ROOT),
            e(
                "volume-panel",
                "Volume",
                "Sound & vibration",
                "volume panel, media volume, ring volume, loudness",
                Panel.ACTION_VOLUME,
                SOUND,
            ),
            e("ringtone", "Ringtone", "Sound & vibration", "ringtone, ring tone, call sound, notification sound", SOUND, ROOT),
            e("vibration", "Vibration & haptics", "Sound & vibration", "vibrate, vibration, haptics, haptic feedback", SOUND, ROOT),
            e(
                "dnd",
                "Do Not Disturb",
                "Notifications",
                "do not disturb, dnd, silence, focus, quiet",
                Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
                SOUND,
            ),
            e(
                "modes",
                "Modes",
                "Notifications",
                "modes, routines, bedtime mode, driving mode, schedules",
                MODES,
                Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
            ),
            e(
                "notifications",
                "App notifications",
                "Notifications",
                "notifications, alerts, badges, notification settings",
                Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS,
                ROOT,
            ),
            e(
                "notification-history",
                "Notification history",
                "Notifications",
                "notification history, dismissed notifications, missed",
                NOTIFICATION_HISTORY,
                Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS,
            ),
            e(
                "notification-access",
                "Notification access",
                "Apps",
                "notification access, notification listener, read notifications",
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                APPS,
            ),
            e(
                "dnd-access",
                "Do Not Disturb access",
                "Apps",
                "dnd access, notification policy, do not disturb permission",
                Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
                APPS,
            ),
            // Apps
            e("apps", "Apps", "Settings", "apps, applications, app info, uninstall, force stop", APPS, ROOT),
            e(
                "all-apps",
                "All apps",
                "Apps",
                "all apps, installed apps, manage apps, app list",
                Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS,
                APPS,
            ),
            e(
                "default-apps",
                "Default apps",
                "Apps",
                "default apps, default browser, default sms, launcher, open by default",
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
                APPS,
            ),
            e(
                "assistant",
                "Digital assistant",
                "Apps",
                "assistant, digital assistant, voice input, default assistant",
                Settings.ACTION_VOICE_INPUT_SETTINGS,
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            ),
            e(
                "home-app",
                "Home app",
                "Apps",
                "home screen, launcher, home app, default launcher",
                Settings.ACTION_HOME_SETTINGS,
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            ),
            e(
                "usage-access",
                "Usage access",
                "Apps",
                "usage access, usage stats, app usage permission",
                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                APPS,
            ),
            e(
                "overlay",
                "Appear on top",
                "Apps",
                "overlay, draw over apps, display over other apps, appear on top",
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                APPS,
            ),
            e(
                "unknown-apps",
                "Install unknown apps",
                "Apps",
                "unknown sources, sideload, install apk, unknown apps",
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                APPS,
            ),
            e(
                "all-files",
                "All files access",
                "Apps",
                "all files access, manage storage permission, file access",
                Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                APPS,
            ),
            e(
                "write-settings",
                "Modify system settings",
                "Apps",
                "modify system settings, write settings",
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                APPS,
            ),
            e(
                "picture-in-picture",
                "Picture-in-picture",
                "Apps",
                "picture in picture, pip, floating video",
                "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                APPS,
            ),
            e(
                "app-languages",
                "App languages",
                "Apps",
                "app language, per app language, app locale",
                Settings.ACTION_APP_LOCALE_SETTINGS,
                Settings.ACTION_LOCALE_SETTINGS,
            ),
            // Battery & storage
            e("battery", "Battery", "Settings", "battery, battery usage, power, charging", BATTERY, ROOT),
            e(
                "battery-saver",
                "Battery saver",
                "Battery",
                "battery saver, power saving, low power mode, power saver",
                Settings.ACTION_BATTERY_SAVER_SETTINGS,
                BATTERY,
            ),
            e(
                "battery-optimisation",
                "Battery optimisation",
                "Battery",
                "battery optimization, battery optimisation, doze, background restriction, sleeping apps",
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                BATTERY,
            ),
            e(
                "storage",
                "Storage",
                "Settings",
                "storage, space, free up space, sd card, memory card, clean up",
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
                ROOT,
            ),
            e(
                "memory-card",
                "Memory card",
                "Storage",
                "memory card, sd card, external storage, usb storage",
                Settings.ACTION_MEMORY_CARD_SETTINGS,
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            ),
            // Location, security & privacy
            e(
                "location",
                "Location",
                "Settings",
                "location, gps, position, location services",
                Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                ROOT,
            ),
            e("security", "Security", "Settings", "security, find my device, encryption, security update", SECURITY, ROOT),
            e("screen-lock", "Screen lock", "Security", "screen lock, lock type, pin, password, pattern, unlock", SECURITY, ROOT),
            e(
                "lock-screen",
                "Lock screen",
                "Security",
                "lock screen, always on display, aod, lock screen notifications",
                LOCK_SCREEN,
                SECURITY,
            ),
            e(
                "biometrics",
                "Biometrics",
                "Security",
                "fingerprint, face unlock, biometrics, face recognition",
                Settings.ACTION_BIOMETRIC_ENROLL,
                SECURITY,
            ),
            e("privacy", "Privacy", "Settings", "privacy, permissions, permission manager, privacy dashboard", PRIVACY, ROOT),
            e(
                "work-profile",
                "Work profile",
                "Accounts",
                "work profile, work apps, managed profile",
                WORK_PROFILE,
                Settings.ACTION_SYNC_SETTINGS,
            ),
            e("emergency", "Safety & emergency", "Settings", "emergency, sos, medical info, emergency contacts, safety", EMERGENCY, ROOT),
            // Accounts & passwords
            e(
                "accounts",
                "Accounts",
                "Settings",
                "accounts, sync, google account, sign in, account sync",
                Settings.ACTION_SYNC_SETTINGS,
                ROOT,
            ),
            e(
                "add-account",
                "Add account",
                "Accounts",
                "add account, new account, sign in account",
                Settings.ACTION_ADD_ACCOUNT,
                Settings.ACTION_SYNC_SETTINGS,
            ),
            e(
                "passwords",
                "Passwords & autofill",
                "Accounts",
                "passwords, autofill, passkeys, password manager, credentials",
                "android.settings.CREDENTIAL_PROVIDER",
                Settings.ACTION_SYNC_SETTINGS,
            ),
            // Accessibility
            e(
                "accessibility",
                "Accessibility",
                "Settings",
                "accessibility, talkback, screen reader, magnification, vision",
                ACCESSIBILITY,
                ROOT,
            ),
            e(
                "captions",
                "Caption preferences",
                "Accessibility",
                "captions, subtitles, live caption, closed captions",
                Settings.ACTION_CAPTIONING_SETTINGS,
                ACCESSIBILITY,
            ),
            e("hearing", "Hearing devices", "Accessibility", "hearing aid, hearing devices, hearing", HEARING_DEVICES, ACCESSIBILITY),
            // System
            e("languages", "Languages", "System", "language, languages, locale, region, translate", Settings.ACTION_LOCALE_SETTINGS, ROOT),
            e(
                "regional",
                "Regional preferences",
                "System",
                "regional preferences, units, temperature unit, first day of week",
                Settings.ACTION_REGIONAL_PREFERENCES_SETTINGS,
                Settings.ACTION_LOCALE_SETTINGS,
            ),
            e(
                "keyboard",
                "Keyboard",
                "System",
                "keyboard, input method, gboard, samsung keyboard, typing",
                Settings.ACTION_INPUT_METHOD_SETTINGS,
                ROOT,
            ),
            e(
                "hard-keyboard",
                "Physical keyboard",
                "System",
                "physical keyboard, hardware keyboard, keyboard layout",
                Settings.ACTION_HARD_KEYBOARD_SETTINGS,
                Settings.ACTION_INPUT_METHOD_SETTINGS,
            ),
            e("date-time", "Date & time", "System", "date, time, time zone, clock, 24 hour", Settings.ACTION_DATE_SETTINGS, ROOT),
            e(
                "gestures",
                "Navigation & gestures",
                "System",
                "gestures, navigation bar, gesture navigation, buttons, swipe gestures",
                GESTURES,
                ROOT,
            ),
            e("backup", "Backup", "System", "backup, restore, google backup, cloud backup", BACKUP, ROOT),
            e("users", "Users", "System", "users, guest, multiple users, add user", USER_SETTINGS, ROOT),
            e(
                "developer",
                "Developer options",
                "System",
                "developer options, usb debugging, adb, developer",
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                ABOUT,
            ),
            e("software-update", "Software update", "System", "software update, system update, update, ota, upgrade", SYSTEM_UPDATE, ABOUT),
            e(
                "about",
                "About phone",
                "Settings",
                "about phone, phone info, model, android version, build number, imei, serial",
                ABOUT,
                ROOT,
            ),
            e("settings", "Settings", "Settings", "settings, preferences, options, setup", ROOT, ROOT),
        )
}
