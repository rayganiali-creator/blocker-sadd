package com.sadd.blocker.data.billing

import android.app.Activity

/**
 * درگاه «در دسترس نیست» — برای نصب دستی/دیباگ که هیچ فروشگاهی پشت
 * اپ نیست. صفحه پرو رندر می‌شود ولی خرید غیرفعال است و صادقانه
 * می‌گوید خرید فقط در نسخه نصب‌شده از فروشگاه کار می‌کند.
 */
class UnavailableBillingGateway : BillingGateway {

    override val isAvailable: Boolean = false

    override val storeNameFa: String? = null

    override suspend fun connect(): Boolean = false

    override suspend fun queryProducts(productIds: List<String>): List<StoreProduct> = emptyList()

    override suspend fun launchPurchase(activity: Activity, productId: String): PurchaseOutcome =
        PurchaseOutcome.Unavailable

    override suspend fun queryOwnedPurchases(): List<OwnedPurchase> = emptyList()

    override suspend fun consume(purchaseToken: String): Boolean = false

    override fun disconnect() = Unit
}
