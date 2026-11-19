package com.sadd.blocker.core.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست‌های «ترکاندن»: شمارنده فقط یک بار به‌ازای هر حباب، بازسازی و ریست. */
class PopEngineTest {

    @Test
    fun popCountsOncePerBubble() {
        val engine = PopEngine(rows = 2, cols = 3)
        assertEquals(6, engine.size)
        assertTrue(engine.pop(0))
        assertTrue(engine.isPopped(0))
        assertEquals(1, engine.poppedCount)
        // ترکاندن دوباره همان حباب نه موفق است نه شمارنده را جلو می‌برد.
        assertFalse(engine.pop(0))
        assertEquals(1, engine.poppedCount)
    }

    @Test
    fun regenerateMakesBubblePoppableAgain() {
        val engine = PopEngine(rows = 2, cols = 3)
        assertTrue(engine.pop(4))
        engine.regenerate(4)
        assertFalse(engine.isPopped(4))
        // شمارنده با بازسازی کم نمی‌شود و ترکیدن بعدی دوباره آن را جلو می‌برد.
        assertEquals(1, engine.poppedCount)
        assertTrue(engine.pop(4))
        assertEquals(2, engine.poppedCount)
    }

    @Test
    fun outOfRangeIndexIsIgnored() {
        val engine = PopEngine(rows = 2, cols = 3)
        assertFalse(engine.pop(6))
        assertFalse(engine.pop(-1))
        assertFalse(engine.isPopped(99))
        engine.regenerate(99)
        assertEquals(0, engine.poppedCount)
    }

    @Test
    fun resetRestoresEverything() {
        val engine = PopEngine(rows = 3, cols = 3)
        engine.pop(0)
        engine.pop(8)
        assertEquals(2, engine.poppedCount)
        engine.reset()
        assertEquals(0, engine.poppedCount)
        assertFalse(engine.isPopped(0))
        assertFalse(engine.isPopped(8))
        assertTrue(engine.pop(0))
    }

    @Test
    fun roundStartsAtOneWithFirstTheme() {
        val engine = PopEngine(rows = 2, cols = 2)
        assertEquals(1, engine.round)
        assertEquals(0, engine.themeIndex)
        assertFalse(engine.isRoundComplete())
    }

    @Test
    fun roundCompletesOnlyWhenAllBubblesPopped() {
        val engine = PopEngine(rows = 2, cols = 2)
        engine.pop(0)
        engine.pop(1)
        engine.pop(2)
        assertFalse(engine.isRoundComplete())
        engine.pop(3)
        assertTrue(engine.isRoundComplete())
    }

    @Test
    fun startNextRoundIncrementsRoundAndClearsGridButKeepsCount() {
        val engine = PopEngine(rows = 1, cols = 2)
        engine.pop(0)
        engine.pop(1)
        assertEquals(2, engine.poppedCount)
        assertTrue(engine.startNextRound())
        assertEquals(2, engine.round)
        assertEquals(1, engine.themeIndex)
        assertFalse(engine.isPopped(0))
        assertFalse(engine.isPopped(1))
        // شمارنده مجموع با شروع دور تازه کم نمی‌شود و ترکیدن بعدی آن را جلو می‌برد.
        assertEquals(2, engine.poppedCount)
        assertTrue(engine.pop(0))
        assertEquals(3, engine.poppedCount)
    }

    @Test
    fun startNextRoundDoesNothingWhenRoundIncomplete() {
        val engine = PopEngine(rows = 2, cols = 2)
        engine.pop(0)
        assertFalse(engine.startNextRound())
        assertEquals(1, engine.round)
        assertTrue(engine.isPopped(0))
    }

    @Test
    fun themeIndexCyclesThroughAllThemes() {
        val engine = PopEngine(rows = 1, cols = 1)
        for (expected in 0 until PopEngine.THEME_COUNT) {
            assertEquals(expected, engine.themeIndex)
            assertTrue(engine.pop(0))
            assertTrue(engine.startNextRound())
        }
        // بعد از آخرین تم، چرخه به تم اول برمی‌گردد.
        assertEquals(0, engine.themeIndex)
        assertEquals(PopEngine.THEME_COUNT + 1, engine.round)
    }

    @Test
    fun resetReturnsToFirstRound() {
        val engine = PopEngine(rows = 1, cols = 1)
        engine.pop(0)
        engine.startNextRound()
        assertEquals(2, engine.round)
        engine.reset()
        assertEquals(1, engine.round)
        assertEquals(0, engine.themeIndex)
        assertEquals(0, engine.poppedCount)
        assertFalse(engine.isPopped(0))
    }
}
