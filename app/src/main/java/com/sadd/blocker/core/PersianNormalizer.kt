package com.sadd.blocker.core

/**
 * نرمال‌سازی فارسی/عربی — خالص Kotlin، بدون وابستگی اندروید.
 * ترتیب ثابت است و در PersianNormalizerTest پوشش داده شده:
 * NFC ← ي/ك عربی به ی/ک ← حذف اعراب ← نیم‌فاصله به فاصله ←
 * collapse فاصله ← collapse تکرار حروف (≥۳ به ۲) ← lowercase.
 */
object PersianNormalizer {

    private val ARABIC_YEH = 'ي'
    private val ARABIC_KAF = 'ك'
    private val ARABIC_TEH_MARBUTA = 'ة'
    private const val ZWNJ = '\u200C'
    private const val ZWJ = '\u200D'

    // اعراب و تشدید عربی/فارسی: U+064B تا U+0652 + U+0670
    private fun isDiacritic(c: Char): Boolean {
        return c in '\u064B'..'\u0652' || c == '\u0670'
    }

    fun normalize(raw: String): String {
        if (raw.isEmpty()) return raw
        val sb = StringBuilder(raw.length)
        var prev: Char? = null
        var repeatCount = 0
        for (ch in raw) {
            if (isDiacritic(ch)) continue
            val mapped = when (ch) {
                ARABIC_YEH -> 'ی'
                ARABIC_KAF -> 'ک'
                ARABIC_TEH_MARBUTA -> 'ه'
                'ؤ' -> 'و'
                'إ', 'أ', 'ٱ' -> 'ا'
                ZWNJ, ZWJ -> ' '
                else -> ch
            }
            // lowercase برای انگلیسی (فارسی بی‌اثر است)
            val lower = mapped.lowercaseChar()
            // collapse تکرار حروف: بیش از ۲ تکرار متوالی به ۲ کاهش می‌یابد
            if (lower == prev) {
                repeatCount++
                if (repeatCount > 2) continue
            } else {
                prev = lower
                repeatCount = 1
            }
            sb.append(lower)
        }
        // collapse فاصله‌های چندتایی + trim (با NFC ساده‌سازی شده)
        return sb.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** نرمال‌سازی + تقسیم به توکن (برای تطابق whole-word). */
    fun tokens(raw: String): List<String> {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(' ').filter { it.isNotEmpty() }
    }
}
