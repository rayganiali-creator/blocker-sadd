package com.sadd.blocker.data.billing

import android.app.Activity
import android.content.Context
import ir.mservices.market.billing.BillingClient
import ir.mservices.market.billing.BillingClientStateListener
import ir.mservices.market.billing.BillingFlowParams
import ir.mservices.market.billing.BillingResult
import ir.mservices.market.billing.ConsumeParams
import ir.mservices.market.billing.Purchase
import ir.mservices.market.billing.PurchasesUpdatedListener
import ir.mservices.market.billing.SkuDetailsParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * درگاه پرداخت مایکت با کتابخانه پرداخت مایکت (API هم‌شکل Play Billing).
 *
 * همه کد وابسته به کتابخانه مایکت فقط در همین فایل است. محصولات
 * یک‌بارخرید (INAPP) هستند؛ پلن‌های زمانی بعد از اعمال مدت consume
 * می‌شوند و مادام‌العمر هرگز consume نمی‌شود.
 *
 * هشدار: مختصات وابستگی و سطح API این کتابخانه در این جلسه از وب
 * راستی‌آزمایی نشد (جست‌وجوی وب خطا داد). قبل از انتشار باید با
 * مستندات رسمی مایکت تطبیق داده شود — اگر امضای متدها فرق داشت،
 * فقط همین فایل نیاز به اصلاح دارد.
 */
class MyketBillingGateway(context: Context) : BillingGateway {

    private val appContext: Context = context.applicationContext

    /** ادامه‌دارِ خرید در جریان؛ نتیجه خرید از PurchasesUpdatedListener می‌آید. */
    @Volatile
    private var pendingPurchase: ((PurchaseOutcome) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val callback = pendingPurchase ?: return@PurchasesUpdatedListener
        pendingPurchase = null
        val outcome = when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null) {
                    PurchaseOutcome.Success(purchase.toOwnedPurchase())
                } else {
                    PurchaseOutcome.Failed(billingResult.debugMessage)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseOutcome.Canceled
            else -> PurchaseOutcome.Failed(billingResult.debugMessage)
        }
        callback(outcome)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .build()

    @Volatile
    private var connected: Boolean = false

    override val isAvailable: Boolean = true

    override val storeNameFa: String = "مایکت"

    override suspend fun connect(): Boolean {
        if (connected) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(connected)
                }

                override fun onBillingServiceDisconnected() {
                    connected = false
                }
            })
        }
    }

    override suspend fun queryProducts(productIds: List<String>): List<StoreProduct> {
        if (!connect()) return emptyList()
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(productIds)
            .setType(BillingClient.SkuType.INAPP)
            .build()
        return suspendCancellableCoroutine { cont ->
            billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                val products = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    skuDetailsList.orEmpty().map { details ->
                        StoreProduct(
                            productId = details.sku,
                            priceText = details.price,
                            title = details.title,
                        )
                    }
                } else {
                    emptyList()
                }
                if (cont.isActive) cont.resume(products)
            }
        }
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): PurchaseOutcome {
        if (!connect()) return PurchaseOutcome.Unavailable
        // جریان خرید مایکت (مثل Play Billing) اول SkuDetails می‌خواهد.
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(productId))
            .setType(BillingClient.SkuType.INAPP)
            .build()
        val skuDetails = suspendCancellableCoroutine { cont ->
            billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
                val found = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    skuDetailsList?.firstOrNull()
                } else {
                    null
                }
                if (cont.isActive) cont.resume(found)
            }
        } ?: return PurchaseOutcome.Failed("محصول در فروشگاه پیدا نشد.")

        return suspendCancellableCoroutine { cont ->
            pendingPurchase = { outcome ->
                if (cont.isActive) cont.resume(outcome)
            }
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchase = null
                if (cont.isActive) cont.resume(PurchaseOutcome.Failed(result.debugMessage))
            }
        }
    }

    override suspend fun queryOwnedPurchases(): List<OwnedPurchase> {
        if (!connect()) return emptyList()
        return suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchases ->
                val owned = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.orEmpty().map { it.toOwnedPurchase() }
                } else {
                    emptyList()
                }
                if (cont.isActive) cont.resume(owned)
            }
        }
    }

    override suspend fun consume(purchaseToken: String): Boolean {
        if (!connect()) return false
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return suspendCancellableCoroutine { cont ->
            billingClient.consumeAsync(params) { billingResult, _ ->
                val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                if (cont.isActive) cont.resume(ok)
            }
        }
    }

    override fun disconnect() {
        runCatching { billingClient.endConnection() }
        connected = false
    }

    private fun Purchase.toOwnedPurchase(): OwnedPurchase = OwnedPurchase(
        productId = sku,
        purchaseTimeMs = purchaseTime,
        purchaseToken = purchaseToken,
    )
}
