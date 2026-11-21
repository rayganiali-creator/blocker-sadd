package com.sadd.blocker.data.system

import android.content.Context
import android.os.Build

/**
 * خواندن منبع نصب اپ از سیستم — برای تشخیص اینکه قفل «تنظیمات
 * محدودشده» (Restricted settings) روی کلید دسترسی‌پذیری اعمال
 * می‌شود یا نه.
 *
 * اندروید این قفل را فقط برای اپ‌هایی می‌گذارد که دستی نصب شده‌اند
 * (از مرورگر، فایل‌منیجر و مانند آن). اپی که از یک فروشگاه نصب شده
 * باشد — یا با adb — کلید دسترسی‌پذیری‌اش از اول روشن است و دنباله
 * سه‌مرحله‌ای «کلید خاکستری» برایش غلط و گیج‌کننده است.
 *
 * تصمیم‌گیری خالص (کدام نصب‌کننده‌ها معاف‌اند) در
 * [com.sadd.blocker.domain.OnboardingSteps.isRestrictedSettingsExpected]
 * است؛ اینجا فقط خواندن از PackageManager انجام می‌شود.
 */
object InstallSource {

    /**
     * نام پکیج نصب‌کننده «سد» (مثلاً com.farsitel.bazaar) یا null اگر
     * خواندنش ممکن نباشد. در API 30+ از getInstallSourceInfo و پایین‌تر
     * از getInstallerPackageName قدیمی استفاده می‌شود (minSdk ‏۲۶ است).
     * هیچ‌وقت استثنا پرتاب نمی‌کند — null یعنی «نامشخص» و تصمیم‌گیرنده
     * آن را امن‌ترین حالت (دنباله کامل) حساب می‌کند.
     */
    fun installerPackage(context: Context): String? =
        runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
}
