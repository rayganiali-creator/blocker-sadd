package com.sadd.blocker.domain

/**
 * تصمیم خالص محافظت از حذف نصب — بدون وابستگی اندروید، تست‌پذیر.
 *
 * قانون اصلی (مثل بقیه تغییرهای Strict): روشن‌کردن محافظت همیشه آزاد است؛
 * خاموش‌کردن آن وسط سشن Strict فعال، نیازمند بازگشایی Strict است
 * (تایپ متن طولانی یا انتظار کامل مدت تعیین‌شده). بیرون از سشن Strict،
 * خاموش‌کردن هم آزاد است. شروع سشن Strict همیشه آزاد است.
 */
object UninstallProtectionGate {

    /** آیا خاموش‌کردن محافظت نیازمند بازگشایی Strict است؟ */
    fun requiresUnlock(strictSessionActive: Boolean, turningOn: Boolean): Boolean =
        strictSessionActive && !turningOn

    /** آیا درخواست انتظار ثبت‌شده برای محافظت از حذف نصب است؟ */
    fun isProtectionRequest(request: StrictCooldownRequest?): Boolean =
        request?.pendingKind == StrictPendingKind.PROTECTION

    /**
     * آیا با روشن‌کردن تاگل باید فوراً صفحه فعال‌سازی باز شود؟
     * فقط وقتی مدیر دستگاه هنوز فعال نیست — اگر فعال است، چیزی برای باز کردن نیست.
     * (روشن‌کردن همیشه آزاد است؛ این فقط تصمیم «باز کن یا نه» است.)
     */
    fun shouldLaunchActivation(turningOn: Boolean, adminActive: Boolean): Boolean =
        turningOn && !adminActive

    /**
     * آیا باید اعلان «محافظت از تنظیمات سیستم غیرفعال شد» نشان داده شود؟
     * فقط وقتی محافظت روشن است و مدیر دستگاه قبلاً فعال بوده ولی الان فعال نیست —
     * یعنی کسی آن را از تنظیمات گوشی غیرفعال کرده است. اپ نمی‌تواند جلوی
     * غیرفعال‌سازی از تنظیمات سیستم را بگیرد؛ فقط می‌تواند خبر بدهد.
     */
    fun shouldShowDeactivatedNotice(
        protectionEnabled: Boolean,
        wasAdminActive: Boolean,
        adminActive: Boolean,
    ): Boolean = protectionEnabled && wasAdminActive && !adminActive

    /**
     * متن یک‌خطی تشخیص برای کارت محافظت: وضعیت مدیر دستگاه + در دسترس بودن
     * صفحه فعال‌سازی روی این گوشی. برای عیب‌یابی از راه دور با کاربر است؛
     * activationAvailable==null یعنی هنوز بررسی نشده.
     */
    fun adminDiagnosticLine(adminActive: Boolean, activationAvailable: Boolean?): String {
        val activeText = if (adminActive) "مدیر دستگاه فعال است" else "مدیر دستگاه فعال نیست"
        val availableText = when (activationAvailable) {
            true -> "صفحه فعال‌سازی در دسترس است"
            false -> "صفحه فعال‌سازی در دسترس نیست"
            null -> "صفحه فعال‌سازی بررسی نشده"
        }
        return "تشخیص: $activeText — $availableText"
    }
}
