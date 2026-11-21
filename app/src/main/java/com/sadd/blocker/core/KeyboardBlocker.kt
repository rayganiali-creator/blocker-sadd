package com.sadd.blocker.core

/**
 * تصمیم «کیبورد هوشمند» سد — منطق خالص، بدون وابستگی اندروید.
 *
 * کیبورد سد مثل هر کیبوردی تایپ می‌کند؛ وسط سشن فعال، بعد از هر
 * commit این تصمیم گرفته می‌شود: آیا کلمه‌ای که همین الان تمام شده
 * کلمه بلاک‌شده است؟ اگر بله، بازه‌اش برای حذف فوری برگردانده
 * می‌شود (بدون صفحه بلاک، بدون تاخیر — خودبه‌خود پاک می‌شود).
 *
 * «تمام‌شدن کلمه» یعنی آخرین کاراکتر متنِ قبل از مکان‌نما جداکننده
 * است (فاصله، علائم نگارشی، خط جدید). تا وقتی کلمه در حال تایپ است
 * (آخرین کاراکتر حرف/رقم/چسباننده/اعراب/کشیدگی است) هیچ حذفی
 * نمی‌شود — وگرنه «فوتبال» وسط تایپ در حالت «فوت» پاک می‌شد و
 * هرگز قابل تایپ نبود (قانون whole-word سد: «فوت» ≠ «فوتبال»).
 * پس کلمه بلاک‌شده لحظه‌ای که تمام می‌شود (فاصله/نقطه/ارسال) پاک
 * می‌شود، نه وسط تایپش.
 *
 * تطابق همان موتور همیشگی است: whole-word + ضدفرار
 * ([TextRedactor.firstMatchRange] که به
 * [KeywordMatcher.findAntiEvasionSpan] می‌رسد) — پس «پور.ن هاب» و
 * «پ و ر ن ه ا ب» و «فوووت» هم گرفته می‌شوند، ولی «فوتبال» هرگز
 * «فوت» را نمی‌گیرد. فقط وقوعی حذف می‌شود که در انتهای متنِ قبل از
 * مکان‌نما تمام شود (همان کلمه‌ای که تازه تمام شده)؛ وقوع‌های
 * قدیمی‌تر وسط متن دست نمی‌خورند.
 *
 * خارج از سشن: همیشه null (تایپ عادی، بدون هیچ چکی).
 */
object KeyboardBlocker {

    /**
     * بازه حذف (شروع، پایانِ‌انحصاری) در [textBeforeCursor] یا null.
     * پایان بازه همیشه انتهای [textBeforeCursor] است (کلمه + جداکننده
     * پایانی‌اش با هم پاک می‌شوند تا فاصله دوتایی نماند).
     */
    fun decide(
        textBeforeCursor: String,
        keywords: List<String>,
        sessionActive: Boolean,
    ): Pair<Int, Int>? {
        if (!sessionActive) return null
        if (keywords.isEmpty() || textBeforeCursor.isEmpty()) return null
        // کلمه هنوز در حال تایپ است — حذف وسط کلمه ممنوع است.
        if (isWordContinuing(textBeforeCursor.last())) return null
        // بدنه = متن بدون جداکننده‌های پایانی؛ وقوع باید در انتهای بدنه تمام شود.
        val body = textBeforeCursor.trimEnd { !isWordContinuing(it) }
        if (body.isEmpty()) return null
        for (keyword in keywords) {
            var offset = 0
            while (offset < body.length) {
                val range = TextRedactor.firstMatchRange(body.substring(offset), keyword) ?: break
                val absStart = offset + range.first
                val absEnd = offset + range.second
                if (endsAtBodyEnd(body, absEnd)) {
                    return absStart to textBeforeCursor.length
                }
                // وقوع قدیمی‌تر وسط متن است؛ از بعدش ادامه بده.
                if (absEnd <= offset) break
                offset = absEnd
            }
        }
        return null
    }

    /**
     * آیا [defaultInputMethod] (مقدار Settings.Secure.DEFAULT_INPUT_METHOD)
     * کیبورد سد است؟ قالب آن «package/class» است (flattened component)،
     * پس پیشوند «package/» سنجیده می‌شود تا پکیجی که فقط با همان حروف
     * شروع می‌شود (مثل com.sadd.blocker2) اشتباه گرفته نشود.
     */
    fun isSaddImeCurrent(defaultInputMethod: String?, ownPackage: String): Boolean {
        if (defaultInputMethod.isNullOrBlank() || ownPackage.isBlank()) return false
        return defaultInputMethod.startsWith("$ownPackage/")
    }

    /** کاراکتر ادامه‌دهنده کلمه: حرف، رقم، چسباننده (نیم‌فاصله/ZWJ)، اعراب، کشیدگی. */
    private fun isWordContinuing(c: Char): Boolean {
        return c.isLetterOrDigit() ||
            PersianNormalizer.isJoiner(c) ||
            PersianNormalizer.isDiacritic(c) ||
            PersianNormalizer.isTatweel(c)
    }

    /**
     * پایان بازه در انتهای بدنه است؟ بازه ضدفرار تا آخرین حرف نگه‌داشته‌شده
     * می‌رود و اعراب/کشیدگیِ چسبیده به انتهای کلمه بیرون بازه می‌مانند
     * («پور.ن هابً») — آن‌ها هم جزو همان کلمه تمام‌شده‌اند.
     */
    private fun endsAtBodyEnd(body: String, spanEnd: Int): Boolean {
        if (spanEnd > body.length) return false
        for (i in spanEnd until body.length) {
            val c = body[i]
            if (!PersianNormalizer.isDiacritic(c) && !PersianNormalizer.isTatweel(c)) return false
        }
        return true
    }
}
