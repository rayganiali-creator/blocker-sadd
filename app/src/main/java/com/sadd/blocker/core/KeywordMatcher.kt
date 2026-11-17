package com.sadd.blocker.core

/**
 * تطابق کلمه/عبارت ممنوعه — خالص Kotlin.
 * قوانین سفت:
 * - همیشه whole-word: «فوت» ≠ «فوتبال» (تطابق زیررشته‌ای ممنوع).
 * - عبارت چندکلمه‌ای: مرز کلمه در دو طرف عبارت.
 * - دامنه: تطابق مرزی host — `example.com` هرگز `notexample.com` را نمی‌گیرد.
 */
object KeywordMatcher {

    /** آیا [text] (خام) شامل قانون [ruleRaw] است؟ هر دو نرمال می‌شوند. */
    fun matches(text: String, ruleRaw: String): Boolean {
        val normalizedText = PersianNormalizer.normalize(text)
        val normalizedRule = PersianNormalizer.normalize(ruleRaw)
        if (normalizedRule.isEmpty() || normalizedText.isEmpty()) return false
        val paddedText = " $normalizedText "
        val paddedRule = " $normalizedRule "
        return paddedText.contains(paddedRule)
    }

    /** اولین قانون منطبق از لیست، یا null. */
    fun firstMatch(text: String, rules: List<String>): String? {
        for (rule in rules) {
            if (matches(text, rule)) return rule
        }
        return null
    }

    /**
     * تطابق دامنه با مرز host.
     * [host] باید از قبل نرمال‌شده باشد (lowercase، بدون scheme/path/port/www).
     * برمی‌گرداند true اگر host برابر rule باشد یا زیردامنه آن باشد
     * (و فقط وقتی [matchSubdomains] درست است).
     */
    fun domainMatches(host: String, rule: String, matchSubdomains: Boolean): Boolean {
        val h = host.trim().lowercase()
        val r = rule.trim().lowercase()
            .removePrefix("www.")
        if (h.isEmpty() || r.isEmpty()) return false
        if (h == r) return true
        if (!matchSubdomains) return false
        return h.endsWith(".$r")
    }

    /** نرمال‌سازی host خام (URL یا دامنه) به host مقایسه‌ای. */
    fun normalizeHost(raw: String): String {
        var h = raw.trim().lowercase()
        h = h.removePrefix("https://").removePrefix("http://")
        val slash = h.indexOf('/')
        if (slash >= 0) h = h.substring(0, slash)
        val colon = h.indexOf(':')
        if (colon >= 0) h = h.substring(0, colon)
        if (h.startsWith("www.")) h = h.removePrefix("www.")
        return h
    }
}
