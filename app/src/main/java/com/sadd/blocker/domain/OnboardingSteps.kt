package com.sadd.blocker.domain

/**
 * ماشین وضعیت مرحله‌های ویزارد راه‌اندازی اول‌نصب — خالص و تست‌پذیر.
 *
 * مرحله «restricted» (اجازه تنظیمات محدودشده) یک مرحله جداست که قبل از
 * فعال‌کردن Accessibility می‌آید: اندروید برای برنامه‌های نصب‌شده دستی
 * (نه از فروشگاه) کلید دسترسی‌پذیری را قفل می‌کند تا وقتی «Allow
 * restricted settings» در صفحه اطلاعات برنامه فعال نشود؛ هیچ کدی
 * نمی‌تواند دورش بزند، پس ویزارد آن را یک مرحله جدا با راهنمای قدم‌به‌قدم
 * نشان می‌دهد، نه یک نکته کوچک کنار مرحله دسترسی‌پذیری.
 *
 * صداقت وضعیت: هیچ API عمومی‌ای وضعیت restricted settings را مستقیم
 * نشان نمی‌دهد، پس تیک این مرحله هرگز جعل نمی‌شود — یا کاربر خودش
 * «انجامش دادم» را می‌زند، یا سرویس Accessibility واقعاً فعال شده باشد
 * (فعال‌شدن سرویس بدون Allow restricted settings ممکن نیست، پس خودش
 * مدرک انجام‌شدن است).
 */
object OnboardingSteps {

    const val KEY_NOTIFICATION = "notification"
    const val KEY_RESTRICTED = "restricted"
    const val KEY_ACCESSIBILITY = "accessibility"
    const val KEY_USAGE = "usage"
    const val KEY_BATTERY = "battery"
    const val KEY_ALARM = "alarm"

    /**
     * ترتیب مرحله‌ها — restricted جدا و قبل از accessibility است؛
     * هر صفحه فقط یک کار دارد.
     */
    val ORDER = listOf(
        KEY_NOTIFICATION,
        KEY_RESTRICTED,
        KEY_ACCESSIBILITY,
        KEY_USAGE,
        KEY_BATTERY,
        KEY_ALARM,
    )

    /**
     * مرحله restricted انجام‌شده حساب می‌شود وقتی یا کاربر خودش تایید کرده
     * باشد، یا سرویس Accessibility واقعاً فعال شده باشد.
     */
    fun isRestrictedDone(accessibilityEnabled: Boolean, userConfirmed: Boolean): Boolean =
        accessibilityEnabled || userConfirmed

    /**
     * اندیس اولین مرحله ناتمام؛ اگر همه انجام شده‌اند، اندازه فهرست
     * برمی‌گردد (یعنی مرحله فعلی‌ای نیست).
     */
    fun firstMissingIndex(granted: List<Boolean>): Int =
        granted.indexOfFirst { !it }.takeIf { it >= 0 } ?: granted.size
}
