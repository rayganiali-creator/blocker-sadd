package com.sadd.blocker.domain.model

/** اپ بلاک‌شده. */
data class BlockedApp(
    val packageName: String,
    val label: String,
    val enabled: Boolean = true,
    /** تاخیر این قانون؛ null یعنی تاخیر سراسری. */
    val delaySeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** وب‌سایت بلاک‌شده (host نرمال‌شده، بدون scheme/www). */
data class BlockedWebsite(
    val id: Long = 0,
    val host: String,
    val matchSubdomains: Boolean = true,
    val enabled: Boolean = true,
    val delaySeconds: Int? = null,
)

/** کلمه/عبارت ممنوعه. */
data class KeywordRule(
    val id: Long = 0,
    val rawText: String,
    val normalizedText: String,
    val category: String = "عمومی",
    val isPhrase: Boolean = false,
    val enabled: Boolean = true,
    val delaySeconds: Int? = null,
)

/** سشن بلاک. باقی‌مانده از endEpochMs محاسبه می‌شود، نه تایمر. */
data class BlockingSession(
    val id: Long = 0,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val strict: Boolean = false,
    val ruleScope: String = "all",
    val status: String = "active",
    val endedReason: String? = null,
    /**
     * مقدار `SystemClock.elapsedRealtime()` موقع شروع سشن (H-6: راستی‌آزمایی
     * در برابر عقب کشیدن ساعت سیستم). ۰ یعنی نامشخص (سشن‌های قدیمی).
     */
    val startElapsedRealtimeMs: Long = 0,
)

/** زمان‌بندی تکرارشونده. */
data class Schedule(
    val id: Long = 0,
    val daysOfWeek: String = "", // مثل "SAT,SUN,MON"
    val startMinute: Int = 0,    // دقیقه از نیمه‌شب
    val endMinute: Int = 0,
    val strict: Boolean = false,
    val ruleScope: String = "all",
    val enabled: Boolean = true,
)

/** تاخیرهای مجاز (ثانیه). */
object DelayOptions {
    val VALUES = listOf(0, 3, 5, 10, 15)
    /** پیش‌فرض تاخیر: فوری (۰ ثانیه) — بلاک همان لحظه اتفاق می‌افتد. */
    const val DEFAULT_SECONDS = 0
}

/** حالت بلاک — در DataStore به‌صورت رشته ذخیره می‌شود. */
enum class BlockMode {
    NORMAL,
    STRICT,
}

/** مدت‌های آماده سشن در داشبورد (میلی‌ثانیه). */
object SessionDurations {
    val OPTIONS = listOf(
        30L * 60L * 1000L,
        60L * 60L * 1000L,
        3L * 60L * 60L * 1000L,
        8L * 60L * 60L * 1000L,
    )

    /** مرزهای مجاز مدت دلخواه (۳۰ ثانیه تا ۹۰ روز) — L-11. */
    const val MIN_MS = 30_000L
    const val MAX_MS = 90L * 24L * 60L * 60L * 1000L
}
