package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.data.system.PermissionStatus
import com.sadd.blocker.domain.OnboardingSteps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val steps: List<PermissionStatus> = emptyList(),
    val currentStepIndex: Int = 0,
    val allGranted: Boolean = false,
    /** مرحله فعلی دنباله دسترسی‌پذیری (۱ تا ۳)؛ 0 یعنی دنباله تمام شده. */
    val accessibilityPhase: Int = 1,
)

/**
 * ویزارد راه‌اندازی اول‌نصب: ۵ مرحله به ترتیب، با وضعیت زنده از سیستم.
 * وضعیت‌ها هیچ‌وقت ساختگی نیستند؛ همه از PermissionHealthChecker خوانده
 * می‌شوند و بعد از هر بازگشت به صفحه و هر نتیجه لانچر تازه می‌شوند.
 * مرحله دسترسی‌پذیری یک دنباله سه‌مرحله‌ای است، دقیقاً طبق ترتیبی که
 * روی گوشی سید واقعاً اتفاق می‌افتد: ۱) زدن کلید خاکستری «سد» در
 * تنظیمات دسترسی‌پذیری و بستن پیام، ۲) روشن‌کردن Allow restricted
 * settings در اطلاعات برنامه، ۳) برگشت و فعال‌کردن «سد». تایید دستی
 * مرحله‌های ۱ و ۲ فقط «تأیید شما»ست تا وقتی سرویس واقعاً فعال شود؛
 * فعال‌شدن سرویس، مدرک انجام‌شدن هر سه مرحله است.
 * (VPN از ویزارد حذف شده — بلاک وب با Accessibility انجام می‌شود، نه VPN.)
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionHealth: PermissionHealthChecker,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    companion object {
        /** ترتیب ویزارد — با ترتیب صفحه عیب‌یابی فرق دارد: اعلان‌ها اول است. */
        val STEP_ORDER = OnboardingSteps.ORDER
    }

    private val _systemStatuses = MutableStateFlow<List<PermissionStatus>>(emptyList())

    /** مرحله‌ای که کاربر با زدن روی آن در فهرست انتخاب کرده؛ null یعنی خودکار. */
    private val _selectedIndex = MutableStateFlow<Int?>(null)

    /** مرحله ذخیره‌شده دنباله دسترسی‌پذیری (در DataStore می‌ماند). */
    private val storedPhase: StateFlow<Int> =
        settingsStore.accessibilityPhase.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            1,
        )

    val uiState: StateFlow<OnboardingUiState> = combine(
        _systemStatuses,
        storedPhase,
        _selectedIndex,
    ) { statuses, stored, selected ->
        val steps = STEP_ORDER.mapNotNull { key -> statuses.firstOrNull { it.key == key } }
        val accessibilityGranted =
            statuses.firstOrNull { it.key == OnboardingSteps.KEY_ACCESSIBILITY }?.granted == true
        val phase = OnboardingSteps.accessibilityPhase(stored, accessibilityGranted)
        val firstMissing = OnboardingSteps.firstMissingIndex(steps.map { it.granted })
        // اگر مرحله انتخاب‌شده هنوز انجام نشده، همان مرحله فعلی است؛
        // در غیر این صورت (انجام شده یا نامعتبر) به اولین مرحله ناتمام برمی‌گردیم.
        val current = if (selected != null && selected in steps.indices && !steps[selected].granted) {
            selected
        } else {
            firstMissing
        }
        OnboardingUiState(
            steps = steps,
            currentStepIndex = current,
            allGranted = steps.isNotEmpty() && steps.all { it.granted },
            accessibilityPhase = phase,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    init {
        refresh()
    }

    /** خواندن دوباره وضعیت واقعی مجوزها از سیستم. */
    fun refresh() {
        _systemStatuses.value = permissionHealth.checkAll()
    }

    /** پرش به یک مرحله با زدن روی آن در فهرست. */
    fun selectStep(index: Int) {
        _selectedIndex.value = index
    }

    /**
     * تایید دستی کاربر که یک مرحله از دنباله دسترسی‌پذیری را انجام داده
     * است؛ مرحله ذخیره‌شده یک قدم جلو می‌رود و هرگز عقب برنمی‌گردد
     * (تایید دوباره مرحله قبلی، پیشرفت را پس نمی‌برد).
     */
    fun confirmAccessibilityPhase(phase: Int) {
        viewModelScope.launch {
            val current = settingsStore.accessibilityPhase.first()
            val next = maxOf(current, OnboardingSteps.nextAccessibilityPhase(phase))
            settingsStore.setAccessibilityPhase(next)
            refresh()
        }
    }

    /** ثبت پایان راه‌اندازی در DataStore (کلید موجود onboarding_done). */
    suspend fun completeOnboarding() {
        settingsStore.setOnboardingDone(true)
    }
}
