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
    /** خاموش‌کردن محافظت از حذف نصب؛ pendingValue خالی است. */
    PROTECTION,
    /**
     * بازگشایی پنجره ۵دقیقه‌ای نگهبان حذف نصب/توقف اجباری؛ pendingValue خالی است.
     * جدا از PROTECTION است: PROTECTION یعنی «محافظت را خاموش کن»، GUARD_UNLOCK
     * یعنی «۵ دقیقه بگذار از صفحه حذف نصب/توقف اجباری رد شوم» و محافظت روشن می‌ماند.
     * این درخواست را ModesScreen اعمال نمی‌کند؛ فقط BlockedActivity (حالت guard) است
     * که بعد از بازگشایی، پنجره را تنظیم می‌کند.
     */
    GUARD_UNLOCK,
    /**
     * تغییر یک تنظیم (حالت بلاک، تم، رنگ‌بندی)؛ pendingValue
     * کدگذاری [StrictSettingChange] است («key|value»). انتظارِ ثبت‌شده
     * برای یک تنظیم، فقط همان تنظیم را باز می‌کند — تطابق با key و value
     * کامل چک می‌شود تا انتظار یک تنظیم خرج تنظیم دیگری نشود.
     * (تاخیر سراسری تنظیم است ولی از مسیر RULE_CHANGE با scope «global»
     * گیت می‌شود، چون تغییر تاخیر مثل تغییر قانونِ بلاک است.)
     */
    SETTING_CHANGE,
    /** تمدید سشن Strict به‌مدت ۱۵ دقیقه؛ pendingValue خالی است. */
    EXTEND_SESSION,
    /** افزودن زمان‌بندی جدید؛ pendingValue کدگذاری [StrictScheduleAdd] است. */
    SCHEDULE_ADD,
    /** ریستور بکاپ (جایگزینی قوانین)؛ pendingValue خالی است. */
    BACKUP_RESTORE,
    /** افزودن قانون جدید؛ pendingValue کدگذاری [StrictRuleAdd] است. */
    RULE_ADD,
}

/**
 * تغییر یک تنظیمِ در انتظار بازگشایی Strict — کدگذاری pendingValue برای
 * SETTING_CHANGE. قالب: "key|value"؛ key یکی از ثابت‌های زیر است.
 * value برای blockMode نام BlockMode، برای theme نام AppTheme و برای
 * colorTheme نام ColorTheme است.
 */
data class StrictSettingChange(
    val key: String,
    val value: String,
) {
    companion object {
        const val KEY_BLOCK_MODE = "blockMode"
        const val KEY_THEME = "theme"
        const val KEY_COLOR_THEME = "colorTheme"

        fun blockMode(modeName: String) = StrictSettingChange(KEY_BLOCK_MODE, modeName)
        fun theme(themeName: String) = StrictSettingChange(KEY_THEME, themeName)
        fun colorTheme(colorName: String) = StrictSettingChange(KEY_COLOR_THEME, colorName)

        fun encode(change: StrictSettingChange): String =
            "${change.key}|${change.value}"

        fun decode(value: String): StrictSettingChange? {
            val parts = value.split("|", limit = 2)
            if (parts.size != 2 || parts[0].isEmpty()) return null
            return StrictSettingChange(parts[0], parts[1])
        }
    }
}

/**
 * افزودن زمان‌بندیِ در انتظار بازگشایی Strict — کدگذاری pendingValue برای
 * SCHEDULE_ADD. قالب: "days|startMinute|endMinute|strict"؛ days همان
 * رشته ذخیره‌شده در Schedule.daysOfWeek است (مثل "SAT,SUN").
 */
data class StrictScheduleAdd(
    val daysOfWeek: String,
    val startMinute: Int,
    val endMinute: Int,
    val strict: Boolean,
) {
    companion object {
        fun encode(add: StrictScheduleAdd): String =
            listOf(add.daysOfWeek, add.startMinute.toString(), add.endMinute.toString(), add.strict.toString())
                .joinToString("|")

        fun decode(value: String): StrictScheduleAdd? {
            val parts = value.split("|")
            if (parts.size != 4) return null
            val start = parts[1].toIntOrNull() ?: return null
            val end = parts[2].toIntOrNull() ?: return null
            val strict = parts[3].toBooleanStrictOrNull() ?: return null
            return StrictScheduleAdd(parts[0], start, end, strict)
        }
    }
}

/**
 * افزودن قانونِ در انتظار بازگشایی Strict — کدگذاری pendingValue برای
 * RULE_ADD. قالب: "scope|id|value"؛ value ممکن است «|» داشته باشد
 * (مثلاً نام+نشانه بخش سفارشی)، پس decode با limit=3 جدا می‌کند.
 * scopeها مثل [StrictRuleChange] هستند؛ برای افزودن دسته‌ای اپ‌ها،
 * id خالی و value پکیج‌ها با «,» جدا شده‌اند.
 */
data class StrictRuleAdd(
    val scope: String,
    val id: String,
    val value: String,
) {
    companion object {
        fun encode(add: StrictRuleAdd): String =
            "${add.scope}|${add.id}|${add.value}"

        fun decode(value: String): StrictRuleAdd? {
            val parts = value.split("|", limit = 3)
            if (parts.size != 3 || parts[0].isEmpty()) return null
            return StrictRuleAdd(parts[0], parts[1], parts[2])
        }
    }
}

/**
 * تغییر قانونِ در انتظار بازگشایی Strict — کدگذاری pendingValue برای RULE_CHANGE.
 * قالب: "scope|action|id|value"؛ برای بخش‌های اپ id به‌صورت "package#sectionKey" است.
 * scopeها: keyword/app/website/section/schedule/global؛ actionها: toggle/remove/delay؛
 * scope «global» تاخیر سراسری است (id «global») و فقط با action «delay» می‌آید.
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
        /** تاخیر سراسری (تنظیم، نه قانونِ یک آیتم) — مال DelayViewModel. */
        const val SCOPE_GLOBAL = "global"
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

    /** تغییر تنظیم درخواستی، فقط وقتی pendingKind == SETTING_CHANGE است. */
    val pendingSettingChange: StrictSettingChange?
        get() {
            if (pendingKind != StrictPendingKind.SETTING_CHANGE) return null
            return StrictSettingChange.decode(pendingValue)
        }

    /** افزودن زمان‌بندی درخواستی، فقط وقتی pendingKind == SCHEDULE_ADD است. */
    val pendingScheduleAdd: StrictScheduleAdd?
        get() {
            if (pendingKind != StrictPendingKind.SCHEDULE_ADD) return null
            return StrictScheduleAdd.decode(pendingValue)
        }

    /** افزودن قانون درخواستی، فقط وقتی pendingKind == RULE_ADD است. */
    val pendingRuleAdd: StrictRuleAdd?
        get() {
            if (pendingKind != StrictPendingKind.RULE_ADD) return null
            return StrictRuleAdd.decode(pendingValue)
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

/**
 * مهاجرت یک‌باره تاخیر پیش‌فرض به فوری (۰ ثانیه).
 * تغییر DEFAULT_SECONDS در کد، نصب‌هایی که مقدار قدیمی (۵ ثانیه) در
 * DataStoreشان ذخیره شده را عوض نمی‌کند؛ پس اولین اجرای بعد از
 * به‌روزرسانی، اگر پرچم مهاجرت هنوز نخورده باشد، مقدار را ۰ می‌کند و
 * پرچم را می‌زند. بعد از آن، مقدار انتخاب‌شده کاربر دست نمی‌خورد.
 */
object DelayMigration {
    /** آیا مهاجرت هنوز انجام نشده و باید اجرا شود؟ */
    fun shouldMigrate(alreadyMigrated: Boolean): Boolean = !alreadyMigrated
}

/**
 * سیاست نگهبان حذف نصب/توقف اجباری: همیشه فوری عمل می‌کند.
 * نگهبان مسیر بلاک عادی را نمی‌رود (نه scheduleBlock و نه تاخیر قانون)؛
 * پرده رهگیری + BACK + صفحه بازگشایی همان لحظه اتفاق می‌افتند و تاخیر
 * تنظیم‌شده کاربر روی آن‌ها اثری ندارد. این ثابت همان سیاست را قفل
 * می‌کند تا تغییر آینده‌ای به اشتباه نگهبان را به تاخیر نیندازد.
 */
object GuardPolicy {
    /** تاخیر نگهبان، به ثانیه — همیشه صفر (فوری). */
    const val ACTION_DELAY_SECONDS = 0

    /** تاخیر موثر نگهبان، مستقل از تاخیر تنظیم‌شده کاربر. */
    fun effectiveDelaySeconds(@Suppress("UNUSED_PARAMETER") configuredDelaySeconds: Int): Int =
        ACTION_DELAY_SECONDS
}
