package com.sadd.blocker.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.sadd.blocker.core.IntentDetector

/**
 * AccessibilityService — ساختار واقعی فاز ۱؛ منطق کامل در فاز ۳ و ۴.
 *
 * اصول سفت (از نسخه ۲٫۰٫۱۰):
 * - همه‌چیز داخل try/catch؛ خطا هرگز گوشی را قفل نمی‌کند (fail-open).
 * - فقط متن فیلد ورودی فوکوس‌شده خوانده می‌شود؛ محتوای صفحه برای بلاک استفاده نمی‌شود.
 * - متن خوانده‌شده هرگز لاگ نمی‌شود و به سرور نمی‌رود.
 * - رویدادهای اپ خود «سد» نادیده گرفته می‌شوند.
 */
class BlockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            // هرگز روی متن خود اپ بلاک نکن.
            if (event.packageName?.toString() == packageName) return
            // TODO (فاز ۳): شناسایی فیلد جستجو + سابمیت + ارزیابی IntentDetector.
            // تا آن زمان هیچ بلاکی از این سرویس صادر نمی‌شود — اسکلت عمداً بی‌اثر است.
            @Suppress("UNUSED_VARIABLE")
            val detector = IntentDetector
        } catch (_: Exception) {
            // fail-open: خطا در تشخیص = عدم بلاک.
        }
    }

    override fun onInterrupt() {
        // وقفه سیستمی؛ سشن از Room بازسازی می‌شود، نه از حافظه.
    }
}
