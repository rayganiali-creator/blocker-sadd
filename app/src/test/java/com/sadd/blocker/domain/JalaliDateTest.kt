package com.sadd.blocker.domain

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateTest {

    private fun gregorianEpochMs(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun knownAnchorDates() {
        // ۲۶ آبان ۱۴۰۵ = ۱۷ نوامبر ۲۰۲۶ (امروزِ ساخت پروژه).
        val j1 = JalaliDate.fromEpochMs(gregorianEpochMs(2026, 11, 17), utc)
        assertEquals(JalaliDate.Jalali(1405, 8, 26), j1)
        // نوروزها
        assertEquals(JalaliDate.Jalali(1405, 1, 1), JalaliDate.fromEpochMs(gregorianEpochMs(2026, 3, 21), utc))
        assertEquals(JalaliDate.Jalali(1404, 1, 1), JalaliDate.fromEpochMs(gregorianEpochMs(2025, 3, 21), utc))
        assertEquals(JalaliDate.Jalali(1403, 1, 1), JalaliDate.fromEpochMs(gregorianEpochMs(2024, 3, 20), utc))
        assertEquals(JalaliDate.Jalali(1402, 1, 1), JalaliDate.fromEpochMs(gregorianEpochMs(2023, 3, 21), utc))
        assertEquals(JalaliDate.Jalali(1379, 1, 1), JalaliDate.fromEpochMs(gregorianEpochMs(2000, 3, 20), utc))
        // ۱۴۰۳ کبیسه است: ۳۰ اسفند دارد.
        assertEquals(JalaliDate.Jalali(1403, 12, 30), JalaliDate.fromEpochMs(gregorianEpochMs(2025, 3, 20), utc))
        assertTrue(JalaliDate.isLeapJalaliYear(1403))
        assertFalse(JalaliDate.isLeapJalaliYear(1404))
    }

    @Test
    fun roundTripOverManyYears() {
        // رفت‌وبرگشت جلالی→میلادی→جلالی برای ۲۰۰ سال باید هویت باشد.
        for (jy in 1300..1500) {
            for ((jm, jd) in listOf(1 to 1, 6 to 31, 12 to 29)) {
                val g = JalaliDate.jalaliToGregorian(jy, jm, jd)
                val back = JalaliDate.gregorianToJalali(g.gy, g.gm, g.gd)
                assertEquals(JalaliDate.Jalali(jy, jm, jd), back)
            }
        }
    }

    @Test
    fun formatDatePersianDigitsAndMonthName() {
        val epoch = gregorianEpochMs(2026, 11, 17)
        assertEquals("۲۶ آبان ۱۴۰۵", JalaliDate.formatDate(epoch, utc))
        assertEquals("۲۶ آبان ۱۴۰۵، ۰۰:۰۰", JalaliDate.formatDateTime(epoch, utc))
        assertEquals("۱۲۳۴", JalaliDate.toPersianDigits("1234"))
    }
}
