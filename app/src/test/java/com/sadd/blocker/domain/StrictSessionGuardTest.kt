package com.sadd.blocker.domain

import com.sadd.blocker.data.FakeSessionRepository
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * گارد سشن Strict (H-2): گیت حذف/غیرفعال/ویرایش قانونِ موجود روی همین
 * گزاره سوار است. افزودن قانون وسط سشن Strict آزاد است و اصلاً این
 * گارد را صدا نمی‌زند (در ViewModelها)؛ این تست‌ها خودِ گزاره‌ای را
 * می‌سنجند که گیت‌های باقی‌مانده به آن تکیه دارند.
 */
class StrictSessionGuardTest {

    private val now = 1_700_000_000_000L

    @Test
    fun strictActiveSessionIsDetected() = runBlocking {
        val repo = FakeSessionRepository()
        repo.start(
            BlockingSession(
                startEpochMs = now - 60_000,
                endEpochMs = now + 3_600_000,
                strict = true,
            ),
        )
        assertTrue(StrictSessionGuard(repo).isStrictSessionActive(now))
    }

    @Test
    fun normalActiveSessionIsNotStrict() = runBlocking {
        val repo = FakeSessionRepository()
        repo.start(
            BlockingSession(
                startEpochMs = now - 60_000,
                endEpochMs = now + 3_600_000,
                strict = false,
            ),
        )
        assertFalse(StrictSessionGuard(repo).isStrictSessionActive(now))
    }

    @Test
    fun noSessionIsNotStrict() = runBlocking {
        assertFalse(StrictSessionGuard(FakeSessionRepository()).isStrictSessionActive(now))
    }

    @Test
    fun expiredStrictSessionIsNotStrict() = runBlocking {
        val repo = FakeSessionRepository()
        repo.start(
            BlockingSession(
                startEpochMs = now - 7_200_000,
                endEpochMs = now - 3_600_000,
                strict = true,
            ),
        )
        assertFalse(StrictSessionGuard(repo).isStrictSessionActive(now))
    }

    @Test
    fun endedStrictSessionIsNotStrict() = runBlocking {
        val repo = FakeSessionRepository()
        val id = repo.start(
            BlockingSession(
                startEpochMs = now - 60_000,
                endEpochMs = now + 3_600_000,
                strict = true,
            ),
        )
        repo.end(id, "manual")
        assertFalse(StrictSessionGuard(repo).isStrictSessionActive(now))
    }
}
