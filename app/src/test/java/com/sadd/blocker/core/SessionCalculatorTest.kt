package com.sadd.blocker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCalculatorTest {

    @Test
    fun remainingIsEndMinusNow() {
        assertEquals(60_000L, SessionCalculator.remainingMs(1_000_000L, 940_000L))
    }

    @Test
    fun expiredReturnsZeroNeverNegative() {
        assertEquals(0L, SessionCalculator.remainingMs(1_000L, 2_000L))
        assertFalse(SessionCalculator.isActive(1_000L, 2_000L))
        assertTrue(SessionCalculator.isExpired(1_000L, 2_000L))
    }

    @Test
    fun activeWhenRemainingPositive() {
        assertTrue(SessionCalculator.isActive(2_000L, 1_000L))
    }

    @Test
    fun extendAdds15MinutesOnlyWhenActive() {
        assertEquals(2_000L + 900_000L, SessionCalculator.extendBy15Min(2_000L, 1_000L))
        assertNull(SessionCalculator.extendBy15Min(1_000L, 2_000L))
    }
}
