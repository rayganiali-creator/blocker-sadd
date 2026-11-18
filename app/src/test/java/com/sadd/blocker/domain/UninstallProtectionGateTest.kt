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

    // ---------- مسلح بودن نگهبان ----------

    @Test
    fun guardArmedWhenSettingOnEvenIfAdminInactive() {
        assertTrue(UninstallProtectionGate.isGuardArmed(protectionEnabled = true, adminActive = false))
    }

    @Test
    fun guardArmedWhenAdminActiveEvenIfSettingOff() {
        // فعال‌سازی دستی از تنظیمات سیستم تنظیم داخل اپ را عوض نمی‌کند —
        // نگهبان در این وضعیت هم باید مسلح باشد (وضعیت واقعی سید).
        assertTrue(UninstallProtectionGate.isGuardArmed(protectionEnabled = false, adminActive = true))
    }

    @Test
    fun guardDisarmedOnlyWhenBothOff() {
        assertFalse(UninstallProtectionGate.isGuardArmed(protectionEnabled = false, adminActive = false))
        assertTrue(UninstallProtectionGate.isGuardArmed(protectionEnabled = true, adminActive = true))
    }

    @Test
    fun guardStatusLineShowsAllFourParts() {
        val armed = UninstallProtectionGate.guardStatusLine(
            protectionEnabled = true,
            adminActive = true,
            accessibilityEnabled = true,
        )
        assertTrue(armed.contains("محافظت روشن"))
        assertTrue(armed.contains("مدیر دستگاه فعال"))
        assertTrue(armed.contains("دسترسی‌پذیری روشن"))
        assertTrue(armed.contains("نگهبان مسلح"))
        val disarmed = UninstallProtectionGate.guardStatusLine(
            protectionEnabled = false,
            adminActive = false,
            accessibilityEnabled = false,
        )
        assertTrue(disarmed.contains("محافظت خاموش"))
        assertTrue(disarmed.contains("مدیر دستگاه غیرفعال"))
        assertTrue(disarmed.contains("دسترسی‌پذیری خاموش"))
        assertTrue(disarmed.contains("نگهبان خاموش"))
    }

    @Test
    fun guardStatusLineDisarmedWhenAccessibilityOffEvenIfArmed() {
        // مسلح بودن بدون Accessibility یعنی نگهبان عملاً کار نمی‌کند.
        val line = UninstallProtectionGate.guardStatusLine(
            protectionEnabled = true,
            adminActive = true,
            accessibilityEnabled = false,
        )
        assertTrue(line.contains("نگهبان خاموش"))
    }

    @Test
    fun adminDiagnosticLineMentionsDeviceAdminListUnavailable() {
        val line = UninstallProtectionGate.adminDiagnosticLine(
            adminActive = false,
            activationAvailable = true,
            deviceAdminListAvailable = false,
        )
        assertTrue(line.contains("فهرست Device admin apps مستقیم باز نمی‌شود"))
        val okLine = UninstallProtectionGate.adminDiagnosticLine(
            adminActive = true,
            activationAvailable = true,
            deviceAdminListAvailable = true,
        )
        assertFalse(okLine.contains("فهرست Device admin apps"))
    }
}
