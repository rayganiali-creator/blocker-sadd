package com.sadd.blocker.domain

/**
 * منطق خالص «سد پرو» — بدون وابستگی اندروید، قابل تست JVM.
 *
 * مدل کسب‌وکار (خواسته سید ۲۰۲۶-۱۱-۲۲):
 * - سه روز اول بعد از نصب، پرو خودکار فعال است (دوره آزمایشی).
 * - بعد از آن، کاربر رایگان است مگر اینکه یکی از پلن‌ها را خریده باشد.
 * - خرید پلن زمانی، مدت آن را از max(الان، پایان فعلی پرو) اضافه می‌کند
 *   (انباشته‌شدن): خرید ۱ماهه وقتی ۱۰ روز مانده، ۳۰ روز به همان پایان
 *   اضافه می‌کند، نه از امروز.
 * - پلن مادام‌العمر و کد فعال‌سازی توسعه‌دهنده، پرو را همیشگی می‌کنند.
 *
 * مدت پلن‌ها ثابت و روزمحور است (۳۰/۶۰/۹۰/۱۸۰ روز) تا رفتارشان
 * قابل‌پیش‌بینی و تست‌پذیر باشد.
 */

/** یک روز به میلی‌ثانیه — ثابت سطح فایل تا ورودی‌های enum بتوانند از آن استفاده کنند. */
private const val DAY_MS: Long = 24L * 60L * 60L * 1000L

/** یک پلن پرو — شناسه محصول در هر دو فروشگاه (بازار و مایکت) یکی است. */
enum class ProPlan(
    val productId: String,
    /** مدت پلن به میلی‌ثانیه؛ برای مادام‌العمر ۰ است. */
    val durationMs: Long,
    /** قیمت پشتیبان (تومان) وقتی قیمت فروشگاه در دسترس نیست. */
    val fallbackPriceToman: Long,
    val labelFa: String,
) {
    MONTH_1("pro_1m", 30L * DAY_MS, 50_000L, "۱ ماهه"),
    MONTH_2("pro_2m", 60L * DAY_MS, 99_000L, "۲ ماهه"),
    MONTH_3("pro_3m", 90L * DAY_MS, 140_000L, "۳ ماهه"),
    MONTH_6("pro_6m", 180L * DAY_MS, 280_000L, "۶ ماهه"),
    LIFETIME("pro_lifetime", 0L, 1_000_000L, "مادام‌العمر"),
    ;

    val isLifetime: Boolean get() = this == LIFETIME

    companion object {
        fun fromProductId(productId: String): ProPlan? =
            entries.firstOrNull { it.productId == productId }

        /** همه شناسه‌های محصول برای پرس‌وجو از فروشگاه. */
        val ALL_PRODUCT_IDS: List<String> = entries.map { it.productId }
    }
}

/** وضعیت ذخیره‌شده پرو در DataStore (همه زمان‌ها epoch میلی‌ثانیه‌اند). */
data class ProState(
    /** زمان نصب اپ؛ ۰ یعنی هنوز ثبت نشده است. */
    val firstInstallAtMs: Long = 0L,
    /** پایان پرو خریداری‌شده؛ ۰ یعنی خرید زمانی فعالی نیست. */
    val proUntilMs: Long = 0L,
    /** پلن مادام‌العمر خریده شده است. */
    val lifetimeOwned: Boolean = false,
    /** فعال‌سازی با کد توسعه‌دهنده (سید روی گوشی خودش). */
    val devUnlock: Boolean = false,
    /**
     * توکن خریدهایی که قبلاً اعمال شده‌اند — برای اینکه آشتی‌دادن
     * (reconcile) با فروشگاه در هر اجرا، مدت یک خرید را دوبار اضافه نکند.
     */
    val processedPurchaseTokens: Set<String> = emptySet(),
)

/** وضعیت ارزیابی‌شده پرو در یک لحظه. */
sealed interface ProStatus {
    /** رایگان — هیچ‌کدام از راه‌های پرو فعال نیست. */
    data object Free : ProStatus

    /** دوره آزمایشی سه‌روزه فعال است. */
    data class Trial(val trialEndsAtMs: Long) : ProStatus

    /** پرو خریداری‌شده تا زمان مشخصی فعال است. */
    data class Active(val untilMs: Long) : ProStatus

    /** پرو همیشگی (مادام‌العمر یا کد توسعه‌دهنده). */
    data object Lifetime : ProStatus
}

/** یک خریدِ مالکیت‌شده از فروشگاه — ورودی خالص آشتی‌دادن. */
data class OwnedPurchaseInfo(
    val productId: String,
    val purchaseTimeMs: Long,
    val purchaseToken: String,
)

/**
 * یک قانون برای محاسبه سقف رایگان: شناسه پایدار (id عددی به‌صورت رشته،
 * یا packageName برای اپ)، فعال بودن، و کلید ترتیب ساخت (id یا createdAt).
 */
data class CappableRule(
    val id: String,
    val enabled: Boolean,
    val createdOrder: Long,
)

object ProEntitlement {

    /** طول دوره آزمایشی: سه روز بعد از نصب. */
    const val TRIAL_MS: Long = 3L * DAY_MS

    /** سقف قانون فعال از هر نوع (کلمه/اپ/وب‌سایت) در نسخه رایگان. */
    const val FREE_RULE_CAP: Int = 3

    /** پایان دوره آزمایشی؛ اگر زمان نصب ثبت نشده باشد ۰. */
    fun trialEndsAtMs(state: ProState): Long =
        if (state.firstInstallAtMs > 0L) state.firstInstallAtMs + TRIAL_MS else 0L

    /**
     * ارزیابی وضعیت پرو در لحظه [nowMs].
     * اولویت: همیشگی (مادام‌العمر/کد) ← خرید زمانی فعال ← آزمایشی ← رایگان.
     * دوره آزمایشی وقتی فعال است که now < firstInstallAt + 3 روز؛ دقیقاً
     * در مرز سه روز، آزمایشی تمام شده است.
     */
    fun evaluate(state: ProState, nowMs: Long): ProStatus {
        if (state.devUnlock || state.lifetimeOwned) return ProStatus.Lifetime
        if (state.proUntilMs > nowMs) return ProStatus.Active(state.proUntilMs)
        val trialEnd = trialEndsAtMs(state)
        if (trialEnd > 0L && nowMs < trialEnd) return ProStatus.Trial(trialEnd)
        return ProStatus.Free
    }

    /** آیا در لحظه [nowMs] پرو فعال است؟ (هر راهی — آزمایشی هم حساب است.) */
    fun isPro(state: ProState, nowMs: Long): Boolean = evaluate(state, nowMs) != ProStatus.Free

    /**
     * روزهای کامل باقی‌مانده از دوره آزمایشی (گرد به بالا: ۲٫۵ روز → ۳).
     * بیرون از دوره آزمایشی ۰ است.
     */
    fun trialDaysLeft(state: ProState, nowMs: Long): Int {
        val trialEnd = trialEndsAtMs(state)
        if (trialEnd <= 0L || nowMs >= trialEnd) return 0
        val remainingMs = trialEnd - nowMs
        return ((remainingMs + DAY_MS - 1L) / DAY_MS).toInt()
    }

    /**
     * تمدید پایان پرو با خرید یک پلن زمانی — انباشته‌شدن:
     * از max(الان، پایان فعلی) به‌اندازه مدت پلن جلو می‌رود.
     */
    fun extendProUntil(currentProUntilMs: Long, plan: ProPlan, nowMs: Long): Long {
        if (plan.isLifetime) return currentProUntilMs
        val base = maxOf(nowMs, currentProUntilMs)
        return base + plan.durationMs
    }

    /**
     * اعمال یک خرید تازه (مسیر خرید موفق): مادام‌العمر پرچم را می‌زند؛
     * پلن زمانی از max(الان، پایان فعلی) تمدید می‌شود و توکنش ثبت
     * می‌شود تا آشتی‌دادن بعدی دوباره اعمالش نکند. اگر توکن قبلاً
     * اعمال شده باشد، وضعیت دست نمی‌خورد (idempotent).
     */
    fun applyPurchase(state: ProState, purchase: OwnedPurchaseInfo, nowMs: Long): ProState {
        val plan = ProPlan.fromProductId(purchase.productId) ?: return state
        if (plan.isLifetime) {
            return state.copy(lifetimeOwned = true)
        }
        if (purchase.purchaseToken.isNotEmpty() &&
            state.processedPurchaseTokens.contains(purchase.purchaseToken)
        ) {
            return state
        }
        val newUntil = extendProUntil(state.proUntilMs, plan, nowMs)
        val newTokens = if (purchase.purchaseToken.isNotEmpty()) {
            state.processedPurchaseTokens + purchase.purchaseToken
        } else {
            state.processedPurchaseTokens
        }
        return state.copy(proUntilMs = newUntil, processedPurchaseTokens = newTokens)
    }

    /**
     * آشتی‌دادن با خریدهای مالکیت‌شده فروشگاه (موقع اجرا و «بازیابی خرید»).
     * خریدها به ترتیب زمانی اعمال می‌شوند؛ پلن زمانیِ قبلاً اعمال‌نشده
     * از max(پایان فعلی، زمان خرید) تمدید می‌شود — پس خریدی که موقع
     * نبودن اپ انجام شده (مثلاً بعد از نصب دوباره) از زمان خودش شمرده
     * می‌شود، نه از امروز. توکن‌های اعمال‌شده ثبت می‌شوند تا اجرای
     * بعدی همان خرید را دوبار حساب نکند. مادام‌العمر همیشه پرچم را
     * می‌زند (idempotent است).
     *
     * برمی‌گرداند: وضعیت جدید + توکن‌هایی که تازه اعمال شدند و باید
     * در فروشگاه consume شوند (فقط پلن‌های زمانی؛ مادام‌العمر هرگز
     * consume نمی‌شود تا بازیابی بعد از نصب دوباره ممکن بماند).
     */
    fun applyOwnedPurchases(
        state: ProState,
        owned: List<OwnedPurchaseInfo>,
        nowMs: Long,
    ): Pair<ProState, List<String>> {
        var current = state
        val tokensToConsume = mutableListOf<String>()
        val ordered = owned.sortedBy { it.purchaseTimeMs }
        for (purchase in ordered) {
            val plan = ProPlan.fromProductId(purchase.productId) ?: continue
            if (plan.isLifetime) {
                if (!current.lifetimeOwned) {
                    current = current.copy(lifetimeOwned = true)
                }
                continue
            }
            if (purchase.purchaseToken.isNotEmpty() &&
                current.processedPurchaseTokens.contains(purchase.purchaseToken)
            ) {
                continue
            }
            val base = maxOf(current.proUntilMs, purchase.purchaseTimeMs)
            val newUntil = base + plan.durationMs
            val newTokens = if (purchase.purchaseToken.isNotEmpty()) {
                current.processedPurchaseTokens + purchase.purchaseToken
            } else {
                current.processedPurchaseTokens
            }
            current = current.copy(proUntilMs = newUntil, processedPurchaseTokens = newTokens)
            if (purchase.purchaseToken.isNotEmpty()) {
                tokensToConsume.add(purchase.purchaseToken)
            }
        }
        return current to tokensToConsume
    }

    /**
     * تصمیم «ثبت یک‌باره زمان نصب»: اگر قبلاً ثبت شده همان می‌ماند؛
     * وگرنه زمان نصب از PackageManager (firstInstallTime) و اگر آن هم
     * در دسترس نبود، زمان اولین اجرا.
     */
    fun resolveFirstInstallAt(storedMs: Long, packageFirstInstallMs: Long, nowMs: Long): Long {
        if (storedMs > 0L) return storedMs
        if (packageFirstInstallMs > 0L) return packageFirstInstallMs
        return nowMs
    }

    /** قیمت تومان با ارقام فارسی و جداکننده هزارگان: «۵۰٬۰۰۰ تومان». */
    fun formatTomanFa(amountToman: Long): String {
        val digits = amountToman.toString()
        val grouped = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            grouped.append(digits[i])
            count++
            if (count == 3 && i > 0) {
                grouped.append('٬')
                count = 0
            }
        }
        return JalaliDate.toPersianDigits(grouped.reverse().toString()) + " تومان"
    }

    // ---------- سقف نسخه رایگان ----------

    /**
     * شناسه قانون‌هایی که در وضعیت فعلی واقعاً اعمال می‌شوند.
     * پرو (هر راهی — آزمایشی، خرید، مادام‌العمر، کد): همه قانون‌های فعال.
     * رایگان: فقط [FREE_RULE_CAP] قانون فعال اول به ترتیب ساخت؛ بقیه
     * «متوقف» حساب می‌شوند — حذف نمی‌شوند، موتور نادیده‌شان می‌گیرد و
     * با پروشدن دوباره همه کار می‌کنند. قانون غیرفعال هیچ‌وقت اعمال
     * نمی‌شود و در شمارش سقف هم نیست.
     */
    fun enforcedRuleIds(rules: List<CappableRule>, state: ProState, nowMs: Long): Set<String> {
        val enabledOrdered = rules
            .filter { it.enabled }
            .sortedWith(compareBy({ it.createdOrder }, { it.id }))
        if (isPro(state, nowMs)) {
            return enabledOrdered.map { it.id }.toSet()
        }
        return enabledOrdered.take(FREE_RULE_CAP).map { it.id }.toSet()
    }

    /**
     * آیا قانون دیگری از این نوع را می‌توان فعال کرد/افزود؟
     * پرو: همیشه بله. رایگان: فقط وقتی تعداد قانون‌های فعالِ اعمال‌شونده
     * کمتر از سقف باشد. (حذف و غیرفعال‌کردن هیچ‌وقت سقف ندارد.)
     */
    fun canEnableMore(enabledCount: Int, state: ProState, nowMs: Long): Boolean {
        if (isPro(state, nowMs)) return true
        return enabledCount < FREE_RULE_CAP
    }
}
