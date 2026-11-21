package com.sadd.blocker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ترتیب حروف کیبورد هوشمند و تم‌های رنگی‌اش (گزارش سید ۲۰۲۶-۱۱-۲۱:
 * «ترتیب حروف فارسی درست نیست»). ترتیب فارسی باید دقیقاً چیدمان
 * استاندارد ایرانی باشد و بی‌سروصدا به هم نریزد.
 */
class KeyboardLayoutsTest {

    @Test
    fun persianRowsMatchStandardIranianLayout() {
        assertEquals(
            listOf(
                listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ"),
                listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
                listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و"),
            ),
            KeyboardLayouts.PERSIAN_ROWS,
        )
    }

    @Test
    fun englishRowsMatchQwerty() {
        assertEquals(
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m"),
            ),
            KeyboardLayouts.ENGLISH_ROWS,
        )
    }

    @Test
    fun persianRowsHaveNoDuplicateLetters() {
        val letters = KeyboardLayouts.PERSIAN_ROWS.flatten()
        assertEquals(letters.size, letters.toSet().size)
    }

    @Test
    fun themeByKeyFallsBackToDefault() {
        assertEquals(KeyboardThemes.DEFAULT, KeyboardThemes.themeByKey(null))
        assertEquals(KeyboardThemes.DEFAULT, KeyboardThemes.themeByKey(""))
        assertEquals(KeyboardThemes.DEFAULT, KeyboardThemes.themeByKey("no_such_theme"))
        assertEquals("light", KeyboardThemes.themeByKey("light").key)
    }

    @Test
    fun nextThemeCyclesThroughAllAndWraps() {
        var current = KeyboardThemes.DEFAULT
        val seen = mutableSetOf(current.key)
        repeat(KeyboardThemes.ALL.size - 1) {
            current = KeyboardThemes.next(current)
            seen.add(current.key)
        }
        assertEquals(KeyboardThemes.ALL.size, seen.size)
        // بعد از آخری، چرخه به اولی برمی‌گردد.
        assertEquals(KeyboardThemes.DEFAULT, KeyboardThemes.next(current))
    }

    @Test
    fun themesHaveDistinctKeysAndOpaqueColors() {
        val keys = KeyboardThemes.ALL.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        for (theme in KeyboardThemes.ALL) {
            for (color in listOf(
                theme.background,
                theme.keyBackground,
                theme.keyText,
                theme.toolbarBackground,
                theme.toolbarText,
            )) {
                // آلفا باید کامل (FF) باشد تا رنگ روی هر پس‌زمینه‌ای خوانا بماند.
                assertTrue((color ushr 24) == 0xFF)
            }
        }
    }
}
