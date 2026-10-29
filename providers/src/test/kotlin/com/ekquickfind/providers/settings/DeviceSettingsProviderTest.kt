package com.ekquickfind.providers.settings

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Query
import com.ekquickfind.providers.FakeAndroidHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceSettingsProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun provider(
        resolvable: (String) -> Boolean = { true },
        toggles: ToggleStates = FakeToggles(),
    ) = DeviceSettingsProvider(SettingsTable.entries, IntentResolver { resolvable(it) }, toggles)

    @Test
    fun hotspotShowsTitleWithItsBreadcrumb() =
        runTest {
            val row = provider().search(Query("hotspot")).first().first()
            assertEquals("Hotspot & tethering", row.title)
            assertEquals("Network & internet", row.subtitle)
        }

    @Test
    fun darkModeFindsTheDarkThemeScreen() =
        runTest {
            assertEquals(
                "Dark theme",
                provider()
                    .search(Query("dark mode"))
                    .first()
                    .first()
                    .title,
            )
        }

    @Test
    fun opensTheExactScreenWhenItExists() =
        runTest {
            val host = FakeAndroidHost(context)
            val outcome =
                provider()
                    .search(Query("vpn"))
                    .first()
                    .first()
                    .primaryAction
                    .perform(host)
            assertEquals(Settings.ACTION_VPN_SETTINGS, host.started.single().action)
            assertEquals(ActionOutcome.Close, outcome)
        }

    @Test
    fun fallsBackToTheParentScreenWhenTheDeviceLacksIt() =
        runTest {
            val host = FakeAndroidHost(context)
            val missing = setOf("android.settings.NETWORK_OPERATOR_SETTINGS")
            provider(resolvable = { it !in missing })
                .search(Query("mobile network"))
                .first()
                .first()
                .primaryAction
                .perform(host)
            assertEquals(Settings.ACTION_WIRELESS_SETTINGS, host.started.single().action)
        }

    @Test
    fun fallsBackToTheSettingsRootWhenTheParentIsMissingToo() =
        runTest {
            val host = FakeAndroidHost(context)
            val missing = setOf("android.settings.NETWORK_OPERATOR_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS)
            provider(resolvable = { it !in missing })
                .search(Query("mobile network"))
                .first()
                .first()
                .primaryAction
                .perform(host)
            assertEquals(Settings.ACTION_SETTINGS, host.started.single().action)
        }

    // 7.3: live toggles

    @Test
    fun bluetoothToggleShowsItsStateAndOpensBluetoothSettings() =
        runTest {
            val toggles = FakeToggles().apply { states[QuickToggle.Bluetooth] = true }
            val row = provider(toggles = toggles).search(Query("bluetooth")).first().first()
            assertEquals("Bluetooth", row.title)
            assertEquals("On", row.subtitle)

            val host = FakeAndroidHost(context)
            row.primaryAction.perform(host)
            assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, host.started.single().action)
        }

    @Test
    fun wifiToggleOpensTheWifiPanel() =
        runTest {
            val row = provider().search(Query("wifi")).first().first()
            assertEquals("Off", row.subtitle)
            val host = FakeAndroidHost(context)
            row.primaryAction.perform(host)
            assertEquals(Settings.Panel.ACTION_WIFI, host.started.single().action)
        }

    @Test
    fun torchAndFlashlightToggleTheTorchWithoutLeavingSearch() =
        runTest {
            val toggles = FakeToggles().apply { controls[QuickToggle.Flashlight] = ToggleControl.Direct }
            listOf("torch", "flashlight").forEach { q ->
                val row = provider(toggles = toggles).search(Query(q)).first().first()
                assertEquals("Flashlight", row.title)
                assertEquals(ActionOutcome.Refresh, row.primaryAction.perform(FakeAndroidHost(context)))
            }
            assertEquals(listOf(QuickToggle.Flashlight to true, QuickToggle.Flashlight to false), toggles.sets)
        }

    // 7.5: inline switches

    @Test
    fun toggleResultsCarryASwitchWithTheCurrentState() =
        runTest {
            val toggles = FakeToggles().apply { states[QuickToggle.Bluetooth] = true }
            val row = provider(toggles = toggles).search(Query("bluetooth")).first().first()
            assertEquals(true, row.toggle?.checked)
            assertEquals(
                null,
                provider()
                    .search(Query("hotspot"))
                    .first()
                    .first()
                    .toggle,
            )
        }

    @Test
    fun directTogglesFlipInPlace() =
        runTest {
            val toggles = FakeToggles().apply { controls[QuickToggle.DoNotDisturb] = ToggleControl.Direct }
            val row = provider(toggles = toggles).search(Query("do not disturb")).first().first()
            val host = FakeAndroidHost(context)
            assertEquals(ActionOutcome.Refresh, row.toggle!!.action.perform(host))
            assertEquals(listOf(QuickToggle.DoNotDisturb to true), toggles.sets)
            assertTrue(host.started.isEmpty())
        }

    @Test
    fun togglesNeedingAccessAskForItFirst() =
        runTest {
            val toggles = FakeToggles().apply { controls[QuickToggle.AutoRotate] = ToggleControl.NeedsAccess }
            val row = provider(toggles = toggles).search(Query("auto rotate")).first().first { it.toggle != null }
            val host = FakeAndroidHost(context)
            assertEquals(ActionOutcome.Stay, row.toggle!!.action.perform(host))
            assertEquals("access.AutoRotate", host.started.single().action)
            assertTrue(toggles.sets.isEmpty())
        }

    // 7.6: Bluetooth through the system dialog

    private fun bluetoothRow(toggles: FakeToggles) =
        kotlinx.coroutines.runBlocking { provider(toggles = toggles).search(Query("bluetooth")).first().first { it.toggle != null } }

    @Test
    fun bluetoothAsksTheSystemToTurnItOnAndWaitsForTheNewState() =
        runTest {
            val toggles = FakeToggles().apply { controls[QuickToggle.Bluetooth] = ToggleControl.Confirm }
            // Allow in the dialog: the radio comes on a moment later.
            val host = FakeAndroidHost(context, onResult = { toggles.states[QuickToggle.Bluetooth] = true })
            assertEquals(ActionOutcome.Refresh, bluetoothRow(toggles).toggle!!.action.perform(host))
            assertEquals(listOf("confirm.on.Bluetooth"), host.startedForResult.map { it.action })
            assertTrue("no settings page", host.started.isEmpty())
        }

    @Test
    fun bluetoothOffUsesTheTurnOffDialog() =
        runTest {
            val toggles =
                FakeToggles().apply {
                    controls[QuickToggle.Bluetooth] = ToggleControl.Confirm
                    states[QuickToggle.Bluetooth] = true
                }
            val host = FakeAndroidHost(context, onResult = { toggles.states[QuickToggle.Bluetooth] = false })
            bluetoothRow(toggles).toggle!!.action.perform(host)
            assertEquals(listOf("confirm.off.Bluetooth"), host.startedForResult.map { it.action })
        }

    @Test
    fun denyingTheDialogKeepsSearchAsItWas() =
        runTest {
            val toggles = FakeToggles().apply { controls[QuickToggle.Bluetooth] = ToggleControl.Confirm }
            val action = bluetoothRow(toggles).toggle!!.action as FlipToggleAction
            // The user looked at the dialog for a while, then said no.
            val clock = sequenceOf(0L, 2_000L).iterator()
            val host = FakeAndroidHost(context, result = android.app.Activity.RESULT_CANCELED)
            assertEquals(ActionOutcome.Stay, action.copy(clock = ElapsedClock { clock.next() }).perform(host))
            assertTrue("denying must not open settings", host.started.isEmpty())
        }

    @Test
    fun anInstantRefusalWithoutADialogFallsBackToBluetoothSettings() =
        runTest {
            val toggles =
                FakeToggles().apply {
                    controls[QuickToggle.Bluetooth] = ToggleControl.Confirm
                    states[QuickToggle.Bluetooth] = true
                }
            val action = bluetoothRow(toggles).toggle!!.action as FlipToggleAction
            val clock = sequenceOf(0L, 40L).iterator()
            val host = FakeAndroidHost(context, result = android.app.Activity.RESULT_CANCELED)
            action.copy(clock = ElapsedClock { clock.next() }).perform(host)
            assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, host.started.single().action)
        }

    @Test
    fun bluetoothAsksForNearbyDevicesFirst() =
        runTest {
            val toggles = FakeToggles().apply { controls[QuickToggle.Bluetooth] = ToggleControl.NeedsPermission }
            val denied = FakeAndroidHost(context, grant = false)
            assertEquals(ActionOutcome.Stay, bluetoothRow(toggles).toggle!!.action.perform(denied))
            assertEquals(listOf(listOf("perm.Bluetooth")), denied.requested)
            assertTrue(denied.startedForResult.isEmpty())

            val granted = FakeAndroidHost(context, grant = true, onResult = { toggles.states[QuickToggle.Bluetooth] = true })
            bluetoothRow(toggles).toggle!!.action.perform(granted)
            assertEquals(listOf("confirm.on.Bluetooth"), granted.startedForResult.map { it.action })
        }

    @Test
    fun systemOnlyTogglesOpenTheirPanelAndKeepSearchOpen() =
        runTest {
            val row = provider().search(Query("wifi")).first().first()
            val host = FakeAndroidHost(context)
            assertEquals(ActionOutcome.Stay, row.toggle!!.action.perform(host))
            assertEquals(Settings.Panel.ACTION_WIFI, host.started.single().action)
        }

    @Test
    fun togglesWithUnknownStateAreHidden() =
        runTest {
            val toggles = FakeToggles().apply { states[QuickToggle.Flashlight] = null }
            assertTrue(provider(toggles = toggles).search(Query("torch")).first().none { it.title == "Flashlight" })
        }

    @Test
    fun togglesRankAboveTheMatchingScreen() =
        runTest {
            val rows = provider().search(Query("battery saver")).first()
            assertEquals(listOf("Battery saver", "Battery saver"), rows.take(2).map { it.title })
            assertEquals("Off", rows.first().subtitle)
        }
}
