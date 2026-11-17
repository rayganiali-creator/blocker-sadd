package com.sadd.blocker.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * Local VPN برای بلاک دامنه — اسکلت فاز ۱؛ پیاده‌سازی کامل در فاز ۴.
 *
 * طراحی (طبق ARCHITECTURE.md بخش ۴):
 * - VPN کاملاً لوکال است؛ هیچ بسته‌ای به سرور خارجی نمی‌رود.
 * - فقط DNS/اتصال به دامنه‌های لیست محلی بررسی و در صورت تطابق قطع می‌شود.
 * - نیازمند رضایت صریح کاربر در دیالوگ سیستمی VPN است.
 * تا فاز ۴، این سرویس هیچ ترافیکی را قطع نمی‌کند.
 */
class BlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO (فاز ۴): ساخت interface + حلقه خواندن بسته‌ها + فیلتر DNS لوکال.
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
            // fail-open
        }
        vpnInterface = null
        super.onDestroy()
    }
}
