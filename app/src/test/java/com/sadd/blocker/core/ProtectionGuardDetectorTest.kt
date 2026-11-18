package com.sadd.blocker.core

import com.sadd.blocker.core.ProtectionGuardDetector.GuardHit
import com.sadd.blocker.core.ProtectionGuardDetector.GuardSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionGuardDetectorTest {

    private val ownPackage = "com.sadd.blocker"
    private val settingsPackage = "com.android.settings"
    private val samsungSettingsPackage = "com.samsung.android.settings"
    private val installerPackage = "com.android.packageinstaller"
    private val samsungInstallerPackage = "com.samsung.android.packageinstaller"

    private fun signals(
        packageName: String,
        texts: List<String>,
        descriptions: List<String> = emptyList(),
    ): GuardSignals {
        return GuardSignals(
            packageName = packageName,
            texts = texts,
            contentDescriptions = descriptions,
        )
    }

    // ---------- APP_INFO ----------

    @Test
    fun appInfoHitSettingsPersian() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "حذف نصب", "توقف اجباری")),
            ownPackage,
        )
        assertEquals(GuardHit.APP_INFO, hit)
    }

    @Test
    fun appInfoHitSamsungSettingsEnglish() {
        val hit = ProtectionGuardDetector.detect(
            signals(samsungSettingsPackage, listOf("سد", "Uninstall", "Force stop")),
            ownPackage,
        )
        assertEquals(GuardHit.APP_INFO, hit)
    }

    @Test
    fun appInfoHitWithZwnjInActionLabel() {
        // «حذف‌نصب» با نیم‌فاصله هم باید مچ شود (canonical نیم‌فاصله را به فاصله تبدیل می‌کند).
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "حذف‌نصب")),
            ownPackage,
        )
        assertEquals(GuardHit.APP_INFO, hit)
    }

    @Test
    fun appInfoHitFromContentDescription() {
        val hit = ProtectionGuardDetector.detect(
            signals(
                settingsPackage,
                texts = listOf("سد"),
                descriptions = listOf("حذف نصب"),
            ),
            ownPackage,
        )
        assertEquals(GuardHit.APP_INFO, hit)
    }

    // ---------- UNINSTALL_DIALOG ----------

    @Test
    fun uninstallDialogHit() {
        val hit = ProtectionGuardDetector.detect(
            signals(installerPackage, listOf("سد", "حذف نصب", "تأیید")),
            ownPackage,
        )
        assertEquals(GuardHit.UNINSTALL_DIALOG, hit)
    }

    @Test
    fun uninstallDialogHitSamsungInstallerEnglish() {
        val hit = ProtectionGuardDetector.detect(
            signals(samsungInstallerPackage, listOf("سد", "Uninstall this app?", "Cancel")),
            ownPackage,
        )
        assertEquals(GuardHit.UNINSTALL_DIALOG, hit)
    }

    // ---------- FORCE_STOP_DIALOG ----------

    @Test
    fun forceStopDialogHitPersian() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "توقف اجباری", "تأیید", "لغو")),
            ownPackage,
        )
        assertEquals(GuardHit.FORCE_STOP_DIALOG, hit)
    }

    @Test
    fun forceStopDialogHitEnglish() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "Force stop", "OK", "Cancel")),
            ownPackage,
        )
        assertEquals(GuardHit.FORCE_STOP_DIALOG, hit)
    }

    @Test
    fun forceStopWithoutDialogSignalFallsBackToAppInfo() {
        // بدون دکمه دیالوگ، همان صفحه اطلاعات برنامه است، نه دیالوگ تایید.
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "توقف اجباری")),
            ownPackage,
        )
        assertEquals(GuardHit.APP_INFO, hit)
    }

    // ---------- منفی‌ها ----------

    @Test
    fun ownPackageNeverMatches() {
        val hit = ProtectionGuardDetector.detect(
            signals(ownPackage, listOf("سد", "حذف نصب", "توقف اجباری", "تأیید")),
            ownPackage,
        )
        assertNull(hit)
    }

    @Test
    fun appNameAloneNeverMatches() {
        assertNull(
            ProtectionGuardDetector.detect(
                signals(settingsPackage, listOf("سد")),
                ownPackage,
            ),
        )
        assertNull(
            ProtectionGuardDetector.detect(
                signals(installerPackage, listOf("سد", "تأیید")),
                ownPackage,
            ),
        )
    }

    @Test
    fun actionWithoutAppNameNeverMatches() {
        assertNull(
            ProtectionGuardDetector.detect(
                signals(settingsPackage, listOf("حذف نصب", "توقف اجباری")),
                ownPackage,
            ),
        )
    }

    @Test
    fun wrongPackageNeverMatches() {
        // همان متن‌ها در یک اپ نامربوط (مثلاً مرورگر) نباید مچ بسازند.
        assertNull(
            ProtectionGuardDetector.detect(
                signals("com.android.chrome", listOf("سد", "حذف نصب", "تأیید")),
                ownPackage,
            ),
        )
        assertNull(
            ProtectionGuardDetector.detect(
                signals("com.android.settings", listOf("سد", "حذف نصب"))
                    .copy(packageName = "com.example.other"),
                ownPackage,
            ),
        )
    }

    @Test
    fun installerWithoutUninstallMarkerNeverMatches() {
        assertNull(
            ProtectionGuardDetector.detect(
                signals(installerPackage, listOf("سد", "تأیید", "لغو")),
                ownPackage,
            ),
        )
    }

    @Test
    fun settingsUninstallTextIsAppInfoNotDialog() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "حذف نصب")),
            ownPackage,
        )
        assertEquals(GuardHit.APP_INFO, hit)
    }

    // ---------- پنجره بازگشایی ----------

    @Test
    fun unlockWindowLengthIsFiveMinutes() {
        assertEquals(5 * 60 * 1000L, ProtectionGuardDetector.PROTECTION_UNLOCK_WINDOW_MS)
    }

    @Test
    fun unlockWindowActiveWhileUntilIsInFuture() {
        assertTrue(ProtectionGuardDetector.isGuardUnlockWindowActive(nowMs = 1_000L, unlockUntilMs = 2_000L))
    }

    @Test
    fun unlockWindowInactiveAtOrAfterExpiry() {
        assertFalse(ProtectionGuardDetector.isGuardUnlockWindowActive(nowMs = 2_000L, unlockUntilMs = 2_000L))
        assertFalse(ProtectionGuardDetector.isGuardUnlockWindowActive(nowMs = 2_001L, unlockUntilMs = 2_000L))
    }

    @Test
    fun unlockWindowInactiveWhenNeverSet() {
        assertFalse(ProtectionGuardDetector.isGuardUnlockWindowActive(nowMs = 1_000L, unlockUntilMs = 0L))
    }
}
