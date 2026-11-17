package com.sadd.blocker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleValidationTest {

    @Test
    fun keywordEmptyIsInvalid() {
        assertFalse(RuleValidation.validateKeyword("").ok)
        assertFalse(RuleValidation.validateKeyword("   ").ok)
        assertFalse(RuleValidation.validateKeyword("‌").ok)
    }

    @Test
    fun keywordValidPersianAndEnglish() {
        assertTrue(RuleValidation.validateKeyword("قمار").ok)
        assertTrue(RuleValidation.validateKeyword("قمار بازی").ok)
        assertTrue(RuleValidation.validateKeyword("Example").ok)
        assertTrue(RuleValidation.validateKeyword("قمار example").ok)
    }

    @Test
    fun keywordTooLongIsInvalid() {
        // رشته بلند با کلمات متنوع (تکرار یک حرف توسط نرمال‌ساز collapse می‌شود).
        val long = "قمار بازی ".repeat(20).trim()
        assertFalse(RuleValidation.validateKeyword(long).ok)
    }

    @Test
    fun websiteValidForms() {
        assertTrue(RuleValidation.validateWebsite("example.com").ok)
        assertTrue(RuleValidation.validateWebsite("https://www.example.com/path").ok)
        assertTrue(RuleValidation.validateWebsite("sub.example.com:8080").ok)
        assertTrue(RuleValidation.validateWebsite("EXAMPLE.COM").ok)
    }

    @Test
    fun websiteInvalidForms() {
        assertFalse(RuleValidation.validateWebsite("").ok)
        assertFalse(RuleValidation.validateWebsite("example").ok)
        assertFalse(RuleValidation.validateWebsite("exa mple.com").ok)
        assertFalse(RuleValidation.validateWebsite("http://").ok)
        // M-2
        assertFalse(RuleValidation.validateWebsite("user@example.com").ok)
        assertFalse(RuleValidation.validateWebsite("example.c").ok)
        assertFalse(RuleValidation.validateWebsite("-example.com").ok)
        assertFalse(RuleValidation.validateWebsite("example..com").ok)
        assertTrue(RuleValidation.validateWebsite("example.com.").ok)
    }

    @Test
    fun isPhraseDetectsMultiword() {
        assertTrue(RuleValidation.isPhrase("قمار بازی"))
        assertFalse(RuleValidation.isPhrase("قمار"))
    }

    @Test
    fun timeFormatRemaining() {
        assertTrue(TimeFormat.formatRemainingMs(3_661_000L) == "01:01:01")
        assertTrue(TimeFormat.formatRemainingMs(0L) == "00:00:00")
        assertTrue(TimeFormat.formatRemainingMs(-5L) == "00:00:00")
        assertTrue(TimeFormat.formatRemainingMs(59_000L) == "00:00:59")
    }

    @Test
    fun timeFormatMinuteOfDay() {
        assertTrue(TimeFormat.formatMinuteOfDay(510) == "08:30")
        assertTrue(TimeFormat.formatMinuteOfDay(0) == "00:00")
        assertTrue(TimeFormat.parseMinuteOfDay("08:30") == 510)
        assertTrue(TimeFormat.parseMinuteOfDay("24:00") == null)
        assertTrue(TimeFormat.parseMinuteOfDay("8:30") == 510)
        assertTrue(TimeFormat.parseMinuteOfDay("abc") == null)
        assertTrue(TimeFormat.parseMinuteOfDay("08:60") == null)
    }

    @Test
    fun durationLabels() {
        assertTrue(TimeFormat.durationLabel(30L * 60L * 1000L) == "30 دقیقه")
        assertTrue(TimeFormat.durationLabel(60L * 60L * 1000L) == "1 ساعت")
        assertTrue(TimeFormat.durationLabel(90L * 60L * 1000L) == "1 ساعت و 30 دقیقه")
    }
}
