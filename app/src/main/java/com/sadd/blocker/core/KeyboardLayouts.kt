package com.sadd.blocker.core

/**
 * چیدمان کلیدها و تم‌های رنگی «کیبورد هوشمند» سد — منطق خالص، بدون
 * وابستگی اندروید (رنگ‌ها Int با قالب ARGB‌اند تا هم سرویس IME و هم
 * صفحه تنظیمات از یک منبع بخوانند و تست واحد JVM داشته باشند).
 *
 * ترتیب حروف فارسی دقیقاً چیدمان استاندارد ایرانی است و ردیف‌ها در
 * همان ترتیبی که روی کیبورد واقعی خوانده می‌شوند نوشته شده‌اند
 * (اولین کلید هر ردیف در سمت راست است؛ سرویس IME ردیف فارسی را
 * راست‌به‌چپ رندر می‌کند). گزارش سید ۲۰۲۶-۱۱-۲۱: «ترتیب حروف فارسی
 * درست نیست» — این فایل منبع واحد ترتیب است و تستش نمی‌گذارد بی‌سروصدا
 * به هم بریزد.
 */
object KeyboardLayouts {

    /** ردیف‌های حروف فارسی (استاندارد ایرانی)؛ کلید بک‌اسپیس را سرویس به ردیف سوم اضافه می‌کند. */
    val PERSIAN_ROWS: List<List<String>> = listOf(
        listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ"),
        listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
        listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و"),
    )

    /** ردیف‌های حروف انگلیسی (QWERTY)؛ شیفت و بک‌اسپیس را سرویس به ردیف سوم اضافه می‌کند. */
    val ENGLISH_ROWS: List<List<String>> = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    )

    /** ردیف‌های صفحه اعداد و نمادها؛ ردیف پایانی (اب/فاصله/اینتر) را سرویس می‌سازد. */
    val SYMBOL_ROWS: List<List<String>> = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
        listOf("-", "_", "=", "+", "[", "]", "{", "}", ":", ";"),
        listOf("آ", "ئ", "ء", "ـ", "؟", "،", "؛", "«", "»", "'", "\"", "/", "\\"),
    )
}

/**
 * یک تم رنگی کیبورد. همه رنگ‌ها ARGB کامل‌اند (آلفا FF) و کنتراست
 * متن روی کلید خوانا نگه داشته شده است.
 */
data class KeyboardTheme(
    val key: String,
    val labelFa: String,
    /** پس‌زمینه کل کیبورد. */
    val background: Int,
    /** پس‌زمینه هر کلید. */
    val keyBackground: Int,
    /** رنگ متن کلیدها. */
    val keyText: Int,
    /** پس‌زمینه نوار ابزار بالای کیبورد. */
    val toolbarBackground: Int,
    /** رنگ متن/آیکون نوار ابزار. */
    val toolbarText: Int,
)

/** تم‌های رنگی کیبورد هوشمند؛ انتخاب کاربر با [KeyboardTheme.key] در DataStore ذخیره می‌شود. */
object KeyboardThemes {

    val ALL: List<KeyboardTheme> = listOf(
        KeyboardTheme(
            key = "sadd",
            labelFa = "سد",
            background = 0xFF14142B.toInt(),
            keyBackground = 0xFF2A2A4A.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            toolbarBackground = 0xFF1E1E3A.toInt(),
            toolbarText = 0xFFFFFFFF.toInt(),
        ),
        KeyboardTheme(
            key = "dark",
            labelFa = "تیره",
            background = 0xFF111111.toInt(),
            keyBackground = 0xFF2B2B2B.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            toolbarBackground = 0xFF1C1C1C.toInt(),
            toolbarText = 0xFFFFFFFF.toInt(),
        ),
        KeyboardTheme(
            key = "light",
            labelFa = "روشن",
            background = 0xFFF2F2F5.toInt(),
            keyBackground = 0xFFFFFFFF.toInt(),
            keyText = 0xFF1A1A2E.toInt(),
            toolbarBackground = 0xFFE8E8EE.toInt(),
            toolbarText = 0xFF1A1A2E.toInt(),
        ),
        KeyboardTheme(
            key = "navy",
            labelFa = "سرمه‌ای",
            background = 0xFF0B1F4B.toInt(),
            keyBackground = 0xFF1D3A7A.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            toolbarBackground = 0xFF12295E.toInt(),
            toolbarText = 0xFFFFFFFF.toInt(),
        ),
        KeyboardTheme(
            key = "mint",
            labelFa = "نعنایی",
            background = 0xFF0E2E26.toInt(),
            keyBackground = 0xFF1E5C4B.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            toolbarBackground = 0xFF14453A.toInt(),
            toolbarText = 0xFFFFFFFF.toInt(),
        ),
    )

    /** تم پیش‌فرض (همان رنگ فعلی کیبورد سد). */
    val DEFAULT: KeyboardTheme = ALL[0]

    /** تم از روی کلید ذخیره‌شده؛ کلید ناشناخته/خالی یعنی پیش‌فرض. */
    fun themeByKey(key: String?): KeyboardTheme {
        if (key.isNullOrBlank()) return DEFAULT
        return ALL.firstOrNull { it.key == key } ?: DEFAULT
    }

    /** تم بعدی در چرخه (دکمه رنگ نوار ابزار)؛ بعد از آخری به اولی برمی‌گردد. */
    fun next(current: KeyboardTheme): KeyboardTheme {
        val index = ALL.indexOfFirst { it.key == current.key }
        if (index < 0) return DEFAULT
        return ALL[(index + 1) % ALL.size]
    }
}
