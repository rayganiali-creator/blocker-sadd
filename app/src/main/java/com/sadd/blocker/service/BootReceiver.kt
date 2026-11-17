package com.sadd.blocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * بعد از ری‌استارت دستگاه، سشن‌های زمان‌بندی‌شده با WorkManager بازسازی می‌شوند (فاز ۵).
 * در فاز ۱ این Receiver فقط ثبت شده است تا Manifest کامل باشد.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // TODO (فاز ۵): زمان‌بندی مجدد WorkManager از Room.
    }
}
