package com.ekquickfind.providers.settings

import android.content.Intent

/** Configurable toggle states for tests; records every set() call. */
class FakeToggles(
    val states: MutableMap<QuickToggle, Boolean?> = QuickToggle.entries.associateWith<QuickToggle, Boolean?> { false }.toMutableMap(),
    val controls: MutableMap<QuickToggle, ToggleControl> = QuickToggle.entries.associateWith { ToggleControl.SystemOnly }.toMutableMap(),
) : ToggleStates {
    val sets = mutableListOf<Pair<QuickToggle, Boolean>>()

    override fun state(toggle: QuickToggle) = states[toggle]

    override fun control(toggle: QuickToggle) = controls.getValue(toggle)

    override fun set(
        toggle: QuickToggle,
        on: Boolean,
    ): Boolean {
        sets += toggle to on
        states[toggle] = on
        return true
    }

    override fun accessIntent(toggle: QuickToggle): Intent? = Intent("access.${toggle.name}")

    override fun permissions(toggle: QuickToggle) = listOf("perm.${toggle.name}")

    override fun confirmIntent(
        toggle: QuickToggle,
        on: Boolean,
    ): Intent? = Intent(if (on) "confirm.on.${toggle.name}" else "confirm.off.${toggle.name}")
}
