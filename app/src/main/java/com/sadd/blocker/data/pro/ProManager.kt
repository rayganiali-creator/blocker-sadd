package com.sadd.blocker.data.pro

import android.app.Activity
import android.content.Context
import com.sadd.blocker.data.billing.BillingGateway
import com.sadd.blocker.data.billing.OwnedPurchase
import com.sadd.blocker.data.billing.PurchaseOutcome
import com.sadd.blocker.data.billing.StoreProduct
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.AdminCode
import com.sadd.blocker.domain.OwnedPurchaseInfo
import com.sadd.blocker.domain.ProEntitlement
import com.sadd.blocker.domain.ProPlan
import com.sadd.blocker.domain.ProState
import com.sadd.blocker.domain.ProStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیر «سد پرو»: نقطه واحد وضعیت پرو برای کل اپ.
 *
 * - وضعیت در DataStore ذخیره می‌شود (SettingsStore.proState) و منطق
 *   خالص ارزیابی/تمدید/سقف در [ProEntitlement] است.
 * - خرید از درگاه فروشگاه (بازار/مایکت بر اساس نصب‌کننده) انجام و
 *   همان‌جا اعمال و ذخیره می‌شود؛ پلن‌های زمانی بعد از اعمال consume
 *   می‌شوند تا خرید دوباره/انباشته‌شدن ممکن باشد و مادام‌العمر هرگز
 *   consume نمی‌شود تا بازیابی بعد از نصب دوباره کار کند.
 * - آشتی‌دادن (reconcile) موقع اجرا و با «بازیابی خرید» خریدهای
 *   مالکیت‌شده را اعمال می‌کند؛ توکن‌های اعمال‌شده ذخیره می‌شوند تا
 *   یک خرید دوبار حساب نشود. محدودیت شناخته‌شده: بعد از نصب دوباره،
 *   مدت پلن‌های زمانیِ consumeشده از فروشگاه قابل بازیابی نیست
 *   (سروری نداریم)؛ مادام‌العمر بازیابی می‌شود.
 */
@Singleton
class ProManager @Inject constructor(
    private val settingsStore: SettingsStore,
    private val billingGateway: BillingGateway,
    @ApplicationContext private val context: Context,
) {

    /** وضعیت ذخیره‌شده پرو — UI و موتور از همین می‌خوانند. */
    val proState: Flow<ProState> = settingsStore.proState

    /** آیا درگاه پرداخت فروشگاه در دسترس است؟ (نصب فروشگاهی) */
    val billingAvailable: Boolean get() = billingGateway.isAvailable

    /** نام فروشگاه برای نمایش؛ null یعنی نصب فروشگاهی نیست. */
    val storeNameFa: String? get() = billingGateway.storeNameFa

    /** ثبت یک‌باره زمان نصب (مبنای دوره آزمایشی) — موقع اجرا صدا زده می‌شود. */
    suspend fun ensureFirstInstallAt(nowMs: Long = System.currentTimeMillis()) {
        val packageFirstInstallMs = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        }.getOrDefault(0L)
        settingsStore.ensureFirstInstallAt(packageFirstInstallMs, nowMs)
    }

    /** وضعیت ارزیابی‌شده پرو در همین لحظه. */
    suspend fun currentStatus(nowMs: Long = System.currentTimeMillis()): ProStatus =
        ProEntitlement.evaluate(settingsStore.proState.first(), nowMs)

    /** آیا همین حالا پرو فعال است؟ (آزمایشی/خرید/مادام‌العمر/کد) */
    suspend fun isProNow(nowMs: Long = System.currentTimeMillis()): Boolean =
        ProEntitlement.isPro(settingsStore.proState.first(), nowMs)

    /** محصولات فروشگاه با قیمت‌های واقعی؛ در دسترس نبود → لیست خالی (قیمت پشتیبان). */
    suspend fun queryStoreProducts(): List<StoreProduct> {
        if (!billingGateway.isAvailable) return emptyList()
        return runCatching { billingGateway.queryProducts(ProPlan.ALL_PRODUCT_IDS) }
            .getOrDefault(emptyList())
    }

    /**
     * خرید یک پلن: جریان فروشگاه → اعمال مدت (انباشته) → ذخیره →
     * consume پلن زمانی (نه مادام‌العمر). نتیجه خام درگاه برگردانده
     * می‌شود تا UI پیام مناسب نشان دهد.
     */
    suspend fun buy(activity: Activity, plan: ProPlan): PurchaseOutcome {
        val outcome = billingGateway.launchPurchase(activity, plan.productId)
        if (outcome is PurchaseOutcome.Success) {
            val now = System.currentTimeMillis()
            val current = settingsStore.proState.first()
            val updated = ProEntitlement.applyPurchase(current, outcome.purchase.toInfo(), now)
            settingsStore.updateProState(updated)
            if (!plan.isLifetime && outcome.purchase.purchaseToken.isNotEmpty()) {
                runCatching { billingGateway.consume(outcome.purchase.purchaseToken) }
            }
        }
        return outcome
    }

    /**
     * آشتی‌دادن با خریدهای فروشگاه (موقع اجرا + «بازیابی خرید»):
     * خریدهای مالکیت‌شده اعمال و ذخیره می‌شوند و توکن پلن‌های زمانیِ
     * تازه اعمال‌شده consume می‌شوند. true یعنی دست‌کم یک خرید تازه
     * اعمال شد یا مادام‌العمر بازیابی شد.
     */
    suspend fun reconcile(): Boolean {
        if (!billingGateway.isAvailable) return false
        val owned = runCatching { billingGateway.queryOwnedPurchases() }
            .getOrDefault(emptyList())
        if (owned.isEmpty()) return false
        val now = System.currentTimeMillis()
        val current = settingsStore.proState.first()
        val (updated, tokensToConsume) = ProEntitlement.applyOwnedPurchases(
            current,
            owned.map { it.toInfo() },
            now,
        )
        if (updated == current) return false
        settingsStore.updateProState(updated)
        for (token in tokensToConsume) {
            runCatching { billingGateway.consume(token) }
        }
        return true
    }

    /**
     * اعمال کد فعال‌سازی توسعه‌دهنده (ژست مخفی روی تصویر خرس):
     * مقایسه با هش SHA-256 ثابت در [AdminCode]؛ درست بود، devUnlock
     * روشن و ذخیره می‌شود. متن ساده کد هیچ‌جا ذخیره نمی‌شود.
     */
    suspend fun applyAdminCode(code: String): Boolean {
        if (!AdminCode.verify(code)) return false
        settingsStore.setDevUnlock(true)
        return true
    }

    private fun OwnedPurchase.toInfo(): OwnedPurchaseInfo = OwnedPurchaseInfo(
        productId = productId,
        purchaseTimeMs = purchaseTimeMs,
        purchaseToken = purchaseToken,
    )
}
