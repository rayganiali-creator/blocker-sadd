package com.sadd.blocker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست شمارنده ضربه مخفی: ۷ ضربه در ۳ ثانیه → شلیک؛ ۶ ضربه → هیچ؛
 * ضربه‌های پخش‌شده در بیش از ۳ ثانیه → پنجره ریست می‌شود؛ بعد از
 * شلیک، شمارنده از صفر شروع می‌شود.
 */
class SecretTapCounterTest {

    @Test
    fun sevenTapsWithinThreeSecondsTrigger() {
        val counter = SecretTapCounter()
        var triggered = false
        for (i in 0 until 7) {
            triggered = counter.onTap(i * 400L) // ۰ تا ۲۴۰۰ms
        }
        assertTrue(triggered)
    }

    @Test
    fun seventhTapExactlyAtWindowEdgeTriggers() {
        val counter = SecretTapCounter()
        // ضربه اول در ۰ و هفتم در دقیقاً ۳۰۰۰ms — داخل پنجره است.
        val times = listOf(0L, 500L, 1000L, 1500L, 2000L, 2500L, 3000L)
        var triggered = false
        for (t in times) {
            triggered = counter.onTap(t)
        }
        assertTrue(triggered)
    }

    @Test
    fun sixTapsDoNotTrigger() {
        val counter = SecretTapCounter()
        var triggered = false
        for (i in 0 until 6) {
            triggered = counter.onTap(i * 300L)
        }
        assertFalse(triggered)
    }

    @Test
    fun tapsSpreadBeyondWindowExpire() {
        val counter = SecretTapCounter()
        // یک ضربه قدیمی، بعد شش ضربه تازه بعد از ۴ ثانیه: قدیمی منقضی شده.
        assertFalse(counter.onTap(0L))
        var triggered = false
        for (i in 0 until 6) {
            triggered = counter.onTap(4_000L + i * 100L)
        }
        assertFalse(triggered)
        // هفتمین ضربه تازه شلیک می‌کند.
        assertTrue(counter.onTap(4_600L))
    }

    @Test
    fun slowTapsNeverAccumulate() {
        val counter = SecretTapCounter()
        var triggered = false
        // هر ثانیه یک ضربه، ۱۰ بار — هیچ‌وقت ۷ ضربه در ۳ ثانیه جمع نمی‌شود.
        for (i in 0 until 10) {
            triggered = counter.onTap(i * 1_000L)
        }
        assertFalse(triggered)
    }

    @Test
    fun counterResetsAfterTrigger() {
        val counter = SecretTapCounter()
        for (i in 0 until 7) {
            counter.onTap(i * 100L)
        }
        // بعد از شلیک، شمارش از صفر است: ۶ ضربه دیگر کافی نیست.
        var triggered = false
        for (i in 0 until 6) {
            triggered = counter.onTap(1_000L + i * 100L)
        }
        assertFalse(triggered)
        assertTrue(counter.onTap(1_600L))
    }

    @Test
    fun manualResetClearsCount() {
        val counter = SecretTapCounter()
        for (i in 0 until 6) {
            counter.onTap(i * 100L)
        }
        counter.reset()
        assertFalse(counter.onTap(700L))
    }
}
