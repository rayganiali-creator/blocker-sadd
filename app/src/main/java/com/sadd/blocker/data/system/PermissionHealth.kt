package com.sadd.blocker.data.system

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.sadd.blocker.service.BlockAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * وضعیت واقعی مجوزها — بدون نمایش ساختگی.
 * هر مورد مستقیم از سیستم خوانده می‌شود و به صفحه تنظیمات مربوطه لینک دارد.
 * action همان Settings.ACTION_* است؛ uri برای درخواست معافیت باتری است.
 */
data class PermissionStatus(
    val key: String,
    val title: String,
    val granted: Boolean,
    val description: String,
    val settingsAction: String,
    val settingsUri: String? = null,
)

@Singleton
class PermissionHealthChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun checkAll(): List<PermissionStatus> {
        return listOf(
            PermissionStatus(
                key = "accessibility",
                title = "دسترسی‌پذیری (Accessibility)",
                granted = isAccessibilityEnabled(),
                description = "برای تشخیص نیت جستجو و مداخله در لحظه لازم است.",
                settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            ),
            PermissionStatus(
                key = "usage",
                title = "دسترسی به مصرف اپ‌ها (Usage Access)",
                granted = hasUsageAccess(),
                description = "فعلاً برای تشخیص استفاده نمی‌شود؛ باز شدن اپ با سرویس Accessibility تشخیص داده می‌شود.",
                settingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            ),
            PermissionStatus(
                key = "notification",
                title = "اعلان‌ها",
                granted = hasNotificationPermission(),
                description = "برای نمایش شمارش معکوس سشن در اعلان لازم است.",
                settingsAction = Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            ),
            PermissionStatus(
                key = "battery",
                title = "معافیت بهینه‌سازی باتری",
                granted = isBatteryExemptionGranted(),
                description = "سامسونگ سرویس پس‌زمینه را می‌کشد؛ معافیت، پایداری سشن را بیشتر می‌کند.",
                settingsAction = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                settingsUri = "package:${context.packageName}",
            ),
            PermissionStatus(
                key = "alarm",
                title = "آلارم دقیق",
                granted = isExactAlarmAllowed(),
                description = "برای شروع دقیق زمان‌بندی‌ها؛ بدون آن ممکن است تا ۱۵ دقیقه دیر شود.",
                settingsAction = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                settingsUri = "package:${context.packageName}",
            ),
        )
    }

    fun missingCount(): Int {
        return checkAll().count { !it.granted }
    }

    fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val ownService = "${context.packageName}/${BlockAccessibilityService::class.java.name}"
        return enabledServices.split(":").any { it == ownService }
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBatteryExemptionGranted(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isExactAlarmAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }
}
