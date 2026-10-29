package com.ekquickfind.app.onboarding

import android.Manifest

/**
 * What the first-start welcome asks for. Notifications aren't: they're only the app's own status
 * (updates, photo tagging, the floating button), and everything works without them.
 */
object OnboardingPermissions {
    val runtime =
        listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
}

sealed interface OnboardingStep {
    /** One system request; Android shows the dialogs one after another. */
    data class AskRuntime(
        val permissions: List<String>,
    ) : OnboardingStep

    /** "Display over other apps" only exists as a system page; it opens right at ekQuickFind's switch. */
    data object OpenOverlaySettings : OnboardingStep

    data object Done : OnboardingStep
}

object OnboardingSteps {
    /** The next thing "Allow all" does. Runtime permissions are asked once, so a denial doesn't loop. */
    fun next(
        missingRuntime: List<String>,
        askedRuntime: Boolean,
        canDrawOverlays: Boolean,
        floatingButton: Boolean,
    ): OnboardingStep =
        when {
            missingRuntime.isNotEmpty() && !askedRuntime -> OnboardingStep.AskRuntime(missingRuntime)
            floatingButton && !canDrawOverlays -> OnboardingStep.OpenOverlaySettings
            else -> OnboardingStep.Done
        }
}
