package com.sadd.blocker.domain

import com.sadd.blocker.domain.model.DelayOptions
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
    fun passageBankHasTwelveLongPassagesWithSources() {
        assertEquals(12, STRICT_PASSAGES.size)
        for (passage in STRICT_PASSAGES) {
            assertTrue(passage.text.length in 300..500)
            assertTrue(passage.source.isNotBlank())
            // هر متن گزیده‌ای از یک خطبه نهج‌البلاغه است.
            assertTrue(passage.source.contains("خطبه"))
            assertTrue(passage.source.contains("نهج‌البلاغه"))
            // هیچ متنی نشانه «» ندارد تا تایپ دستی همه متن ممکن باشد.
            assertTrue(!passage.text.contains('«'))
            assertTrue(!passage.text.contains('»'))
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

    @Test
    fun settingChangeEncodeDecodeRoundTrip() {
        val blockMode = StrictSettingChange.blockMode("STRICT")
        assertEquals(blockMode, StrictSettingChange.decode(StrictSettingChange.encode(blockMode)))
        val theme = StrictSettingChange.theme("DARK")
        assertEquals(theme, StrictSettingChange.decode(StrictSettingChange.encode(theme)))
        val color = StrictSettingChange.colorTheme("FOREST")
        assertEquals(color, StrictSettingChange.decode(StrictSettingChange.encode(color)))
    }

    @Test
    fun settingChangeDecodeRejectsGarbage() {
        assertEquals(null, StrictSettingChange.decode(""))
        assertEquals(null, StrictSettingChange.decode("blockMode"))
        assertEquals(null, StrictSettingChange.decode("|STRICT"))
        // value عمداً می‌تواند «|» داشته باشد (limit=2)، پس این معتبر است.
        assertEquals(
            StrictSettingChange("blockMode", "STRICT|extra"),
            StrictSettingChange.decode("blockMode|STRICT|extra"),
        )
    }

    @Test
    fun scheduleAddEncodeDecodeRoundTrip() {
        val add = StrictScheduleAdd("SAT,SUN", 960, 1320, true)
        assertEquals(add, StrictScheduleAdd.decode(StrictScheduleAdd.encode(add)))
        val overnight = StrictScheduleAdd("FRI", 1200, 60, false)
        assertEquals(overnight, StrictScheduleAdd.decode(StrictScheduleAdd.encode(overnight)))
    }

    @Test
    fun scheduleAddDecodeRejectsGarbage() {
        assertEquals(null, StrictScheduleAdd.decode(""))
        assertEquals(null, StrictScheduleAdd.decode("SAT|960|1320"))
        assertEquals(null, StrictScheduleAdd.decode("SAT|xx|1320|true"))
        assertEquals(null, StrictScheduleAdd.decode("SAT|960|1320|yes"))
        assertEquals(null, StrictScheduleAdd.decode("SAT|960|1320|true|extra"))
    }

    @Test
    fun ruleAddEncodeDecodeRoundTrip() {
        val keyword = StrictRuleAdd(StrictRuleChange.SCOPE_KEYWORD, "فوت", "block")
        assertEquals(keyword, StrictRuleAdd.decode(StrictRuleAdd.encode(keyword)))
        // بخش سفارشی: value خودش «|» دارد و باید سالم بماند.
        val customSection = StrictRuleAdd(
            StrictRuleChange.SCOPE_SECTION,
            "com.whatsapp",
            "استوری|وضعیت",
        )
        assertEquals(customSection, StrictRuleAdd.decode(StrictRuleAdd.encode(customSection)))
        // افزودن دسته‌ای اپ‌ها: id خالی و value پکیج‌ها با «,».
        val batchApps = StrictRuleAdd(
            StrictRuleChange.SCOPE_APP,
            "",
            "com.example.a,com.example.b",
        )
        assertEquals(batchApps, StrictRuleAdd.decode(StrictRuleAdd.encode(batchApps)))
    }

    @Test
    fun ruleAddDecodeRejectsGarbage() {
        assertEquals(null, StrictRuleAdd.decode(""))
        assertEquals(null, StrictRuleAdd.decode("keyword|فوت"))
        assertEquals(null, StrictRuleAdd.decode("|فوت|block"))
        // value عمداً می‌تواند «|» داشته باشد (limit=3)، پس این معتبر است.
        assertEquals(
            StrictRuleAdd("keyword", "فوت", "block|extra"),
            StrictRuleAdd.decode("keyword|فوت|block|extra"),
        )
    }

    @Test
    fun pendingAddGettersExposeDecodedValueOnlyForTheirKind() {
        val setting = StrictSettingChange.blockMode("NORMAL")
        val settingReq = StrictCooldownRequest(
            1L,
            StrictPendingKind.SETTING_CHANGE,
            StrictSettingChange.encode(setting),
            24,
        )
        assertEquals(setting, settingReq.pendingSettingChange)
        assertEquals(null, settingReq.pendingScheduleAdd)
        assertEquals(null, settingReq.pendingRuleAdd)

        val schedule = StrictScheduleAdd("MON", 480, 600, false)
        val scheduleReq = StrictCooldownRequest(
            1L,
            StrictPendingKind.SCHEDULE_ADD,
            StrictScheduleAdd.encode(schedule),
            12,
        )
        assertEquals(schedule, scheduleReq.pendingScheduleAdd)
        assertEquals(null, scheduleReq.pendingSettingChange)
        assertEquals(null, scheduleReq.pendingRuleAdd)

        val rule = StrictRuleAdd(StrictRuleChange.SCOPE_WEBSITE, "example.com", "true")
        val ruleReq = StrictCooldownRequest(
            1L,
            StrictPendingKind.RULE_ADD,
            StrictRuleAdd.encode(rule),
            48,
        )
        assertEquals(rule, ruleReq.pendingRuleAdd)
        assertEquals(null, ruleReq.pendingSettingChange)
        assertEquals(null, ruleReq.pendingScheduleAdd)

        val endReq = StrictCooldownRequest(1L, StrictPendingKind.END_SESSION, "", 24)
        assertEquals(null, endReq.pendingSettingChange)
        assertEquals(null, endReq.pendingScheduleAdd)
        assertEquals(null, endReq.pendingRuleAdd)
    }

    @Test
    fun newPendingKindsAreResolvable() {
        assertEquals(StrictPendingKind.SETTING_CHANGE, StrictPendingKind.valueOf("SETTING_CHANGE"))
        assertEquals(StrictPendingKind.EXTEND_SESSION, StrictPendingKind.valueOf("EXTEND_SESSION"))
        assertEquals(StrictPendingKind.SCHEDULE_ADD, StrictPendingKind.valueOf("SCHEDULE_ADD"))
        assertEquals(StrictPendingKind.BACKUP_RESTORE, StrictPendingKind.valueOf("BACKUP_RESTORE"))
        assertEquals(StrictPendingKind.RULE_ADD, StrictPendingKind.valueOf("RULE_ADD"))
    }

    @Test
    fun delayMigrationRunsOnlyOnce() {
        assertTrue(DelayMigration.shouldMigrate(alreadyMigrated = false))
        assertFalse(DelayMigration.shouldMigrate(alreadyMigrated = true))
    }

    @Test
    fun guardPolicyIsAlwaysImmediate() {
        assertEquals(0, GuardPolicy.ACTION_DELAY_SECONDS)
        assertEquals(0, GuardPolicy.effectiveDelaySeconds(0))
        assertEquals(0, GuardPolicy.effectiveDelaySeconds(5))
        assertEquals(0, GuardPolicy.effectiveDelaySeconds(15))
    }

    @Test
    fun pasteInsertSingleCharIsNotPaste() {
        // تایپ تک‌نویسه در ابتدا، وسط و انتها چسباندن نیست.
        assertFalse(StrictUnlock.isPasteInsert("", "ک"))
        assertFalse(StrictUnlock.isPasteInsert("کتاب", "کتاب "))
        assertFalse(StrictUnlock.isPasteInsert("کتاب", "کتااب"))
        assertFalse(StrictUnlock.isPasteInsert("کتاب", " کتاب"))
    }

    @Test
    fun pasteInsertMultiCharIsPaste() {
        // درج چند نویسه در ابتدا، وسط و انتها چسباندن است.
        assertTrue(StrictUnlock.isPasteInsert("", "کتاب"))
        assertTrue(StrictUnlock.isPasteInsert("کتاب", "کتاب یادگیری"))
        assertTrue(StrictUnlock.isPasteInsert("یادگیری", "کتاب یادگیری"))
        assertTrue(StrictUnlock.isPasteInsert("کتاب یادگیری", "کتاب خوب یادگیری"))
    }

    @Test
    fun pasteInsertDeletionAndIdenticalAreNotPaste() {
        // حذف و رشته یکسان چسباندن نیستند.
        assertFalse(StrictUnlock.isPasteInsert("کتاب یادگیری", "کتاب"))
        assertFalse(StrictUnlock.isPasteInsert("کتاب یادگیری", ""))
        assertFalse(StrictUnlock.isPasteInsert("کتاب", "کتاب"))
        assertFalse(StrictUnlock.isPasteInsert("", ""))
    }

    @Test
    fun pasteInsertSelectionReplacementWithManyCharsIsPaste() {
        // جایگزینی یک انتخاب با چند نویسه چسباندن است، ولی با یک نویسه نه.
        assertTrue(StrictUnlock.isPasteInsert("کتاب یادگیری", "دفتر یادگیری"))
        assertTrue(StrictUnlock.isPasteInsert("کتاب", "دفتر"))
        assertFalse(StrictUnlock.isPasteInsert("کتاب", "کتات"))
    }

    @Test
    fun delayOptionsDefaultIsImmediate() {
        assertEquals(0, DelayOptions.DEFAULT_SECONDS)
        assertTrue(DelayOptions.VALUES.contains(DelayOptions.DEFAULT_SECONDS))
    }
}
