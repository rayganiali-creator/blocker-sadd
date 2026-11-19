package com.sadd.blocker.core.games

/**
 * منطق خالص بازی «ترکاندن»: شبکه‌ای از حباب‌ها که هر کدام یک بار می‌ترکند
 * و بعد از بازسازی (regenerate) دوباره قابل ترکاندن می‌شوند.
 * شمارنده [poppedCount] مجموع همه ترکیدن‌ها از شروع است و با بازسازی کم نمی‌شود.
 * اندیس بیرون از بازه بی‌اثر است (pop مقدار false می‌دهد و بقیه هیچ کاری نمی‌کنند).
 */
class PopEngine(val rows: Int, val cols: Int) {

    val size: Int = rows * cols

    private val popped: BooleanArray = BooleanArray(size)

    var poppedCount: Int = 0
        private set

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

    /** همه حباب‌ها سالم و شمارنده صفر. */
    fun reset() {
        popped.fill(false)
        poppedCount = 0
    }
}
