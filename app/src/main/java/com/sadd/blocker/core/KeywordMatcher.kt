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
        if (matchesWholeWord(text, ruleRaw)) return true
        // گذر دوم ضدفرار (گزارش سید ۲۰۲۶-۱۱-۲۰): «پور.ن هاب» و
        // «پ و ر ن ه ا ب» و «پووورنهاب» نباید از «پورنهاب» فرار کنند؛
        // مرز کلمه در متن اصلی چک می‌شود تا «فوتبال» همچنان «فوت» را
        // نگیرد.
        return findAntiEvasionSpan(text, ruleRaw) != null
    }

    /** تطابق whole-word کلاسیک (اتم‌ها + مرز فاصله/نیم‌فاصله). */
    private fun matchesWholeWord(text: String, ruleRaw: String): Boolean {
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

    // ---------- ضدفرار (anti-evasion) ----------

    /**
     * یک حرف نگه‌داشته‌شده در جریان تمیزشده + بازه‌اش در متن اصلی +
     * تعداد تکرار متوالی‌اش ([count]) — تکرارها فرو می‌ریزند ولی
     * شمارشان نگه داشته می‌شود تا کلمه ناقص مچ نشود.
     */
    private data class KeptChar(val ch: Char, val start: Int, var end: Int, var count: Int = 1)

    /**
     * تاشدن حروف هم‌شکل برای مسیر ضدفرار: ي→ی، ك→ک، ة→ه، ؤ→و، ئ→ی،
     * أ/إ/آ/ٱ→ا و leet محافظه‌کارانه لاتین (0→o، 1→l، 3→e، 4→a،
     * ‎@→a، $→s) + lowercase. بقیه حروف دست نمی‌خورند.
     */
    private fun foldLookalike(c: Char): Char {
        return when (c) {
            'ي' -> 'ی'
            'ك' -> 'ک'
            'ة' -> 'ه'
            'ؤ' -> 'و'
            'ئ' -> 'ی'
            'أ', 'إ', 'آ', 'ٱ' -> 'ا'
            '0' -> 'o'
            '1' -> 'l'
            '3' -> 'e'
            '4' -> 'a'
            '@' -> 'a'
            '$' -> 's'
            else -> c.lowercaseChar()
        }
    }

    /** آیا این حرف در جریان ضدفرار نگه داشته می‌شود؟ (حرف/رقم، یا @ و $ برای تاشدن leet) */
    private fun isKeptChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '@' || c == '$'
    }

    /**
     * جریان تمیزشده متن خام: فقط حروف/ارقام (و @/$) نگه داشته می‌شوند؛
     * جداکننده‌ها (نقطه، فاصله، خط تیره، زیرخط، نیم‌فاصله، اسلش و…)
     * و اعراب و کشیدگی حذف می‌شوند. تکرار متوالی یک حرف به یک حرف
     * فرو می‌ریزد («پووور» → «پور») ولی بازه حرف نگه‌داشته‌شده تا
     * آخرین تکرار کش می‌آید تا حذف، کل بازه اصلی را بردارد، و تعداد
     * تکرار ([KeptChar.count]) هم نگه داشته می‌شود: مچ ضدفرار فقط
     * وقتی است که تکرار هر حرف در متن دست‌کم به اندازه تکرار همان
     * حرف در کلیدواژه باشد — پس «feeeet» همان «feet» است ولی کلمه
     * ناقص «fet» هرگز «feet» نیست (گزارش سید ۲۰۲۶-۱۱-۲۱). اندیس
     * اصلی هر حرف نگه داشته می‌شود تا مچ به بازه متن اصلی برگردد.
     */
    private fun cleanedStream(text: String): List<KeptChar> {
        val kept = ArrayList<KeptChar>()
        for (i in text.indices) {
            val raw = text[i]
            if (PersianNormalizer.isDiacritic(raw) || PersianNormalizer.isTatweel(raw)) continue
            if (!isKeptChar(raw)) continue
            val folded = foldLookalike(raw)
            val last = kept.lastOrNull()
            if (last != null && last.ch == folded) {
                // تکرار همان حرف: فروبریز، ولی بازه و شمار را تا اینجا بکش.
                last.end = i + 1
                last.count++
            } else {
                kept.add(KeptChar(folded, i, i + 1))
            }
        }
        return kept
    }

    /**
     * کلیدواژه تمیزشده برای مسیر ضدفرار: دنباله (حرف، طول تکرار) —
     * همان تاشدن و فروپاشی تکرار [cleanedStream]، ولی طول هر تکرار
     * نگه داشته می‌شود تا مچ، کلیدواژه کامل را بخواهد نه پیشوندش را.
     */
    private fun cleanedKeywordRuns(ruleRaw: String): List<Pair<Char, Int>> {
        return cleanedStream(ruleRaw).map { it.ch to it.count }
    }

    /**
     * مرز کلمه در متن اصلی: حرف بلافاصله قبل/بعد بازه باید غیرحرف
     * باشد (یا لبه رشته). نیم‌فاصله/ZWJ چسباننده داخل کلمه‌اند و مرز
     * حساب نمی‌شوند («فوت‌بال» همچنان «فوت» نیست). اعراب و کشیدگی
     * چسبیده به لبه بازه رد می‌شوند تا حرف واقعی کناری سنجیده شود.
     */
    private fun isOriginalBoundary(text: String, spanStart: Int, spanEnd: Int): Boolean {
        var before = spanStart - 1
        while (before >= 0 &&
            (PersianNormalizer.isDiacritic(text[before]) || PersianNormalizer.isTatweel(text[before]))
        ) {
            before--
        }
        if (before >= 0) {
            val c = text[before]
            if (c.isLetter() || PersianNormalizer.isJoiner(c)) return false
        }
        var after = spanEnd
        while (after < text.length &&
            (PersianNormalizer.isDiacritic(text[after]) || PersianNormalizer.isTatweel(text[after]))
        ) {
            after++
        }
        if (after < text.length) {
            val c = text[after]
            if (c.isLetter() || PersianNormalizer.isJoiner(c)) return false
        }
        return true
    }

    /**
     * بازه (شروع، پایانِ‌انحصاری) اولین وقوع ضدفرار قانون [ruleRaw]
     * در متن خام [text]، یا null.
     *
     * کلیدواژه تمیزشده در جریان تمیزشده جستجو می‌شود؛ هر وقوع به بازه
     * اصلی برگردانده می‌شود (شامل جداکننده‌های داخل بازه) و فقط وقتی
     * پذیرفته می‌شود که مرز کلمه در متن اصلی برقرار باشد — پس
     * «پور.ن هاب» و «پ و ر ن ه ا ب» و «پووورنهاب» مچ می‌شوند ولی
     * «فوتبال» هرگز «فوت» را نمی‌گیرد.
     *
     * کلیدواژه باید کامل در جریان باشد: حرف‌ها یکی‌یکی و طول تکرار هر
     * حرف در متن دست‌کم به اندازه تکرار همان حرف در کلیدواژه — پس
     * «feeeet» و «fee,t» و «Fee    t» همان «feet»‌اند، ولی کلمه
     * ناقص «fet» و پیشوند «fe» هرگز «feet» نیستند (گزارش سید
     * ۲۰۲۶-۱۱-۲۱: فروپاشی تکرار به‌تنهایی «feet» و «fet» را یکی
     * می‌کرد و کلمه ناتمام وسط تایپ پاک می‌شد).
     */
    fun findAntiEvasionSpan(text: String, ruleRaw: String): Pair<Int, Int>? {
        val keywordRuns = cleanedKeywordRuns(ruleRaw)
        if (keywordRuns.isEmpty()) return null
        val stream = cleanedStream(text)
        if (stream.size < keywordRuns.size) return null
        for (start in 0..stream.size - keywordRuns.size) {
            var ok = true
            for (j in keywordRuns.indices) {
                val kept = stream[start + j]
                val (kwCh, kwCount) = keywordRuns[j]
                if (kept.ch != kwCh || kept.count < kwCount) {
                    ok = false
                    break
                }
            }
            if (!ok) continue
            val spanStart = stream[start].start
            val spanEnd = stream[start + keywordRuns.size - 1].end
            if (isOriginalBoundary(text, spanStart, spanEnd)) {
                return spanStart to spanEnd
            }
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
