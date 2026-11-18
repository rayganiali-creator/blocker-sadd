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

    // ---------- DEVICE_ADMIN_LIST ----------

    @Test
    fun deviceAdminListHitPersian() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("Device admin apps", "سد", "مدیر دستگاه")),
            ownPackage,
        )
        assertEquals(GuardHit.DEVICE_ADMIN_LIST, hit)
    }

    @Test
    fun deviceAdminListHitSamsungSettingsEnglish() {
        val hit = ProtectionGuardDetector.detect(
            signals(samsungSettingsPackage, listOf("Device admin apps", "سد")),
            ownPackage,
        )
        assertEquals(GuardHit.DEVICE_ADMIN_LIST, hit)
    }

    @Test
    fun deviceAdminListWithoutAppNameNeverMatches() {
        // فهرست Device admin apps بدون ورودی «سد» نباید مچ بسازد.
        assertNull(
            ProtectionGuardDetector.detect(
                signals(settingsPackage, listOf("Device admin apps", "مدیر دستگاه")),
                ownPackage,
            ),
        )
    }

    @Test
    fun securityPageWithDeviceAdminRowButNoAppNeverMatches() {
        // صفحه امنیت فقط ردیف «Device admin apps» را دارد، نه ورودی «سد».
        assertNull(
            ProtectionGuardDetector.detect(
                signals(settingsPackage, listOf("امنیت", "Device admin apps", "قفل صفحه")),
                ownPackage,
            ),
        )
    }

    // ---------- DEACTIVATE_ADMIN_DIALOG ----------

    @Test
    fun deactivateAdminDialogHitPersian() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "غیرفعال‌کردن مدیر دستگاه", "تأیید", "لغو")),
            ownPackage,
        )
        assertEquals(GuardHit.DEACTIVATE_ADMIN_DIALOG, hit)
    }

    @Test
    fun deactivateAdminDialogHitEnglish() {
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("سد", "Deactivate device administrator?", "Deactivate", "Cancel")),
            ownPackage,
        )
        assertEquals(GuardHit.DEACTIVATE_ADMIN_DIALOG, hit)
    }

    @Test
    fun deactivateAdminDialogHitWithoutAppNameButWithDeviceAdminMarker() {
        // دیالوگ غیرفعال‌سازی حتی بدون نام اپ هم مچ می‌سازد، چون «مدیر دستگاه» هست.
        val hit = ProtectionGuardDetector.detect(
            signals(settingsPackage, listOf("غیرفعال‌کردن مدیر دستگاه", "تأیید", "لغو")),
            ownPackage,
        )
        assertEquals(GuardHit.DEACTIVATE_ADMIN_DIALOG, hit)
    }

    @Test
    fun deactivateInterstitialBeatsUninstall() {
        // پنجره میانی «این برنامه مدیر دستگاه است» هم «حذف نصب» دارد و هم
        // «غیرفعال» — باید غیرفعال‌سازی برگردد، نه حذف نصب.
        val hit = ProtectionGuardDetector.detect(
            signals(
                settingsPackage,
                listOf("سد", "این برنامه مدیر دستگاه است", "برای حذف نصب، اول آن را غیرفعال کن", "غیرفعال کردن", "لغو"),
            ),
            ownPackage,
        )
        assertEquals(GuardHit.DEACTIVATE_ADMIN_DIALOG, hit)
    }

    @Test
    fun disableAppDialogForAnotherAppNeverMatches() {
        // دیالوگ «غیرفعال کردن برنامه» برای اپ دیگر: نه نام هست، نه «مدیر دستگاه».
        assertNull(
            ProtectionGuardDetector.detect(
                signals(settingsPackage, listOf("غیرفعال کردن برنامه", "تأیید", "لغو")),
                ownPackage,
            ),
        )
    }

    // ---------- LAUNCHER ----------

    @Test
    fun launcherUninstallHitSamsungPersian() {
        val hit = ProtectionGuardDetector.detect(
            signals("com.sec.android.app.launcher", listOf("سد", "حذف نصب", "تأیید", "لغو")),
            ownPackage,
        )
        assertEquals(GuardHit.UNINSTALL_DIALOG, hit)
    }

    @Test
    fun launcherUninstallHitEnglish() {
        val hit = ProtectionGuardDetector.detect(
            signals("com.google.android.apps.nexuslauncher", listOf("سد", "Uninstall", "OK", "Cancel")),
            ownPackage,
        )
        assertEquals(GuardHit.UNINSTALL_DIALOG, hit)
    }

    @Test
    fun launcherUninstallWithoutAppNameNeverMatches() {
        // لانچر برای همه اپ‌ها دیالوگ نشان می‌دهد؛ بدون نام «سد» نباید مچ بسازد.
        assertNull(
            ProtectionGuardDetector.detect(
                signals("com.sec.android.app.launcher", listOf("حذف نصب", "تأیید", "لغو")),
                ownPackage,
            ),
        )
    }

    @Test
    fun installerDialogWithoutAppNameNeverMatches() {
        assertNull(
            ProtectionGuardDetector.detect(
                signals(installerPackage, listOf("حذف نصب", "تأیید", "لغو")),
                ownPackage,
            ),
        )
    }

    // ---------- پکیج‌های تحت پوشش ----------

    @Test
    fun guardedPackagesCoverSettingsInstallerLauncher() {
        assertTrue(ProtectionGuardDetector.isGuardedPackage(settingsPackage))
        assertTrue(ProtectionGuardDetector.isGuardedPackage(samsungSettingsPackage))
        assertTrue(ProtectionGuardDetector.isGuardedPackage(installerPackage))
        assertTrue(ProtectionGuardDetector.isGuardedPackage(samsungInstallerPackage))
        assertTrue(ProtectionGuardDetector.isGuardedPackage("com.sec.android.app.launcher"))
        assertFalse(ProtectionGuardDetector.isGuardedPackage("com.android.chrome"))
        assertFalse(ProtectionGuardDetector.isGuardedPackage(ownPackage))
    }

    @Test
    fun launcherHeuristicMatchesUnknownLaunchers() {
        assertTrue(ProtectionGuardDetector.isLauncherPackage("com.example.foo.launcher"))
        assertTrue(ProtectionGuardDetector.isLauncherPackage("com.example.launcher"))
        assertFalse(ProtectionGuardDetector.isLauncherPackage("com.android.settings"))
    }
}
