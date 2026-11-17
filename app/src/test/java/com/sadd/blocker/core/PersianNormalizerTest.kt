package com.sadd.blocker.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianNormalizerTest {

    @Test
    fun arabicYehAndKafNormalizeToPersian() {
        assertEquals("یادگیری", PersianNormalizer.normalize("يادگيري"))
        assertEquals("کتاب", PersianNormalizer.normalize("كتاب"))
    }

    @Test
    fun zwnjPreservedAsJoinerNotSpace() {
        // رفع H-3: نیم‌فاصله چسباننده کلمه است، نه جداکننده.
        // رفتار قبلی (تبدیل به فاصله) عمداً عوض شد چون «فوت‌بال» را
        // به‌اشتباه با «فوت» مچ می‌کرد.
        assertEquals("می‌خواهم", PersianNormalizer.normalize("می‌خواهم"))
        assertEquals("فوت‌بال", PersianNormalizer.normalize("فوت‌بال"))
    }

    @Test
    fun canonicalTurnsZwnjIntoSpaceForDedupe() {
        assertEquals("می خواهم", PersianNormalizer.canonical("می‌خواهم"))
        assertEquals("فوت بال", PersianNormalizer.canonical("فوت‌بال"))
    }

    @Test
    fun punctuationBecomesSpace() {
        // M-1: کلمه چسبیده به علائم نگارشی، همان کلمه است.
        assertEquals("فوت دیدم", PersianNormalizer.normalize("فوت، دیدم"))
        assertEquals("فوت آمد", PersianNormalizer.normalize("(فوت) آمد"))
    }

    @Test
    fun loneZwnjIsEmpty() {
        assertEquals("", PersianNormalizer.normalize("‌"))
    }

    @Test
    fun extraSpacesCollapseAndTrim() {
        assertEquals("سلام دنیا", PersianNormalizer.normalize("  سلام    دنیا  "))
    }

    @Test
    fun diacriticsRemoved() {
        assertEquals("مدرسه", PersianNormalizer.normalize("مَدْرَسَه"))
    }

    @Test
    fun repeatedLettersCollapseToTwo() {
        assertEquals("خووب", PersianNormalizer.normalize("خووووب"))
        assertEquals("سلام", PersianNormalizer.normalize("سلام"))
    }

    @Test
    fun englishLowercased() {
        assertEquals("example", PersianNormalizer.normalize("ExAmPlE"))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", PersianNormalizer.normalize(""))
    }

    @Test
    fun tokensSplitOnSpace() {
        assertEquals(listOf("سلام", "دنیا"), PersianNormalizer.tokens("سلام   دنیا"))
    }
}
