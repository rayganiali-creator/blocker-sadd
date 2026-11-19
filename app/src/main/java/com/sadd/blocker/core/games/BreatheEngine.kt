package com.sadd.blocker.core.games

/** فاز تنفس در بازی «نفس بکش». */
enum class BreathPhase {
    INHALE, EXHALE,
}

/**
 * وضعیت تنفس در یک لحظه: فاز جاری، پیشرفت ۰ تا ۱ داخل همان فاز و
 * تعداد چرخه‌های کامل‌شده تا آن لحظه.
 */
data class BreathState(
    val phase: BreathPhase,
    val progress: Float,
    val completedCycles: Int,
)

/**
 * منطق خالص بازی «نفس بکش»: چرخه ۱۰ ثانیه‌ای — ۴ ثانیه دم، ۶ ثانیه بازدم.
 * زمان منفی مثل صفر حساب می‌شود تا UI مجبور به گارد جدا نباشد.
 */
object BreatheEngine {

    const val INHALE_MS: Long = 4000L
    const val EXHALE_MS: Long = 6000L
    const val CYCLE_MS: Long = INHALE_MS + EXHALE_MS

    fun breathPhaseAt(elapsedMs: Long): BreathState {
        val elapsed = if (elapsedMs < 0L) 0L else elapsedMs
        val cycles = (elapsed / CYCLE_MS).toInt()
        val within = elapsed % CYCLE_MS
        return if (within < INHALE_MS) {
            BreathState(
                phase = BreathPhase.INHALE,
                progress = within.toFloat() / INHALE_MS.toFloat(),
                completedCycles = cycles,
            )
        } else {
            BreathState(
                phase = BreathPhase.EXHALE,
                progress = (within - INHALE_MS).toFloat() / EXHALE_MS.toFloat(),
                completedCycles = cycles,
            )
        }
    }
}
