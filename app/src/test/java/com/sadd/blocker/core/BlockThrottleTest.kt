package com.sadd.blocker.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون باگ ران ۱۷۹: بلاک فقط بار اول می‌زد؛ تلاش دوم/سوم (همان کلمه،
 * همان اپ، همان دامنه) هرگز بلاک نمی‌شد — چون cooldown ۱۵ ثانیه‌ایِ
 * scheduleBlock هر تلاش تازه را تا ۱۵ ثانیه بعد از بلاک قبلی رد می‌کرد.
 * این تست‌ها با ساعت جعلی همان سناریو را بازتولید می‌کنند: با قانون
 * تازه، تلاش دومِ ۲ ثانیه بعد از بلاک اول باید زمان‌بندی شود، نه رد.
 */
class BlockThrottleTest {

    private fun throttleAt(startMs: Long): Pair<BlockThrottle, MutableLong> {
        val clock = MutableLong(startMs)
        return BlockThrottle(nowMs = { clock.value }) to clock
    }

    private class MutableLong(var value: Long)

    @Test
    fun secondAttemptShortlyAfterFirstBlockIsScheduled() {
        val (throttle, clock) = throttleAt(1_000_000L)
        val key = "keyword:org.telegram.messenger:42"
        // تلاش اول زمان‌بندی می‌شود.
        assertTrue(throttle.trySchedule(key))
        // تلاش دوباره همان کلید، وقتی بلاک در حال انتظار است، صف نمی‌شود.
        assertFalse(throttle.trySchedule(key))
        // بلاک اول واقعاً اجرا شد (صفحه بلاک آمد).
        throttle.markFired(key)
        // انفجار رویداد صفحه بلاک (بلافاصله بعد) debounce می‌شود.
        assertFalse(throttle.trySchedule(key))
        // ولی تلاش تازه ۲ ثانیه بعد — که با cooldown ۱۵ ثانیه‌ای قدیمی
        // رد می‌شد — باید زمان‌بندی شود.
        clock.value += 2_000L
        assertTrue(throttle.trySchedule(key))
        throttle.markFired(key)
        // و بار سوم هم، دوباره بعد از debounce:
        clock.value += 2_000L
        assertTrue(throttle.trySchedule(key))
    }

    @Test
    fun suppressedAttemptsDoNotExtendTheDebounce() {
        val (throttle, clock) = throttleAt(1_000_000L)
        val key = "section:com.instagram.android:instagram_app"
        assertTrue(throttle.trySchedule(key))
        throttle.markFired(key)
        // چند تلاش ردشده وسط debounce، تایمر را عقب نمی‌اندازند.
        assertFalse(throttle.trySchedule(key))
        clock.value += 500L
        assertFalse(throttle.trySchedule(key))
        clock.value += 500L
        assertFalse(throttle.trySchedule(key))
        clock.value += 600L
        assertTrue(throttle.trySchedule(key))
    }

    @Test
    fun abandonedScheduleLeavesNoDebounce() {
        val (throttle, clock) = throttleAt(1_000_000L)
        val key = "website:com.android.chrome:7"
        assertTrue(throttle.trySchedule(key))
        // بلاک اجرا نشد (مثلاً سشن تمام شد یا دامنه عوض شد): هیچ
        // debounceای نمی‌ماند و تلاش بعدی بی‌درنگ زمان‌بندی می‌شود.
        throttle.markAbandoned(key)
        assertTrue(throttle.trySchedule(key))
        // ساعت جلو نرفته؛ markFired قبلی هم نبود، پس دوباره آزاد است.
        throttle.markAbandoned(key)
        clock.value += 100L
        assertTrue(throttle.trySchedule(key))
    }

    @Test
    fun differentKeysAreIndependent() {
        val (throttle, clock) = throttleAt(1_000_000L)
        val keywordKey = "keyword:org.telegram.messenger:42"
        val appKey = "section:com.instagram.android:instagram_app"
        val domainKey = "website:com.android.chrome:7"
        assertTrue(throttle.trySchedule(keywordKey))
        throttle.markFired(keywordKey)
        // بلاک کلمه، بلاک اپ یا دامنه را نمی‌گیرد.
        assertTrue(throttle.trySchedule(appKey))
        assertTrue(throttle.trySchedule(domainKey))
        // ولی همان کلمه بلافاصله بعد هنوز در debounce است.
        assertFalse(throttle.trySchedule(keywordKey))
        clock.value += 2_000L
        assertTrue(throttle.trySchedule(keywordKey))
    }

    @Test
    fun guardFireAndRetry() {
        val (throttle, clock) = throttleAt(1_000_000L)
        val key = "guard:UNINSTALL_DIALOG"
        assertTrue(throttle.trySchedule(key))
        throttle.markFired(key)
        // ضربه دوباره روی دیالوگ حذف نصب، بلافاصله بعد، debounce می‌شود.
        assertFalse(throttle.trySchedule(key))
        // ولی ۲ ثانیه بعد نگهبان دوباره باید جلویش را بگیرد.
        clock.value += 2_000L
        assertTrue(throttle.trySchedule(key))
    }
}
