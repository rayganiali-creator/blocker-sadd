package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.data.system.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class OnboardingUiState(
    val steps: List<PermissionStatus> = emptyList(),
    val currentStepIndex: Int = 0,
    val allGranted: Boolean = false,
)

/**
 * ویزارد راه‌اندازی اول‌نصب: ۵ مجوز به ترتیب، با وضعیت زنده از سیستم.
 * وضعیت‌ها هیچ‌وقت ساختگی نیستند؛ همه از PermissionHealthChecker خوانده
 * می‌شوند و بعد از هر بازگشت به صفحه و هر نتیجه لانچر تازه می‌شوند.
 * (VPN از ویزارد حذف شده — بلاک وب با Accessibility انجام می‌شود، نه VPN.)
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionHealth: PermissionHealthChecker,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    companion object {
        /** ترتیب ویزارد — با ترتیب صفحه عیب‌یابی فرق دارد: اعلان‌ها اول است. */
        val STEP_ORDER = listOf(
            "notification",
            "accessibility",
            "usage",
            "battery",
            "alarm",
        )
    }

    private val _steps = MutableStateFlow<List<PermissionStatus>>(emptyList())

    /** مرحله‌ای که کاربر با زدن روی آن در فهرست انتخاب کرده؛ null یعنی خودکار. */
    private val _selectedIndex = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<OnboardingUiState> = combine(_steps, _selectedIndex) { steps, selected ->
        val firstMissing = steps.indexOfFirst { !it.granted }.takeIf { it >= 0 } ?: steps.size
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

    /** خواندن دوباره وضعیت واقعی مجوزها از سیستم. */
    fun refresh() {
        val byKey = permissionHealth.checkAll().associateBy { it.key }
        _steps.value = STEP_ORDER.mapNotNull { byKey[it] }
    }

    /** پرش به یک مرحله با زدن روی آن در فهرست. */
    fun selectStep(index: Int) {
        _selectedIndex.value = index
    }

    /** ثبت پایان راه‌اندازی در DataStore (کلید موجود onboarding_done). */
    suspend fun completeOnboarding() {
        settingsStore.setOnboardingDone(true)
    }
}
