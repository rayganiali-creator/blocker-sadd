package com.sadd.blocker.core

/**
 * تصمیم‌های خالص متنی موتور بلاک — بدون وابستگی اندروید، قابل تست JVM.
 *
 * همه تطابق‌ها از [KeywordMatcher] می‌آیند (whole-word، نیم‌فاصله
 * چسباننده است): «فوت» هرگز «فوتبال» یا «فوت‌بال» را نمی‌گیرد — پس
 * حذف کلمه هم فقط همان کلمه کامل را از فیلد برمی‌دارد، نه بخشی از
 * یک کلمه دیگر را.
 */
object TextRedactor {

    /** اولین قانون کلیدی منطبق در [text]، یا null. */
    fun findMatchedKeyword(text: String, rules: List<String>): String? {
        return KeywordMatcher.firstMatch(text, rules)
    }

    /** قانون دامنه برای تطابق host. */
    data class DomainRule(val host: String, val matchSubdomains: Boolean)

    /**
     * اولین قانون دامنه منطبق با [hostCandidate] (host نرمال‌شده)، یا null.
     * host قانون هم دوباره نرمال می‌شود تا داده قدیمی/کثیف هم درست کار کند.
     */
    fun findMatchedDomain(hostCandidate: String, rules: List<DomainRule>): DomainRule? {
        val host = KeywordMatcher.normalizeHost(hostCandidate)
        if (host.isEmpty()) return null
        for (rule in rules) {
            val ruleHost = KeywordMatcher.normalizeHost(rule.host)
            if (KeywordMatcher.domainMatches(host, ruleHost, rule.matchSubdomains)) {
                return rule
            }
        }
        return null
    }

    /**
     * حدس host از متن فیلد (omnibox کروم یا جعبه جستجو): اولین توکن
     * سفیدفاصله‌دار، اگر شبیه host/URL باشد (نقطه داشته باشد). متن
     * جستجوی ساده («سلام دنیا») null برمی‌گرداند — آن را تطابق کلیدی
     * بررسی می‌کند، نه دامنه‌ای.
     */
    fun extractHostCandidate(text: String): String? {
        val token = text.trim().split(' ', '\t', '\n').firstOrNull { it.isNotEmpty() } ?: return null
        if (!token.contains('.')) return null
        val host = KeywordMatcher.normalizeHost(token)
        if (host.isEmpty() || !host.contains('.')) return null
        return host
    }

    /** تاخیر نهایی: تاخیر اختصاصی قانون، وگرنه تاخیر سراسری (ثانیه). */
    fun resolveDelay(ruleDelaySeconds: Int?, globalDelaySeconds: Int): Int {
        val value = ruleDelaySeconds ?: globalDelaySeconds
        return value.coerceIn(0, 3600)
    }

    /** آیا رویداد متعلق به خود اپ «سد» است؟ */
    fun isOwnPackage(packageName: String?, ownPackage: String): Boolean {
        return packageName != null && packageName == ownPackage
    }

    /**
     * حذف همه وقوع‌های whole-word قانون [ruleRaw] از [text].
     * متن به «کلمه» (توالی بدون فاصله) تقسیم می‌شود؛ پنجره‌ای از
     * کلمه‌ها که با قانون مچ شود حذف می‌شود، بقیه با یک فاصله به هم
     * چسبانده می‌شوند. اگر هیچ مچی نباشد، همان متن ورودی برمی‌گردد.
     * مثال: «من فوت کردم» − «فوت» → «من کردم»؛ «فوتبال» دست نمی‌خورد.
     */
    fun removeMatchedRule(text: String, ruleRaw: String): String {
        val ruleAtoms = PersianNormalizer.tokens(ruleRaw)
        if (ruleAtoms.isEmpty()) return text
        val words = text.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return text
        val n = ruleAtoms.size
        val removed = BooleanArray(words.size)
        var start = 0
        while (start <= words.size - n) {
            val candidate = words.subList(start, start + n).joinToString(" ")
            if (KeywordMatcher.matches(candidate, ruleRaw)) {
                for (i in 0 until n) removed[start + i] = true
                start += n
            } else {
                start++
            }
        }
        if (removed.none { it }) return text
        val kept = ArrayList<String>()
        for (i in words.indices) {
            if (!removed[i]) kept.add(words[i])
        }
        return kept.joinToString(" ")
    }

    /** تعداد کاراکتر حذف‌شده — برای fallback دکمه backspace. */
    fun removedCharCount(original: String, redacted: String): Int {
        val diff = original.length - redacted.length
        return if (diff < 0) 0 else diff
    }

    /**
     * بازه کاراکتری اولین وقوع whole-word قانون [ruleRaw] در [text]
     * (شروع، پایانِ‌انحصاری)، یا null. همان منطق پنجره‌کلمه‌ای
     * [removeMatchedRule] است، ولی به‌جای متنِ‌حذف‌شده بازه را می‌دهد
     * تا fallback انتخاب + CUT بتواند همان کلمه را حذف کند.
     */
    fun firstMatchRange(text: String, ruleRaw: String): Pair<Int, Int>? {
        val ruleAtoms = PersianNormalizer.tokens(ruleRaw)
        if (ruleAtoms.isEmpty()) return null
        val spans = ArrayList<Pair<Int, Int>>()
        var i = 0
        while (i < text.length) {
            while (i < text.length && text[i] == ' ') i++
            if (i >= text.length) break
            val start = i
            while (i < text.length && text[i] != ' ') i++
            spans.add(start to i)
        }
        if (spans.isEmpty()) return null
        val words = spans.map { text.substring(it.first, it.second) }
        val n = ruleAtoms.size
        var start = 0
        while (start <= words.size - n) {
            val candidate = words.subList(start, start + n).joinToString(" ")
            if (KeywordMatcher.matches(candidate, ruleRaw)) {
                return spans[start].first to spans[start + n - 1].second
            }
            start++
        }
        return null
    }
}
