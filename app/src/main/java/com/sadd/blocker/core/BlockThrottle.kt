package com.sadd.blocker.core

import java.util.concurrent.ConcurrentHashMap

/**
 * ضد بلاک تکراری موتور Accessibility — خالص Kotlin، بدون وابستگی اندروید، قابل تست JVM.
 *
 * مشکل (ران ۱۷۹، گزارش سید روی A35): بلاک فقط بار اول می‌زد؛ تلاش دوم/سوم
 * (همان کلمه تایپ‌شده دوباره، همان اپ بازشده دوباره، همان دامنه رفته‌شده
 * دوباره) هرگز بلاک نمی‌شد. علت: scheduleBlock یک cooldown پانزده
 * ثانیه‌ای برای هر کلید داشت — هر تلاش تازه‌ای که تا ۱۵ ثانیه بعد از
 * بلاک قبلی می‌آمد بی‌صدا رد می‌شد. ۱۵ ثانیه برای حلقه‌ی رویدادهای
 * صفحه‌ی بلاک لازم نبود، ولی برای حلقه‌ی تلاشِ واقعی کاربر خیلی زیاد
 * بود: سید بلاک می‌شد، برمی‌گشت، همان کلمه را دوباره می‌زد و چون هنوز
 * در همان ۱۵ ثانیه بود، بلاک نمی‌شد؛ و چون تشخیص بخش/دامنه رویدادمحور
 * است (فقط روی تغییر پنجره ارزیابی می‌شود)، تلاشِ ردشده هیچ رویداد
 * تازه‌ای برای جبران نداشت و کاربر در بخش بلاک‌شده می‌ماند بدون بلاک.
 *
 * قانون تازه (جایگزین cooldown ۱۵ ثانیه‌ای):
 * - وقتی یک بلاک زمان‌بندی شد ولی هنوز اجرا نشده (در حال انتظارِ تاخیر)،
 *   زمان‌بندیِ دوباره همان کلید رد می‌شود — تا برای یک اتفاق، چند بلاک
 *   پشت سر هم صف نشود. این همان کارکرد ضد حلقه است، ولی فقط تا وقتی
 *   بلاک در صف است، نه ۱۵ ثانیه بعد از آن.
 * - وقتی بلاک واقعاً اجرا شد (صفحه بلاک آمد)، فقط یک debounce کوتاه
 *   اعمال می‌شود تا انفجار رویدادهای پنجره که خودِ صفحه بلاک می‌سازد
 *   بلاک دوباره نسازد؛ بعد از آن هر تلاش تازه بلاک می‌شود.
 * - اگر بلاکِ زمان‌بندی‌شده اجرا نشد (سشن تمام شد، کلمه پاک شد، بخش
 *   رفت)، هیچ debounceای نمی‌ماند و تلاش بعدی بی‌درنگ زمان‌بندی می‌شود.
 *
 * کلیدها همان کلیدهای قبلی scheduleBlock هستند: «keyword:<package>:<ruleId>»،
 * «section:<package>:<sectionKey>»، «website:<package>:<siteId>» و «guard:<hit>».
 */
class BlockThrottle(
    private val postFireCooldownMs: Long = POST_FIRE_COOLDOWN_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    /** کلیدهایی که بلاک‌شان زمان‌بندی شده و هنوز اجرا نشده است. */
    private val pending = ConcurrentHashMap<String, Boolean>()

    /** آخرین زمانی که بلاک هر کلید واقعاً اجرا شد (میلی‌ثانیه). */
    private val lastFiredAtMs = ConcurrentHashMap<String, Long>()

    /**
     * آیا می‌توان برای [key] بلاک زمان‌بندی کرد؟ در صورت قبول، کلید pending
     * می‌شود تا زمان‌بندیِ دوباره همان کلید تا اجرای بلاک رد شود.
     */
    fun trySchedule(key: String): Boolean {
        val now = nowMs()
        val last = lastFiredAtMs[key] ?: 0L
        if (now - last < postFireCooldownMs) return false
        return pending.putIfAbsent(key, true) == null
    }

    /** بلاک زمان‌بندی‌شده واقعاً اجرا شد: pending پاک و debounce کوتاه شروع می‌شود. */
    fun markFired(key: String) {
        pending.remove(key)
        lastFiredAtMs[key] = nowMs()
    }

    /** بلاک زمان‌بندی‌شده اجرا نشد: pending پاک می‌شود، بدون هیچ debounce. */
    fun markAbandoned(key: String) {
        pending.remove(key)
    }

    companion object {
        /** debounce بعد از بلاک واقعی — فقط انفجار رویداد صفحه بلاک را می‌خورد. */
        const val POST_FIRE_COOLDOWN_MS = 1_500L
    }
}
