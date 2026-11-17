package com.sadd.blocker.domain

import java.util.Calendar
import java.util.TimeZone

/**
 * تقویم جلالی — خالص Kotlin (java.util برای گرفتن تاریخ میلادی).
 *
 * تبدیل میلادی↔جلالی با الگوریتم استاندارد jalaali (چرخه ۳۳ ساله با جدول
 * breaks) پیاده شده؛ بدون وابستگی خارجی. تاریخ‌ها در UI جلالی نشان داده
 * می‌شوند (M-11) — کاربر فارسی‌زبان انتظار تاریخ میلادی ندارد.
 *
 * ارقام در خروجی فارسی‌اند (مثل «۲۶ آبان ۱۴۰۵»)؛ شمارش معکوس همچنان
 * ارقام لاتین دارد (طبق موکاپ تاییدشده).
 */
object JalaliDate {

    data class Jalali(val year: Int, val month: Int, val day: Int)

    private val MONTHS_FA = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** ارقام ASCII را به فارسی تبدیل می‌کند. */
    fun toPersianDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c in '0'..'9') sb.append(PERSIAN_DIGITS[c - '0']) else sb.append(c)
        }
        return sb.toString()
    }

    fun monthNameFa(month: Int): String {
        if (month < 1 || month > 12) return ""
        return MONTHS_FA[month - 1]
    }

    // ---------- تبدیل (الگوریتم jalaali) ----------

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    // تقسیم و باقیمانده با معنای trunc (مثل ~~ در جاوااسکریپت که الگوریتم
    // jalaali با آن نوشته شده): div(-3, 6) = 0 است، نه -1. استفاده از
    // floorDiv اینجا سال را یک واحد جابه‌جا می‌کند.
    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a % b

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        var i = 1
        while (i < bl) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
            i++
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    /** روز ژولیانی تاریخ میلادی. */
    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d -= div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) - 752
        return d
    }

    data class Gregorian(val gy: Int, val gm: Int, val gd: Int)

    /** روز ژولیانی به تاریخ میلادی. */
    private fun d2g(jdn: Int): Gregorian {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Gregorian(gy, gm, gd)
    }

    /** تاریخ جلالی به روز ژولیانی. */
    fun jalaliToDayNumber(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    /** روز ژولیانی به تاریخ جلالی. */
    private fun dayNumberToJalali(jdn: Int): Jalali {
        val gy = d2g(jdn).gy
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + div(k, 31)
                val jd = mod(k, 31) + 1
                return Jalali(jy, jm, jd)
            } else {
                k -= 186
            }
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        val jm = 7 + div(k, 30)
        val jd = mod(k, 30) + 1
        return Jalali(jy, jm, jd)
    }

    /** میلادی به جلالی. */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Jalali {
        return dayNumberToJalali(g2d(gy, gm, gd))
    }

    /** جلالی به میلادی. */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Gregorian {
        val d = jalaliToDayNumber(jy, jm, jd)
        return d2g(d)
    }

    /** آیا سال جلالی کبیسه است؟ (leap == 0 یعنی کبیسه) */
    fun isLeapJalaliYear(jy: Int): Boolean = jalCal(jy).leap == 0

    // ---------- قالب‌بندی ----------

    /** epoch میلی‌ثانیه به تاریخ جلالی در تایم‌زون داده‌شده. */
    fun fromEpochMs(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): Jalali {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = epochMs
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** «۲۶ آبان ۱۴۰۵». */
    fun formatDate(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val j = fromEpochMs(epochMs, zone)
        return toPersianDigits("${j.day} ${monthNameFa(j.month)} ${j.year}")
    }

    /** «۲۶ آبان ۱۴۰۵، ۲۱:۴۴». */
    fun formatDateTime(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = epochMs
        val hh = cal.get(Calendar.HOUR_OF_DAY)
        val mm = cal.get(Calendar.MINUTE)
        val time = "%02d:%02d".format(java.util.Locale.US, hh, mm)
        return "${formatDate(epochMs, zone)}، ${toPersianDigits(time)}"
    }
}
