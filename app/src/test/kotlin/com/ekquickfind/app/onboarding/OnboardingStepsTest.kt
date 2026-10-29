package com.ekquickfind.app.onboarding

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepsTest {
    private val all = OnboardingPermissions.runtime

    @Test
    fun firstTheMissingPermissionsAreAskedForInOneGo() {
        val missing = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR)
        assertEquals(
            OnboardingStep.AskRuntime(missing),
            OnboardingSteps.next(missing, askedRuntime = false, canDrawOverlays = false, floatingButton = true),
        )
    }

    @Test
    fun afterAskingOnceItMovesOnEvenIfSomeWereDenied() {
        assertEquals(
            OnboardingStep.OpenOverlaySettings,
            OnboardingSteps.next(
                listOf(Manifest.permission.READ_CONTACTS),
                askedRuntime = true,
                canDrawOverlays = false,
                floatingButton = true,
            ),
        )
    }

    @Test
    fun theFloatingButtonPageOpensOnlyWhenTheButtonIsOnAndNotAllowedYet() {
        assertEquals(
            OnboardingStep.Done,
            OnboardingSteps.next(emptyList(), askedRuntime = true, canDrawOverlays = true, floatingButton = true),
        )
        assertEquals(
            OnboardingStep.Done,
            OnboardingSteps.next(emptyList(), askedRuntime = true, canDrawOverlays = false, floatingButton = false),
        )
        assertEquals(
            OnboardingStep.OpenOverlaySettings,
            OnboardingSteps.next(emptyList(), askedRuntime = false, canDrawOverlays = false, floatingButton = true),
        )
    }

    @Test
    fun everythingAllowedIsDone() {
        assertEquals(
            OnboardingStep.Done,
            OnboardingSteps.next(emptyList(), askedRuntime = false, canDrawOverlays = true, floatingButton = true),
        )
    }

    @Test
    fun notificationsAreNotAskedFor() {
        assertEquals(false, Manifest.permission.POST_NOTIFICATIONS in all)
        assertEquals(true, Manifest.permission.READ_CALENDAR in all)
    }
}
