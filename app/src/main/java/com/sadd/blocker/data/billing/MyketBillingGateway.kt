package com.sadd.blocker.data.billing

import android.app.Activity
import android.content.Context
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.Inventory
import ir.myket.billingclient.util.Purchase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * درگاه پرداخت مایکت با کتابخانه رسمی مایکت (ir.myket:billing:2.2 از
 * مخزن https://maven.myket.ir) — API از نوع IabHelper کلاسیک
 * (init با کلید RSA، queryInventory، launchPurchaseFlow، consume).
 * امضاها از سورس پلاگین رسمی myket_iap و خود AAR راستی‌آزمایی شدند.
 *
 * همه کد وابسته به کتابخانه مایکت فقط در همین فایل است. محصولات
 * یک‌بارخرید (INAPP) هستند؛ پلن‌های زمانی بعد از اعمال مدت consume
 * می‌شوند و مادام‌العمر هرگز consume نمی‌شود.
 *
 * TODO(انتشار): [MYKET_RSA_PUBLIC_KEY] باید با کلید عمومی RSA پنل
 * توسعه‌دهنده مایکت سید پر شود. کتابخانه امضای خرید را همیشه با همین
 * کلید راستی‌آزمایی می‌کند و کلید خالی یعنی خرید با خطای
 * IABHELPER_VERIFICATION_FAILED رد می‌شود (کرش نمی‌کند؛ اتصال و
 * استعلام قیمت کار می‌کنند). کلید را سید از پنل مایکت می‌گیرد.
 */
class MyketBillingGateway(context: Context) : BillingGateway {

    private val appContext: Context = context.applicationContext

    @Volatile
    private var helper: IabHelper? = null

    @Volatile
    private var connected: Boolean = false

    /**
     * کش توکن ← Purchase: consume مایکت خودِ آبجکت Purchase را می‌خواهد
     * نه فقط توکن؛ از استعلام موجودی و نتیجه خرید پر می‌شود.
     */
    private val purchasesByToken = mutableMapOf<String, Purchase>()

    override val isAvailable: Boolean = true

    override val storeNameFa: String = "مایکت"

    private fun obtainHelper(): IabHelper {
        val existing = helper
        if (existing != null) return existing
        val created = IabHelper(appContext, MYKET_RSA_PUBLIC_KEY)
        helper = created
        return created
    }

    override suspend fun connect(): Boolean {
        if (connected) return true
        val iabHelper = obtainHelper()
        return suspendCancellableCoroutine { cont ->
            iabHelper.startSetup { result ->
                connected = result.isSuccess()
                if (cont.isActive) cont.resume(connected)
            }
        }
    }

    override suspend fun queryProducts(productIds: List<String>): List<StoreProduct> {
        if (!connect()) return emptyList()
        val inventory = queryInventory(querySkuDetails = true, skus = productIds)
            ?: return emptyList()
        return inventory.getAllProducts()
            .filter { it.sku in productIds }
            .map { details ->
                StoreProduct(
                    productId = details.sku,
                    priceText = details.price,
                    title = details.title,
                )
            }
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): PurchaseOutcome {
        if (!connect()) return PurchaseOutcome.Unavailable
        val iabHelper = obtainHelper()
        return suspendCancellableCoroutine { cont ->
            iabHelper.launchPurchaseFlow(
                activity,
                productId,
                IabHelper.OnIabPurchaseFinishedListener { result, purchase ->
                    if (!cont.isActive) return@OnIabPurchaseFinishedListener
                    val outcome = when {
                        result.isSuccess() && purchase != null -> {
                            cachePurchase(purchase)
                            PurchaseOutcome.Success(purchase.toOwnedPurchase())
                        }
                        result.response == IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED ||
                            result.response == IabHelper.IABHELPER_USER_CANCELLED ->
                            PurchaseOutcome.Canceled
                        else -> PurchaseOutcome.Failed(result.message)
                    }
                    cont.resume(outcome)
                },
                /* payload = */ "",
            )
        }
    }

    override suspend fun queryOwnedPurchases(): List<OwnedPurchase> {
        if (!connect()) return emptyList()
        val inventory = queryInventory(querySkuDetails = false, skus = emptyList())
            ?: return emptyList()
        val purchases = inventory.getAllPurchases()
        purchases.forEach { cachePurchase(it) }
        return purchases.map { it.toOwnedPurchase() }
    }

    override suspend fun consume(purchaseToken: String): Boolean {
        if (!connect()) return false
        val purchase = purchasesByToken[purchaseToken] ?: run {
            // کش خالی است (مثلاً بعد از ری‌استارت اپ): یک استعلام موجودی
            // تا آبجکت Purchase پیدا شود.
            val inventory = queryInventory(querySkuDetails = false, skus = emptyList())
            val found = inventory?.getAllPurchases()?.firstOrNull { it.token == purchaseToken }
            if (found != null) cachePurchase(found)
            found
        } ?: return false
        val iabHelper = obtainHelper()
        return suspendCancellableCoroutine { cont ->
            iabHelper.consumeAsync(purchase) { _, result ->
                if (result.isSuccess()) purchasesByToken.remove(purchaseToken)
                if (cont.isActive) cont.resume(result.isSuccess())
            }
        }
    }

    override fun disconnect() {
        runCatching { helper?.dispose() }
        helper = null
        connected = false
        purchasesByToken.clear()
    }

    private suspend fun queryInventory(
        querySkuDetails: Boolean,
        skus: List<String>,
    ): Inventory? {
        val iabHelper = obtainHelper()
        return suspendCancellableCoroutine { cont ->
            iabHelper.queryInventoryAsync(querySkuDetails, skus) { result, inventory ->
                if (cont.isActive) {
                    cont.resume(if (result.isSuccess()) inventory else null)
                }
            }
        }
    }

    private fun cachePurchase(purchase: Purchase) {
        val token = purchase.token
        if (!token.isNullOrEmpty()) purchasesByToken[token] = purchase
    }

    private fun Purchase.toOwnedPurchase(): OwnedPurchase = OwnedPurchase(
        productId = sku,
        purchaseTimeMs = purchaseTime,
        purchaseToken = token.orEmpty(),
    )

    companion object {
        /**
         * TODO(انتشار): کلید عمومی RSA پنل توسعه‌دهنده مایکت سید اینجا
         * گذاشته شود. فعلاً خالی است — خرید تا وقتی پر نشود با خطای
         * راستی‌آزمایی امضا رد می‌شود (اتصال و استعلام قیمت کار می‌کنند).
         */
        const val MYKET_RSA_PUBLIC_KEY: String = ""
    }
}
