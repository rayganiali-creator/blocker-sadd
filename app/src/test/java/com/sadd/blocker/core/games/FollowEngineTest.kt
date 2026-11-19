package com.sadd.blocker.core.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * تست‌های «دنبال کن»: موقعیت هدف همیشه داخل بوم (با حاشیه از لبه‌ها) است،
 * قطعی است و با زمان حرکت می‌کند.
 */
class FollowEngineTest {

    @Test
    fun atZeroPositionIsInsideCanvas() {
        val (x, y) = FollowEngine.targetAt(0f)
        assertTrue(x in 0f..1f)
        assertTrue(y in 0f..1f)
    }

    @Test
    fun allSampledPositionsStayWithinMargins() {
        var t = 0f
        while (t <= 60000f) {
            val (x, y) = FollowEngine.targetAt(t)
            assertTrue("x=$x at t=$t", x in 0.05f..0.95f)
            assertTrue("y=$y at t=$t", y in 0.05f..0.95f)
            t += 137f
        }
    }

    @Test
    fun sameTimeGivesSamePosition() {
        val first = FollowEngine.targetAt(1234f)
        val second = FollowEngine.targetAt(1234f)
        assertEquals(first.first, second.first, 0.0001f)
        assertEquals(first.second, second.second, 0.0001f)
    }

    @Test
    fun targetMovesOverTime() {
        val start = FollowEngine.targetAt(0f)
        val later = FollowEngine.targetAt(1500f)
        val moved = abs(later.first - start.first) + abs(later.second - start.second)
        assertTrue(moved > 0.1f)
    }

    @Test
    fun negativeTimeBehavesLikeZero() {
        val negative = FollowEngine.targetAt(-500f)
        val zero = FollowEngine.targetAt(0f)
        assertEquals(zero.first, negative.first, 0.0001f)
        assertEquals(zero.second, negative.second, 0.0001f)
    }

    @Test
    fun hitRadiusIsPositiveAndSmall() {
        assertTrue(FollowEngine.HIT_RADIUS > 0f)
        assertTrue(FollowEngine.HIT_RADIUS < 0.25f)
    }
}
