package com.sadd.blocker.domain

/**
 * قالب‌بندی زمان — خالص Kotlin.
 * ارقام لاتین استفاده می‌شوند (همان‌طور که در موکاپ‌های تاییدشده آمده است).
 */
object TimeFormat {

    /** باقی‌مانده به‌صورت HH:MM:SS؛ ورودی منفی صفر حساب می‌شود. */
    fun formatRemainingMs(ms: Long): String {
        val totalSeconds = (if (ms < 0) 0 else ms) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    /** دقیقه از نیمه‌شب به‌صورت HH:MM. */
    fun formatMinuteOfDay(minute: Int): String {
        val h = minute / 60
        val m = minute % 60
        return "%02d:%02d".format(h, m)
    }

    /** تجزیه "HH:MM" به دقیقه از نیمه‌شب؛ نامعتبر null برمی‌گرداند. */
    fun parseMinuteOfDay(text: String): Int? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h < 0 || h > 23 || m < 0 || m > 59) return null
        return h * 60 + m
    }

    /** برچسب فارسی مدت سشن، مثل "30 دقیقه" یا "3 ساعت". */
    fun durationLabel(durationMs: Long): String {
        val minutes = durationMs / 60_000L
        if (minutes < 60) return "$minutes دقیقه"
        val hours = minutes / 60
        val rem = minutes % 60
        if (rem == 0L) return "$hours ساعت"
        return "$hours ساعت و $rem دقیقه"
    }
}
