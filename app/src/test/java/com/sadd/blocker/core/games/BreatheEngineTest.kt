package com.sadd.blocker.core.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های «نفس بکش»: مرزهای فاز در ۰، ۳۹۹۹، ۴۰۰۰، ۹۹۹۹ و ۱۰۰۰۰ میلی‌ثانیه
 * (۴ ثانیه دم + ۶ ثانیه بازدم = چرخه ۱۰ ثانیه‌ای) و شمارش چرخه‌ها.
 */
class BreatheEngineTest {

    @Test
    fun atZeroStartsInhale() {
        val state = BreatheEngine.breathPhaseAt(0L)
        assertEquals(BreathPhase.INHALE, state.phase)
        assertEquals(0.0, state.progress.toDouble(), 0.0001)
        assertEquals(0, state.completedCycles)
    }

    @Test
    fun at3999StillInhaleNearEnd() {
        val state = BreatheEngine.breathPhaseAt(3999L)
        assertEquals(BreathPhase.INHALE, state.phase)
        assertTrue(state.progress > 0.99f)
        assertTrue(state.progress < 1.0f)
        assertEquals(0, state.completedCycles)
    }

    @Test
    fun at4000ExhaleBegins() {
        val state = BreatheEngine.breathPhaseAt(4000L)
        assertEquals(BreathPhase.EXHALE, state.phase)
        assertEquals(0.0, state.progress.toDouble(), 0.0001)
        assertEquals(0, state.completedCycles)
    }

    @Test
    fun at9999StillExhaleNearEnd() {
        val state = BreatheEngine.breathPhaseAt(9999L)
        assertEquals(BreathPhase.EXHALE, state.phase)
        assertTrue(state.progress > 0.99f)
        assertTrue(state.progress < 1.0f)
        assertEquals(0, state.completedCycles)
    }

    @Test
    fun at10000NewCycleStarts() {
        val state = BreatheEngine.breathPhaseAt(10000L)
        assertEquals(BreathPhase.INHALE, state.phase)
        assertEquals(0.0, state.progress.toDouble(), 0.0001)
        assertEquals(1, state.completedCycles)
    }

    @Test
    fun laterCyclesCountUp() {
        // ۲۵ ثانیه = ۲ چرخه کامل + ۵ ثانیه (۱ ثانیه داخل بازدم چرخه سوم).
        val state = BreatheEngine.breathPhaseAt(25000L)
        assertEquals(BreathPhase.EXHALE, state.phase)
        assertEquals(1000.0 / 6000.0, state.progress.toDouble(), 0.0001)
        assertEquals(2, state.completedCycles)
    }

    @Test
    fun midInhaleProgressIsProportional() {
        val state = BreatheEngine.breathPhaseAt(2000L)
        assertEquals(BreathPhase.INHALE, state.phase)
        assertEquals(0.5, state.progress.toDouble(), 0.0001)
    }

    @Test
    fun negativeTimeBehavesLikeZero() {
        val state = BreatheEngine.breathPhaseAt(-500L)
        assertEquals(BreathPhase.INHALE, state.phase)
        assertEquals(0.0, state.progress.toDouble(), 0.0001)
        assertEquals(0, state.completedCycles)
    }
}
