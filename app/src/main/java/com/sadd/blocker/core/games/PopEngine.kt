package com.sadd.blocker.core.games

/**
 * منطق خالص بازی «ترکاندن»: شبکه‌ای از حباب‌ها که هر کدام یک بار می‌ترکند.
 * وقتی همه حباب‌های شبکه ترکیدند، دور کامل شده و [startNextRound] دور
 * بعدی را با طرح و رنگ متفاوت شروع می‌کند ([themeIndex] بین
 * [THEME_COUNT] تم می‌چرخد). شمارنده [poppedCount] مجموع همه ترکیدن‌ها از
 * شروع است و با شروع دور تازه کم نمی‌شود.
 * اندیس بیرون از بازه بی‌اثر است (pop مقدار false می‌دهد و بقیه هیچ کاری نمی‌کنند).
 */
class PopEngine(val rows: Int, val cols: Int) {

    val size: Int = rows * cols

    private val popped: BooleanArray = BooleanArray(size)

    var poppedCount: Int = 0
        private set

    /** شماره دور جاری (از ۱ شروع می‌شود). */
    var round: Int = 1
        private set

    /** اندیس تم رنگی دور جاری؛ با هر دور یکی جلو می‌رود و بعد از آخرین تم به اول برمی‌گردد. */
    val themeIndex: Int
        get() = (round - 1) % THEME_COUNT

    fun isPopped(index: Int): Boolean {
        if (index < 0 || index >= size) {
            return false
        }
        return popped[index]
    }

    /** ترکاندن حباب؛ true یعنی همین حالا ترکید و شمارنده یکی جلو رفت. */
    fun pop(index: Int): Boolean {
        if (index < 0 || index >= size) {
            return false
        }
        if (popped[index]) {
            return false
        }
        popped[index] = true
        poppedCount += 1
        return true
    }

    /** بازسازی حباب تا دوباره قابل ترکاندن شود (شمارنده دست نمی‌خورد). */
    fun regenerate(index: Int) {
        if (index < 0 || index >= size) {
            return
        }
        popped[index] = false
    }

    /** true یعنی همه حباب‌های شبکه ترکیده‌اند و دور کامل شده است. */
    fun isRoundComplete(): Boolean = popped.all { it }

    /**
     * شروع دور بعدی: شماره دور یکی جلو می‌رود و همه حباب‌ها سالم می‌شوند
     * (شمارنده مجموع دست نمی‌خورد). اگر دور کامل نشده باشد هیچ کاری
     * نمی‌کند و false می‌دهد.
     */
    fun startNextRound(): Boolean {
        if (!isRoundComplete()) {
            return false
        }
        round += 1
        popped.fill(false)
        return true
    }

    /** همه حباب‌ها سالم، شمارنده صفر و برگشت به دور اول. */
    fun reset() {
        popped.fill(false)
        poppedCount = 0
        round = 1
    }

    companion object {
        /** تعداد تم‌های رنگی حباب که دورها بین آن‌ها می‌چرخند. */
        const val THEME_COUNT = 6
    }
}
