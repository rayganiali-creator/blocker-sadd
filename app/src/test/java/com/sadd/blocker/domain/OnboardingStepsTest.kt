package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStepsTest {

    @Test
    fun orderHasSixStepsWithAccessibilitySecondAndProtectionLast() {
        assertEquals(6, OnboardingSteps.ORDER.size)
        assertEquals("notification", OnboardingSteps.ORDER.first())
        assertEquals("accessibility", OnboardingSteps.ORDER[1])
        assertEquals("protection", OnboardingSteps.ORDER.last())
    }

    @Test
    fun permissionOrderHasFiveStepsEndingWithAlarm() {
        assertEquals(5, OnboardingSteps.PERMISSION_ORDER.size)
        assertEquals("alarm", OnboardingSteps.PERMISSION_ORDER.last())
        assertFalse(OnboardingSteps.PERMISSION_ORDER.contains("protection"))
    }

    @Test
    fun accessibilityPhaseIsZeroWhenServiceEnabled() {
        assertEquals(0, OnboardingSteps.accessibilityPhase(storedPhase = 2, accessibilityEnabled = true))
    }

    @Test
    fun accessibilityPhaseCoercesStoredValueIntoRange() {
        assertEquals(1, OnboardingSteps.accessibilityPhase(storedPhase = 0, accessibilityEnabled = false))
        assertEquals(3, OnboardingSteps.accessibilityPhase(storedPhase = 99, accessibilityEnabled = false))
        assertEquals(2, OnboardingSteps.accessibilityPhase(storedPhase = 2, accessibilityEnabled = false))
    }

    @Test
    fun accessibilityPhaseDoneWhenServiceEnabled() {
        assertTrue(OnboardingSteps.isAccessibilityPhaseDone(1, 1, true))
        assertTrue(OnboardingSteps.isAccessibilityPhaseDone(3, 3, true))
    }

    @Test
    fun accessibilityPhaseDoneOnlyForEarlierPhases() {
        // مرحله فعلی ۲ است: مرحله ۱ دستی تایید شده، مرحله ۲ و ۳ نه.
        assertTrue(OnboardingSteps.isAccessibilityPhaseDone(1, 2, false))
        assertFalse(OnboardingSteps.isAccessibilityPhaseDone(2, 2, false))
        assertFalse(OnboardingSteps.isAccessibilityPhaseDone(3, 2, false))
    }

    @Test
    fun nextAccessibilityPhaseAdvancesButNotPastThree() {
        assertEquals(2, OnboardingSteps.nextAccessibilityPhase(1))
        assertEquals(3, OnboardingSteps.nextAccessibilityPhase(2))
        assertEquals(3, OnboardingSteps.nextAccessibilityPhase(3))
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

    @Test
    fun protectionStepDoneWhenChoiceMadeOrAdminActive() {
        // «فعال کن» یا «الان نه» — هر دو انتخاب را ثبت می‌کنند.
        assertTrue(OnboardingSteps.isProtectionStepDone(choiceDone = true, adminActive = false))
        // مدیر دستگاه دستی فعال شده، بدون انتخاب در ویزارد.
        assertTrue(OnboardingSteps.isProtectionStepDone(choiceDone = false, adminActive = true))
        assertTrue(OnboardingSteps.isProtectionStepDone(choiceDone = true, adminActive = true))
        assertFalse(OnboardingSteps.isProtectionStepDone(choiceDone = false, adminActive = false))
    }

    @Test
    fun tutorialShowsOnlyAfterAllStepsAndOnlyOnce() {
        assertTrue(OnboardingSteps.shouldShowTutorial(allStepsDone = true, tutorialDone = false))
        // هنوز مرحله‌ای مانده — آموزش نشان داده نمی‌شود.
        assertFalse(OnboardingSteps.shouldShowTutorial(allStepsDone = false, tutorialDone = false))
        // آموزش دیده شده — دیگر نشان داده نمی‌شود.
        assertFalse(OnboardingSteps.shouldShowTutorial(allStepsDone = true, tutorialDone = true))
        assertFalse(OnboardingSteps.shouldShowTutorial(allStepsDone = false, tutorialDone = true))
    }
}
