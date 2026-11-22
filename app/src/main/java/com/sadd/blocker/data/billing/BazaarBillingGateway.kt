package com.sadd.blocker.data.billing

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * درگاه پرداخت کافه‌بازار با کتابخانه Poolakey.
 *
 * همه کد وابسته به Poolakey فقط در همین فایل است. محصولات یک‌بارخرید
 * (INAPP) هستند؛ پلن‌های زمانی بعد از اعمال مدت consume می‌شوند و
 * مادام‌العمر هرگز consume نمی‌شود.
 *
 * TODO(انتشار): قبل از انتشار در بازار، [SecurityCheck.Disable] باید
 * با SecurityCheck.Enable و کلید عمومی RSA پنل بازار سید جایگزین شود
 * تا امضای خریدها روی دستگاه راستی‌آزمایی شود. کلید را سید از پنل
 * توسعه‌دهنده بازار می‌گیرد.
 */
class BazaarBillingGateway(context: Context) : BillingGateway {

    private val payment: Payment = Payment(
        context.applicationContext,
        PaymentConfiguration(localSecurityCheck = SecurityCheck.Disable),
    )

    @Volatile
    private var connected: Boolean = false

    /** اتصال برگشتی از connect — قطع اتصال فقط از همین آبجکت ممکن است. */
    @Volatile
    private var connection: Connection? = null

    override val isAvailable: Boolean = true

    override val storeNameFa: String = "کافه‌بازار"

    override suspend fun connect(): Boolean {
        if (connected) return true
        return suspendCancellableCoroutine { cont ->
            connection = payment.connect {
                connectionSucceed {
                    connected = true
                    if (cont.isActive) cont.resume(true)
                }
                connectionFailed {
                    connected = false
                    if (cont.isActive) cont.resume(false)
                }
                disconnected {
                    connected = false
                }
            }
        }
    }

    override suspend fun queryProducts(productIds: List<String>): List<StoreProduct> {
        if (!connect()) return emptyList()
        return suspendCancellableCoroutine { cont ->
            payment.getInAppSkuDetails(productIds) {
                getSkuDetailsSucceed { skuDetails ->
                    val products = skuDetails.map { details ->
                        StoreProduct(
                            productId = details.sku,
                            priceText = details.price,
                            title = details.title,
                        )
                    }
                    if (cont.isActive) cont.resume(products)
                }
                getSkuDetailsFailed {
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        }
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): PurchaseOutcome {
        if (!connect()) return PurchaseOutcome.Unavailable
        // Poolakey 2.2.0 به‌جای خود Activity، ActivityResultRegistry می‌گیرد.
        val registry = (activity as? ComponentActivity)?.activityResultRegistry
            ?: return PurchaseOutcome.Failed("اکتیویتی نامعتبر برای جریان خرید بازار.")
        return suspendCancellableCoroutine { cont ->
            payment.purchaseProduct(
                registry,
                PurchaseRequest(
                    productId = productId,
                    payload = "",
                    dynamicPriceToken = null,
                ),
            ) {
                purchaseSucceed { purchaseInfo: PurchaseInfo ->
                    if (cont.isActive) {
                        cont.resume(PurchaseOutcome.Success(purchaseInfo.toOwnedPurchase()))
                    }
                }
                purchaseCanceled {
                    if (cont.isActive) cont.resume(PurchaseOutcome.Canceled)
                }
                purchaseFailed { throwable ->
                    if (cont.isActive) cont.resume(PurchaseOutcome.Failed(throwable.message))
                }
            }
        }
    }

    override suspend fun queryOwnedPurchases(): List<OwnedPurchase> {
        if (!connect()) return emptyList()
        return suspendCancellableCoroutine { cont ->
            payment.getPurchasedProducts {
                querySucceed { purchaseInfos ->
                    if (cont.isActive) {
                        cont.resume(purchaseInfos.map { it.toOwnedPurchase() })
                    }
                }
                queryFailed {
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        }
    }

    override suspend fun consume(purchaseToken: String): Boolean {
        if (!connect()) return false
        return suspendCancellableCoroutine { cont ->
            payment.consumeProduct(purchaseToken) {
                consumeSucceed {
                    if (cont.isActive) cont.resume(true)
                }
                consumeFailed {
                    if (cont.isActive) cont.resume(false)
                }
            }
        }
    }

    override fun disconnect() {
        runCatching { connection?.disconnect() }
        connection = null
        connected = false
    }

    private fun PurchaseInfo.toOwnedPurchase(): OwnedPurchase = OwnedPurchase(
        productId = productId,
        purchaseTimeMs = purchaseTime,
        purchaseToken = purchaseToken,
    )
}
