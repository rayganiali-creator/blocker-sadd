package com.sadd.blocker.data.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.sadd.blocker.service.SaddDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * محافظت از حذف نصب با مدیر دستگاه (Device Admin).
 *
 * وقتی «سد» مدیر دستگاه فعال است، سیستم اندروید حذف نصب اپ را مسدود می‌کند.
 * محدودیت‌های صادقانه (در متن UI هم گفته می‌شود):
 * - مسیر غیرفعال‌سازی از تنظیمات سیستم همیشه هست و فقط یک هشدار نشان می‌دهد
 *   ([SaddDeviceAdminReceiver.onDisableRequested])؛ پس این قفل مطلق نیست.
 * - مدیر دستگاه جلوی force-stop را نمی‌گیرد، فقط حذف نصب را.
 * - قفل مطلق فقط با Device Owner ممکن است که معمولاً ریست فکتوری می‌خواهد
 *   و خارج از محدوده این اپ است.
 */
@Singleton
class UninstallProtection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        fun adminComponent(context: Context): ComponentName =
            ComponentName(context, SaddDeviceAdminReceiver::class.java)

        /** اینتنت سیستمی فعال‌سازی مدیر دستگاه، با توضیح فارسی. */
        fun enableIntent(context: Context): Intent {
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "«سد» با مدیر دستگاه فعال، وسط سشن Strict قابل حذف نصب نیست. " +
                        "غیرفعال‌کردن آن از تنظیمات سیستم همیشه ممکن است و فقط یک هشدار نشان می‌دهد؛ " +
                        "این محافظت جلوی force-stop را نمی‌گیرد.",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /** آیا «سد» الان مدیر دستگاه فعال است؟ */
    fun isAdminActive(): Boolean =
        devicePolicyManager.isAdminActive(adminComponent(context))

    /** حذف «سد» از مدیر دستگاه (فقط اگر فعال باشد). */
    fun removeAdmin() {
        if (isAdminActive()) {
            devicePolicyManager.removeActiveAdmin(adminComponent(context))
        }
    }
}
