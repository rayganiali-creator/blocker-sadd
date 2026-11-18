package com.sadd.blocker.core

/**
 * تشخیص «بخش» درون اپ‌ها — خالص Kotlin، بدون وابستگی اندروید.
 *
 * ورودی فقط داده‌های ساده است (عنوان پنجره، متن گره‌ها، توضیح محتوایی،
 * وجود فیلد متنی قابل‌ویرایش و hint آن)؛ سرویس Accessibility آن‌ها را از
 * درخت Accessibility جمع می‌کند و اینجا تصمیم خالص گرفته می‌شود تا
 * قابل تست واحد JVM باشد.
 *
 * اصل محافظه‌کارانه: آیکون «جستجو» به‌تنهایی کافی نیست — صفحه اصلی
 * تلگرام/یوتیوب هم آیکون جستجو دارد. بخش «جستجو» فقط وقتی تشخیص داده
 * می‌شود که یک فیلد متنی قابل‌ویرایش با hint جستجو واقعاً روی صفحه باشد.
 * این یعنی باز کردن صفحه اصلی اپ هرگز بلاک نمی‌شود؛ فقط ورود به
 * جستجو/اکسپلور بلاک می‌شود.
 *
 * هیوریستیک هر پیش‌فرض (مستند، چون وابسته به UI نسخه فعلی اپ‌هاست و
 * ممکن است با آپدیت آن اپ‌ها عوض شود):
 *
 * - telegram_app / instagram_app / youtube_app («کل اپ»): فقط تطابق
 *   پکیج. سرویس این را روی TYPE_WINDOW_STATE_CHANGED می‌بیند، یعنی
 *   وقتی اپ به پیش‌زمینه می‌آید. سیگنالی لازم نیست: خودِ باز شدن اپ
 *   یعنی بخش «کل اپ» روی صفحه است. چون evaluateWindow روی تغییر محتوا
 *   و کلیک هم صدا می‌شود، ضد حلقه ۱۵ ثانیه‌ای سرویس جلوی بلاک تکراری
 *   را می‌گیرد؛ و راستی‌آزمایی دوباره در actOnSection پکیج پیش‌زمینه
 *   را چک می‌کند تا اگر کاربر خودش از اپ بیرون رفت، بلاک دیرکرده
 *   روی صفحه دیگری BACK نزند.
 * - telegram_search (org.telegram.messenger): فیلد قابل‌ویرایش هست و hint
 *   آن یا عنوان پنجره شامل «search» / «جستجو» است. در تلگرام، صفحه
 *   چت‌ها فقط آیکون جستجو دارد؛ با زدن آن، فیلدی با hint «Search» باز
 *   می‌شود. پس ترکیب «فیلد ویرایشی + hint جستجو» یعنی کاربر وارد
 *   جستجوی سراسری تلگرام شده است.
 * - instagram_search (com.instagram.android): مثل تلگرام — فیلد
 *   قابل‌ویرایش با hint «Search» / «جستجو». تب جستجوی اینستاگرام یک
 *   نوار جستجو با placeholder «Search» دارد.
 * - instagram_explore (com.instagram.android): فیلد قابل‌ویرایش نیست و
 *   عنوان/کلاس پنجره یا یکی از متن‌ها/توضیح‌ها شامل «explore» /
 *   «اکسپلور» است. تب اکسپلور یک گرید رسانه است بدون فیلد جستجو؛ تب آن
 *   content-description «Search and explore» دارد. نبودِ فیلد ویرایشی،
 *   اکسپلور را از صفحه جستجو جدا می‌کند. بهترین تلاش است: اگر
 *   اینستاگرام گرید دیگری را «Explore» بنامد، آن هم بلاک می‌شود.
 * - instagram_reels (com.instagram.android): یکی از عنوان/کلاس پنجره،
 *   متن‌ها یا توضیح‌های محتوایی شامل «reels» / «ریلز» است، و فیلد
 *   جستجو باز نیست (فیلد ویرایشی با hint جستجو یعنی صفحه جستجو است،
 *   نه ریلز؛ فیلد کامنت ریلز hint جستجو ندارد و بلاک را لغو نمی‌کند).
 *   بهترین تلاش است و دو ضعف شناخته‌شده دارد: برچسب تب «Reels» در
 *   ناوبری پایین روی همه صفحه‌های اینستاگرام هست، پس صفحه اصلی هم
 *   ممکن است بلاک شود؛ و اگر اینستاگرام پخش‌کننده ریلز را دیگر
 *   «Reels» ننامد، بلاک نمی‌شود. برای بلاک قطعی کل اینستاگرام،
 *   «خود اینستاگرام» گزینه مطمئن است.
 * - youtube_search (com.google.android.youtube): فیلد قابل‌ویرایش هست و
 *   hint آن یا عنوان پنجره شامل «search» / «جستجو» است (hint یوتیوب
 *   «Search YouTube» است). صفحه اصلی یوتیوب فقط آیکون جستجو دارد.
 * - youtube_shorts (com.google.android.youtube): مثل ریلز — یکی از
 *   عنوان/کلاس پنجره، متن‌ها یا توضیح‌های محتوایی شامل «shorts» /
 *   «شورتس» است و فیلد جستجو باز نیست. بهترین تلاش است: قفسه «Shorts»
 *   در صفحه اصلی و تب پایین «Shorts» هم همین برچسب را دارند، پس
 *   صفحه اصلی یوتیوب هم ممکن است بلاک شود؛ برای بلاک قطعی کل
 *   یوتیوب، «خود یوتیوب» گزینه مطمئن است.
 */
object SectionDetector {

    const val TELEGRAM_PACKAGE = "org.telegram.messenger"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val CHROME_PACKAGE = "com.android.chrome"

    const val SECTION_TELEGRAM_APP = "telegram_app"
    const val SECTION_TELEGRAM_SEARCH = "telegram_search"
    const val SECTION_INSTAGRAM_APP = "instagram_app"
    const val SECTION_INSTAGRAM_SEARCH = "instagram_search"
    const val SECTION_INSTAGRAM_EXPLORE = "instagram_explore"
    const val SECTION_INSTAGRAM_REELS = "instagram_reels"
    const val SECTION_YOUTUBE_APP = "youtube_app"
    const val SECTION_YOUTUBE_SEARCH = "youtube_search"
    const val SECTION_YOUTUBE_SHORTS = "youtube_shorts"

    /** کلیدهای «کل اپ» — سرویس وقتی یکی از آن‌ها مچ شد، بقیه بخش‌های همان اپ را رد می‌کند. */
    val WHOLE_APP_SECTIONS: Set<String> = setOf(
        SECTION_TELEGRAM_APP,
        SECTION_INSTAGRAM_APP,
        SECTION_YOUTUBE_APP,
    )

    /** یک بخش قابل‌بلاک در یک اپ؛ پیش‌فرض‌های سیستمی از اینجا seed می‌شوند. */
    data class SectionPreset(
        val packageName: String,
        val sectionKey: String,
        val labelFa: String,
        val appLabelFa: String,
    )

    /**
     * ترتیب لیست همان ترتیب نمایش در صفحه بخش‌هاست: در هر اپ اول
     * «خود اپ»، بعد جستجو، بعد بقیه بخش‌ها.
     */
    val PRESETS: List<SectionPreset> = listOf(
        SectionPreset(TELEGRAM_PACKAGE, SECTION_TELEGRAM_APP, "خود تلگرام", "تلگرام"),
        SectionPreset(TELEGRAM_PACKAGE, SECTION_TELEGRAM_SEARCH, "جستجوی تلگرام", "تلگرام"),
        SectionPreset(INSTAGRAM_PACKAGE, SECTION_INSTAGRAM_APP, "خود اینستاگرام", "اینستاگرام"),
        SectionPreset(INSTAGRAM_PACKAGE, SECTION_INSTAGRAM_SEARCH, "جستجوی اینستاگرام", "اینستاگرام"),
        SectionPreset(INSTAGRAM_PACKAGE, SECTION_INSTAGRAM_EXPLORE, "اکسپلور اینستاگرام", "اینستاگرام"),
        SectionPreset(INSTAGRAM_PACKAGE, SECTION_INSTAGRAM_REELS, "ریلز اینستاگرام", "اینستاگرام"),
        SectionPreset(YOUTUBE_PACKAGE, SECTION_YOUTUBE_APP, "خود یوتیوب", "یوتیوب"),
        SectionPreset(YOUTUBE_PACKAGE, SECTION_YOUTUBE_SEARCH, "جستجوی یوتیوب", "یوتیوب"),
        SectionPreset(YOUTUBE_PACKAGE, SECTION_YOUTUBE_SHORTS, "شورتس یوتیوب", "یوتیوب"),
    )

    /** سیگنال‌های خام جمع‌شده از درخت Accessibility یک پنجره. */
    data class SectionSignals(
        val windowTitle: String? = null,
        val windowClassName: String? = null,
        val texts: List<String> = emptyList(),
        val contentDescriptions: List<String> = emptyList(),
        val hasEditableTextField: Boolean = false,
        val editableHint: String? = null,
        /** متن فیلدهای قابل‌ویرایش (برای کروم: متن omnibox / جعبه جستجو). */
        val editableTexts: List<String> = emptyList(),
    )

    private val SEARCH_MARKERS = listOf("search", "جستجو")
    private val EXPLORE_MARKERS = listOf("explore", "اکسپلور")
    private val REELS_MARKERS = listOf("reels", "ریلز")
    private val SHORTS_MARKERS = listOf("shorts", "شورتس")

    private fun normalized(value: String?): String {
        if (value == null) return ""
        return PersianNormalizer.normalize(value)
    }

    private fun containsMarker(value: String?, markers: List<String>): Boolean {
        val text = normalized(value)
        if (text.isEmpty()) return false
        for (marker in markers) {
            if (text.contains(marker)) return true
        }
        return false
    }

    private fun anyContains(values: List<String>, markers: List<String>): Boolean {
        for (value in values) {
            if (containsMarker(value, markers)) return true
        }
        return false
    }

    /** آیا فیلد جستجو باز است؟ فیلد ویرایشی با hint جستجو (یا عنوان جستجو). */
    private fun hasSearchFieldOpen(signals: SectionSignals): Boolean {
        if (!signals.hasEditableTextField) return false
        return containsMarker(signals.editableHint, SEARCH_MARKERS) ||
            containsMarker(signals.windowTitle, SEARCH_MARKERS)
    }

    /** آیا یکی از منابع (عنوان/کلاس پنجره، متن‌ها، توضیح‌ها) مارکر را دارد؟ */
    private fun anySourceContains(signals: SectionSignals, markers: List<String>): Boolean {
        return containsMarker(signals.windowTitle, markers) ||
            containsMarker(signals.windowClassName, markers) ||
            anyContains(signals.texts, markers) ||
            anyContains(signals.contentDescriptions, markers)
    }

    /** آیا [sectionKey] یک بخش «کل اپ» است؟ */
    fun isWholeAppSection(sectionKey: String): Boolean {
        return WHOLE_APP_SECTIONS.contains(sectionKey)
    }

    /** آیا بخش [sectionKey] از اپ [packageName] الان روی صفحه است؟ */
    fun isSectionShowing(
        packageName: String,
        sectionKey: String,
        signals: SectionSignals,
    ): Boolean {
        when (sectionKey) {
            SECTION_TELEGRAM_APP -> {
                if (packageName != TELEGRAM_PACKAGE) return false
                // کل اپ: خودِ پیش‌زمینه بودن اپ کافی است؛ سیگنالی لازم نیست.
                return true
            }
            SECTION_TELEGRAM_SEARCH -> {
                if (packageName != TELEGRAM_PACKAGE) return false
                if (!signals.hasEditableTextField) return false
                return containsMarker(signals.editableHint, SEARCH_MARKERS) ||
                    containsMarker(signals.windowTitle, SEARCH_MARKERS)
            }
            SECTION_INSTAGRAM_APP -> {
                if (packageName != INSTAGRAM_PACKAGE) return false
                return true
            }
            SECTION_INSTAGRAM_SEARCH -> {
                if (packageName != INSTAGRAM_PACKAGE) return false
                if (!signals.hasEditableTextField) return false
                return containsMarker(signals.editableHint, SEARCH_MARKERS) ||
                    containsMarker(signals.windowTitle, SEARCH_MARKERS)
            }
            SECTION_INSTAGRAM_EXPLORE -> {
                if (packageName != INSTAGRAM_PACKAGE) return false
                // صفحه جستجو فیلد ویرایشی دارد؛ اکسپلور ندارد.
                if (signals.hasEditableTextField) return false
                return containsMarker(signals.windowTitle, EXPLORE_MARKERS) ||
                    containsMarker(signals.windowClassName, EXPLORE_MARKERS) ||
                    anyContains(signals.texts, EXPLORE_MARKERS) ||
                    anyContains(signals.contentDescriptions, EXPLORE_MARKERS)
            }
            SECTION_INSTAGRAM_REELS -> {
                if (packageName != INSTAGRAM_PACKAGE) return false
                // فیلد جستجوی باز یعنی صفحه جستجو است، نه ریلز.
                if (hasSearchFieldOpen(signals)) return false
                return anySourceContains(signals, REELS_MARKERS)
            }
            SECTION_YOUTUBE_APP -> {
                if (packageName != YOUTUBE_PACKAGE) return false
                return true
            }
            SECTION_YOUTUBE_SEARCH -> {
                if (packageName != YOUTUBE_PACKAGE) return false
                if (!signals.hasEditableTextField) return false
                return containsMarker(signals.editableHint, SEARCH_MARKERS) ||
                    containsMarker(signals.windowTitle, SEARCH_MARKERS)
            }
            SECTION_YOUTUBE_SHORTS -> {
                if (packageName != YOUTUBE_PACKAGE) return false
                // فیلد جستجوی باز یعنی صفحه جستجو است، نه شورتس.
                if (hasSearchFieldOpen(signals)) return false
                return anySourceContains(signals, SHORTS_MARKERS)
            }
            else -> return false
        }
    }

    /** آیا [packageName] یکی از اپ‌های دارای پیش‌فرض بخش است؟ */
    fun hasPresets(packageName: String): Boolean {
        return PRESETS.any { it.packageName == packageName }
    }
}
