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
    fun zwnjBecomesSpaceForComparison() {
        assertEquals("می خواهم", PersianNormalizer.normalize("می‌خواهم"))
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
