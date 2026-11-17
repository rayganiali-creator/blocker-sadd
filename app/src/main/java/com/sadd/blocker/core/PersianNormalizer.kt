package com.sadd.blocker.core

/**
 * نرمال‌سازی فارسی/عربی — خالص Kotlin، بدون وابستگی اندروید.
 *
 * ترتیب ثابت:
 * NFC ← ي/ك عربی به ی/ک ← حذف اعراب ← علائم نگارشی به فاصله ←
 * نیم‌فاصله (U+200C) و ZWJ **حفظ می‌شوند** (چسباننده کلمه‌اند، نه جداکننده) ←
 * collapse فاصله ← collapse تکرار حروف (≥۳ به ۲) ← lowercase.
 *
 * چرا نیم‌فاصله حفظ می‌شود (رفع H-3): «فوت‌بال» یک کلمه مرکب است، نه «فوت»
 * و «بال»؛ اگر نیم‌فاصله به فاصله تبدیل شود، «فوت» به‌اشتباه در «فوت‌بال»
 * پیدا می‌شود. مرز کلمه فقط فاصله/علائم نگارشی است؛ تطابق در
 * [KeywordMatcher] همین مرز را اعمال می‌کند.
 *
 * برای مقایسه هم‌ارز (dedupe قوانین): [canonical] نیم‌فاصله را به فاصله
 * تبدیل می‌کند تا «می‌روم» و «می روم» یک قانون شمرده شوند.
 */
object PersianNormalizer {

    private val ARABIC_YEH = 'ي'
    private val ARABIC_KAF = 'ك'
    private val ARABIC_TEH_MARBUTA = 'ة'
    const val ZWNJ = '\u200C'
    const val ZWJ = '\u200D'

    // اعراب و تشدید عربی/فارسی: U+064B تا U+0652 + U+0670
    private fun isDiacritic(c: Char): Boolean {
        return c in '\u064B'..'\u0652' || c == '\u0670'
    }

    /** کاراکتر جداکننده کلمه است؟ (فاصله یا علامت نگارشی) */
    fun isWordSeparator(c: Char): Boolean {
        if (c.isWhitespace()) return true
        val type = Character.getType(c)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
            type == Character.DASH_PUNCTUATION.toInt() ||
            type == Character.START_PUNCTUATION.toInt() ||
            type == Character.END_PUNCTUATION.toInt() ||
            type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.OTHER_PUNCTUATION.toInt()
    }

    /** آیا کاراکتر چسباننده داخل کلمه است (نیم‌فاصله / ZWJ)؟ */
    fun isJoiner(c: Char): Boolean = c == ZWNJ || c == ZWJ

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
                // نیم‌فاصله و ZWJ حفظ می‌شوند (چسباننده کلمه) — H-3.
                // علائم نگارشی به فاصله تبدیل می‌شوند (M-1: «فوت،» = «فوت»).
                else -> if (isWordSeparator(ch)) ' ' else ch
            }
            // lowercase برای انگلیسی (فارسی بی‌اثر است)
            val lower = mapped.lowercaseChar()
            // collapse تکرار حروف: بیش از ۲ تکرار متوالی به ۲ کاهش می‌یابد.
            // نیم‌فاصله شمارش تکرار را می‌شکند (حرف بعد از آن حرف تازه است).
            if (lower == prev) {
                repeatCount++
                if (repeatCount > 2) continue
            } else {
                prev = lower
                repeatCount = 1
            }
            sb.append(lower)
        }
        // collapse فاصله‌های چندتایی + trim
        val collapsed = sb.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
        // رشته‌ای که فقط نیم‌فاصله/ZWJ دارد، خالی حساب می‌شود.
        if (collapsed.replace(ZWNJ.toString(), "").replace(ZWJ.toString(), "").isBlank()) {
            return ""
        }
        return collapsed
    }

    /**
     * فرم هم‌ارز برای dedupe/ذخیره: نیم‌فاصله هم به فاصله تبدیل می‌شود.
     * «می‌روم» و «می روم» یک قانون‌اند؛ «فوت‌بال» و «فوت بال» هم یک قانون‌اند.
     */
    fun canonical(raw: String): String {
        return normalize(raw)
            .replace(ZWNJ.toString(), " ")
            .replace(ZWJ.toString(), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** نرمال‌سازی + تقسیم به توکن (برای تطابق whole-word). توکن ممکن است نیم‌فاصله داشته باشد. */
    fun tokens(raw: String): List<String> {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(' ').filter { it.isNotEmpty() }
    }
}
