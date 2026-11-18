package com.sadd.blocker.core

/**
 * نگهبان حذف نصب / توقف اجباری — تشخیص خالص، بدون وابستگی اندروید.
 *
 * وقتی محافظت از حذف نصب روشن است، سرویس Accessibility روی تغییر پنجره
 * متن‌های صفحه را می‌خواند و اینجا تصمیم می‌گیرد که آیا صفحه‌ای که باز
 * شده، صفحه اطلاعات برنامه «سد» در تنظیمات، دیالوگ حذف نصب، یا دیالوگ
 * توقف اجباری است یا نه. تطابق فقط روی سیگنال‌های محتوایی (متن + توضیح
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
 * - مسابقه زمانی (race): اگر کاربر خیلی سریع روی «حذف نصب»/«تایید» بزند،
 *   ممکن است قبل از BACK نگهبان، ضربه ثبت شود.
 * - صفحه غیرفعال‌سازی مدیر دستگاه و صفحه غیرفعال‌کردن خود سرویس
 *   Accessibility عمداً گیت نمی‌شوند — فقط هشدار غیرفعال‌شدن (که از قبل
 *   هست) آن‌ها را گزارش می‌کند. یک اپ معمولی نمی‌تواند آن صفحه‌ها را
 *   مسدود کند.
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
    }

    private val APP_NAME_MARKERS = listOf("سد")
    private val UNINSTALL_MARKERS = listOf("حذف نصب", "uninstall")
    private val FORCE_STOP_MARKERS = listOf("توقف اجباری", "force stop", "توقف")
    private val APP_INFO_ACTION_MARKERS = listOf("حذف نصب", "توقف اجباری", "uninstall", "force stop")
    private val DIALOG_BUTTON_MARKERS = listOf("تأیید", "لغو", "ok", "cancel")

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
     */
    fun detect(signals: GuardSignals, ownPackageName: String): GuardHit? {
        if (signals.packageName == ownPackageName) return null
        val sources = allSources(signals)
        // نام اپ به‌تنهایی کافی نیست؛ بدون آن هم هیچ مچی ساخته نمی‌شود.
        if (!anyContains(sources, APP_NAME_MARKERS)) return null
        val pkg = signals.packageName
        if (PACKAGE_INSTALLER_PACKAGES.contains(pkg)) {
            if (anyContains(sources, UNINSTALL_MARKERS)) return GuardHit.UNINSTALL_DIALOG
            return null
        }
        if (SETTINGS_PACKAGES.contains(pkg)) {
            if (anyContains(sources, FORCE_STOP_MARKERS) &&
                anyContains(sources, DIALOG_BUTTON_MARKERS)
            ) {
                return GuardHit.FORCE_STOP_DIALOG
            }
            if (anyContains(sources, APP_INFO_ACTION_MARKERS)) return GuardHit.APP_INFO
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
