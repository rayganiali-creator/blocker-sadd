package com.sadd.blocker.core

/**
 * تشخیص «بخش» درون اپ‌ها — خالص Kotlin، بدون وابستگی اندروید.
 *
 * ورودی فقط داده‌های ساده است (عنوان پنجره، متن گره‌ها، توضیح محتوایی،
 * وجود فیلد متنی قابل‌ویرایش و hint آن، فوکوس/متن فیلد جستجو و
 * متن/توضیح گره‌های انتخاب‌شده)؛ سرویس Accessibility آن‌ها را از
 * درخت Accessibility جمع می‌کند و اینجا تصمیم خالص گرفته می‌شود تا
 * قابل تست واحد JVM باشد.
 *
 * اصل محافظه‌کارانه (به‌روزرسانی ۲۰۲۶-۱۱-۲۰، گزارش سید): بخش فقط با
 * شاهد تعامل واقعی بلاک می‌شود، نه با صرفِ دیده شدن عنصر در درخت UI.
 * اسکرول‌کردن در تلگرام که نوار جستجو از جلوی چشم رد می‌شود، «جستجو»
 * نیست؛ جستجو فقط وقتی بلاک می‌شود که کاربر روی نوار/فیلد جستجو زده
 * باشد (TYPE_VIEW_CLICKED)، فیلد فوکوس گرفته باشد
 * (TYPE_VIEW_FOCUSED / فوکوس دسترس‌پذیری) یا در آن تایپ کند
 * (TYPE_VIEW_TEXT_CHANGED). فیلدی که فقط متن نگه داشته (مثلاً متن
 * قبلی یا hint که به‌صورت متن گزارش می‌شود) شاهد نیست. فید هم فقط با
 * تب واقعاً انتخاب‌شده (selected/checked) بلاک می‌شود؛ عنوان/کلاس
 * پنجره دیگر شاهد فید نیست. اگر اپ وضعیت انتخاب را در اختیار نگذارد،
 * بخش فید fail-open می‌ماند (بلاک نمی‌شود) — محدودیت صادقانه،
 * نه بیش‌بلاک. مرور خصوصی کروم هم فقط با کلیک روی آیتم «New Incognito
 * tab» یا با خودِ صفحه تب خصوصی (متن خالیِ «You've gone incognito»)
 * بلاک می‌شود؛ کلمه «incognito» در منو به‌تنهایی هرگز بلاک نمی‌کند.
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
 * - telegram_search (org.telegram.messenger): فیلد ویرایشی با hint
 *   «search» / «جستجو» که واقعاً فوکوس دارد (کاربر رویش زده) یا متن
 *   تایپ‌شده دارد. صرفِ دیده شدن فیلد جستجو در بالای صفحه (بدون فوکوس
 *   و بدون متن) بلاک نمی‌شود — این همان باگی بود که سید گزارش کرد:
 *   باز کردن تلگرام فقط چون نوار جستجو آنجاست بلاک می‌شد.
 * - instagram_search (com.instagram.android): مثل تلگرام — فیلد جستجو
 *   با hint «Search» / «جستجو» واقعاً فعال است (فوکوس یا متن
 *   تایپ‌شده). تب جستجوی اینستاگرام یک نوار جستجو با placeholder
 *   «Search» دارد. کلیک روی خودِ تب «Search and explore» هرگز شاهد
 *   جستجو نیست (گزارش سید ۲۰۲۶-۱۱-۲۱: زدن روی اکسپلور، با اینکه
 *   قانون اکسپلور خاموش و فقط جستجو روشن بود، بلاک می‌شد — چون متن
 *   تب «Search» داشت و کلیک روی آن «کلیک روی جستجو» حساب می‌شد).
 *   شاهد کلیک فقط وقتی است که گره کلیک‌شده (یا فرزند مستقیمش) خودِ
 *   فیلد ویرایشی جستجو باشد ([isSearchFieldClick])؛ برچسب تب هرگز.
 * - instagram_explore (com.instagram.android): مارکر «explore» /
 *   «اکسپلور» فقط در متن/توضیح گره‌ای که واقعاً
 *   انتخاب شده (تب فعال اکسپلور)؛ و فیلد جستجو فعال نیست. برچسب تب
 *   «Search and explore» که روی همه صفحه‌ها هست به‌تنهایی کافی نیست.
 *   بهترین تلاش است و fail-open: اگر اینستاگرام وضعیت انتخاب تب را
 *   در اختیار نگذارد، اکسپلور بلاک نمی‌شود.
 * - instagram_reels (com.instagram.android): مارکر «reels» / «ریلز» در
 *   فقط در گره انتخاب‌شده، و فیلد جستجو فعال نیست
 *   (فیلد کامنت ریلز hint جستجو ندارد و بلاک را لغو نمی‌کند). برچسب
 *   تب «Reels» در ناوبری پایین روی همه صفحه‌ها هست و به‌تنهایی بلاک
 *   نمی‌کند — صفحه اصلی دیگر بیش‌بلاک نمی‌شود. بهترین تلاش است و
 *   fail-open: اگر اینستاگرام تب فعال را selected گزارش نکند، ریلز
 *   بلاک نمی‌شود؛ و اگر پخش‌کننده ریلز دیگر «Reels» نامیده نشود هم
 *   بلاک نمی‌شود. برای بلاک قطعی کل اینستاگرام، «خود اینستاگرام»
 *   گزینه مطمئن است.
 * - youtube_search (com.google.android.youtube): مثل تلگرام — فیلد
 *   جستجو واقعاً فعال است (hint «Search YouTube» و فوکوس یا متن
 *   تایپ‌شده). صفحه اصلی یوتیوب فقط آیکون جستجو دارد.
 * - youtube_shorts (com.google.android.youtube): مثل ریلز — مارکر
 *   «shorts» / «شورتس» فقط در گره انتخاب‌شده، و فیلد
 *   جستجو فعال نیست. قفسه «Shorts» در صفحه اصلی و تب پایین «Shorts»
 *   به‌تنهایی بلاک نمی‌کنند؛ اگر یوتیوب تب فعال را selected گزارش
 *   نکند، شورتس fail-open می‌ماند. برای بلاک قطعی کل یوتیوب،
 *   «خود یوتیوب» گزینه مطمئن است.
 * - whatsapp_search (com.whatsapp): مثل تلگرام — فیلد جستجو واقعاً
 *   فعال است (فوکوس یا متن تایپ‌شده).
 * - whatsapp_status (com.whatsapp): مارکر «status» / «وضعیت» /
 *   «updates» / «به‌روزرسانی» در فقط در گره انتخاب‌شده، و فیلد
 *   جستجو فعال نیست.
 *   برچسب تب وضعیت در ناوبری پایین به‌تنهایی بلاک نمی‌کند. بهترین
 *   تلاش است و fail-open: واتساپ در نسخه‌های جدید تب را «Updates»
 *   نامیده است (وضعیت داخل همان تب است)، پس «updates» هم مارکر
 *   است و قانون وضعیت روی تب Updates بلاک می‌کند؛ اگر وضعیت انتخاب
 *   تب گزارش نشود بلاک نمی‌شود. برای بلاک قطعی کل
 *   واتساپ، «خود واتساپ» گزینه مطمئن است.
 * - tiktok_search / tiktok_foryou: هر دو پکیج تیک‌تاک
 *   (com.zhiliaoapp.musically و com.ss.android.ugc.trill)؛ جستجو
 *   مثل بقیه (فیلد واقعاً فعال)، و For You با مارکر «for you» /
 *   «برای شما» فقط در گره انتخاب‌شده و بدون فیلد
 *   جستجوی فعال. برچسب تب «For You» در ناوبری به‌تنهایی بلاک
 *   نمی‌کند — بهترین تلاش و fail-open است.
 * - x_search / x_home (com.twitter.android): جستجو مثل بقیه؛ تایم‌لاین
 *   خانه با مارکر «home» / «خانه» فقط در گره
 *   انتخاب‌شده و بدون فیلد جستجوی فعال. «Home» برچسب تب پایین است
 *   و به‌تنهایی بلاک نمی‌کند — بهترین تلاش و fail-open است.
 * - facebook_search / facebook_reels (com.facebook.katana): جستجو
 *   مثل بقیه؛ ریلز و واچ با مارکر «reels» / «ریلز» / «watch» /
 *   «واچ» فقط در گره انتخاب‌شده و بدون فیلد جستجوی
 *   فعال — بهترین تلاش و fail-open است، تب‌ها به‌تنهایی بلاک نمی‌کنند.
 * - snapchat_search / snapchat_spotlight (com.snapchat.android):
 *   جستجو مثل بقیه؛ اسپات‌لایت با مارکر «spotlight» /
 *   «اسپات‌لایت» فقط در گره انتخاب‌شده و بدون فیلد
 *   جستجوی فعال — بهترین تلاش و fail-open است.
 * - chrome_app (com.android.chrome): فقط تطابق پکیج؛ مثل بقیه
 *   «کل اپ»ها، خودِ باز شدن کروم یعنی بخش روی صفحه است و بقیه
 *   بخش‌های کروم را دربر می‌گیرد.
 * - chrome_incognito (com.android.chrome): فقط دو شاهد — کلیک روی
 *   آیتم منوی «New Incognito tab» / «زبانه ناشناس جدید» (متن گره
 *   کلیک‌شده)، یا خودِ صفحه تب خصوصی (متن «You've gone incognito» و
 *   معادل‌های فارسی‌اش روی صفحه). کلمه «incognito»/«ناشناس» که فقط
 *   در منو یا فهرستی دیده شود هرگز بلاک نمی‌کند (گزارش سید
 *   ۲۰۲۶-۱۱-۲۰). بهترین تلاش است: کروم موظف نیست حالت خصوصی را از
 *   طریق Accessibility نشان دهد. بلاک دامنه/کلمه در تب خصوصی به این
 *   قانون وابسته نیست — متن omnibox در تب خصوصی هم از همان مسیر
 *   خوانده می‌شود.
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
 * اج و Brave به یک شکل خوانده می‌شود ([isBrowserPackage])؛ بلاک
 * دامنه/کلمه در تب خصوصی به تشخیص Incognito وابسته نیست — متن omnibox
 * در تب خصوصی هم از همان مسیر خوانده می‌شود. قانون‌های بخش کروم
 * (خود کروم، Incognito) جداگانه از مسیر بخش ارزیابی می‌شوند. اینکه
 * مرورگر نوار آدرس را از طریق Accessibility در اختیار بگذارد،
 * تضمین‌شده نیست؛ کروم مسیر تأییدشده است، بقیه بهترین تلاش‌اند.
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
    const val SECTION_CHROME_APP = "chrome_app"
    /** مرور خصوصی کروم — بهترین تلاش، وابسته به آنچه کروم نشان می‌دهد. */
    const val SECTION_CHROME_INCOGNITO = "chrome_incognito"
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
        SECTION_CHROME_APP,
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
        SectionPreset(CHROME_PACKAGE, SECTION_CHROME_APP, "خود کروم", "کروم"),
        SectionPreset(CHROME_PACKAGE, SECTION_CHROME_INCOGNITO, "مرور خصوصی (Incognito) کروم (بهترین تلاش)", "کروم"),
    )

    /**
     * سیگنال‌های خام جمع‌شده از درخت Accessibility یک پنجره.
     *
     * شواهد فعال‌سازی (برای دقت بخش، نه صرفِ دیده شدن):
     * - [searchFieldClicked]: سرویس این پرچم را فقط از رویداد واقعی
     *   TYPE_VIEW_CLICKED پر می‌کند، وقتی گره کلیک‌شده (یا فرزند
     *   مستقیمش) خودِ فیلد ویرایشی جستجو است ([isSearchFieldClick]).
     *   کلیک روی تبی که برچسبش «Search» دارد (مثل تب «Search and
     *   explore» اینستاگرام) هرگز این پرچم را نمی‌گیرد — آن کلیک
     *   ورود به اکسپلور است، نه جستجو (گزارش سید ۲۰۲۶-۱۱-۲۱).
     * - [searchFieldFocused]: فیلد ویرایشی با hint جستجو فوکوس دارد
     *   (isFocused یا isAccessibilityFocused) — کاربر رویش زده است.
     * - [searchFieldHasText]: فقط از مسیر رویداد تایپ
     *   (TYPE_VIEW_TEXT_CHANGED) پر می‌شود — یعنی کاربر همین حالا دارد
     *   در فیلد جستجو تایپ می‌کند. فیلدی که فقط متن نگه داشته (متن
     *   قبلی یا hint گزارش‌شده به‌صورت متن) این پرچم را نمی‌گیرد؛
     *   اسکرول‌کردن از کنار نوار جستجو هرگز جستجو نیست.
     * - [selectedTexts] / [selectedContentDescriptions]: متن/توضیح
     *   گره‌هایی که واقعاً انتخاب/چک شده‌اند (isSelected یا isChecked)
     *   — تب فعال. برچسب تبی که فقط در درخت هست ولی انتخاب نشده،
     *   اینجا نمی‌آید. این تنها شاهد بخش‌های فیدی است.
     * - [clickedTexts] / [clickedContentDescriptions]: متن/توضیح گره
     *   منبع رویداد TYPE_VIEW_CLICKED (و فرزندان مستقیمش) — برای
     *   تشخیص کلیک روی آیتم منوی «New Incognito tab» کروم. صرفِ بودن
     *   این متن‌ها در درخت، بدون کلیک، شاهد نیست.
     */
    data class SectionSignals(
        val windowTitle: String? = null,
        val windowClassName: String? = null,
        val texts: List<String> = emptyList(),
        val contentDescriptions: List<String> = emptyList(),
        val hasEditableTextField: Boolean = false,
        val editableHint: String? = null,
        /** متن فیلدهای قابل‌ویرایش (برای کروم: متن omnibox / جعبه جستجو). */
        val editableTexts: List<String> = emptyList(),
        /** کاربر روی نوار/فیلد جستجو کلیک کرده است (رویداد TYPE_VIEW_CLICKED). */
        val searchFieldClicked: Boolean = false,
        /** فیلد جستجو واقعاً فوکوس دارد (کاربر رویش زده است). */
        val searchFieldFocused: Boolean = false,
        /** کاربر همین حالا در فیلد جستجو تایپ می‌کند (رویداد TYPE_VIEW_TEXT_CHANGED). */
        val searchFieldHasText: Boolean = false,
        /** متن گره‌های واقعاً انتخاب/چک‌شده (تب فعال). */
        val selectedTexts: List<String> = emptyList(),
        /** توضیح محتوایی گره‌های واقعاً انتخاب/چک‌شده (تب فعال). */
        val selectedContentDescriptions: List<String> = emptyList(),
        /** متن گره کلیک‌شده در رویداد TYPE_VIEW_CLICKED. */
        val clickedTexts: List<String> = emptyList(),
        /** توضیح محتوایی گره کلیک‌شده در رویداد TYPE_VIEW_CLICKED. */
        val clickedContentDescriptions: List<String> = emptyList(),
    )

    private val SEARCH_MARKERS = listOf("search", "جستجو")
    private val EXPLORE_MARKERS = listOf("explore", "اکسپلور")
    private val REELS_MARKERS = listOf("reels", "ریلز")
    private val SHORTS_MARKERS = listOf("shorts", "شورتس")
    private val STATUS_MARKERS = listOf("status", "وضعیت", "updates", "به‌روزرسانی")
    private val FORYOU_MARKERS = listOf("for you", "برای شما")
    private val HOME_MARKERS = listOf("home", "خانه")
    private val FACEBOOK_REELS_MARKERS = listOf("reels", "ریلز", "watch", "واچ")
    private val SPOTLIGHT_MARKERS = listOf("spotlight", "اسپات‌لایت")
    /**
     * مارکرهای کلیک روی آیتم منوی مرور خصوصی کروم — فقط روی متن گره
     * کلیک‌شده (TYPE_VIEW_CLICKED) سنجیده می‌شوند، نه روی کل درخت:
     * «new incognito tab» همان آیتم منوی کروم انگلیسی است؛ «زبانه
     * ناشناس جدید» / «برگه ناشناس جدید» ترجمه‌های فارسی همان آیتم‌اند؛
     * «مرور خصوصی» هم ممکن است برچسب همان آیتم در UI فارسی باشد.
     * صرفِ دیده شدن این‌ها در منو (بدون کلیک) هرگز بلاک نمی‌کند.
     */
    private val INCOGNITO_CLICK_MARKERS = listOf(
        "new incognito tab",
        "incognito tab",
        "زبانه ناشناس جدید",
        "برگه ناشناس جدید",
        "مرور خصوصی",
    )

    /**
     * مارکرهای خودِ صفحه تب خصوصی کروم (صفحه خالی تب تازه خصوصی):
     * «you've gone incognito» تیتر همان صفحه در کروم انگلیسی است؛
     * عبارت‌های فارسی معادل همان تیتر در UI فارسی‌اند. این‌ها فقط روی
     * متن/توضیح صفحه سنجیده می‌شوند — یعنی تب خصوصی واقعاً باز شده
     * است، نه اینکه کلمه‌اش جایی در منو دیده شده باشد.
     */
    private val INCOGNITO_PAGE_MARKERS = listOf(
        "you've gone incognito",
        "you have gone incognito",
        "gone incognito",
        "ناشناس شده‌اید",
        "در حالت ناشناس هستید",
        "حالت ناشناس فعال",
    )

    /** کلیدهای بخش «جستجو» — سرویس برای رویداد تایپ فقط این‌ها را چک می‌کند. */
    private val SEARCH_SECTION_KEYS: Set<String> = setOf(
        SECTION_TELEGRAM_SEARCH,
        SECTION_INSTAGRAM_SEARCH,
        SECTION_YOUTUBE_SEARCH,
        SECTION_WHATSAPP_SEARCH,
        SECTION_TIKTOK_SEARCH,
        SECTION_X_SEARCH,
        SECTION_FACEBOOK_SEARCH,
        SECTION_SNAPCHAT_SEARCH,
    )

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

    /** آیا hint فیلد ویرایشی، hint جستجو است؟ (برای سرویس: پر کردن پرچم‌های فعال‌سازی.) */
    fun isSearchHint(hint: String?): Boolean {
        return containsMarker(hint, SEARCH_MARKERS)
    }

    /** آیا [sectionKey] یکی از بخش‌های «جستجو» است؟ */
    fun isSearchSection(sectionKey: String): Boolean {
        return SEARCH_SECTION_KEYS.contains(sectionKey)
    }

    /**
     * آیا فیلد جستجو شاهد تعامل واقعی دارد؟ کلیک روی نوار/فیلد جستجو،
     * فوکوس، یا تایپ‌کردن همین حالا در آن. صرفِ دیده شدن فیلد در درخت
     * (مثلاً نوار جستجوی بالای صفحه اصلی تلگرام هنگام اسکرول) و فیلدی
     * که فقط متن نگه داشته، شاهد نیست — همان باگی که سید گزارش کرد.
     */
    private fun isSearchFieldActive(signals: SectionSignals): Boolean {
        return signals.searchFieldClicked ||
            signals.searchFieldFocused ||
            signals.searchFieldHasText
    }

    /**
     * آیا کلیک روی این گره، کلیک روی خودِ فیلد جستجو است؟ فقط وقتی
     * گره ویرایشی است و hint جستجو دارد. سرویس از این برای پر کردن
     * [SectionSignals.searchFieldClicked] روی رویداد TYPE_VIEW_CLICKED
     * استفاده می‌کند (خود گره و فرزندان مستقیمش).
     *
     * چرا فقط فیلد ویرایشی؟ برچسب تب هم می‌تواند «Search» داشته باشد
     * (تب «Search and explore» اینستاگرام، تب جستجوی ایکس) و کلیک روی
     * آن ورود به فید/اکسپلور است، نه جستجو — با معیار متنی قبلی، زدن
     * روی اکسپلور اینستاگرام بخش «جستجو» را بلاک می‌کرد (گزارش سید
     * ۲۰۲۶-۱۱-۲۱). آیکون/نوار جستجوی غیرقابل‌ویرایش هم شاهد کلیک
     * نیست؛ زدنش فیلد واقعی را فوکوس می‌کند و همان فوکوس شاهد است.
     */
    fun isSearchFieldClick(isEditable: Boolean, hint: String?): Boolean {
        return isEditable && isSearchHint(hint)
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

    /**
     * جستجو: شاهد تعامل واقعی با جستجو هست (کلیک روی نوار/فیلد، فوکوس،
     * یا تایپ همین حالا)، در یکی از پکیج‌های مجاز. عنوان «Search» یا
     * فیلدِ صرفاً دیده‌شده یا فیلدِ فقط متن‌نگه‌داشته کافی نیست.
     */
    private fun isSearchShowing(
        packageName: String,
        signals: SectionSignals,
        vararg packages: String,
    ): Boolean {
        if (!packages.contains(packageName)) return false
        return isSearchFieldActive(signals)
    }

    /**
     * بخش فیدی (اکسپلور/ریلز/شورتس/وضعیت/فید/خانه/اسپات‌لایت): مارکر
     * فقط در متن/توضیح گره‌ای که واقعاً انتخاب شده (تب فعال) — و فیلد
     * جستجو فعال نیست. عنوان/کلاس پنجره و برچسب تب که فقط در درخت هست
     * (مثلاً «Reels» در ناوبری پایین روی همه صفحه‌ها) شاهد نیستند؛ اگر
     * اپ وضعیت انتخاب را گزارش نکند، بخش fail-open می‌ماند (بلاک
     * نمی‌شود).
     */
    private fun isFeedShowing(
        packageName: String,
        signals: SectionSignals,
        markers: List<String>,
        vararg packages: String,
    ): Boolean {
        if (!packages.contains(packageName)) return false
        if (isSearchFieldActive(signals)) return false
        return anyContains(signals.selectedTexts, markers) ||
            anyContains(signals.selectedContentDescriptions, markers)
    }

    /**
     * مرور خصوصی کروم: یا کاربر روی آیتم منوی «New Incognito tab» کلیک
     * کرده است (متن گره کلیک‌شده مارکر کلیک دارد)، یا خودِ صفحه تب
     * خصوصی باز است (متن خالیِ «You've gone incognito» روی صفحه).
     * کلمه «incognito»/«ناشناس» که فقط در منو یا فهرستی دیده شود،
     * هرگز بلاک نمی‌کند.
     */
    private fun isIncognitoShowing(signals: SectionSignals): Boolean {
        val clicked = anyContains(signals.clickedTexts, INCOGNITO_CLICK_MARKERS) ||
            anyContains(signals.clickedContentDescriptions, INCOGNITO_CLICK_MARKERS)
        if (clicked) return true
        return anyContains(signals.texts, INCOGNITO_PAGE_MARKERS) ||
            anyContains(signals.contentDescriptions, INCOGNITO_PAGE_MARKERS)
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
                return isSearchShowing(packageName, signals, TELEGRAM_PACKAGE)
            }
            SECTION_INSTAGRAM_APP -> {
                if (packageName != INSTAGRAM_PACKAGE) return false
                return true
            }
            SECTION_INSTAGRAM_SEARCH -> {
                return isSearchShowing(packageName, signals, INSTAGRAM_PACKAGE)
            }
            SECTION_INSTAGRAM_EXPLORE -> {
                return isFeedShowing(packageName, signals, EXPLORE_MARKERS, INSTAGRAM_PACKAGE)
            }
            SECTION_INSTAGRAM_REELS -> {
                return isFeedShowing(packageName, signals, REELS_MARKERS, INSTAGRAM_PACKAGE)
            }
            SECTION_YOUTUBE_APP -> {
                if (packageName != YOUTUBE_PACKAGE) return false
                return true
            }
            SECTION_YOUTUBE_SEARCH -> {
                return isSearchShowing(packageName, signals, YOUTUBE_PACKAGE)
            }
            SECTION_YOUTUBE_SHORTS -> {
                return isFeedShowing(packageName, signals, SHORTS_MARKERS, YOUTUBE_PACKAGE)
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
            SECTION_CHROME_APP -> {
                if (packageName != CHROME_PACKAGE) return false
                // کل اپ: خودِ پیش‌زمینه بودن کروم کافی است؛ سیگنالی لازم نیست.
                return true
            }
            SECTION_CHROME_INCOGNITO -> {
                if (packageName != CHROME_PACKAGE) return false
                // فقط کلیک روی آیتم منوی مرور خصوصی، یا خودِ صفحه تب
                // خصوصی؛ کلمه «incognito» در منو به‌تنهایی بلاک نمی‌کند.
                return isIncognitoShowing(signals)
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
