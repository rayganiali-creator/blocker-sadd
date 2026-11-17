package com.sadd.blocker.core

/**
 * محاسبه زمان سشن — خالص Kotlin.
 * باقی‌مانده همیشه از `endEpochMs - nowMs` محاسبه می‌شود، نه از تایمر در حافظه،
 * تا بعد از kill پروسه یا ری‌استارت دستگاه هم درست بماند.
 *
 * محافظت در برابر دست‌کاری ساعت (H-6): موقع شروع سشن،
 * `SystemClock.elapsedRealtime()` هم ذخیره می‌شود. باقی‌مانده واقعی،
 * کمینه باقی‌مانده wall-clock و باقی‌مانده elapsed است؛ اگر کاربر ساعت
 * سیستم را به عقب بکشد، باقی‌مانده elapsed جلوی تمدید بی‌نهایت سشن را
 * می‌گیرد. برای سشن‌های قدیمی که elapsed ندارند (مقدار ۰)، فقط wall-clock
 * استفاده می‌شود.
 */
object SessionCalculator {

    /** میلی‌ثانیه باقی‌مانده؛ صفر یعنی منقضی. هرگز منفی برنمی‌گرداند. */
    fun remainingMs(endEpochMs: Long, nowMs: Long): Long {
        val diff = endEpochMs - nowMs
        return if (diff < 0) 0 else diff
    }

    /**
     * باقی‌مانده با راستی‌آزمایی elapsed.
     * [startElapsedMs] مقدار `elapsedRealtime` موقع شروع سشن است (۰ یعنی نامشخص)،
     * [nowElapsedMs] مقدار فعلی آن، و [startEpochMs] شروع wall-clock سشن.
     */
    fun remainingMs(
        endEpochMs: Long,
        nowMs: Long,
        startElapsedMs: Long,
        nowElapsedMs: Long,
        startEpochMs: Long,
    ): Long {
        val wallRemaining = remainingMs(endEpochMs, nowMs)
        if (startElapsedMs <= 0L || nowElapsedMs < 0L) return wallRemaining
        val durationMs = endEpochMs - startEpochMs
        if (durationMs <= 0L) return wallRemaining
        val elapsedEnd = startElapsedMs + durationMs
        val elapsedRemaining = elapsedEnd - nowElapsedMs
        val safeElapsed = if (elapsedRemaining < 0) 0 else elapsedRemaining
        return minOf(wallRemaining, safeElapsed)
    }

    fun isActive(endEpochMs: Long, nowMs: Long): Boolean = remainingMs(endEpochMs, nowMs) > 0

    fun isExpired(endEpochMs: Long, nowMs: Long): Boolean = !isActive(endEpochMs, nowMs)

    /** تمدید ۱۵ دقیقه‌ای فقط وقتی سشن فعال است. */
    fun extendBy15Min(endEpochMs: Long, nowMs: Long): Long? {
        if (!isActive(endEpochMs, nowMs)) return null
        return endEpochMs + 15L * 60L * 1000L
    }
}
