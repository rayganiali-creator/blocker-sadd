package com.sadd.blocker.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * گیرنده مدیر دستگاه برای محافظت از حذف نصب.
 * فعال بودن آن باعث می‌شود سیستم اندروید حذف نصب «سد» را مسدود کند.
 * وقتی کاربر از تنظیمات سیستم بخواهد آن را غیرفعال کند، سیستم همین هشدار
 * را نشان می‌دهد — ولی مسیر غیرفعال‌سازی همیشه هست؛ قفل مطلق نیست.
 */
class SaddDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "«سد» مدیر دستگاه است تا وسط سشن Strict نتوانی آن را حذف کنی. " +
            "اگر غیرفعالش کنی، محافظت از حذف نصب از کار می‌افتد و اپ قابل حذف می‌شود."
    }
}
