package com.sadd.blocker.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.billing.PurchaseOutcome
import com.sadd.blocker.data.billing.StoreProduct
import com.sadd.blocker.data.pro.ProManager
import com.sadd.blocker.domain.ProEntitlement
import com.sadd.blocker.domain.ProPlan
import com.sadd.blocker.domain.ProStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProUiState(
    val status: ProStatus = ProStatus.Free,
    /** روزهای کامل باقی‌مانده از دوره آزمایشی (فقط در وضعیت Trial معنا دارد). */
    val trialDaysLeft: Int = 0,
    /** محصولات فروشگاه بر اساس شناسه؛ خالی یعنی قیمت پشتیبان نشان داده شود. */
    val storeProducts: Map<String, StoreProduct> = emptyMap(),
    val billingAvailable: Boolean = false,
    val storeNameFa: String? = null,
    /** پلنی که خریدش در جریان است؛ null یعنی خریدی در جریان نیست. */
    val buyingPlan: ProPlan? = null,
    val restoring: Boolean = false,
    val message: String? = null,
)

/**
 * ViewModel صفحه «سد پرو»: وضعیت فعلی (آزمایشی/خرید/مادام‌العمر/رایگان)،
 * پلن‌ها با قیمت فروشگاه یا پشتیبان، خرید و بازیابی خرید.
 * هیچ فیلد کد فعال‌سازی‌ای اینجا نیست — فعال‌سازی توسعه‌دهنده فقط با
 * ژست مخفی روی تصویر خرس داشبورد انجام می‌شود.
 */
@HiltViewModel
class ProViewModel @Inject constructor(
    private val proManager: ProManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProUiState(
            billingAvailable = proManager.billingAvailable,
            storeNameFa = proManager.storeNameFa,
        ),
    )
    val uiState: StateFlow<ProUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            proManager.proState.collect { state ->
                val now = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        status = ProEntitlement.evaluate(state, now),
                        trialDaysLeft = ProEntitlement.trialDaysLeft(state, now),
                    )
                }
            }
        }
        viewModelScope.launch {
            val products = proManager.queryStoreProducts()
            _uiState.update {
                it.copy(storeProducts = products.associateBy { product -> product.productId })
            }
        }
    }

    /** خرید یک پلن از فروشگاه؛ نتیجه با پیام فارسی نشان داده می‌شود. */
    fun buy(activity: Activity, plan: ProPlan) {
        if (_uiState.value.buyingPlan != null) return
        _uiState.update { it.copy(buyingPlan = plan, message = null) }
        viewModelScope.launch {
            val outcome = proManager.buy(activity, plan)
            val message = when (outcome) {
                is PurchaseOutcome.Success -> "سد پرو فعال شد. ممنون از حمایتت!"
                PurchaseOutcome.Canceled -> "خرید لغو شد."
                is PurchaseOutcome.Failed -> "خرید انجام نشد. دوباره تلاش کن."
                PurchaseOutcome.Unavailable -> "خرید فقط در نسخه نصب‌شده از فروشگاه کار می‌کند."
            }
            _uiState.update { it.copy(buyingPlan = null, message = message) }
        }
    }

    /** «بازیابی خرید»: آشتی‌دادن با خریدهای مالکیت‌شده فروشگاه. */
    fun restore() {
        if (_uiState.value.restoring) return
        _uiState.update { it.copy(restoring = true, message = null) }
        viewModelScope.launch {
            val restored = proManager.reconcile()
            _uiState.update {
                it.copy(
                    restoring = false,
                    message = if (restored) {
                        "خریدت بازیابی و سد پرو فعال شد."
                    } else {
                        "خرید قابل بازیابی‌ای پیدا نشد."
                    },
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * اعمال کد فعال‌سازی توسعه‌دهنده (از دیالوگ ژست مخفی روی خرس).
     * نتیجه عمداً به UI برنمی‌گردد تا رفتار دیالوگ برای کد درست و غلط
     * یکسان بماند.
     */
    fun applyAdminCode(code: String) {
        viewModelScope.launch {
            proManager.applyAdminCode(code)
        }
    }
}
