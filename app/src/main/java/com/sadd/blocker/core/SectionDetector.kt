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
 *   و کلیک هم صدا می‌شود، BlockThrottle سرویس جلوی بلاک تکراری
 *   را می‌گیرد (زمان‌بندیِ دوباره همان کلید تا اجرای بلاک رد می‌شود؛
 *   بعد از بلاک واقعی فقط ۱٫۵ ثانیه debounce است)؛ و راستی‌آزمایی دوباره در actOnSection پکیج پیش‌زمینه
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
 * - whatsapp_search (com.whatsapp): مثل تلگرام — فیلد قابل‌ویرایش
 *   با hint «Search» / «جستجو».
 * - whatsapp_status (com.whatsapp): یکی از منابع شامل «status» /
 *   «وضعیت» است و فیلد جستجو باز نیست. بهترین تلاش است و دو ضعف دارد:
 *   برچسب تب وضعیت در ناوبری پایین روی همه صفحه‌ها هست (پس صفحه
 *   اصلی هم ممکن است بلاک شود)؛ و واتساپ در نسخه‌های جدید تب را
 *   «Updates» نامیده است، پس اگر «Status» دیگر جایی نباشد بلاک
 *   نمی‌شود. برای بلاک قطعی کل واتساپ، «خود واتساپ» گزینه مطمئن است.
 * - tiktok_search / tiktok_foryou: هر دو پکیج تیک‌تاک
 *   (com.zhiliaoapp.musically و com.ss.android.ugc.trill)؛ جستجو
 *   مثل بقیه، و For You با مارکر «for you» / «برای شما» و بدون فیلد
 *   جستجوی باز. برچسب تب «For You» در ناوبری هم هست، پس صفحه اصلی
 *   هم ممکن است بلاک شود — بهترین تلاش است.
 * - x_search / x_home (com.twitter.android): جستجو مثل بقیه؛ تایم‌لاین
 *   خانه با مارکر «home» / «خانه» و بدون فیلد جستجوی باز. «Home»
 *   برچسب تب پایین است و روی همه صفحه‌ها دیده می‌شود، پس بیش‌بلاک
 *   محتمل است — بهترین تلاش است.
 * - facebook_search / facebook_reels (com.facebook.katana): جستجو
 *   مثل بقیه؛ ریلز و واچ با مارکر «reels» / «ریلز» / «watch» /
 *   «واچ» و بدون فیلد جستجوی باز — بهترین تلاش است، تب‌ها روی همه
 *   صفحه‌ها برچسب دارند.
 * - snapchat_search / snapchat_spotlight (com.snapchat.android):
 *   جستجو مثل بقیه؛ اسپات‌لایت با مارکر «spotlight» /
 *   «اسپات‌لایت» و بدون فیلد جستجوی باز — بهترین تلاش است.
 * - custom_app («خود اپ» برای هر اپ نصب‌شده): فقط تطابق پکیج؛
 *   مثل بقیه «کل اپ»ها، سیگنالی لازم نیست و بقیه بخش‌های همان اپ
 *   را دربر می‌گیرد.
 * - custom:<signal> (بخش سفارشی کاربر): یکی از منابع (عنوان/کلاس
 *   پنجره، متن‌ها، توضیح‌ها) یا hint فیلد ویرایشی شامل متن نشانه‌ای
 *   است که کاربر تایپ کرده است. نرمال‌سازی فارسی/عربی روی نشانه
 *   اعمال می‌شود. کاملاً بهترین تلاش است: نشانه کوتاه یا عمومی
 *   (مثلاً «خانه») بیش‌بلاک می‌کند و نشانه‌ای که در UI آن اپ نیست
 *   هرگز بلاک نمی‌کند.
 *
 * مرورگرها: دامنه/کلمه در نوار آدرس کروم، فایرفاکس، سامسونگ اینترنت،
 * اج و Brave به یک شکل خوانده می‌شود ([isBrowserPackage])؛ حالت
 * Incognito نیازی به تشخیص جدا ندارد — متن omnibox در تب خصوصی هم
 * از همان مسیر خوانده می‌شود. اینکه مرورگر نوار آدرس را از طریق
 * Accessibility در اختیار بگذارد، تضمین‌شده نیست؛ کروم مسیر
 * تأییدشده است، بقیه بهترین تلاش‌اند.
 */
object SectionDetector {

    const val TELEGRAM_PACKAGE = "org.telegram.messenger"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"
    const val TIKTOK_PACKAGE_ALT = "com.ss.android.ugc.trill"
    const val X_PACKAGE = "com.twitter.android"
    const val FACEBOOK_PACKAGE = "com.facebook.katana"
    const val SNAPCHAT_PACKAGE = "com.snapchat.android"
    const val CHROME_PACKAGE = "com.android.chrome"
    const val FIREFOX_PACKAGE = "org.mozilla.firefox"
    const val SAMSUNG_INTERNET_PACKAGE = "com.sec.android.app.sbrowser"
    const val EDGE_PACKAGE = "com.microsoft.emmx"
    const val BRAVE_PACKAGE = "com.brave.browser"

    /** مرورگرهایی که نوار آدرس‌شان مثل کروم خوانده می‌شود (بهترین تلاش). */
    val BROWSER_PACKAGES: Set<String> = setOf(
        CHROME_PACKAGE,
        FIREFOX_PACKAGE,
        SAMSUNG_INTERNET_PACKAGE,
        EDGE_PACKAGE,
        BRAVE_PACKAGE,
    )

    const val SECTION_TELEGRAM_APP = "telegram_app"
    const val SECTION_TELEGRAM_SEARCH = "telegram_search"
    const val SECTION_INSTAGRAM_APP = "instagram_app"
    const val SECTION_INSTAGRAM_SEARCH = "instagram_search"
    const val SECTION_INSTAGRAM_EXPLORE = "instagram_explore"
    const val SECTION_INSTAGRAM_REELS = "instagram_reels"
    const val SECTION_YOUTUBE_APP = "youtube_app"
    const val SECTION_YOUTUBE_SEARCH = "youtube_search"
    const val SECTION_YOUTUBE_SHORTS = "youtube_shorts"
    const val SECTION_WHATSAPP_APP = "whatsapp_app"
    const val SECTION_WHATSAPP_SEARCH = "whatsapp_search"
    const val SECTION_WHATSAPP_STATUS = "whatsapp_status"
    const val SECTION_TIKTOK_APP = "tiktok_app"
    const val SECTION_TIKTOK_SEARCH = "tiktok_search"
    const val SECTION_TIKTOK_FORYOU = "tiktok_foryou"
    const val SECTION_X_APP = "x_app"
    const val SECTION_X_SEARCH = "x_search"
    const val SECTION_X_HOME = "x_home"
    const val SECTION_FACEBOOK_APP = "facebook_app"
    const val SECTION_FACEBOOK_SEARCH = "facebook_search"
    const val SECTION_FACEBOOK_REELS = "facebook_reels"
    const val SECTION_SNAPCHAT_APP = "snapchat_app"
    const val SECTION_SNAPCHAT_SEARCH = "snapchat_search"
    const val SECTION_SNAPCHAT_SPOTLIGHT = "snapchat_spotlight"
    /** «خود اپ» برای هر اپ نصب‌شده که کاربر دستی اضافه می‌کند. */
    const val SECTION_CUSTOM_APP = "custom_app"
    /** پیشوند کلید بخش سفارشی کاربر: «custom:» + متن نشانه نرمال‌شده. */
    const val CUSTOM_SECTION_PREFIX = "custom:"

    /** کلیدهای «کل اپ» — سرویس وقتی یکی از آن‌ها مچ شد، بقیه بخش‌های همان اپ را رد می‌کند. */
    val WHOLE_APP_SECTIONS: Set<String> = setOf(
        SECTION_TELEGRAM_APP,
        SECTION_INSTAGRAM_APP,
        SECTION_YOUTUBE_APP,
        SECTION_WHATSAPP_APP,
        SECTION_TIKTOK_APP,
        SECTION_X_APP,
        SECTION_FACEBOOK_APP,
        SECTION_SNAPCHAT_APP,
        SECTION_CUSTOM_APP,
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
        SectionPreset(WHATSAPP_PACKAGE, SECTION_WHATSAPP_APP, "خود واتساپ", "واتساپ"),
        SectionPreset(WHATSAPP_PACKAGE, SECTION_WHATSAPP_SEARCH, "جستجوی واتساپ", "واتساپ"),
        SectionPreset(WHATSAPP_PACKAGE, SECTION_WHATSAPP_STATUS, "وضعیت واتساپ", "واتساپ"),
        SectionPreset(TIKTOK_PACKAGE, SECTION_TIKTOK_APP, "خود تیک‌تاک", "تیک‌تاک"),
        SectionPreset(TIKTOK_PACKAGE, SECTION_TIKTOK_SEARCH, "جستجوی تیک‌تاک", "تیک‌تاک"),
        SectionPreset(TIKTOK_PACKAGE, SECTION_TIKTOK_FORYOU, "فید For You تیک‌تاک", "تیک‌تاک"),
        SectionPreset(TIKTOK_PACKAGE_ALT, SECTION_TIKTOK_APP, "خود تیک‌تاک", "تیک‌تاک"),
        SectionPreset(TIKTOK_PACKAGE_ALT, SECTION_TIKTOK_SEARCH, "جستجوی تیک‌تاک", "تیک‌تاک"),
        SectionPreset(TIKTOK_PACKAGE_ALT, SECTION_TIKTOK_FORYOU, "فید For You تیک‌تاک", "تیک‌تاک"),
        SectionPreset(X_PACKAGE, SECTION_X_APP, "خود ایکس", "ایکس"),
        SectionPreset(X_PACKAGE, SECTION_X_SEARCH, "جستجوی ایکس", "ایکس"),
        SectionPreset(X_PACKAGE, SECTION_X_HOME, "تایم‌لاین خانه ایکس", "ایکس"),
        SectionPreset(FACEBOOK_PACKAGE, SECTION_FACEBOOK_APP, "خود فیسبوک", "فیسبوک"),
        SectionPreset(FACEBOOK_PACKAGE, SECTION_FACEBOOK_SEARCH, "جستجوی فیسبوک", "فیسبوک"),
        SectionPreset(FACEBOOK_PACKAGE, SECTION_FACEBOOK_REELS, "ریلز و واچ فیسبوک", "فیسبوک"),
        SectionPreset(SNAPCHAT_PACKAGE, SECTION_SNAPCHAT_APP, "خود اسنپ‌چت", "اسنپ‌چت"),
        SectionPreset(SNAPCHAT_PACKAGE, SECTION_SNAPCHAT_SEARCH, "جستجوی اسنپ‌چت", "اسنپ‌چت"),
        SectionPreset(SNAPCHAT_PACKAGE, SECTION_SNAPCHAT_SPOTLIGHT, "اسپات‌لایت اسنپ‌چت", "اسنپ‌چت"),
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
    private val STATUS_MARKERS = listOf("status", "وضعیت")
    private val FORYOU_MARKERS = listOf("for you", "برای شما")
    private val HOME_MARKERS = listOf("home", "خانه")
    private val FACEBOOK_REELS_MARKERS = listOf("reels", "ریلز", "watch", "واچ")
    private val SPOTLIGHT_MARKERS = listOf("spotlight", "اسپات‌لایت")

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

    /** آیا بخش [sectionKey] یک بخش «کل اپ» است؟ */
    fun isWholeAppSection(sectionKey: String): Boolean {
        return WHOLE_APP_SECTIONS.contains(sectionKey)
    }

    /** آیا [packageName] مرورگری است که نوار آدرسش خوانده می‌شود؟ */
    fun isBrowserPackage(packageName: String): Boolean {
        return BROWSER_PACKAGES.contains(packageName)
    }

    /** آیا [sectionKey] کلید بخش سفارشی کاربر است؟ */
    fun isCustomSectionKey(sectionKey: String): Boolean {
        return sectionKey.startsWith(CUSTOM_SECTION_PREFIX)
    }

    /**
     * کلید ذخیره بخش سفارشی از متن نشانه کاربر؛ نشانه نرمال می‌شود
     * تا «جستجو» با ي/ك عربی هم همان کلید را بسازد. خالی یا کوتاه‌تر
     * از ۲ حرف، null برمی‌گرداند (نشانه تک‌حرفی همه‌جا مچ می‌شود).
     */
    fun customSectionKey(signal: String): String? {
        val normalized = PersianNormalizer.normalize(signal.trim())
        if (normalized.length < 2) return null
        return CUSTOM_SECTION_PREFIX + normalized
    }

    /** متن نشانه از کلید بخش سفارشی؛ اگر کلید سفارشی نباشد null. */
    fun customSignal(sectionKey: String): String? {
        if (!isCustomSectionKey(sectionKey)) return null
        val signal = sectionKey.removePrefix(CUSTOM_SECTION_PREFIX)
        if (signal.isBlank()) return null
        return signal
    }

    /** جستجو: فیلد ویرایشی با hint جستجو، در یکی از پکیج‌های مجاز. */
    private fun isSearchShowing(
        packageName: String,
        signals: SectionSignals,
        vararg packages: String,
    ): Boolean {
        if (!packages.contains(packageName)) return false
        if (!signals.hasEditableTextField) return false
        return containsMarker(signals.editableHint, SEARCH_MARKERS) ||
            containsMarker(signals.windowTitle, SEARCH_MARKERS)
    }

    /** بخش فیدی (وضعیت/فید/خانه/ریلز...): مارکر هست و فیلد جستجو باز نیست. */
    private fun isFeedShowing(
        packageName: String,
        signals: SectionSignals,
        markers: List<String>,
        vararg packages: String,
    ): Boolean {
        if (!packages.contains(packageName)) return false
        if (hasSearchFieldOpen(signals)) return false
        return anySourceContains(signals, markers)
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
            SECTION_WHATSAPP_APP -> {
                if (packageName != WHATSAPP_PACKAGE) return false
                return true
            }
            SECTION_WHATSAPP_SEARCH -> {
                return isSearchShowing(packageName, signals, WHATSAPP_PACKAGE)
            }
            SECTION_WHATSAPP_STATUS -> {
                return isFeedShowing(packageName, signals, STATUS_MARKERS, WHATSAPP_PACKAGE)
            }
            SECTION_TIKTOK_APP -> {
                if (packageName != TIKTOK_PACKAGE && packageName != TIKTOK_PACKAGE_ALT) return false
                return true
            }
            SECTION_TIKTOK_SEARCH -> {
                return isSearchShowing(packageName, signals, TIKTOK_PACKAGE, TIKTOK_PACKAGE_ALT)
            }
            SECTION_TIKTOK_FORYOU -> {
                return isFeedShowing(packageName, signals, FORYOU_MARKERS, TIKTOK_PACKAGE, TIKTOK_PACKAGE_ALT)
            }
            SECTION_X_APP -> {
                if (packageName != X_PACKAGE) return false
                return true
            }
            SECTION_X_SEARCH -> {
                return isSearchShowing(packageName, signals, X_PACKAGE)
            }
            SECTION_X_HOME -> {
                return isFeedShowing(packageName, signals, HOME_MARKERS, X_PACKAGE)
            }
            SECTION_FACEBOOK_APP -> {
                if (packageName != FACEBOOK_PACKAGE) return false
                return true
            }
            SECTION_FACEBOOK_SEARCH -> {
                return isSearchShowing(packageName, signals, FACEBOOK_PACKAGE)
            }
            SECTION_FACEBOOK_REELS -> {
                return isFeedShowing(packageName, signals, FACEBOOK_REELS_MARKERS, FACEBOOK_PACKAGE)
            }
            SECTION_SNAPCHAT_APP -> {
                if (packageName != SNAPCHAT_PACKAGE) return false
                return true
            }
            SECTION_SNAPCHAT_SEARCH -> {
                return isSearchShowing(packageName, signals, SNAPCHAT_PACKAGE)
            }
            SECTION_SNAPCHAT_SPOTLIGHT -> {
                return isFeedShowing(packageName, signals, SPOTLIGHT_MARKERS, SNAPCHAT_PACKAGE)
            }
            SECTION_CUSTOM_APP -> {
                // «خود اپ» دستی: پکیج از سطر قانون می‌آید و سرویس پیش‌تر
                // قانون را با همین packageName فیلتر کرده است؛ سیگنالی لازم نیست.
                return true
            }
            else -> {
                if (isCustomSectionKey(sectionKey)) {
                    val signal = customSignal(sectionKey) ?: return false
                    return anySourceContains(signals, listOf(signal)) ||
                        containsMarker(signals.editableHint, listOf(signal))
                }
                return false
            }
        }
    }

    /** آیا [packageName] یکی از اپ‌های دارای پیش‌فرض بخش است؟ */
    fun hasPresets(packageName: String): Boolean {
        return PRESETS.any { it.packageName == packageName }
    }
}
