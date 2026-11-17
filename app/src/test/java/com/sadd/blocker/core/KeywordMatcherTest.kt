package com.sadd.blocker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {

    @Test
    fun wholeWordMatch() {
        assertTrue(KeywordMatcher.matches("من فوتبال بازی می‌کنم", "فوتبال"))
    }

    @Test
    fun substringIsNotAWholeWordMatch() {
        // «فوت» ≠ «فوتبال» — مهم‌ترین قانون تطابق.
        assertFalse(KeywordMatcher.matches("فوتبال", "فوت"))
        assertFalse(KeywordMatcher.matches("من فوتبال دوست دارم", "فوت"))
    }

    @Test
    fun multiWordPhraseMatchesWithBoundaries() {
        assertTrue(KeywordMatcher.matches("رفتم به سمت قمار آنلاین", "قمار آنلاین"))
        assertFalse(KeywordMatcher.matches("قمارباز حرفه‌ای", "قمار آنلاین"))
    }

    @Test
    fun arabicVariantsMatchPersianRule() {
        assertTrue(KeywordMatcher.matches("يادگيري كتاب", "یادگیری کتاب"))
    }

    @Test
    fun caseInsensitiveEnglish() {
        assertTrue(KeywordMatcher.matches("I visited EXAMPLE today", "example"))
    }

    @Test
    fun mixedPersianEnglishPhrase() {
        assertTrue(KeywordMatcher.matches("سایت example باز شد", "سایت example"))
    }

    @Test
    fun firstMatchReturnsFirstMatchingRuleInListOrder() {
        // firstMatch اولین قانونِ منطبق «در ترتیب لیست» را برمی‌گرداند.
        assertEquals("شرطبندی", KeywordMatcher.firstMatch("قمار و شرطبندی", listOf("شرطبندی", "قمار")))
        assertEquals("قمار", KeywordMatcher.firstMatch("قمار و شرطبندی", listOf("قمار", "شرطبندی")))
    }

    @Test
    fun firstMatchNullWhenNoRule() {
        assertNull(KeywordMatcher.firstMatch("سلام دنیا", listOf("قمار", "شرط")))
    }

    @Test
    fun domainExactMatch() {
        assertTrue(KeywordMatcher.domainMatches("example.com", "example.com", true))
    }

    @Test
    fun domainSubdomainMatchWhenAllowed() {
        assertTrue(KeywordMatcher.domainMatches("sub.example.com", "example.com", true))
        assertFalse(KeywordMatcher.domainMatches("sub.example.com", "example.com", false))
    }

    @Test
    fun domainNeverMatchesLookalike() {
        // example.com هرگز notexample.com را بلاک نمی‌کند.
        assertFalse(KeywordMatcher.domainMatches("notexample.com", "example.com", true))
        assertFalse(KeywordMatcher.domainMatches("example.com.evil.com", "example.com", true))
    }

    @Test
    fun normalizeHostStripsSchemePathPortAndWww() {
        assertEquals("example.com", KeywordMatcher.normalizeHost("https://www.example.com:443/path?q=1"))
        assertEquals("example.com", KeywordMatcher.normalizeHost("HTTP://EXAMPLE.COM/"))
    }
}
