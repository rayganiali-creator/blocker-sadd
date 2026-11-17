package com.sadd.blocker.core

/**
 * تطابق کلمه/عبارت ممنوعه — خالص Kotlin.
 * قوانین سفت:
 * - همیشه whole-word: «فوت» ≠ «فوتبال» (تطابق زیررشته‌ای ممنوع).
 * - نیم‌فاصله چسباننده کلمه است، نه مرز: «فوت‌بال» یک کلمه مرکب است و
 *   قانون تک‌کلمه‌ای «فوت» در آن مچ نمی‌شود (H-3)؛ ولی عبارت چندکلمه‌ای
 *   «می روم» در «می‌روم» مچ می‌شود، چون مرز داخلی عبارت می‌تواند فاصله
 *   یا نیم‌فاصله باشد.
 * - کلمه چسبیده به علائم نگارشی مچ می‌شود: «فوت،» = «فوت» (M-1).
 * - عبارت چندکلمه‌ای: مرز کلمه در دو طرف عبارت.
 * - دامنه: تطابق مرزی host — `example.com` هرگز `notexample.com` را نمی‌گیرد.
 */
object KeywordMatcher {

    /**
     * متن نرمال‌شده به «اتم» (بخش‌های کلمه) + نوع جداکننده بین اتم‌ها.
     * جداکننده `true` یعنی نیم‌فاصله/ZWJ (چسباننده)، `false` یعنی فاصله.
     * مثال: «فوت‌بال» → atoms=[فوت، بال]، joiners=[true]
     *       «می روم»   → atoms=[می، روم]،   joiners=[false]
     */
    private data class AtomSeq(val atoms: List<String>, val joiners: List<Boolean>)

    private fun atomize(normalized: String): AtomSeq {
        val atoms = ArrayList<String>()
        val joiners = ArrayList<Boolean>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                atoms.add(current.toString())
                current.clear()
            }
        }
        for (ch in normalized) {
            when {
                // جداکننده فقط وقتی ثبت می‌شود که اتمی قبلش باشد؛
                // جداکننده‌های ابتدا/انتها و پشت‌سرهم نادیده گرفته می‌شوند.
                // پس همیشه joiners.size == atoms.size - 1 است.
                ch == ' ' -> {
                    if (current.isNotEmpty()) {
                        flush()
                        joiners.add(false)
                    }
                }
                PersianNormalizer.isJoiner(ch) -> {
                    if (current.isNotEmpty()) {
                        flush()
                        joiners.add(true)
                    }
                }
                else -> current.append(ch)
            }
        }
        flush()
        return AtomSeq(atoms, joiners)
    }

    /** آیا [text] (خام) شامل قانون [ruleRaw] است؟ هر دو نرمال می‌شوند. */
    fun matches(text: String, ruleRaw: String): Boolean {
        val normalizedText = PersianNormalizer.normalize(text)
        val normalizedRule = PersianNormalizer.normalize(ruleRaw)
        if (normalizedRule.isEmpty() || normalizedText.isEmpty()) return false
        val textSeq = atomize(normalizedText)
        val ruleSeq = atomize(normalizedRule)
        val ruleAtoms = ruleSeq.atoms
        if (ruleAtoms.isEmpty()) return false
        val n = ruleAtoms.size
        // دنباله اتم‌های قانون باید به‌صورت پیوسته در متن باشد و مرز قبل و بعدش
        // مرز کلمه (شروع/پایان متن یا فاصله) باشد — نه نیم‌فاصله.
        for (start in 0..textSeq.atoms.size - n) {
            var ok = true
            for (i in 0 until n) {
                if (textSeq.atoms[start + i] != ruleAtoms[i]) {
                    ok = false
                    break
                }
            }
            if (!ok) continue
            // مرز قبل: شروع متن یا جداکننده فاصله (نه نیم‌فاصله).
            if (start > 0 && textSeq.joiners[start - 1]) continue
            // مرز بعد: پایان متن یا جداکننده فاصله (نه نیم‌فاصله).
            if (start + n < textSeq.atoms.size && textSeq.joiners[start + n - 1]) continue
            // مرزهای داخلی قانون مهم نیستند: «می روم» در «می‌روم» مچ می‌شود.
            return true
        }
        return false
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

    /**
     * نرمال‌سازی host خام (URL یا دامنه) به host مقایسه‌ای (M-2):
     * lowercase، حذف scheme/path/port/www، حذف نقطه‌های ابتدا/انتها.
     * «example.com.» → «example.com»؛ «user@example.com» دست نمی‌خورد
     * (اعتبارسنجی در [com.sadd.blocker.domain.RuleValidation] آن را رد می‌کند).
     */
    fun normalizeHost(raw: String): String {
        var h = raw.trim().lowercase()
        h = h.removePrefix("https://").removePrefix("http://")
        val slash = h.indexOf('/')
        if (slash >= 0) h = h.substring(0, slash)
        val question = h.indexOf('?')
        if (question >= 0) h = h.substring(0, question)
        val hash = h.indexOf('#')
        if (hash >= 0) h = h.substring(0, hash)
        val colon = h.indexOf(':')
        if (colon >= 0) h = h.substring(0, colon)
        if (h.startsWith("www.")) h = h.removePrefix("www.")
        h = h.trim('.')
        return h
    }
}
