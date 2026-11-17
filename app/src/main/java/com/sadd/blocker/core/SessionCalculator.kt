package com.sadd.blocker.core

/**
 * محاسبه زمان سشن — خالص Kotlin.
 * باقی‌مانده همیشه از `endEpochMs - nowMs` محاسبه می‌شود، نه از تایمر در حافظه،
 * تا بعد از kill پروسه یا ری‌استارت دستگاه هم درست بماند.
 */
object SessionCalculator {

    /** میلی‌ثانیه باقی‌مانده؛ صفر یعنی منقضی. هرگز منفی برنمی‌گرداند. */
    fun remainingMs(endEpochMs: Long, nowMs: Long): Long {
        val diff = endEpochMs - nowMs
        return if (diff < 0) 0 else diff
    }

    fun isActive(endEpochMs: Long, nowMs: Long): Boolean = remainingMs(endEpochMs, nowMs) > 0

    fun isExpired(endEpochMs: Long, nowMs: Long): Boolean = !isActive(endEpochMs, nowMs)

    /** تمدید ۱۵ دقیقه‌ای فقط وقتی سشن فعال است. */
    fun extendBy15Min(endEpochMs: Long, nowMs: Long): Long? {
        if (!isActive(endEpochMs, nowMs)) return null
        return endEpochMs + 15L * 60L * 1000L
    }
}
