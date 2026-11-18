package com.sadd.blocker.core

import com.sadd.blocker.core.TextRedactor.DomainRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextRedactorTest {

    @Test
    fun removesSingleMatchedWord() {
        assertEquals("من کردم", TextRedactor.removeMatchedRule("من فوت کردم", "فوت"))
    }

    @Test
    fun substringWordIsNotRemoved() {
        // «فوت» ≠ «فوتبال» — حذف هم whole-word است.
        assertEquals("فوتبال", TextRedactor.removeMatchedRule("فوتبال", "فوت"))
        assertEquals("من فوتبال دوست دارم", TextRedactor.removeMatchedRule("من فوتبال دوست دارم", "فوت"))
    }

    @Test
    fun zwnjCompoundWordIsNotRemoved() {
        assertEquals("فوت‌بال", TextRedactor.removeMatchedRule("فوت‌بال", "فوت"))
    }

    @Test
    fun removesMultiWordPhrase() {
        assertEquals(
            "خطرناک است",
            TextRedactor.removeMatchedRule("قمار آنلاین خطرناک است", "قمار آنلاین"),
        )
    }

    @Test
    fun removesWordAdjacentToPunctuation() {
        assertEquals("رفتم دیدم", TextRedactor.removeMatchedRule("رفتم فوت، دیدم", "فوت"))
    }

    @Test
    fun noMatchReturnsOriginalText() {
        val text = "سلام دنیا"
        assertEquals(text, TextRedactor.removeMatchedRule(text, "فوت"))
    }

    @Test
    fun removesAllOccurrences() {
        assertEquals("رفتم و آمدم", TextRedactor.removeMatchedRule("فوت رفتم فوت و آمدم فوت", "فوت"))
    }

    @Test
    fun arabicVariantOfRuleIsRemoved() {
        assertEquals("کتاب", TextRedactor.removeMatchedRule("يادگيري کتاب", "یادگیری"))
    }

    @Test
    fun removedCharCount() {
        val original = "من فوت کردم"
        val redacted = TextRedactor.removeMatchedRule(original, "فوت")
        assertEquals(original.length - redacted.length, TextRedactor.removedCharCount(original, redacted))
    }

    @Test
    fun firstMatchRangeFindsWord() {
        assertEquals(3 to 6, TextRedactor.firstMatchRange("من فوت کردم", "فوت"))
        assertEquals(0 to 3, TextRedactor.firstMatchRange("فوت کردم", "فوت"))
        assertEquals(0 to 11, TextRedactor.firstMatchRange("قمار آنلاین خطرناک است", "قمار آنلاین"))
    }

    @Test
    fun firstMatchRangeIgnoresSubstrings() {
        assertNull(TextRedactor.firstMatchRange("فوتبال", "فوت"))
        assertNull(TextRedactor.firstMatchRange("من فوتبال دوست دارم", "فوت"))
        assertNull(TextRedactor.firstMatchRange("سلام دنیا", "فوت"))
    }

    @Test
    fun resolveDelayPrefersRuleDelay() {
        assertEquals(3, TextRedactor.resolveDelay(3, 5))
        assertEquals(5, TextRedactor.resolveDelay(null, 5))
        assertEquals(0, TextRedactor.resolveDelay(0, 5))
    }

    @Test
    fun resolveDelayClampsOutOfRange() {
        assertEquals(0, TextRedactor.resolveDelay(-4, 5))
        assertEquals(3600, TextRedactor.resolveDelay(99999, 5))
    }

    @Test
    fun findMatchedDomainMatchesSubdomain() {
        val rules = listOf(DomainRule("example.com", true))
        assertEquals(rules[0], TextRedactor.findMatchedDomain("sub.example.com", rules))
        assertEquals(rules[0], TextRedactor.findMatchedDomain("example.com", rules))
    }

    @Test
    fun findMatchedDomainRejectsLookalike() {
        val rules = listOf(DomainRule("example.com", true))
        assertNull(TextRedactor.findMatchedDomain("notexample.com", rules))
        assertNull(TextRedactor.findMatchedDomain("example.com.evil.com", rules))
    }

    @Test
    fun findMatchedDomainRespectsMatchSubdomainsFalse() {
        val rules = listOf(DomainRule("example.com", false))
        assertNull(TextRedactor.findMatchedDomain("sub.example.com", rules))
        assertEquals(rules[0], TextRedactor.findMatchedDomain("example.com", rules))
    }

    @Test
    fun extractHostCandidateFromUrl() {
        assertEquals("example.com", TextRedactor.extractHostCandidate("https://www.example.com/path?q=1"))
    }

    @Test
    fun extractHostCandidateNullForPlainQuery() {
        assertNull(TextRedactor.extractHostCandidate("سلام دنیا"))
        assertNull(TextRedactor.extractHostCandidate(""))
    }

    @Test
    fun isOwnPackage() {
        assertTrue(TextRedactor.isOwnPackage("com.sadd.blocker", "com.sadd.blocker"))
        assertFalse(TextRedactor.isOwnPackage("com.android.chrome", "com.sadd.blocker"))
        assertFalse(TextRedactor.isOwnPackage(null, "com.sadd.blocker"))
    }
}
