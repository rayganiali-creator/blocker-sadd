package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictUnlockTest {

    @Test
    fun passageExactMatch() {
        val passage = STRICT_PASSAGES[0].text
        assertTrue(StrictUnlock.isPassageMatch(passage, passage))
    }

    @Test
    fun passageArabicYehAndKafMatch() {
        assertTrue(StrictUnlock.isPassageMatch("كتاب يادگيري", "کتاب یادگیری"))
        assertTrue(StrictUnlock.isPassageMatch("کتاب یادگیری", "كتاب يادگيري"))
    }

    @Test
    fun passageExtraWhitespaceMatches() {
        assertTrue(StrictUnlock.isPassageMatch("  کتاب   یادگیری  ", "کتاب یادگیری"))
        assertTrue(StrictUnlock.isPassageMatch("کتاب\nیادگیری", "کتاب یادگیری"))
    }

    @Test
    fun passageZwnjAndSpaceAreEquivalent() {
        // نیم‌فاصله با فاصله هم‌ارز است تا کیبورد فارسی بی‌جهت رد نکند.
        assertTrue(StrictUnlock.isPassageMatch("می روم", "می‌روم"))
        assertTrue(StrictUnlock.isPassageMatch("می‌روم", "می روم"))
    }

    @Test
    fun passageMismatchCases() {
        assertFalse(StrictUnlock.isPassageMatch("کتاب دیگر", "کتاب یادگیری"))
        assertFalse(StrictUnlock.isPassageMatch("", "کتاب یادگیری"))
        assertFalse(StrictUnlock.isPassageMatch("کتاب", "کتاب یادگیری"))
        assertFalse(StrictUnlock.isPassageMatch("کتاب یادگیری اضافه", "کتاب یادگیری"))
    }

    @Test
    fun passageProgressAndPrefix() {
        assertEquals(0f, StrictUnlock.passageProgress("", "کتاب یادگیری"), 0.001f)
        assertEquals(1f, StrictUnlock.passageProgress("کتاب یادگیری", "کتاب یادگیری"), 0.001f)
        val partial = StrictUnlock.passageProgress("کتاب", "کتاب یادگیری")
        assertTrue(partial > 0f && partial < 1f)
        assertEquals(4f / 12f, partial, 0.001f)
        assertTrue(StrictUnlock.isPassagePrefix("کتاب", "کتاب یادگیری"))
        assertTrue(StrictUnlock.isPassagePrefix("", "کتاب یادگیری"))
        assertFalse(StrictUnlock.isPassagePrefix("کتاب دیگر", "کتاب یادگیری"))
        // پیشرفت تا اولین اختلاف شمرده می‌شود، نه کل متن تایپ‌شده.
        val diverged = StrictUnlock.passageProgress("کتاب دیگر", "کتاب یادگیری")
        assertEquals(5f / 12f, diverged, 0.001f)
    }

    private fun request(atMs: Long, hours: Int = 24): StrictCooldownRequest {
        return StrictCooldownRequest(atMs, StrictPendingKind.END_SESSION, "", hours)
    }

    @Test
    fun cooldownNotYetElapsed() {
        val now = 1_700_000_000_000L
        val req = request(now)
        assertFalse(StrictUnlock.isCooldownElapsed(req, now))
        assertEquals(24L * 3_600_000L, StrictUnlock.cooldownRemainingMs(req, now))
        assertFalse(StrictUnlock.isCooldownElapsed(req, now + 23L * 3_600_000L))
        assertEquals(3_600_000L, StrictUnlock.cooldownRemainingMs(req, now + 23L * 3_600_000L))
    }

    @Test
    fun cooldownExactlyElapsed() {
        val now = 1_700_000_000_000L
        val req = request(now)
        val atEnd = now + 24L * 3_600_000L
        assertTrue(StrictUnlock.isCooldownElapsed(req, atEnd))
        assertEquals(0L, StrictUnlock.cooldownRemainingMs(req, atEnd))
        assertTrue(StrictUnlock.isCooldownElapsed(req, atEnd + 1L))
        assertEquals(0L, StrictUnlock.cooldownRemainingMs(req, atEnd + 1L))
    }

    @Test
    fun cooldownJustBeforeElapsed() {
        val now = 1_700_000_000_000L
        val req = request(now)
        val justBefore = now + 24L * 3_600_000L - 1L
        assertFalse(StrictUnlock.isCooldownElapsed(req, justBefore))
        assertEquals(1L, StrictUnlock.cooldownRemainingMs(req, justBefore))
    }

    @Test
    fun cooldownTwelveAndFortyEightHours() {
        val now = 1_700_000_000_000L
        assertEquals(12L * 3_600_000L, StrictUnlock.cooldownRemainingMs(request(now, 12), now))
        assertEquals(48L * 3_600_000L, StrictUnlock.cooldownRemainingMs(request(now, 48), now))
        assertTrue(
            StrictUnlock.isCooldownElapsed(request(now, 12), now + 12L * 3_600_000L),
        )
        assertFalse(
            StrictUnlock.isCooldownElapsed(request(now, 48), now + 12L * 3_600_000L),
        )
    }

    @Test
    fun cooldownFormatRemaining() {
        assertEquals("24:00:00", StrictUnlock.formatCooldownRemaining(24L * 3_600_000L))
        assertEquals("47:59:59", StrictUnlock.formatCooldownRemaining(47L * 3_600_000L + 59L * 60_000L + 59_000L))
        assertEquals("00:00:00", StrictUnlock.formatCooldownRemaining(0L))
    }

    @Test
    fun pendingChangeParsing() {
        val methodReq = StrictCooldownRequest(1L, StrictPendingKind.METHOD, "COOLDOWN", 24)
        assertEquals(StrictUnlockMethod.COOLDOWN, methodReq.pendingMethod)
        assertEquals(null, methodReq.pendingHours)
        val hoursReq = StrictCooldownRequest(1L, StrictPendingKind.HOURS, "48", 24)
        assertEquals(48, hoursReq.pendingHours)
        assertEquals(null, hoursReq.pendingMethod)
        val endReq = StrictCooldownRequest(1L, StrictPendingKind.END_SESSION, "", 12)
        assertEquals(null, endReq.pendingMethod)
        assertEquals(null, endReq.pendingHours)
        val badReq = StrictCooldownRequest(1L, StrictPendingKind.METHOD, "NOPE", 24)
        assertEquals(null, badReq.pendingMethod)
    }

    @Test
    fun passageBankHasEightLongPassagesWithSources() {
        assertEquals(8, STRICT_PASSAGES.size)
        for (passage in STRICT_PASSAGES) {
            assertTrue(passage.text.length in 500..750)
            assertTrue(passage.source.isNotBlank())
        }
    }

    @Test
    fun randomPassageIndexStaysInRangeAndDiffers() {
        repeat(20) {
            val idx = StrictUnlock.randomPassageIndex(except = 0)
            assertTrue(idx in STRICT_PASSAGES.indices)
            assertTrue(idx != 0)
        }
        val any = StrictUnlock.randomPassageIndex()
        assertTrue(any in STRICT_PASSAGES.indices)
    }

    @Test
    fun ruleChangeEncodeDecodeRoundTrip() {
        val toggle = StrictRuleChange.toggle(StrictRuleChange.SCOPE_KEYWORD, "12", false)
        assertEquals(toggle, StrictRuleChange.decode(StrictRuleChange.encode(toggle)))
        val remove = StrictRuleChange.remove(StrictRuleChange.SCOPE_APP, "com.example.app")
        assertEquals(remove, StrictRuleChange.decode(StrictRuleChange.encode(remove)))
        val delay = StrictRuleChange.delay(StrictRuleChange.SCOPE_WEBSITE, "7", null)
        assertEquals(delay, StrictRuleChange.decode(StrictRuleChange.encode(delay)))
        val section = StrictRuleChange.toggle(
            StrictRuleChange.SCOPE_SECTION,
            "org.telegram.messenger#search",
            false,
        )
        assertEquals(section, StrictRuleChange.decode(StrictRuleChange.encode(section)))
    }

    @Test
    fun ruleChangeDecodeRejectsGarbage() {
        assertEquals(null, StrictRuleChange.decode(""))
        assertEquals(null, StrictRuleChange.decode("keyword|toggle"))
        assertEquals(null, StrictRuleChange.decode("keyword|toggle|12|false|extra"))
        assertEquals(null, StrictRuleChange.decode("||12|false"))
    }

    @Test
    fun ruleChangeRequestExposesDecodedChangeOnlyForItsKind() {
        val change = StrictRuleChange.toggle(StrictRuleChange.SCOPE_SCHEDULE, "3", false)
        val ruleReq = StrictCooldownRequest(
            1L,
            StrictPendingKind.RULE_CHANGE,
            StrictRuleChange.encode(change),
            24,
        )
        assertEquals(change, ruleReq.pendingRuleChange)
        val endReq = StrictCooldownRequest(1L, StrictPendingKind.END_SESSION, "", 24)
        assertEquals(null, endReq.pendingRuleChange)
    }
}
