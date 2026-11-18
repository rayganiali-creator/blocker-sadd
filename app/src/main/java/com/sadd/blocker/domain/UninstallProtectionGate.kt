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
     * آیا با ضربه کاربر باید صفحه فعال‌سازی باز شود؟
     * فقط وقتی مدیر دستگاه هنوز فعال نیست — اگر فعال است، چیزی برای باز کردن نیست.
     * عمداً به جهت تاگل وابسته نیست: تاگل وضعیت واقعی مدیر دستگاه را نشان
     * می‌دهد، پس ضربه روی تاگلِ خاموش همیشه یعنی «فعال کن» و ضربه روی
     * تاگلِ روشن (مدیر فعال) هرگز به اینجا نمی‌رسد (اول دیالوگ تایید می‌آید).
     */
    fun shouldLaunchActivation(adminActive: Boolean): Boolean = !adminActive

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
     * آیا نگهبان مسلح است؟ تنظیم روشن بودن کافی است — ولی مدیر دستگاه
     * می‌تواند دستی از تنظیمات سیستم فعال شده باشد، بدون اینکه تنظیم
     * داخل اپ روشن شده باشد؛ در آن حالت هم نگهبان باید مسلح باشد، وگرنه
     * دقیقاً در وضعیت سید (مدیر فعال، تنظیم خاموش) نگهبان بی‌صدا خاموش
     * می‌ماند و هیچ‌وقت شلیک نمی‌کند. برعکسش هم درست است: تنظیم روشن
     * ولی مدیر غیرفعال یعنی کاربر می‌خواهد محافظت باشد، پس نگهبان مسلح است.
     */
    fun isGuardArmed(protectionEnabled: Boolean, adminActive: Boolean): Boolean =
        protectionEnabled || adminActive

    /**
     * خط یک‌خطی «وضعیت محافظ» برای کارت محافظت: محافظت، مدیر دستگاه،
     * دسترسی‌پذیری و مسلح بودن نگهبان — تا کاربر روی گوشی خودش ببیند نگهبان
     * مسلح است یا نه، بدون اینکه از او پرسیده شود.
     */
    fun guardStatusLine(
        protectionEnabled: Boolean,
        adminActive: Boolean,
        accessibilityEnabled: Boolean,
    ): String {
        val armed = isGuardArmed(protectionEnabled, adminActive)
        val protection = if (protectionEnabled) "محافظت روشن" else "محافظت خاموش"
        val admin = if (adminActive) "مدیر دستگاه فعال" else "مدیر دستگاه غیرفعال"
        val accessibility = if (accessibilityEnabled) "دسترسی‌پذیری روشن" else "دسترسی‌پذیری خاموش"
        val guard = if (armed && accessibilityEnabled) "نگهبان مسلح ✓" else "نگهبان خاموش"
        return "وضعیت محافظ: $protection • $admin • $accessibility • $guard"
    }

    /**
     * متن یک‌خطی تشخیص برای کارت محافظت: وضعیت مدیر دستگاه + در دسترس بودن
     * صفحه فعال‌سازی روی این گوشی. برای عیب‌یابی از راه دور با کاربر است؛
     * activationAvailable==null یعنی هنوز بررسی نشده.
     * deviceAdminListAvailable==false یعنی فهرست «Device admin apps»
     * مستقیم باز نمی‌شود و دکمه آن به تنظیمات امنیتی می‌رود.
     */
    fun adminDiagnosticLine(
        adminActive: Boolean,
        activationAvailable: Boolean?,
        deviceAdminListAvailable: Boolean? = null,
    ): String {
        val activeText = if (adminActive) "مدیر دستگاه فعال است" else "مدیر دستگاه فعال نیست"
        val availableText = when (activationAvailable) {
            true -> "صفحه فعال‌سازی در دسترس است"
            false -> "صفحه فعال‌سازی در دسترس نیست"
            null -> "صفحه فعال‌سازی بررسی نشده"
        }
        val listText = if (deviceAdminListAvailable == false) {
            " — فهرست Device admin apps مستقیم باز نمی‌شود"
        } else {
            ""
        }
        return "تشخیص: $activeText — $availableText$listText"
    }
}
