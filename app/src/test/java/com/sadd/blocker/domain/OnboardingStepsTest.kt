package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStepsTest {

    @Test
    fun orderHasSixStepsWithRestrictedBeforeAccessibility() {
        assertEquals(6, OnboardingSteps.ORDER.size)
        assertEquals("notification", OnboardingSteps.ORDER.first())
        assertTrue(
            OnboardingSteps.ORDER.indexOf(OnboardingSteps.KEY_RESTRICTED) <
                OnboardingSteps.ORDER.indexOf(OnboardingSteps.KEY_ACCESSIBILITY),
        )
    }

    @Test
    fun restrictedDoneWhenUserConfirmed() {
        assertTrue(
            OnboardingSteps.isRestrictedDone(
                accessibilityEnabled = false,
                userConfirmed = true,
            ),
        )
    }

    @Test
    fun restrictedDoneWhenAccessibilityEnabled() {
        assertTrue(
            OnboardingSteps.isRestrictedDone(
                accessibilityEnabled = true,
                userConfirmed = false,
            ),
        )
    }

    @Test
    fun restrictedNotDoneWithoutConfirmationOrAccessibility() {
        assertFalse(
            OnboardingSteps.isRestrictedDone(
                accessibilityEnabled = false,
                userConfirmed = false,
            ),
        )
    }

    @Test
    fun firstMissingIndexFindsFirstUngranted() {
        assertEquals(0, OnboardingSteps.firstMissingIndex(listOf(false, true, true)))
        assertEquals(2, OnboardingSteps.firstMissingIndex(listOf(true, true, false)))
    }

    @Test
    fun firstMissingIndexReturnsSizeWhenAllGranted() {
        assertEquals(3, OnboardingSteps.firstMissingIndex(listOf(true, true, true)))
        assertEquals(0, OnboardingSteps.firstMissingIndex(emptyList()))
    }
}
