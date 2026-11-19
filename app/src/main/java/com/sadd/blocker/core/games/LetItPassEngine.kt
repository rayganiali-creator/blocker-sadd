package com.sadd.blocker.core.games

/**
 * منطق خالص بازی «بگذار برود»: پیشرفت شناورشدن برگ روی جویبار.
 * برگ در [DRIFT_MS] میلی‌ثانیه مسیر را کامل می‌کند؛ پیشرفت همیشه بین
 * ۰ و ۱ نگه داشته می‌شود و زمان منفی مثل صفر حساب می‌شود.
 */
object LetItPassEngine {

    /** مدت کامل رفتن برگ از آغاز تا پایان مسیر (میلی‌ثانیه). */
    const val DRIFT_MS: Float = 10000f

    /** پیشرفت ۰ تا ۱ برگ در زمان سپری‌شده از لحظه رهاکردن. */
    fun progressAt(elapsedMs: Float): Float {
        if (elapsedMs <= 0f) {
            return 0f
        }
        val progress = elapsedMs / DRIFT_MS
        return if (progress > 1f) 1f else progress
    }
}
