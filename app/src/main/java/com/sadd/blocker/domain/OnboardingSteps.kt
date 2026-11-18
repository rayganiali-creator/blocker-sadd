package com.sadd.blocker.domain

/**
 * ماشین وضعیت مرحله‌های ویزارد راه‌اندازی اول‌نصب — خالص و تست‌پذیر.
 *
 * مرحله دسترسی‌پذیری یک دنباله سه‌مرحله‌ای است، دقیقاً طبق ترتیبی که
 * روی گوشی سید (A35) واقعاً اتفاق می‌افتد:
 *  ۱. اول به تنظیمات دسترسی‌پذیری می‌روی و کلید خاکستری «سد» را می‌زنی؛
 *     پیامی می‌آید و آن را می‌بندی.
 *  ۲. بعد به اطلاعات برنامه می‌روی و «Allow restricted settings» را
 *     از ⋮ روشن می‌کنی.
 *  ۳. بعد برمی‌گردی به دسترسی‌پذیری؛ کلید حالا روشن شده و «سد» را
 *     فعال می‌کنی.
 * پس «اجازه تنظیمات محدودشده» مرحله جدایی قبل از دسترسی‌پذیری نیست؛
 * مرحله دومِ همین دنباله است. ترتیب قبلی (restricted جدا، قبل از
 * accessibility) با جریان واقعی گوشی جور نبود و همین گیج‌کننده بود.
 *
 * صداقت وضعیت: وضعیت restricted settings را هیچ API عمومی‌ای مستقیم
 * نشان نمی‌دهد، پس تایید دستی مرحله‌های ۱ و ۲ فقط «تأیید شما»ست تا
 * وقتی سرویس Accessibility واقعاً فعال شود؛ فعال‌شدن سرویس بدون Allow
 * restricted settings ممکن نیست، پس خودش مدرک انجام‌شدن هر سه مرحله است.
 * مرحله‌ها در فهرست قابل‌زدن و برگشت‌پذیرند.
 */
object OnboardingSteps {

    const val KEY_NOTIFICATION = "notification"
    const val KEY_ACCESSIBILITY = "accessibility"
    const val KEY_USAGE = "usage"
    const val KEY_BATTERY = "battery"
    const val KEY_ALARM = "alarm"

    /** ترتیب مرحله‌ها — دسترسی‌پذیری با دنباله سه‌مرحله‌ای‌اش یک مرحله است. */
    val ORDER = listOf(
        KEY_NOTIFICATION,
        KEY_ACCESSIBILITY,
        KEY_USAGE,
        KEY_BATTERY,
        KEY_ALARM,
    )

    /** تعداد مرحله‌های دنباله دسترسی‌پذیری. */
    const val ACCESSIBILITY_PHASE_COUNT = 3

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
     * اندیس اولین مرحله ناتمام؛ اگر همه انجام شده‌اند، اندازه فهرست
     * برمی‌گردد (یعنی مرحله فعلی‌ای نیست).
     */
    fun firstMissingIndex(granted: List<Boolean>): Int =
        granted.indexOfFirst { !it }.takeIf { it >= 0 } ?: granted.size
}
