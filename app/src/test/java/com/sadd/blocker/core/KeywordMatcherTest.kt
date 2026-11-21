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
    fun zwnjCompoundWordDoesNotMatchSingleWordRule() {
        // H-3 — مهم‌ترین رگرسیون این دور: «فوت‌بال» یک کلمه مرکب است.
        assertFalse(KeywordMatcher.matches("فوت‌بال", "فوت"))
        assertFalse(KeywordMatcher.matches("من فوت‌بال بازی می‌کنم", "فوت"))
        assertFalse(KeywordMatcher.matches("می‌روم", "روم"))
        assertFalse(KeywordMatcher.matches("می‌روم", "می"))
    }

    @Test
    fun zwnjPhraseMatchesAcrossSpaceOrZwnj() {
        // عبارت چندکلمه‌ای: مرز داخلی می‌تواند فاصله یا نیم‌فاصله باشد.
        assertTrue(KeywordMatcher.matches("می‌روم", "می روم"))
        assertTrue(KeywordMatcher.matches("می روم", "می‌روم"))
        assertTrue(KeywordMatcher.matches("می روم", "می روم"))
        assertTrue(KeywordMatcher.matches("می‌روم", "می‌روم"))
    }

    @Test
    fun punctuationAdjacentWordMatches() {
        // M-1: کلمه چسبیده به علائم نگارشی مچ می‌شود.
        assertTrue(KeywordMatcher.matches("رفتم فوت، دیدم", "فوت"))
        assertTrue(KeywordMatcher.matches("(فوت) آمد", "فوت"))
        assertTrue(KeywordMatcher.matches("«فوت»؟", "فوت"))
        assertTrue(KeywordMatcher.matches("فوت!", "فوت"))
    }

    @Test
    fun normalizeHostStripsSchemePathPortAndWww() {
        assertEquals("example.com", KeywordMatcher.normalizeHost("https://www.example.com:443/path?q=1"))
        assertEquals("example.com", KeywordMatcher.normalizeHost("HTTP://EXAMPLE.COM/"))
    }

    @Test
    fun normalizeHostStripsTrailingDotsAndRejectsAt() {
        // M-2
        assertEquals("example.com", KeywordMatcher.normalizeHost("example.com."))
        assertEquals("example.com", KeywordMatcher.normalizeHost(".example.com."))
        assertEquals("user@example.com", KeywordMatcher.normalizeHost("user@example.com"))
    }

    // ---------- ضدفرار (گزارش سید ۲۰۲۶-۱۱-۲۰) ----------

    @Test
    fun antiEvasionSeparatorBetweenLettersMatches() {
        // «پور.ن هاب» نباید از «پورنهاب» فرار کند.
        assertTrue(KeywordMatcher.matches("پور.ن هاب", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("پور-ن_هاب", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("پور/ن هاب", "پورنهاب"))
    }

    @Test
    fun antiEvasionSpacedLettersMatch() {
        assertTrue(KeywordMatcher.matches("پ و ر ن ه ا ب", "پورنهاب"))
    }

    @Test
    fun antiEvasionRepeatedLettersMatch() {
        assertTrue(KeywordMatcher.matches("پووورنهاب", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("پورنهاببب", "پورنهاب"))
    }

    @Test
    fun antiEvasionDiacriticsAndTatweelMatch() {
        // اعراب (U+064B..U+0652) و کشیدگی (U+0640) حذف می‌شوند.
        assertTrue(KeywordMatcher.matches("پ\u064Fورنهاب", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("پورن\u0640هاب", "پورنهاب"))
    }

    @Test
    fun antiEvasionLookalikeFoldsMatch() {
        // ي/ك عربی، ة→ه، ئ→ی، آ→ا.
        assertTrue(KeywordMatcher.matches("پورنهاب", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("كتاب", "کتاب"))
        assertTrue(KeywordMatcher.matches("مدرسة", "مدرسه"))
        assertTrue(KeywordMatcher.matches("رئیس", "رییس"))
        assertTrue(KeywordMatcher.matches("آب", "اب"))
    }

    @Test
    fun antiEvasionLatinLeetFoldsMatch() {
        assertTrue(KeywordMatcher.matches("p0rn", "porn"))
        assertTrue(KeywordMatcher.matches("p@rn", "parn"))
        assertTrue(KeywordMatcher.matches("s3x", "sex"))
    }

    @Test
    fun antiEvasionStillRejectsSubstringOfLongerWord() {
        // مرز کلمه در متن اصلی: «فوتبال» هرگز «فوت» نیست.
        assertFalse(KeywordMatcher.matches("فوتبال", "فوت"))
        assertFalse(KeywordMatcher.matches("رفتم فوتبال دیدم", "فوت"))
        // نیم‌فاصله چسباننده است، نه مرز (H-3).
        assertFalse(KeywordMatcher.matches("فوت‌بال", "فوت"))
        // حرف چسبیده قبل از بازه هم مچ را رد می‌کند.
        assertFalse(KeywordMatcher.matches("اپورنهاب", "پورنهاب"))
    }

    @Test
    fun antiEvasionBoundaryAtStringEdges() {
        assertTrue(KeywordMatcher.matches("پورنهاب", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("پورنهاب آمد", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("آمد پورنهاب", "پورنهاب"))
        // جداکننده بلافاصله بعد از بازه، مرز معتبر است.
        assertTrue(KeywordMatcher.matches("پور.ن هاب!", "پورنهاب"))
        assertTrue(KeywordMatcher.matches("(پور.ن هاب)", "پورنهاب"))
    }

    @Test
    fun antiEvasionSpanCoversSeparatorsInside() {
        val span = KeywordMatcher.findAntiEvasionSpan("سلام پور.ن هاب خداحافظ", "پورنهاب")
        assertEquals(5 to 14, span)
    }

    @Test
    fun plainExactMatchStillWorks() {
        assertTrue(KeywordMatcher.matches("من فوت کردم", "فوت"))
        assertTrue(KeywordMatcher.matches("پورنهاب", "پورنهاب"))
    }

    // ---------- کلمه ناقص هرگز مچ نیست (گزارش سید ۲۰۲۶-۱۱-۲۱: «feet») ----------

    @Test
    fun antiEvasionIncompleteWordNeverMatches() {
        // وسط تایپ، کلمه ناتمام نباید پاک شود: «fe» و «fet» هرگز «feet» نیستند.
        assertFalse(KeywordMatcher.matches("fe", "feet"))
        assertFalse(KeywordMatcher.matches("f", "feet"))
        assertFalse(KeywordMatcher.matches("fee", "feet"))
        assertFalse(KeywordMatcher.matches("fet", "feet"))
        assertNull(KeywordMatcher.findAntiEvasionSpan("fet", "feet"))
    }

    @Test
    fun antiEvasionLongerWordWithKeywordLettersDoesNotMatch() {
        // کلمه بلندتر که حروف کلیدواژه را در توالی دارد، مچ نیست.
        assertFalse(KeywordMatcher.matches("federal", "feet"))
        assertFalse(KeywordMatcher.matches("feetball", "feet"))
        // حروف در دو کلمه جدا و از وسط کلمه شروع شده‌اند — مرز کلمه رد می‌کند.
        assertFalse(KeywordMatcher.matches("coffee time", "feet"))
    }

    @Test
    fun antiEvasionFullKeywordEvasionsStillMatch() {
        // فرار با تکرار اضافه، جداکننده و فاصله، هنوز گرفته می‌شود.
        assertTrue(KeywordMatcher.matches("feet", "feet"))
        assertTrue(KeywordMatcher.matches("f.e.e.t", "feet"))
        assertTrue(KeywordMatcher.matches("feeeet", "feet"))
        assertTrue(KeywordMatcher.matches("fee,t", "feet"))
        assertTrue(KeywordMatcher.matches("fee.t", "feet"))
        assertTrue(KeywordMatcher.matches("Fee    t", "feet"))
        assertTrue(KeywordMatcher.matches("fee t", "feet"))
        assertTrue(KeywordMatcher.matches("Poooor   n", "porn"))
        assertTrue(KeywordMatcher.matches("I saw feet today", "feet"))
    }

    @Test
    fun antiEvasionSpacedSpanCoversInnerSpaces() {
        // بازه «Fee    t» کل کلمه را با فاصله‌های داخلش می‌گیرد.
        assertEquals(0 to 8, KeywordMatcher.findAntiEvasionSpan("Fee    t", "feet"))
        assertEquals(0 to 10, KeywordMatcher.findAntiEvasionSpan("Poooor   n", "porn"))
    }
}
