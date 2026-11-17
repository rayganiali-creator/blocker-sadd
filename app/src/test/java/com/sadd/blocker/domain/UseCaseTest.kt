package com.sadd.blocker.domain

import com.sadd.blocker.data.FakeBlockedAppRepository
import com.sadd.blocker.data.FakeKeywordRepository
import com.sadd.blocker.data.FakeScheduleRepository
import com.sadd.blocker.data.FakeSessionRepository
import com.sadd.blocker.data.FakeWebsiteRepository
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule
import com.sadd.blocker.domain.repository.PinCheckResult
import com.sadd.blocker.domain.repository.PinGate
import com.sadd.blocker.domain.usecase.AddKeywordResult
import com.sadd.blocker.domain.usecase.AddKeywordRuleUseCase
import com.sadd.blocker.domain.usecase.AddWebsiteResult
import com.sadd.blocker.domain.usecase.AddWebsiteUseCase
import com.sadd.blocker.domain.usecase.EndSessionResult
import com.sadd.blocker.domain.usecase.EndSessionUseCase
import com.sadd.blocker.domain.usecase.ExtendSessionUseCase
import com.sadd.blocker.domain.usecase.RestoreBackupUseCase
import com.sadd.blocker.domain.usecase.RestoreResult
import com.sadd.blocker.domain.usecase.StartSessionResult
import com.sadd.blocker.domain.usecase.StartSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseTest {

    @Test
    fun addKeywordNormalizesArabicLetters() = runBlocking {
        val repo = FakeKeywordRepository()
        val useCase = AddKeywordRuleUseCase(repo)
        val result = useCase.execute("قمار", "عمومی")
        assertTrue(result is AddKeywordResult.Added)
        val added = (result as AddKeywordResult.Added).rule
        assertEquals("قمار", added.normalizedText)
        assertFalse(added.isPhrase)
    }

    @Test
    fun addKeywordDuplicateBlocked() = runBlocking {
        val repo = FakeKeywordRepository()
        val useCase = AddKeywordRuleUseCase(repo)
        assertTrue(useCase.execute("قمار", "عمومی") is AddKeywordResult.Added)
        assertTrue(useCase.execute("قمار", "دیگر") is AddKeywordResult.Duplicate)
        assertEquals(1, repo.list().size)
    }

    @Test
    fun addKeywordInvalidRejected() = runBlocking {
        val repo = FakeKeywordRepository()
        val useCase = AddKeywordRuleUseCase(repo)
        val result = useCase.execute("   ", "عمومی")
        assertTrue(result is AddKeywordResult.Invalid)
        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun addKeywordPhraseDetected() = runBlocking {
        val repo = FakeKeywordRepository()
        val useCase = AddKeywordRuleUseCase(repo)
        val result = useCase.execute("قمار بازی", "عمومی")
        val added = (result as AddKeywordResult.Added).rule
        assertTrue(added.isPhrase)
    }

    @Test
    fun addWebsiteNormalizesAndDedupes() = runBlocking {
        val repo = FakeWebsiteRepository()
        val useCase = AddWebsiteUseCase(repo)
        val first = useCase.execute("https://WWW.Example.com/path", true)
        assertTrue(first is AddWebsiteResult.Added)
        assertEquals("example.com", (first as AddWebsiteResult.Added).site.host)
        assertTrue(useCase.execute("example.com", true) is AddWebsiteResult.Duplicate)
        assertTrue(useCase.execute("not-a-domain", true) is AddWebsiteResult.Invalid)
        assertEquals(1, repo.list().size)
    }

    @Test
    fun startSessionBoundsAndAlreadyActive() = runBlocking {
        val repo = FakeSessionRepository()
        val useCase = StartSessionUseCase(repo)
        val now = 1_700_000_000_000L
        assertTrue(useCase.execute(10_000L, false, now) is StartSessionResult.InvalidDuration)
        assertTrue(useCase.execute(91L * 24L * 60L * 60L * 1000L, false, now) is StartSessionResult.InvalidDuration)
        assertTrue(useCase.execute(30_000L, false, now) is StartSessionResult.Started)
        assertTrue(useCase.execute(60_000L, false, now) is StartSessionResult.AlreadyActive)
    }

    @Test
    fun endAndExtendSession() = runBlocking {
        val repo = FakeSessionRepository()
        val now = 1_700_000_000_000L
        val end = EndSessionUseCase(repo)
        val extend = ExtendSessionUseCase(repo)
        assertTrue(end.execute("user", null, now) is EndSessionResult.NoActive)
        assertNull(extend.execute(now))
        StartSessionUseCase(repo).execute(3_600_000L, false, now)
        val newEnd = extend.execute(now)
        assertEquals(now + 3_600_000L + 15L * 60L * 1000L, newEnd)
        assertTrue(end.execute("user", null, now) is EndSessionResult.Ended)
        assertNull(repo.getActive(now))
    }

    @Test
    fun strictSessionEndRequiresPinInUseCase() = runBlocking {
        // H-1: پایان سشن Strict فقط با PIN درست — در UseCase، نه ViewModel.
        val repo = FakeSessionRepository()
        val gate = object : PinGate {
            override suspend fun isPinSet(): Boolean = true
            override suspend fun verify(pin: String): PinCheckResult {
                return if (pin == "1234") PinCheckResult.Ok else PinCheckResult.Wrong(4)
            }
        }
        val now = 1_700_000_000_000L
        StartSessionUseCase(repo).execute(3_600_000L, true, now)
        val end = EndSessionUseCase(repo, gate)
        assertTrue(end.execute("user", null, now) is EndSessionResult.PinRequired)
        assertTrue(end.execute("user", "0000", now) is EndSessionResult.WrongPin)
        assertTrue(repo.getActive(now) != null)
        assertTrue(end.execute("user", "1234", now) is EndSessionResult.Ended)
        assertNull(repo.getActive(now))
    }

    @Test
    fun nonStrictSessionEndsWithoutPin() = runBlocking {
        val repo = FakeSessionRepository()
        val gate = object : PinGate {
            override suspend fun isPinSet(): Boolean = true
            override suspend fun verify(pin: String): PinCheckResult = PinCheckResult.Wrong(4)
        }
        val now = 1_700_000_000_000L
        StartSessionUseCase(repo).execute(3_600_000L, false, now)
        val end = EndSessionUseCase(repo, gate)
        assertTrue(end.execute("user", null, now) is EndSessionResult.Ended)
    }

    @Test
    fun concurrentStartsCreateOnlyOneActiveSession() = runBlocking {
        // H-9: دو شروع هم‌زمان → فقط یکی Started.
        val repo = FakeSessionRepository()
        val useCase = StartSessionUseCase(repo)
        val now = 1_700_000_000_000L
        val first = async { useCase.execute(3_600_000L, false, now) }
        val second = async { useCase.execute(3_600_000L, false, now) }
        val results = listOf(first.await(), second.await())
        assertEquals(1, results.count { it is StartSessionResult.Started })
        assertEquals(1, results.count { it is StartSessionResult.AlreadyActive })
        // فقط یک سشن فعال در ریپو هست (دومی یتیم نمانده).
        assertTrue(repo.getActive(now) != null)
    }

    @Test
    fun restoreBackupSkipsDuplicates() = runBlocking {
        val apps = FakeBlockedAppRepository()
        val websites = FakeWebsiteRepository()
        val keywords = FakeKeywordRepository()
        val schedules = FakeScheduleRepository()
        keywords.add(KeywordRule(rawText = "قمار", normalizedText = "قمار"))
        val restore = RestoreBackupUseCase(apps, websites, keywords, schedules)
        val json = BackupCodec.encode(
            BackupData(
                apps = listOf(com.sadd.blocker.domain.model.BlockedApp("com.a", "اپ الف")),
                websites = listOf(BlockedWebsite(host = "example.com")),
                keywords = listOf(
                    KeywordRule(rawText = "قمار", normalizedText = "قمار"),
                    KeywordRule(rawText = "بازی", normalizedText = "بازی"),
                ),
                schedules = emptyList(),
            ),
            0L,
        )
        val result = restore.execute(json)
        assertTrue(result is RestoreResult.Restored)
        val summary = (result as RestoreResult.Restored).summary
        assertEquals(1, summary.appsAdded)
        assertEquals(1, summary.websitesAdded)
        assertEquals(1, summary.keywordsAdded)
        assertEquals(1, summary.skipped)
        val again = restore.execute(json)
        val summary2 = (again as RestoreResult.Restored).summary
        assertEquals(0, summary2.appsAdded)
        assertEquals(0, summary2.keywordsAdded)
    }

    @Test
    fun restoreMalformedBackupFails() = runBlocking {
        val restore = RestoreBackupUseCase(
            FakeBlockedAppRepository(),
            FakeWebsiteRepository(),
            FakeKeywordRepository(),
            FakeScheduleRepository(),
        )
        assertTrue(restore.execute("not json") is RestoreResult.Failed)
        assertTrue(restore.execute("{\"version\":9}") is RestoreResult.Failed)
    }

    @Test
    fun restoreDedupesSchedules() = runBlocking {
        // M-3: ایمپورت دوباره همان فایل، زمان‌بندی‌ها را دوبرابر نمی‌کند.
        val schedules = FakeScheduleRepository()
        val restore = RestoreBackupUseCase(
            FakeBlockedAppRepository(),
            FakeWebsiteRepository(),
            FakeKeywordRepository(),
            schedules,
        )
        val json = BackupCodec.encode(
            BackupData(
                apps = emptyList(),
                websites = emptyList(),
                keywords = emptyList(),
                schedules = listOf(
                    Schedule(daysOfWeek = "SAT", startMinute = 480, endMinute = 600),
                    Schedule(daysOfWeek = "SAT", startMinute = 480, endMinute = 600),
                ),
            ),
            0L,
        )
        val result = restore.execute(json)
        val summary = (result as RestoreResult.Restored).summary
        assertEquals(1, summary.schedulesAdded)
        assertEquals(1, summary.skipped)
        assertEquals(1, schedules.list().size)
        val again = restore.execute(json) as RestoreResult.Restored
        assertEquals(0, again.summary.schedulesAdded)
        assertEquals(1, schedules.list().size)
    }

    @Test
    fun addKeywordCanonicalDedupe() = runBlocking {
        // «می‌روم» و «می روم» یک قانون‌اند (dedupe با فرم هم‌ارز).
        val repo = FakeKeywordRepository()
        val useCase = AddKeywordRuleUseCase(repo)
        assertTrue(useCase.execute("می‌روم", "عمومی") is AddKeywordResult.Added)
        assertTrue(useCase.execute("می روم", "عمومی") is AddKeywordResult.Duplicate)
        assertEquals(1, repo.list().size)
    }

    @Test
    fun addWebsiteRejectsUserAtAndTrailingDotNormalizes() = runBlocking {
        // M-2
        val repo = FakeWebsiteRepository()
        val useCase = AddWebsiteUseCase(repo)
        assertTrue(useCase.execute("user@example.com", true) is AddWebsiteResult.Invalid)
        val added = useCase.execute("example.com.", true)
        assertTrue(added is AddWebsiteResult.Added)
        assertEquals("example.com", (added as AddWebsiteResult.Added).site.host)
    }
}
