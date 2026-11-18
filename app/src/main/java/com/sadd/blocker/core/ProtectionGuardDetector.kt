package com.sadd.blocker.core

/**
 * نگهبان حذف نصب / توقف اجباری — تشخیص خالص، بدون وابستگی اندروید.
 *
 * وقتی محافظت از حذف نصب روشن است، سرویس Accessibility روی تغییر پنجره
 * متن‌های صفحه را می‌خواند و اینجا تصمیم می‌گیرد که آیا صفحه‌ای که باز
 * شده، صفحه اطلاعات برنامه «سد» در تنظیمات، دیالوگ حذف نصب، دیالوگ
 * توقف اجباری، فهرست «Device admin apps» یا دیالوگ غیرفعال‌کردن مدیر
 * دستگاه است یا نه. تطابق فقط روی سیگنال‌های محتوایی (متن + توضیح
 * محتوایی گره‌ها) انجام می‌شود، نه فقط روی نام پکیج — چون نام پکیج
 * تنظیمات در سامسونگ و بقیه سازنده‌ها فرق می‌کند.
 *
 * همه مقایسه‌ها با [PersianNormalizer.canonical] انجام می‌شود (ي/ك عربی
 * به ی/ک، حذف اعراب، نیم‌فاصله با فاصله هم‌ارز) تا «حذف‌نصب» و «حذف نصب»
 * و «حذف نصب» با ي عربی یکی شمرده شوند.
 *
 * قوانین سخت:
 * - نام اپ («سد») به‌تنهایی هرگز مچ نمی‌سازد؛ همیشه باید عبارت اقدام
 *   (حذف نصب / توقف اجباری / ...) هم در متن باشد.
 * - وقتی پکیج پیش‌زمینه خودِ «سد» است، هرگز مچ نمی‌سازد — متن خود اپ
 *   (مثلاً صفحه محافظت که «حذف نصب» را توضیح می‌دهد) نباید نگهبان را
 *   فعال کند.
 *
 * محدودیت صادقانه (بهترین تلاش، نه قفل قطعی):
 * - نگهبان فقط وقتی Accessibility روشن است کار می‌کند؛ اگر سرویس خاموش
 *   یا کشته شده باشد، هیچ چیزی جلوی حذف نصب را نمی‌گیرد.
 * - مسابقه زمانی (race): پرده رهگیری ضربه روی دکمه زیرین را می‌خورد، ولی
 *   اگر کاربر خیلی سریع بزند — قبل از اینکه سرویس اصلاً رویداد را ببیند —
 *   ممکن است ضربه ثبت شود.
 * - صفحه غیرفعال‌سازی مدیر دستگاه رهگیری می‌شود (فهرست و دیالوگ تاییدش)،
 *   ولی خود صفحه سیستمی را نمی‌توان قفل کرد؛ و صفحه غیرفعال‌کردن خود
 *   سرویس Accessibility عمداً گیت نمی‌شود — فقط هشدار غیرفعال‌شدن آن
 *   گزارش می‌شود. یک اپ معمولی نمی‌تواند آن صفحه‌ها را مسدود کند.
 */
object ProtectionGuardDetector {

    /** پکیج‌های تنظیمات سیستم (AOSP و سامسونگ). */
    val SETTINGS_PACKAGES: Set<String> = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
    )

    /** پکیج‌های نصب‌کننده/حذف‌کننده پکیج (AOSP، گوگل، سامسونگ). */
    val PACKAGE_INSTALLER_PACKAGES: Set<String> = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
    )

    /** لانچرها — حذف نصب با نگه‌داشتن آیکون از اینجا شروع می‌شود. */
    val LAUNCHER_PACKAGES: Set<String> = setOf(
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.oneplus.launcher",
        "com.samsung.android.app.launcher",
    )

    /** آیا پکیج پیش‌زمینه یکی از جاهایی است که نگهبان باید آن را ببیند؟ */
    fun isGuardedPackage(packageName: String): Boolean {
        return SETTINGS_PACKAGES.contains(packageName) ||
            PACKAGE_INSTALLER_PACKAGES.contains(packageName) ||
            isLauncherPackage(packageName)
    }

    /** لانچر با نام صریح، یا هر پکیجی که «launcher» در نامش هست. */
    fun isLauncherPackage(packageName: String): Boolean {
        if (LAUNCHER_PACKAGES.contains(packageName)) return true
        return packageName.endsWith(".launcher") || packageName.contains(".launcher.")
    }

    /** مدت پنجره بازگشایی محافظت بعد از بازگشایی Strict: ۵ دقیقه. */
    const val PROTECTION_UNLOCK_WINDOW_MS = 5 * 60 * 1000L

    /** سیگنال‌های خام جمع‌شده از درخت Accessibility یک پنجره. */
    data class GuardSignals(
        val packageName: String,
        val texts: List<String> = emptyList(),
        val contentDescriptions: List<String> = emptyList(),
        val windowClassName: String? = null,
    )

    /** نوع صفحه محافظت‌شده‌ای که تشخیص داده شد. */
    enum class GuardHit {
        /** صفحه اطلاعات برنامه «سد» در تنظیمات (دکمه حذف نصب/توقف اجباری دیده می‌شود). */
        APP_INFO,

        /** دیالوگ تایید حذف نصب. */
        UNINSTALL_DIALOG,

        /** دیالوگ تایید توقف اجباری. */
        FORCE_STOP_DIALOG,

        /** فهرست «Device admin apps» در تنظیمات (ورودی «سد» دیده می‌شود). */
        DEVICE_ADMIN_LIST,

        /** دیالوگ تایید غیرفعال‌کردن مدیر دستگاه. */
        DEACTIVATE_ADMIN_DIALOG,
    }

    private val APP_NAME_MARKERS = listOf("سد")
    private val UNINSTALL_MARKERS = listOf("حذف نصب", "uninstall")
    private val FORCE_STOP_MARKERS = listOf("توقف اجباری", "force stop", "توقف")
    private val APP_INFO_ACTION_MARKERS = listOf("حذف نصب", "توقف اجباری", "uninstall", "force stop")
    private val DIALOG_BUTTON_MARKERS = listOf("تأیید", "لغو", "ok", "cancel")
    /** عبارت‌های غیرفعال‌کردن مدیر دستگاه — «غیرفعال» تنها کافی نیست. */
    private val DEACTIVATE_MARKERS =
        listOf("غیرفعال‌کردن", "غیرفعال کردن", "غیرفعال‌سازی", "deactivate", "deactivation")
    private val DEVICE_ADMIN_MARKERS =
        listOf("مدیر دستگاه", "device admin", "device administrator")

    private fun canonical(value: String?): String {
        if (value == null) return ""
        return PersianNormalizer.canonical(value)
    }

    private fun anyContains(values: List<String>, markers: List<String>): Boolean {
        for (value in values) {
            val text = canonical(value)
            if (text.isEmpty()) continue
            for (marker in markers) {
                if (text.contains(canonical(marker))) return true
            }
        }
        return false
    }

    private fun allSources(signals: GuardSignals): List<String> {
        val sources = ArrayList<String>(signals.texts.size + signals.contentDescriptions.size + 1)
        sources.addAll(signals.texts)
        sources.addAll(signals.contentDescriptions)
        if (signals.windowClassName != null) sources.add(signals.windowClassName)
        return sources
    }

    /**
     * آیا صفحه فعلی یکی از صفحه‌های محافظت‌شده است؟ null یعنی نیست.
     * [ownPackageName] پکیج خود «سد» است؛ اگر پکیج پیش‌زمینه همان باشد،
     * همیشه null برمی‌گردد (متن خود اپ هرگز نگهبان را فعال نمی‌کند).
     *
     * قانون نام اپ: نصب‌کننده و لانچر برای همه اپ‌ها دیالوگ نشان می‌دهند،
     * پس آنجا نام «سد» الزامی است؛ وگرنه حذف نصب هر اپ دیگری هم نگهبان را
     * فعال می‌کرد. در تنظیمات هم دیالوگ حذف نصب/توقف اجباری نام می‌خواهد —
     * صفحه اطلاعات برنامه (که نام را همیشه نشان می‌دهد) گلوگاه است و
     * کاربر بدون گذشتن از آن به دکمه حذف نصب نمی‌رسد. فقط دیالوگ
     * غیرفعال‌سازی مدیر دستگاه استثناست: حتی بدون نام، عبارت «مدیر دستگاه»
     * کنار «غیرفعال‌کردن» کافی است — چون آن دیالوگ فقط برای مدیر دستگاه
     * فعال باز می‌شود. «غیرفعال» تنها (بدون نام و بدون «مدیر دستگاه») هرگز
     * کافی نیست، وگرنه دیالوگ «غیرفعال کردن برنامه» برای هر اپ دیگری هم
     * مچ می‌ساخت.
     */
    fun detect(signals: GuardSignals, ownPackageName: String): GuardHit? {
        if (signals.packageName == ownPackageName) return null
        val sources = allSources(signals)
        // نام اپ به‌تنهایی کافی نیست؛ بدون آن هم هیچ مچی ساخته نمی‌شود —
        // مگر دیالوگ غیرفعال‌سازی مدیر دستگاه (پایین).
        val hasName = anyContains(sources, APP_NAME_MARKERS)
        val pkg = signals.packageName
        val hasDialogButtons = anyContains(sources, DIALOG_BUTTON_MARKERS)
        if (PACKAGE_INSTALLER_PACKAGES.contains(pkg)) {
            if (hasName && anyContains(sources, UNINSTALL_MARKERS)) return GuardHit.UNINSTALL_DIALOG
            return null
        }
        if (isLauncherPackage(pkg)) {
            if (hasName && anyContains(sources, UNINSTALL_MARKERS)) return GuardHit.UNINSTALL_DIALOG
            return null
        }
        if (SETTINGS_PACKAGES.contains(pkg)) {
            // اول غیرفعال‌سازی مدیر دستگاه: پنجره میانی «این برنامه مدیر
            // دستگاه است» هم «حذف نصب» و هم «غیرفعال» دارد — اگر اول حذف
            // نصب چک می‌شد، اشتباه برمی‌گشت.
            if (anyContains(sources, DEACTIVATE_MARKERS) &&
                (hasName || anyContains(sources, DEVICE_ADMIN_MARKERS))
            ) {
                return GuardHit.DEACTIVATE_ADMIN_DIALOG
            }
            if (anyContains(sources, DEVICE_ADMIN_MARKERS) && hasName) {
                return GuardHit.DEVICE_ADMIN_LIST
            }
            if (hasName && anyContains(sources, FORCE_STOP_MARKERS) && hasDialogButtons) {
                return GuardHit.FORCE_STOP_DIALOG
            }
            if (hasName && anyContains(sources, UNINSTALL_MARKERS) && hasDialogButtons) {
                return GuardHit.UNINSTALL_DIALOG
            }
            if (hasName && anyContains(sources, APP_INFO_ACTION_MARKERS)) return GuardHit.APP_INFO
            return null
        }
        return null
    }

    /**
     * آیا پنجره بازگشایی محافظت هنوز فعال است؟ یعنی زمان پایانِ ذخیره‌شده
     * از «الان» جلوتر باشد. ۰ یا گذشته یعنی پنجره بسته است.
     */
    fun isGuardUnlockWindowActive(nowMs: Long, unlockUntilMs: Long): Boolean {
        return unlockUntilMs > nowMs
    }
}
