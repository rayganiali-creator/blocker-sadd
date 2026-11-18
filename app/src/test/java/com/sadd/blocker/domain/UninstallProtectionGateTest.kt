package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UninstallProtectionGateTest {

    @Test
    fun disableDuringStrictSessionRequiresUnlock() {
        assertTrue(UninstallProtectionGate.requiresUnlock(strictSessionActive = true, turningOn = false))
    }

    @Test
    fun enableDuringStrictSessionIsFree() {
        assertFalse(UninstallProtectionGate.requiresUnlock(strictSessionActive = true, turningOn = true))
    }

    @Test
    fun disableOutsideStrictSessionIsFree() {
        assertFalse(UninstallProtectionGate.requiresUnlock(strictSessionActive = false, turningOn = false))
    }

    @Test
    fun enableOutsideStrictSessionIsFree() {
        assertFalse(UninstallProtectionGate.requiresUnlock(strictSessionActive = false, turningOn = true))
    }

    @Test
    fun protectionKindRoundTrip() {
        assertEquals(StrictPendingKind.PROTECTION, StrictPendingKind.valueOf("PROTECTION"))
        val request = StrictCooldownRequest(1_000L, StrictPendingKind.PROTECTION, "", 24)
        assertEquals(StrictPendingKind.PROTECTION, request.pendingKind)
        assertEquals("", request.pendingValue)
        assertTrue(UninstallProtectionGate.isProtectionRequest(request))
    }

    @Test
    fun isProtectionRequestOnlyForProtectionKind() {
        assertFalse(
            UninstallProtectionGate.isProtectionRequest(
                StrictCooldownRequest(1_000L, StrictPendingKind.END_SESSION, "", 24),
            ),
        )
        assertFalse(
            UninstallProtectionGate.isProtectionRequest(
                StrictCooldownRequest(1_000L, StrictPendingKind.RULE_CHANGE, "keyword|toggle|1|false", 24),
            ),
        )
        assertFalse(UninstallProtectionGate.isProtectionRequest(null))
    }

    @Test
    fun shouldLaunchActivationOnlyWhenAdminInactive() {
        // ضربه برای فعال‌کردن فقط وقتی مدیر دستگاه فعال نیست صفحه را باز می‌کند؛
        // به جهت تاگل وابسته نیست (تاگل وضعیت واقعی مدیر دستگاه را نشان می‌دهد).
        assertTrue(UninstallProtectionGate.shouldLaunchActivation(adminActive = false))
        assertFalse(UninstallProtectionGate.shouldLaunchActivation(adminActive = true))
    }

    @Test
    fun adminDiagnosticLineCoversAllStates() {
        assertEquals(
            "تشخیص: مدیر دستگاه فعال است — صفحه فعال‌سازی در دسترس است",
            UninstallProtectionGate.adminDiagnosticLine(adminActive = true, activationAvailable = true),
        )
        assertEquals(
            "تشخیص: مدیر دستگاه فعال است — صفحه فعال‌سازی در دسترس نیست",
            UninstallProtectionGate.adminDiagnosticLine(adminActive = true, activationAvailable = false),
        )
        assertEquals(
            "تشخیص: مدیر دستگاه فعال نیست — صفحه فعال‌سازی در دسترس است",
            UninstallProtectionGate.adminDiagnosticLine(adminActive = false, activationAvailable = true),
        )
        assertEquals(
            "تشخیص: مدیر دستگاه فعال نیست — صفحه فعال‌سازی در دسترس نیست",
            UninstallProtectionGate.adminDiagnosticLine(adminActive = false, activationAvailable = false),
        )
        assertEquals(
            "تشخیص: مدیر دستگاه فعال نیست — صفحه فعال‌سازی بررسی نشده",
            UninstallProtectionGate.adminDiagnosticLine(adminActive = false, activationAvailable = null),
        )
    }

    @Test
    fun deactivatedNoticeOnlyWhenEnabledAndWasActiveAndNowInactive() {
        // محافظت روشن بود، مدیر فعال بود، الان فعال نیست = از تنظیمات سیستم غیرفعال شده.
        assertTrue(
            UninstallProtectionGate.shouldShowDeactivatedNotice(
                protectionEnabled = true,
                wasAdminActive = true,
                adminActive = false,
            ),
        )
        // مدیر هنوز فعال است — اعلانی نیست.
        assertFalse(
            UninstallProtectionGate.shouldShowDeactivatedNotice(
                protectionEnabled = true,
                wasAdminActive = true,
                adminActive = true,
            ),
        )
        // مدیر هیچ‌وقت فعال نشده بود — همان «هنوز فعال نشده» است، نه «غیرفعال شد».
        assertFalse(
            UninstallProtectionGate.shouldShowDeactivatedNotice(
                protectionEnabled = true,
                wasAdminActive = false,
                adminActive = false,
            ),
        )
        // محافظت خاموش است — اعلانی نیست (خاموش‌کردنِ خودِ اپ).
        assertFalse(
            UninstallProtectionGate.shouldShowDeactivatedNotice(
                protectionEnabled = false,
                wasAdminActive = true,
                adminActive = false,
            ),
        )
    }

    @Test
    fun protectionRequestSurvivesCooldownElapsedCheck() {
        // درخواست محافظت مثل بقیه درخواست‌ها با زمان دیواری می‌ماند.
        val request = StrictCooldownRequest(1_000L, StrictPendingKind.PROTECTION, "", 24)
        assertTrue(StrictUnlock.isCooldownElapsed(request, 1_000L + 24L * StrictUnlock.HOUR_MS))
        assertFalse(StrictUnlock.isCooldownElapsed(request, 1_000L + 23L * StrictUnlock.HOUR_MS))
    }
}
