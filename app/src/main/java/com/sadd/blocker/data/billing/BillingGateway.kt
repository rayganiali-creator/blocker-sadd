package com.sadd.blocker.data.billing

import android.app.Activity

/**
 * قرارداد یکپارچه پرداخت درون‌برنامه‌ای — بقیه اپ فقط همین را می‌بیند
 * و هیچ کد فروشگاهی (بازار/مایکت) بیرون از پیاده‌سازی‌های این پوشه
 * نیست.
 *
 * همه محصولات «سد پرو» یک‌بارخرید (INAPP) هستند، نه اشتراک فروشگاهی:
 * مدت پلن‌ها را خود اپ با proUntilMs مدیریت می‌کند تا انباشته‌شدن
 * (خرید دوباره قبل از پایان) ممکن باشد. پلن‌های زمانی بعد از اعمال
 * consume می‌شوند تا همان پلن دوباره قابل خرید باشد؛ مادام‌العمر
 * هرگز consume نمی‌شود تا بازیابی بعد از نصب دوباره کار کند.
 */

/** یک محصول فروشگاه با قیمت نمایشی خودش (اگر فروشگاه داده باشد). */
data class StoreProduct(
    val productId: String,
    /** قیمت آماده نمایش از فروشگاه (مثلاً «۵۰٬۰۰۰ تومان»)؛ null یعنی فروشگاه نداده. */
    val priceText: String?,
    val title: String?,
)

/** یک خرید مالکیت‌شده از فروشگاه. */
data class OwnedPurchase(
    val productId: String,
    val purchaseTimeMs: Long,
    val purchaseToken: String,
)

/** نتیجه تلاش برای خرید. */
sealed interface PurchaseOutcome {
    data class Success(val purchase: OwnedPurchase) : PurchaseOutcome
    data object Canceled : PurchaseOutcome
    data class Failed(val message: String?) : PurchaseOutcome
    data object Unavailable : PurchaseOutcome
}

interface BillingGateway {

    /** آیا این درگاه اصلاً قابل استفاده است؟ (نصب فروشگاهی + کتابخانه حاضر) */
    val isAvailable: Boolean

    /** نام فروشگاه برای نمایش («کافه‌بازار»/«مایکت»)؛ null یعنی فروشگاهی نیست. */
    val storeNameFa: String?

    /** اتصال به سرویس پرداخت فروشگاه؛ true یعنی متصل شد. */
    suspend fun connect(): Boolean

    /** قیمت و مشخصات محصولات از فروشگاه؛ خطا → لیست خالی (قیمت پشتیبان نشان داده می‌شود). */
    suspend fun queryProducts(productIds: List<String>): List<StoreProduct>

    /** شروع جریان خرید یک محصول؛ نتیجه با [PurchaseOutcome] برمی‌گردد. */
    suspend fun launchPurchase(activity: Activity, productId: String): PurchaseOutcome

    /** خریدهای مالکیت‌شده کاربر (برای بازیابی/آشتی‌دادن). */
    suspend fun queryOwnedPurchases(): List<OwnedPurchase>

    /** consume کردن یک خرید (فقط پلن‌های زمانی، بعد از اعمال مدت). */
    suspend fun consume(purchaseToken: String): Boolean

    /** قطع اتصال و آزادسازی منابع. */
    fun disconnect()
}
