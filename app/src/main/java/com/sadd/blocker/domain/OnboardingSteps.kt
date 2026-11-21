package com.sadd.blocker.domain

/**
 * ماشین وضعیت مرحله‌های ویزارد راه‌اندازی اول‌نصب — خالص و تست‌پذیر.
 *
 * مرحله دسترسی‌پذیری یک دنباله سه‌مرحله‌ای است، دقیقاً طبق ترتیبی که
 * روی گوشی سید (A35) واقعاً اتفاق می‌افتد:
 *  ۱. اول به تنظیمات دسترسی‌پذیری می‌روی، Installed apps و بعد «سد» را
 *     می‌زنی، کلید خاکستری پایین صفحه را می‌زنی و پیام را با Close می‌بندی.
 *  ۲. بعد به اطلاعات برنامه می‌روی و «Allow restricted settings» را
 *     از ⋮ روشن می‌کنی.
 *  ۳. بعد برمی‌گردی به دسترسی‌پذیری (Installed apps ← «سد»)؛ کلید حالا
 *     روشن شده و «سد» را فعال می‌کنی (Allow).
 * پس «اجازه تنظیمات محدودشده» مرحله جدایی قبل از دسترسی‌پذیری نیست؛
 * مرحله دومِ همین دنباله است. ترتیب قبلی (restricted جدا، قبل از
 * accessibility) با جریان واقعی گوشی جور نبود و همین گیج‌کننده بود.
 *
 * صداقت وضعیت: وضعیت restricted settings را هیچ API عمومی‌ای مستقیم
 * نشان نمی‌دهد، پس تایید دستی مرحله‌های ۱ و ۲ فقط «تأیید شما»ست تا
 * وقتی سرویس Accessibility واقعاً فعال شود؛ فعال‌شدن سرویس بدون Allow
 * restricted settings ممکن نیست، پس خودش مدرک انجام‌شدن هر سه مرحله است.
 * مرحله‌ها در فهرست قابل‌زدن و برگشت‌پذیرند.
 *
 * بعد از ۵ مرحله مجوز، یک مرحله انتخاب محافظت از حذف نصب می‌آید
 * ([KEY_PROTECTION]) — انتخابی است و ردکردنش ویزارد را قفل نمی‌کند.
 * بعد از آن، آموزش آشنایی با بخش‌های اپ یک‌بار نشان داده می‌شود
 * (پرچم tutorial_done در DataStore) و فقط داخل همین جریان راه‌اندازی
 * است، نه موقع هر استارت اپ.
 */
object OnboardingSteps {

    const val KEY_NOTIFICATION = "notification"
    const val KEY_ACCESSIBILITY = "accessibility"
    const val KEY_USAGE = "usage"
    const val KEY_BATTERY = "battery"
    const val KEY_ALARM = "alarm"
    /** مرحله انتخاب محافظت از حذف نصب — بعد از مجوزها، انتخابی و ردشدنی. */
    const val KEY_PROTECTION = "protection"

    /** ترتیب ۵ مرحله مجوز — برای نگاشت وضعیت‌های زنده سیستم. */
    val PERMISSION_ORDER = listOf(
        KEY_NOTIFICATION,
        KEY_ACCESSIBILITY,
        KEY_USAGE,
        KEY_BATTERY,
        KEY_ALARM,
    )

    /** ترتیب کامل ویزارد: ۵ مرحله مجوز + انتخاب محافظت از حذف نصب. */
    val ORDER = PERMISSION_ORDER + KEY_PROTECTION

    /** تعداد مرحله‌های دنباله دسترسی‌پذیری. */
    const val ACCESSIBILITY_PHASE_COUNT = 3

    /**
     * نصب‌کننده‌هایی که قفل «تنظیمات محدودشده» شاملشان نمی‌شود:
     * فروشگاه‌های شناخته‌شده و adb. اپی که از این‌ها نصب شده باشد،
     * کلید دسترسی‌پذیری‌اش از اول روشن است.
     */
    val KNOWN_STORE_INSTALLERS = setOf(
        "com.android.vending", // Google Play
        "com.farsitel.bazaar", // کافه‌بازار
        "ir.mservices.market", // مایکت
        "com.android.shell", // adb
        "com.sec.android.app.samsungapps", // Galaxy Store
        "com.huawei.appmarket", // AppGallery
    )

    /**
     * آیا انتظار می‌رود قفل «تنظیمات محدودشده» روی کلید دسترسی‌پذیری
     * اعمال شود؟ برای نصب‌کننده‌های فروشگاهی/adb نه (مسیر مستقیم
     * تک‌مرحله‌ای)؛ برای هر چیز دیگر — از جمله null و نامشخص — بله
     * (دنباله سه‌مرحله‌ای کلید خاکستری). پیش‌فرض امن همین است: اگر
     * قفل در واقع اعمال نشود، کاربر در مرحله ۱ کلید را می‌زند و
     * روشن می‌شود و تشخیص خودکار مرحله را تمام می‌کند؛ برعکسش
     * (مسیر مستقیم ولی کلید خاکستری) کاربر را گیر می‌اندازد.
     */
    fun isRestrictedSettingsExpected(installerPackage: String?): Boolean =
        installerPackage !in KNOWN_STORE_INSTALLERS

    /**
     * مرحله فعلی دنباله دسترسی‌پذیری؛ 0 یعنی دنباله تمام شده
     * (سرویس فعال است). مقدار ذخیره‌شده به ۱ تا ۳ محدود می‌شود.
     */
    fun accessibilityPhase(storedPhase: Int, accessibilityEnabled: Boolean): Int =
        if (accessibilityEnabled) 0 else storedPhase.coerceIn(1, ACCESSIBILITY_PHASE_COUNT)

    /**
     * مرحله‌ای انجام‌شده حساب می‌شود وقتی سرویس فعال باشد، یا شماره‌اش
     * از مرحله فعلی گذشته باشد (یعنی کاربر دستی تاییدش کرده).
     */
    fun isAccessibilityPhaseDone(
        phase: Int,
        currentPhase: Int,
        accessibilityEnabled: Boolean,
    ): Boolean = accessibilityEnabled || phase < currentPhase

    /** مرحله بعد از تایید دستی یک مرحله؛ از ۳ جلوتر نمی‌رود. */
    fun nextAccessibilityPhase(currentPhase: Int): Int =
        (currentPhase + 1).coerceAtMost(ACCESSIBILITY_PHASE_COUNT)

    /**
     * مرحله محافظت انجام‌شده است وقتی کاربر انتخابش را کرده باشد
     * («فعال کن» یا «الان نه») یا مدیر دستگاه همین حالا فعال باشد
     * (مثلاً دستی از تنظیمات سیستم فعال شده).
     */
    fun isProtectionStepDone(choiceDone: Boolean, adminActive: Boolean): Boolean =
        choiceDone || adminActive

    /**
     * آموزش آشنایی فقط وقتی نشان داده می‌شود که همه مرحله‌های ویزارد
     * (مجوزها + انتخاب محافظت) انجام شده باشند و آموزش قبلاً دیده
     * نشده باشد. به جریان راه‌اندازی گره خورده، نه به استارت اپ:
     * نصب قدیمی‌ای که راه‌اندازی را تمام کرده، آموزش را نمی‌بیند.
     */
    fun shouldShowTutorial(allStepsDone: Boolean, tutorialDone: Boolean): Boolean =
        allStepsDone && !tutorialDone

    /**
     * اندیس اولین مرحله ناتمام؛ اگر همه انجام شده‌اند، اندازه فهرست
     * برمی‌گردد (یعنی مرحله فعلی‌ای نیست).
     */
    fun firstMissingIndex(granted: List<Boolean>): Int =
        granted.indexOfFirst { !it }.takeIf { it >= 0 } ?: granted.size
}
