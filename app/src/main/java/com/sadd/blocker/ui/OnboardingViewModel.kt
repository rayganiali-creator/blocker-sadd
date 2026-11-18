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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val steps: List<PermissionStatus> = emptyList(),
    val currentStepIndex: Int = 0,
    val allGranted: Boolean = false,
)

/**
 * ویزارد راه‌اندازی اول‌نصب: ۶ مرحله به ترتیب، با وضعیت زنده از سیستم.
 * وضعیت‌ها هیچ‌وقت ساختگی نیستند؛ همه از PermissionHealthChecker خوانده
 * می‌شوند و بعد از هر بازگشت به صفحه و هر نتیجه لانچر تازه می‌شوند.
 * مرحله restricted settings یک مرحله جداست که قبل از Accessibility
 * می‌آید؛ تیک آن یا تایید دستی کاربر است یا فعال‌شدن واقعی سرویس
 * Accessibility (منطق خالص در OnboardingSteps).
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

    /** تایید دستی کاربر برای مرحله restricted settings (در DataStore می‌ماند). */
    private val restrictedConfirmed: StateFlow<Boolean> =
        settingsStore.restrictedSettingsConfirmed.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )

    val uiState: StateFlow<OnboardingUiState> = combine(
        _systemStatuses,
        restrictedConfirmed,
        _selectedIndex,
    ) { statuses, confirmed, selected ->
        val steps = buildSteps(statuses, confirmed)
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    init {
        refresh()
    }

    /**
     * ساختن فهرست مرحله‌ها طبق ترتیب ویزارد؛ تیک مرحله restricted یا
     * تایید دستی کاربر است یا فعال‌بودن واقعی Accessibility.
     */
    private fun buildSteps(
        statuses: List<PermissionStatus>,
        confirmed: Boolean,
    ): List<PermissionStatus> {
        val byKey = statuses.associateBy { it.key }
        val accessibilityGranted = byKey[OnboardingSteps.KEY_ACCESSIBILITY]?.granted == true
        val restricted = byKey[OnboardingSteps.KEY_RESTRICTED]?.copy(
            granted = OnboardingSteps.isRestrictedDone(accessibilityGranted, confirmed),
        )
        return STEP_ORDER.mapNotNull { key ->
            if (key == OnboardingSteps.KEY_RESTRICTED) restricted else byKey[key]
        }
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
     * تایید کاربر که مرحله restricted settings را انجام داده است؛
     * بعد از ذخیره، ویزارد خودکار به مرحله دسترسی‌پذیری می‌رود.
     */
    fun confirmRestrictedSettings() {
        viewModelScope.launch {
            settingsStore.setRestrictedSettingsConfirmed(true)
            refresh()
        }
    }

    /** ثبت پایان راه‌اندازی در DataStore (کلید موجود onboarding_done). */
    suspend fun completeOnboarding() {
        settingsStore.setOnboardingDone(true)
    }
}
