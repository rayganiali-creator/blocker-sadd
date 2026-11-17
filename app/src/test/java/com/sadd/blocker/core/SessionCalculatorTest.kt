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

    @Test
    fun elapsedRealtimeGuardsAgainstClockSetBack() {
        // H-6: ساعت به عقب کشیده شده ولی elapsed جلو رفته — باقی‌مانده
        // elapsed معیار است، نه wall-clock بادکرده.
        val startEpoch = 1_700_000_000_000L
        val duration = 3_600_000L
        val endEpoch = startEpoch + duration
        val startElapsed = 100_000L
        // ساعت ۱۰ دقیقه به عقب کشیده شده، ولی elapsed فقط ۵ دقیقه جلو رفته.
        val nowWall = startEpoch + 300_000L - 600_000L // ۵ دقیقه قبل از شروعِ ظاهری
        val nowElapsed = startElapsed + 300_000L
        val remaining = SessionCalculator.remainingMs(endEpoch, nowWall, startElapsed, nowElapsed, startEpoch)
        assertEquals(duration - 300_000L, remaining)
    }

    @Test
    fun elapsedUnknownFallsBackToWallClock() {
        // سشن‌های قدیمی بدون elapsed: فقط wall-clock.
        assertEquals(60_000L, SessionCalculator.remainingMs(1_000_000L, 940_000L, 0L, -1L, 900_000L))
    }
}
