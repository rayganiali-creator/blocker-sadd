package com.sadd.blocker.data

import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {

    @Test
    fun appsCrudAndToggle() = runBlocking {
        val repo = FakeBlockedAppRepository()
        repo.add(BlockedApp("com.a", "اپ الف"))
        repo.add(BlockedApp("com.b", "اپ ب"))
        assertEquals(2, repo.list().size)
        assertEquals(2, repo.countEnabled())
        repo.setEnabled("com.a", false)
        assertEquals(1, repo.countEnabled())
        assertEquals(false, repo.getByPackage("com.a")?.enabled)
        repo.setDelay("com.b", 10)
        assertEquals(10, repo.getByPackage("com.b")?.delaySeconds)
        repo.remove("com.a")
        assertNull(repo.getByPackage("com.a"))
        assertEquals(1, repo.list().size)
    }

    @Test
    fun appsObserveEmitsChanges() = runBlocking {
        val repo = FakeBlockedAppRepository()
        repo.add(BlockedApp("com.a", "اپ الف"))
        val observed = repo.observe().first()
        assertEquals(1, observed.size)
        assertEquals("com.a", observed[0].packageName)
    }

    @Test
    fun websitesCrudAndFindByHost() = runBlocking {
        val repo = FakeWebsiteRepository()
        val id = repo.add(BlockedWebsite(host = "example.com"))
        assertTrue(repo.findByHost("example.com")?.id == id)
        assertNull(repo.findByHost("other.com"))
        repo.setEnabled(id, false)
        assertEquals(0, repo.countEnabled())
        repo.remove(id)
        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun keywordsCrudAndCountByNormalized() = runBlocking {
        val repo = FakeKeywordRepository()
        repo.add(KeywordRule(rawText = "قمار", normalizedText = "قمار"))
        repo.add(KeywordRule(rawText = "بازی", normalizedText = "بازی"))
        assertEquals(1, repo.countByNormalized("قمار"))
        assertEquals(0, repo.countByNormalized("دیگر"))
        assertEquals(2, repo.countEnabled())
        val id = repo.list().first { it.normalizedText == "بازی" }.id
        repo.setEnabled(id, false)
        assertEquals(1, repo.countEnabled())
        repo.remove(id)
        assertEquals(1, repo.list().size)
    }

    @Test
    fun sessionsActiveAndHistory() = runBlocking {
        val repo = FakeSessionRepository()
        val now = 1_700_000_000_000L
        assertNull(repo.getActive(now))
        val id = repo.start(
            com.sadd.blocker.domain.model.BlockingSession(
                startEpochMs = now,
                endEpochMs = now + 3_600_000L,
                strict = true,
            ),
        )
        val active = repo.getActive(now)
        assertTrue(active?.id == id)
        assertTrue(active?.strict == true)
        assertNull(repo.getActive(now + 3_600_001L))
        repo.end(id, "user")
        assertNull(repo.getActive(now))
        assertEquals(1, repo.history().size)
        assertEquals("user", repo.history()[0].endedReason)
    }

    @Test
    fun expiredSessionTransitionsToHistory() = runBlocking {
        // H-5: سشن منقضی‌شده به expired تبدیل می‌شود و در تاریخچه دیده می‌شود.
        val repo = FakeSessionRepository()
        val now = 1_700_000_000_000L
        repo.start(
            com.sadd.blocker.domain.model.BlockingSession(
                startEpochMs = now,
                endEpochMs = now + 3_600_000L,
                strict = false,
            ),
        )
        assertNull(repo.getActive(now + 3_600_001L))
        assertEquals(1, repo.history().size)
        assertEquals("expired", repo.history()[0].status)
        assertEquals("expired", repo.history()[0].endedReason)
    }

    @Test
    fun startIfNoActiveReturnsMinusOneWhenActive() = runBlocking {
        // H-9
        val repo = FakeSessionRepository()
        val now = 1_700_000_000_000L
        val session = com.sadd.blocker.domain.model.BlockingSession(
            startEpochMs = now,
            endEpochMs = now + 3_600_000L,
        )
        assertTrue(repo.startIfNoActive(session, now) > 0)
        assertEquals(-1L, repo.startIfNoActive(session, now))
    }

    @Test
    fun expireOldReturnsCount() = runBlocking {
        val repo = FakeSessionRepository()
        val now = 1_700_000_000_000L
        repo.start(
            com.sadd.blocker.domain.model.BlockingSession(
                startEpochMs = now,
                endEpochMs = now + 1_000L,
            ),
        )
        assertEquals(0, repo.expireOld(now))
        assertEquals(1, repo.expireOld(now + 2_000L))
        assertEquals(0, repo.expireOld(now + 2_000L))
    }

    @Test
    fun schedulesCrud() = runBlocking {
        val repo = FakeScheduleRepository()
        val id = repo.add(Schedule(daysOfWeek = "SAT", startMinute = 480, endMinute = 600))
        assertEquals(1, repo.list().size)
        repo.setEnabled(id, false)
        assertEquals(false, repo.list()[0].enabled)
        repo.update(Schedule(id = id, daysOfWeek = "SAT,SUN", startMinute = 500, endMinute = 600))
        assertEquals("SAT,SUN", repo.list()[0].daysOfWeek)
        repo.remove(id)
        assertTrue(repo.list().isEmpty())
    }
}
