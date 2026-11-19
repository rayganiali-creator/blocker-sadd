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
}
