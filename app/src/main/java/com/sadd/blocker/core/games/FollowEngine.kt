package com.sadd.blocker.core.games

import kotlin.math.sin

/**
 * منطق خالص بازی «دنبال کن»: مسیر سرگردان نقطه نورانی روی بوم.
 * موقعیت هدف با یک مسیر لیساژو (دو سینوس با فرکانس متفاوت) در مختصات
 * نرمال‌شده ۰ تا ۱ داده می‌شود؛ حاشیه ۰٫۱۲ از لبه‌ها نگه داشته می‌شود تا
 * نقطه و هاله‌اش همیشه داخل بوم بمانند. زمان منفی مثل صفر حساب می‌شود.
 */
object FollowEngine {

    /** شعاع تشخیص «روی نقطه بودن» در مختصات نرمال‌شده (نسبت به اندازه بوم). */
    const val HIT_RADIUS: Float = 0.09f

    private const val CENTER: Float = 0.5f
    private const val AMPLITUDE: Float = 0.38f
    private const val X_PERIOD_MS: Float = 1700f
    private const val Y_PERIOD_MS: Float = 2300f
    private const val Y_PHASE: Float = 1.3f

    /** موقعیت نرمال‌شده (x, y) نقطه در زمان داده‌شده (میلی‌ثانیه). */
    fun targetAt(tMs: Float): Pair<Float, Float> {
        val t = if (tMs < 0f) 0f else tMs
        val x = CENTER + AMPLITUDE * sin(t / X_PERIOD_MS)
        val y = CENTER + AMPLITUDE * sin(t / Y_PERIOD_MS + Y_PHASE)
        return Pair(x, y)
    }
}
