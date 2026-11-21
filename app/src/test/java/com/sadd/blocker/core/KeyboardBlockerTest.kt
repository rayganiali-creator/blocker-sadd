package com.sadd.blocker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست تصمیم کیبورد هوشمند: کلمه بلاک‌شده لحظه تمام‌شدن (فاصله/علائم)
 * پاک می‌شود؛ وسط تایپ هرگز؛ «فوتبال» هرگز «فوت» را نمی‌گیرد؛
 * ضدفرار («پور.ن هاب») گرفته می‌شود؛ خارج از سشن هیچ چکی نیست.
 */
class KeyboardBlockerTest {

    private val keywords = listOf("فوت", "پورنهاب", "می روم", "porn")

    @Test
    fun noSessionNoDelete() {
        assertNull(KeyboardBlocker.decide("فوت ", keywords, sessionActive = false))
    }

    @Test
    fun emptyKeywordsNoDelete() {
        assertNull(KeyboardBlocker.decide("فوت ", emptyList(), sessionActive = true))
    }

    @Test
    fun emptyTextNoDelete() {
        assertNull(KeyboardBlocker.decide("", keywords, sessionActive = true))
        assertNull(KeyboardBlocker.decide("   ", keywords, sessionActive = true))
    }

    @Test
    fun wordStillBeingTypedIsNotDeleted() {
        // «فوت» وسط تایپ است (جداکننده‌ای نیامده) — حذف نمی‌شود تا
        // «فوتبال» قابل تایپ بماند.
        assertNull(KeyboardBlocker.decide("فوت", keywords, sessionActive = true))
        assertNull(KeyboardBlocker.decide("سلام فوت", keywords, sessionActive = true))
    }

    @Test
    fun completedBlockedWordIsDeletedWithItsSeparator() {
        assertEquals(0 to 4, KeyboardBlocker.decide("فوت ", keywords, sessionActive = true))
    }

    @Test
    fun completedBlockedWordAfterOtherText() {
        // «سلام فوت » — فقط «فوت » پاک می‌شود.
        assertEquals(5 to 9, KeyboardBlocker.decide("سلام فوت ", keywords, sessionActive = true))
    }

    @Test
    fun footballDoesNotTripFoot() {
        assertNull(KeyboardBlocker.decide("فوتبال ", keywords, sessionActive = true))
        assertNull(KeyboardBlocker.decide("فوتبال", keywords, sessionActive = true))
    }

    @Test
    fun punctuationCompletesTheWord() {
        assertEquals(0 to 4, KeyboardBlocker.decide("فوت،", keywords, sessionActive = true))
        assertEquals(0 to 4, KeyboardBlocker.decide("فوت.", keywords, sessionActive = true))
    }

    @Test
    fun newlineCompletesTheWord() {
        assertEquals(0 to 4, KeyboardBlocker.decide("فوت\n", keywords, sessionActive = true))
    }

    @Test
    fun antiEvasionSeparatorsAreCaught() {
        // «پور.ن هاب » با قانون «پورنهاب» — کل بازه گریخته پاک می‌شود.
        assertEquals(0 to 10, KeyboardBlocker.decide("پور.ن هاب ", keywords, sessionActive = true))
    }

    @Test
    fun antiEvasionSpacedLettersAreCaught() {
        assertEquals(
            0 to 14,
            KeyboardBlocker.decide("پ و ر ن ه ا ب ", keywords, sessionActive = true),
        )
    }

    @Test
    fun antiEvasionRepeatedLettersAreCaught() {
        assertEquals(0 to 6, KeyboardBlocker.decide("فوووت ", keywords, sessionActive = true))
    }

    @Test
    fun antiEvasionWithDiacriticTailIsCaught() {
        // اعراب چسبیده به انتهای کلمه گریخته («پور.ن هابً») هم پاک می‌شود.
        assertEquals(0 to 11, KeyboardBlocker.decide("پور.ن هابً ", keywords, sessionActive = true))
    }

    @Test
    fun phraseKeywordMatches() {
        assertEquals(0 to 7, KeyboardBlocker.decide("می روم ", keywords, sessionActive = true))
    }

    @Test
    fun phraseKeywordMatchesWithZwnj() {
        // «می‌روم» با نیم‌فاصله همان «می روم» است.
        assertEquals(0 to 7, KeyboardBlocker.decide("می‌روم ", keywords, sessionActive = true))
    }

    @Test
    fun earlierMatchNotAtEndIsNotDeleted() {
        // «فوت» اول متن است و کلمه تازه‌تمام‌شده «سلام» است — چیزی پاک نمی‌شود.
        assertNull(KeyboardBlocker.decide("فوت سلام ", keywords, sessionActive = true))
    }

    @Test
    fun englishKeywordMatchesWholeWordOnly() {
        assertEquals(0 to 5, KeyboardBlocker.decide("porn ", keywords, sessionActive = true))
        assertNull(KeyboardBlocker.decide("pornography ", keywords, sessionActive = true))
    }

    @Test
    fun innocentWordIsNotDeleted() {
        assertNull(KeyboardBlocker.decide("سلام دنیا ", keywords, sessionActive = true))
    }

    @Test
    fun secondKeywordInListMatches() {
        assertEquals(
            0 to 8,
            KeyboardBlocker.decide("پورنهاب ", keywords, sessionActive = true),
        )
    }

    @Test
    fun isSaddImeCurrentRecognizesOwnKeyboard() {
        assertTrue(
            KeyboardBlocker.isSaddImeCurrent(
                "com.sadd.blocker/.service.SaddKeyboardService",
                "com.sadd.blocker",
            ),
        )
        assertTrue(
            KeyboardBlocker.isSaddImeCurrent(
                "com.sadd.blocker/com.sadd.blocker.service.SaddKeyboardService",
                "com.sadd.blocker",
            ),
        )
    }

    @Test
    fun isSaddImeCurrentRejectsOthers() {
        assertFalse(
            KeyboardBlocker.isSaddImeCurrent(
                "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME",
                "com.sadd.blocker",
            ),
        )
        // پکیجی که فقط با همان حروف شروع می‌شود، کیبورد سد نیست.
        assertFalse(
            KeyboardBlocker.isSaddImeCurrent(
                "com.sadd.blocker2/.service.SaddKeyboardService",
                "com.sadd.blocker",
            ),
        )
        assertFalse(KeyboardBlocker.isSaddImeCurrent(null, "com.sadd.blocker"))
        assertFalse(KeyboardBlocker.isSaddImeCurrent("", "com.sadd.blocker"))
        assertFalse(KeyboardBlocker.isSaddImeCurrent("com.sadd.blocker/.service.SaddKeyboardService", ""))
    }
}
