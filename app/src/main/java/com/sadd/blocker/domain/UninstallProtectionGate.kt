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
}
