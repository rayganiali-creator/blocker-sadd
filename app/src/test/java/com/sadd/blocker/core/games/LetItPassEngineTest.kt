package com.sadd.blocker.core.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های «بگذار برود»: پیشرفت برگ از ۰ تا ۱ در مدت شناورشدن، با سقف ۱
 * و رفتار زمان منفی مثل صفر.
 */
class LetItPassEngineTest {

    @Test
    fun atZeroProgressIsZero() {
        assertEquals(0.0, LetItPassEngine.progressAt(0f).toDouble(), 0.0001)
    }

    @Test
    fun halfwayProgressIsHalf() {
        val half = LetItPassEngine.DRIFT_MS / 2f
        assertEquals(0.5, LetItPassEngine.progressAt(half).toDouble(), 0.0001)
    }

    @Test
    fun atDriftEndProgressIsOne() {
        assertEquals(1.0, LetItPassEngine.progressAt(LetItPassEngine.DRIFT_MS).toDouble(), 0.0001)
    }

    @Test
    fun beyondDriftEndClampsToOne() {
        assertEquals(1.0, LetItPassEngine.progressAt(LetItPassEngine.DRIFT_MS * 3f).toDouble(), 0.0001)
    }

    @Test
    fun negativeTimeBehavesLikeZero() {
        assertEquals(0.0, LetItPassEngine.progressAt(-1000f).toDouble(), 0.0001)
    }

    @Test
    fun progressIsMonotonic() {
        var previous = LetItPassEngine.progressAt(0f)
        var t = 250f
        while (t <= LetItPassEngine.DRIFT_MS) {
            val current = LetItPassEngine.progressAt(t)
            assertTrue("t=$t", current >= previous)
            previous = current
            t += 250f
        }
    }
}
