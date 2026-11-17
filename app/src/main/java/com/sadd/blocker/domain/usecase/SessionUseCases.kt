package com.sadd.blocker.domain.usecase

import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.domain.model.BlockingSession
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

    suspend fun execute(durationMs: Long, strict: Boolean, nowMs: Long = System.currentTimeMillis()): StartSessionResult {
        if (durationMs < MIN_DURATION_MS) {
            return StartSessionResult.InvalidDuration("مدت سشن حداقل ۳۰ ثانیه است.")
        }
        if (durationMs > MAX_DURATION_MS) {
            return StartSessionResult.InvalidDuration("مدت سشن حداکثر ۹۰ روز است.")
        }
        if (repo.getActive(nowMs) != null) {
            return StartSessionResult.AlreadyActive
        }
        val session = BlockingSession(
            startEpochMs = nowMs,
            endEpochMs = nowMs + durationMs,
            strict = strict,
        )
        val id = repo.start(session)
        return StartSessionResult.Started(session.copy(id = id))
    }
}

class EndSessionUseCase(private val repo: SessionRepository) {
    /** false یعنی سشن فعالی نبود. */
    suspend fun execute(reason: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val active = repo.getActive(nowMs) ?: return false
        repo.end(active.id, reason)
        return true
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
