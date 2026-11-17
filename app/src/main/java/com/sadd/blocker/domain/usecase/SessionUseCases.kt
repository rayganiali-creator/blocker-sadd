package com.sadd.blocker.domain.usecase

import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.repository.PinCheckResult
import com.sadd.blocker.domain.repository.PinGate
import com.sadd.blocker.domain.repository.SessionRepository

/**
 * UseCaseهای سشن — خالص Kotlin.
 * مدت مجاز سشن ۳۰ ثانیه تا ۹۰ روز است (طبق ARCHITECTURE.md)؛
 * باقی‌مانده همیشه از endEpochMs محاسبه می‌شود، نه تایمر در حافظه.
 */
sealed class StartSessionResult {
    data class Started(val session: BlockingSession) : StartSessionResult()
    object AlreadyActive : StartSessionResult()
    data class InvalidDuration(val reason: String) : StartSessionResult()
}

class StartSessionUseCase(private val repo: SessionRepository) {

    companion object {
        const val MIN_DURATION_MS = 30_000L
        const val MAX_DURATION_MS = 90L * 24L * 60L * 60L * 1000L
    }

    /**
     * شروع سشن. [nowElapsedMs] مقدار `SystemClock.elapsedRealtime()` موقع
     * شروع است (-1 یعنی نامشخص)؛ برای راستی‌آزمایی در برابر عقب کشیدن
     * ساعت سیستم ذخیره می‌شود (H-6).
     */
    suspend fun execute(
        durationMs: Long,
        strict: Boolean,
        nowMs: Long = System.currentTimeMillis(),
        nowElapsedMs: Long = -1L,
    ): StartSessionResult {
        if (durationMs < MIN_DURATION_MS) {
            return StartSessionResult.InvalidDuration("مدت سشن حداقل ۳۰ ثانیه است.")
        }
        if (durationMs > MAX_DURATION_MS) {
            return StartSessionResult.InvalidDuration("مدت سشن حداکثر ۹۰ روز است.")
        }
        val session = BlockingSession(
            startEpochMs = nowMs,
            endEpochMs = nowMs + durationMs,
            strict = strict,
            startElapsedRealtimeMs = if (nowElapsedMs >= 0) nowElapsedMs else 0L,
        )
        // شروع اتمی (H-9): اگر هم‌زمان سشن فعالی باشد، ‎-1‎ برمی‌گردد و
        // سشن دوم ساخته نمی‌شود.
        val id = repo.startIfNoActive(session, nowMs)
        if (id == -1L) return StartSessionResult.AlreadyActive
        return StartSessionResult.Started(session.copy(id = id))
    }
}

/** نتیجه پایان سشن — مسیر واحد پایان (H-1): داشبورد و صفحه سشن‌ها یکی‌اند. */
sealed class EndSessionResult {
    object Ended : EndSessionResult()
    object NoActive : EndSessionResult()
    /** سشن Strict است و PIN تنظیم شده؛ بدون PIN نمی‌شود پایانش داد. */
    object PinRequired : EndSessionResult()
    data class WrongPin(val attemptsLeft: Int) : EndSessionResult()
    data class Locked(val lockedUntilMs: Long) : EndSessionResult()
}

class EndSessionUseCase(
    private val repo: SessionRepository,
    private val pinGate: PinGate? = null,
) {
    /**
     * پایان سشن فعال. اگر سشن Strict باشد و PIN تنظیم شده باشد،
     * پایان فقط با PIN درست انجام می‌شود — این بررسی در UseCase است،
     * نه در ViewModel، تا هیچ مسیری (داشبورد/سشن‌ها/آینده) آن را دور نزند.
     */
    suspend fun execute(
        reason: String,
        pin: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): EndSessionResult {
        val active = repo.getActive(nowMs) ?: return EndSessionResult.NoActive
        val gate = pinGate
        if (active.strict && gate != null && gate.isPinSet()) {
            if (pin == null) return EndSessionResult.PinRequired
            when (val check = gate.verify(pin)) {
                is PinCheckResult.Ok -> Unit
                is PinCheckResult.Wrong -> return EndSessionResult.WrongPin(check.attemptsLeft)
                is PinCheckResult.Locked -> return EndSessionResult.Locked(check.lockedUntilMs)
                // PIN بین دو بررسی حذف شده؛ پایان ادامه می‌یابد.
                is PinCheckResult.NotSet -> Unit
            }
        }
        repo.end(active.id, reason)
        return EndSessionResult.Ended
    }
}

class ExtendSessionUseCase(private val repo: SessionRepository) {
    /** انتهای جدید (میلی‌ثانیه) یا null اگر سشن فعالی نباشد. */
    suspend fun execute(nowMs: Long = System.currentTimeMillis()): Long? {
        val active = repo.getActive(nowMs) ?: return null
        val newEnd = SessionCalculator.extendBy15Min(active.endEpochMs, nowMs) ?: return null
        repo.updateEnd(active.id, newEnd)
        return newEnd
    }
}
