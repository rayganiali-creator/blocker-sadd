package com.sadd.blocker.domain

import com.sadd.blocker.core.PersianNormalizer

/**
 * قفل بازگشایی Strict («واقعاً سخت‌گیرانه») — منطق خالص، بدون وابستگی اندروید.
 *
 * دو روش بازگشایی که کاربر در ModesScreen انتخاب می‌کند (پیش‌فرض PASSAGE):
 * - PASSAGE: تایپ دقیق یک متن طولانی از بانک [STRICT_PASSAGES].
 * - COOLDOWN: ثبت درخواست با زمان دیواری و صبر کردن کامل مدت انتخاب‌شده
 *   (۱۲/۲۴/۴۸ ساعت)؛ درخواست در DataStore می‌ماند و با ری‌استارت پاک نمی‌شود.
 *
 * مقایسه متن: [PersianNormalizer.canonical] — یعنی ي/ك عربی به ی/ک،
 * اعراب حذف، علائم نگارشی به فاصله، collapse فاصله و trim؛ نیم‌فاصله هم
 * با فاصله هم‌ارز شمرده می‌شود تا تایپ با کیبورد فارسی (که نیم‌فاصله‌اش
 * با فاصله فرق می‌کند) بی‌جهت رد نشود.
 */
enum class StrictUnlockMethod {
    PASSAGE,
    COOLDOWN,
}

/** کاری که درخواست انتظار (cooldown) برای آن ثبت شده است. */
enum class StrictPendingKind {
    /** پایان سشن Strict. */
    END_SESSION,
    /** تغییر روش بازگشایی؛ pendingValue نام StrictUnlockMethod است. */
    METHOD,
    /** تغییر مدت انتظار؛ pendingValue عدد ساعت است. */
    HOURS,
    /** تغییر قانون (غیرفعال/حذف/تاخیر)؛ pendingValue کدگذاری StrictRuleChange است. */
    RULE_CHANGE,
}

/**
 * تغییر قانونِ در انتظار بازگشایی Strict — کدگذاری pendingValue برای RULE_CHANGE.
 * قالب: "scope|action|id|value"؛ برای بخش‌های اپ id به‌صورت "package#sectionKey" است.
 * scopeها: keyword/app/website/section/schedule؛ actionها: toggle/remove/delay؛
 * value برای toggle «true»/«false»، برای delay عدد ثانیه یا «null»، برای remove خالی است.
 * کدگذاری در DataStore می‌ماند تا اگر اپ وسط انتظار بسته شد، بعد از پایان
 * انتظار همان تغییرِ ثبت‌شده اعمال شود، نه تغییر دیگری.
 */
data class StrictRuleChange(
    val scope: String,
    val action: String,
    val id: String,
    val value: String,
) {
    companion object {
        const val SCOPE_KEYWORD = "keyword"
        const val SCOPE_APP = "app"
        const val SCOPE_WEBSITE = "website"
        const val SCOPE_SECTION = "section"
        const val SCOPE_SCHEDULE = "schedule"
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_REMOVE = "remove"
        const val ACTION_DELAY = "delay"

        fun toggle(scope: String, id: String, enabled: Boolean) =
            StrictRuleChange(scope, ACTION_TOGGLE, id, enabled.toString())

        fun remove(scope: String, id: String) =
            StrictRuleChange(scope, ACTION_REMOVE, id, "")

        fun delay(scope: String, id: String, delaySeconds: Int?) =
            StrictRuleChange(scope, ACTION_DELAY, id, delaySeconds?.toString() ?: "null")

        fun encode(change: StrictRuleChange): String =
            listOf(change.scope, change.action, change.id, change.value).joinToString("|")

        fun decode(value: String): StrictRuleChange? {
            val parts = value.split("|")
            if (parts.size != 4) return null
            if (parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) return null
            return StrictRuleChange(parts[0], parts[1], parts[2], parts[3])
        }
    }
}

/**
 * درخواست انتظار ثبت‌شده (در DataStore).
 * [requestedAtMs] زمان دیواری (System.currentTimeMillis) ثبت درخواست است؛
 * [durationHours] مدت انتظاری است که موقع ثبت در تنظیمات بوده و تا پایان
 * همین درخواست ثابت می‌ماند.
 */
data class StrictCooldownRequest(
    val requestedAtMs: Long,
    val pendingKind: StrictPendingKind,
    val pendingValue: String = "",
    val durationHours: Int,
) {
    /** روش درخواستی، فقط وقتی pendingKind == METHOD است. */
    val pendingMethod: StrictUnlockMethod?
        get() {
            if (pendingKind != StrictPendingKind.METHOD) return null
            return runCatching { StrictUnlockMethod.valueOf(pendingValue) }.getOrNull()
        }

    /** مدت درخواستی (ساعت)، فقط وقتی pendingKind == HOURS است. */
    val pendingHours: Int?
        get() {
            if (pendingKind != StrictPendingKind.HOURS) return null
            return pendingValue.toIntOrNull()
        }

    /** تغییر قانون درخواستی، فقط وقتی pendingKind == RULE_CHANGE است. */
    val pendingRuleChange: StrictRuleChange?
        get() {
            if (pendingKind != StrictPendingKind.RULE_CHANGE) return null
            return StrictRuleChange.decode(pendingValue)
        }
}

object StrictUnlock {

    /** یک ساعت به میلی‌ثانیه. */
    const val HOUR_MS = 3_600_000L

    /** مدت انتظار پیش‌فرض (ساعت). */
    const val DEFAULT_COOLDOWN_HOURS = 24

    /** گزینه‌های مدت انتظار (ساعت). */
    val COOLDOWN_HOUR_OPTIONS = listOf(12, 24, 48)

    /**
     * آیا متن تایپ‌شده دقیقاً با متن مطابقت دارد؟ (نرمال‌شده + trim +
     * collapse فاصله؛ نیم‌فاصله با فاصله هم‌ارز است.)
     */
    fun isPassageMatch(typed: String, passage: String): Boolean {
        val expected = PersianNormalizer.canonical(passage)
        if (expected.isEmpty()) return false
        return PersianNormalizer.canonical(typed) == expected
    }

    /** آیا متن تایپ‌شده پیشوندی از متن است؟ (برای پیام «هنوز کامل نیست».) */
    fun isPassagePrefix(typed: String, passage: String): Boolean {
        val expected = PersianNormalizer.canonical(passage)
        val actual = PersianNormalizer.canonical(typed)
        return expected.startsWith(actual)
    }

    /**
     * پیشرفت تایپ: طول بلندترین پیشوند مشترک متن تایپ‌شده با متن،
     * تقسیم بر طول متن (۰ تا ۱). برای نوار پیشرفت دیالوگ.
     */
    fun passageProgress(typed: String, passage: String): Float {
        val expected = PersianNormalizer.canonical(passage)
        if (expected.isEmpty()) return 0f
        val actual = PersianNormalizer.canonical(typed)
        var common = 0
        val limit = minOf(actual.length, expected.length)
        while (common < limit && actual[common] == expected[common]) {
            common++
        }
        return common.toFloat() / expected.length.toFloat()
    }

    /** میلی‌ثانیه باقی‌مانده تا پایان انتظار؛ ۰ یعنی انتظار تمام شده است. */
    fun cooldownRemainingMs(request: StrictCooldownRequest, nowMs: Long): Long {
        if (request.requestedAtMs <= 0L) return request.durationHours * HOUR_MS
        val remaining = request.requestedAtMs + request.durationHours * HOUR_MS - nowMs
        return remaining.coerceAtLeast(0L)
    }

    /** آیا مدت انتظار کامل گذشته است؟ دقیقاً در لحظه پایان هم true است. */
    fun isCooldownElapsed(request: StrictCooldownRequest, nowMs: Long): Boolean {
        return request.requestedAtMs > 0L && cooldownRemainingMs(request, nowMs) == 0L
    }

    /** باقی‌مانده به‌صورت HH:MM:SS (ساعت می‌تواند بیش از ۲۴ باشد). */
    fun formatCooldownRemaining(remainingMs: Long): String {
        return TimeFormat.formatRemainingMs(remainingMs)
    }

    /** شماره یک متن تصادفی از بانک، متفاوت با [except] (اگر ممکن باشد). */
    fun randomPassageIndex(except: Int = -1): Int {
        if (STRICT_PASSAGES.size <= 1) return 0
        var next = except
        while (next == except) {
            next = STRICT_PASSAGES.indices.random()
        }
        return next
    }
}
