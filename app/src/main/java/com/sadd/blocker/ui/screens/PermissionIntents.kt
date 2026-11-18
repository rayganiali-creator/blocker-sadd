package com.sadd.blocker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.sadd.blocker.data.system.PermissionStatus
import com.sadd.blocker.data.system.UninstallProtection

/**
 * لانچرهای سیستمی درخواست مجوز — یک‌بار در صفحه ساخته می‌شوند و به
 * requestPermission داده می‌شوند تا منطق باز کردن مجوز در یک جا بماند
 * (صفحه عیب‌یابی و ویزارد راه‌اندازی هر دو همین را استفاده می‌کنند).
 * (VPN حذف شده — بلاک وب با Accessibility انجام می‌شود، پس لانچر
 * VpnService.prepare هم حذف شد.)
 */
data class PermissionLaunchers(
    val notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
)

@Composable
fun rememberPermissionLaunchers(onRefresh: () -> Unit): PermissionLaunchers {
    // اعلان‌ها: درخواست مستقیم runtime permission (اندروید ۱۳+)، نه فقط تنظیمات.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }
    return PermissionLaunchers(notificationPermissionLauncher)
}

/**
 * درخواست/باز کردن یک مجوز: اعلان مسیر سیستمی خودش را دارد؛
 * بقیه صفحه تنظیمات مربوطه را باز می‌کنند.
 */
fun requestPermission(
    context: Context,
    permission: PermissionStatus,
    launchers: PermissionLaunchers,
    onRefresh: () -> Unit,
) {
    when (permission.key) {
        "notification" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launchers.notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openSettingsPage(context, permission)
            }
        }
        else -> openSettingsPage(context, permission)
    }
}

/**
 * باز کردن صفحه فعال‌سازی مدیر دستگاه؛ true یعنی اینتنت فرستاده شد.
 * اول resolveActivity چک می‌شود؛ اگر صفحه‌ای برایش نباشد، Toast می‌گوید
 * این گوشی صفحه فعال‌سازی ندارد و کاربر باید از دکمه «تنظیمات امنیتی گوشی»
 * دستی فعال کند. خطای startActivity هم با متن واقعی خطا Toast می‌شود —
 * هیچ‌وقت بی‌صدا شکست نمی‌خورد.
 */
fun openAdminActivationPage(context: Context): Boolean {
    val intent = UninstallProtection.enableIntent(context)
    val resolves = runCatching { intent.resolveActivity(context.packageManager) }.getOrNull() != null
    if (!resolves) {
        Toast.makeText(
            context,
            "این گوشی صفحه فعال‌سازی مدیر دستگاه ندارد؛ از دکمه «تنظیمات امنیتی گوشی» دستی فعال کن.",
            Toast.LENGTH_LONG,
        ).show()
        return false
    }
    return runCatching { context.startActivity(intent) }
        .onFailure { e ->
            Toast.makeText(
                context,
                "صفحه فعال‌سازی باز نشد: ${e.message ?: "خطای نامشخص"}",
                Toast.LENGTH_LONG,
            ).show()
        }.isSuccess
}

/** باز کردن صفحه امنیت سیستم (جایی که Device admin apps هست)؛ خطا Toast می‌شود. */
fun openSecuritySettingsPage(context: Context): Boolean {
    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(
                context,
                "صفحه امنیت باز نشد؛ از تنظیمات گوشی: امنیت ← \u2066Device admin apps\u2069 ← «سد»",
                Toast.LENGTH_LONG,
            ).show()
        }.isSuccess
}

/**
 * باز کردن مستقیم فهرست «Device admin apps»؛ true یعنی فهرست باز شد.
 * اگر روی این گوشی مستقیم باز نشود (resolveActivity null)، به صفحه امنیت
 * سیستم می‌رود و false برمی‌گردد تا کارت بگوید مستقیم باز نشد — هیچ‌وقت
 * بی‌صدا شکست نمی‌خورد. خطای startActivity هم با متن واقعی Toast می‌شود.
 */
fun openDeviceAdminAppsPage(context: Context): Boolean {
    val intent = UninstallProtection.deviceAdminListIntent()
    val resolves = runCatching { intent.resolveActivity(context.packageManager) }.getOrNull() != null
    if (!resolves) {
        Toast.makeText(
            context,
            "فهرست Device admin apps مستقیم باز نشد؛ تنظیمات امنیتی باز شد.",
            Toast.LENGTH_LONG,
        ).show()
        openSecuritySettingsPage(context)
        return false
    }
    return runCatching { context.startActivity(intent) }
        .onFailure { e ->
            Toast.makeText(
                context,
                "فهرست Device admin apps باز نشد: ${e.message ?: "خطای نامشخص"}",
                Toast.LENGTH_LONG,
            ).show()
            openSecuritySettingsPage(context)
        }.isSuccess
}

/**
 * باز کردن صفحه اطلاعات برنامه (App info)؛ برای مرحله دوم دنباله
 * دسترسی‌پذیری (Allow restricted settings از ⋮). خطا Toast + fallback
 * دارد، مثل openSettingsPage — هیچ‌وقت بی‌صدا شکست نمی‌خورد.
 */
fun openAppInfoPage(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val opened = runCatching { context.startActivity(intent) }.isSuccess
    if (!opened) {
        Toast.makeText(
            context,
            "صفحه اطلاعات برنامه باز نشد؛ از تنظیمات گوشی: برنامه‌ها ← «سد»",
            Toast.LENGTH_LONG,
        ).show()
    }
}

/**
 * باز کردن صفحه تنظیمات مربوط به یک مجوز.
 * خطای startActivity بی‌صدا قورت داده نمی‌شود: FLAG_ACTIVITY_NEW_TASK اضافه
 * شده، در صورت شکست Toast نشان داده می‌شود و fallback به صفحه اطلاعات اپ است.
 */
fun openSettingsPage(context: Context, permission: PermissionStatus) {
    val intent = Intent(permission.settingsAction).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (permission.key == "notification") {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        permission.settingsUri?.let { data = Uri.parse(it) }
    }
    val opened = runCatching { context.startActivity(intent) }.isSuccess
    if (!opened) {
        Toast.makeText(
            context,
            "صفحه تنظیمات باز نشد؛ از تنظیمات گوشی اقدام کن",
            Toast.LENGTH_LONG,
        ).show()
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
