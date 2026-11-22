package com.sadd.blocker.domain

/**
 * شمارنده ضربه‌های مخفی — خالص Kotlin، قابل تست JVM.
 *
 * برای بازکردن دیالوگ کد فعال‌سازی: [requiredTaps] ضربه روی تصویر خرس
 * در پنجره لغزان [windowMs] میلی‌ثانیه‌ای. ضربه‌های قدیمی‌تر از پنجره
 * خودکار منقضی می‌شوند؛ بعد از شلیک، شمارنده از صفر شروع می‌شود.
 * هیچ متن یا نشانه‌ای در UI به این ژست اشاره نمی‌کند.
 */
class SecretTapCounter(
    private val requiredTaps: Int = DEFAULT_REQUIRED_TAPS,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
) {
    private val tapTimesMs = mutableListOf<Long>()

    /**
     * ثبت یک ضربه در زمان [nowMs]. true یعنی ژست کامل شده و باید
     * دیالوگ باز شود؛ شمارنده همان‌جا ریست می‌شود.
     */
    fun onTap(nowMs: Long): Boolean {
        tapTimesMs.add(nowMs)
        val cutoff = nowMs - windowMs
        tapTimesMs.removeAll { it < cutoff }
        if (tapTimesMs.size >= requiredTaps) {
            tapTimesMs.clear()
            return true
        }
        return false
    }

    /** ریست دستی شمارنده (مثلاً وقتی صفحه عوض می‌شود). */
    fun reset() {
        tapTimesMs.clear()
    }

    companion object {
        const val DEFAULT_REQUIRED_TAPS: Int = 7
        const val DEFAULT_WINDOW_MS: Long = 3_000L
    }
}
