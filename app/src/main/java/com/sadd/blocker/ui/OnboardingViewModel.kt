package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.data.system.PermissionStatus
import com.sadd.blocker.data.system.UninstallProtection
import com.sadd.blocker.domain.OnboardingSteps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val steps: List<PermissionStatus> = emptyList(),
    val currentStepIndex: Int = 0,
    val allGranted: Boolean = false,
    /** مرحله فعلی دنباله دسترسی‌پذیری (۱ تا ۳)؛ 0 یعنی دنباله تمام شده. */
    val accessibilityPhase: Int = 1,
    /** «سد» الان مدیر دستگاه فعال است یا نه (وضعیت زنده سیستم). */
    val protectionAdminActive: Boolean = false,
    /** صفحه فعال‌سازی مدیر دستگاه روی این گوشی در دسترس است یا نه (تشخیص؛ null = هنوز بررسی نشده). */
    val protectionActivationAvailable: Boolean? = null,
    /** فهرست «Device admin apps» روی این گوشی مستقیم باز می‌شود یا نه (تشخیص). */
    val protectionListAvailable: Boolean? = null,
    /** نتیجه آخرین تلاش فعال‌سازی مدیر دستگاه در مرحله محافظت (فقط در حافظه). */
    val activationResult: String? = null,
    /** آموزش آشنایی باید نشان داده شود: همه مرحله‌ها تمام و آموزش دیده نشده. */
    val showTutorial: Boolean = false,
)

/** وضعیت زنده مدیر دستگاه برای مرحله محافظت (فقط در حافظه). */
private data class AdminState(
    val adminActive: Boolean = false,
    val activationAvailable: Boolean? = null,
    val listAvailable: Boolean? = null,
    val activationResult: String? = null,
)

/** بخش مجوزهای حالت ویزارد، قبل از چسباندن مرحله محافظت و آموزش. */
private data class PermissionPart(
    val steps: List<PermissionStatus>,
    val accessibilityPhase: Int,
    val selectedIndex: Int?,
)

/**
 * ویزارد راه‌اندازی اول‌نصب: ۵ مرحله مجوز به ترتیب + مرحله انتخاب
 * محافظت از حذف نصب، با وضعیت زنده از سیستم. وضعیت‌ها هیچ‌وقت
 * ساختگی نیستند؛ همه از PermissionHealthChecker و وضعیت واقعی مدیر
 * دستگاه خوانده می‌شوند و بعد از هر بازگشت به صفحه و هر نتیجه
 * لانچر تازه می‌شوند.
 *
 * مرحله دسترسی‌پذیری یک دنباله سه‌مرحله‌ای است، دقیقاً طبق ترتیبی که
 * روی گوشی سید واقعاً اتفاق می‌افتد: ۱) Installed apps ← «سد» ← زدن
 * کلید خاکستری و بستن پیام با Close، ۲) روشن‌کردن Allow restricted
 * settings در اطلاعات برنامه، ۳) برگشت و فعال‌کردن «سد» (Allow).
 * تایید دستی مرحله‌های ۱ و ۲ فقط «تأیید شما»ست تا وقتی سرویس واقعاً
 * فعال شود؛ فعال‌شدن سرویس، مدرک انجام‌شدن هر سه مرحله است.
 * (VPN از ویزارد حذف شده — بلاک وب با Accessibility انجام می‌شود، نه VPN.)
 *
 * مرحله محافظت انتخابی است: «فعال کن» تنظیم محافظت را روشن و انتخاب
 * را ثبت می‌کند و صفحه فعال‌سازی مدیر دستگاه همان‌جا با ضربه باز
 * می‌شود (در لایه UI، مثل صفحه حالت بلاک)؛ «الان نه» فقط انتخاب را
 * ثبت می‌کند و ویزارد جلو می‌رود. بعد از همه مرحله‌ها، آموزش
 * آشنایی یک‌بار نشان داده می‌شود (پرچم tutorial_done).
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionHealth: PermissionHealthChecker,
    private val settingsStore: SettingsStore,
    private val uninstallProtection: UninstallProtection,
) : ViewModel() {

    private val _systemStatuses = MutableStateFlow<List<PermissionStatus>>(emptyList())

    /** مرحله‌ای که کاربر با زدن روی آن در فهرست انتخاب کرده؛ null یعنی خودکار. */
    private val _selectedIndex = MutableStateFlow<Int?>(null)

    /** وضعیت زنده مدیر دستگاه (مرحله محافظت). */
    private val _adminState = MutableStateFlow(AdminState())

    /** تلاش فعال‌سازی مدیر دستگاه در جریان است (فقط در حافظه؛ با refresh بعدی مصرف می‌شود). */
    private var activationAttempted = false

    /** مرحله ذخیره‌شده دنباله دسترسی‌پذیری (در DataStore می‌ماند). */
    private val storedPhase: StateFlow<Int> =
        settingsStore.accessibilityPhase.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            1,
        )

    private val permissionPart = combine(
        _systemStatuses,
        storedPhase,
        _selectedIndex,
    ) { statuses, stored, selected ->
        val steps = OnboardingSteps.PERMISSION_ORDER.mapNotNull { key ->
            statuses.firstOrNull { it.key == key }
        }
        val accessibilityGranted =
            statuses.firstOrNull { it.key == OnboardingSteps.KEY_ACCESSIBILITY }?.granted == true
        PermissionPart(
            steps = steps,
            accessibilityPhase = OnboardingSteps.accessibilityPhase(stored, accessibilityGranted),
            selectedIndex = selected,
        )
    }

    val uiState: StateFlow<OnboardingUiState> = combine(
        permissionPart,
        settingsStore.protectionChoiceDone,
        settingsStore.tutorialDone,
        _adminState,
    ) { part, choiceDone, tutorialDone, admin ->
        // مرحله محافظت وضعیت سیستمی ندارد؛ از انتخاب ثبت‌شده و وضعیت
        // واقعی مدیر دستگاه ساخته می‌شود و آخر فهرست می‌نشیند.
        val protectionStatus = PermissionStatus(
            key = OnboardingSteps.KEY_PROTECTION,
            title = "محافظت در برابر حذف برنامه",
            granted = OnboardingSteps.isProtectionStepDone(choiceDone, admin.adminActive),
            description = "با فعال‌کردنش، «سد» مدیر دستگاه می‌شود و بدون بازکردن " +
                "قفل سخت‌گیرانه (نوشتن متن یا صبر ۱۲/۲۴/۴۸ ساعته) حذف نمی‌شود.",
            settingsAction = android.provider.Settings.ACTION_SECURITY_SETTINGS,
        )
        val steps = part.steps + protectionStatus
        val firstMissing = OnboardingSteps.firstMissingIndex(steps.map { it.granted })
        // اگر مرحله انتخاب‌شده هنوز انجام نشده، همان مرحله فعلی است؛
        // در غیر این صورت (انجام شده یا نامعتبر) به اولین مرحله ناتمام برمی‌گردیم.
        val selected = part.selectedIndex
        val current = if (selected != null && selected in steps.indices && !steps[selected].granted) {
            selected
        } else {
            firstMissing
        }
        val allGranted = steps.isNotEmpty() && steps.all { it.granted }
        OnboardingUiState(
            steps = steps,
            currentStepIndex = current,
            allGranted = allGranted,
            accessibilityPhase = part.accessibilityPhase,
            protectionAdminActive = admin.adminActive,
            protectionActivationAvailable = admin.activationAvailable,
            protectionListAvailable = admin.listAvailable,
            activationResult = admin.activationResult,
            showTutorial = OnboardingSteps.shouldShowTutorial(allGranted, tutorialDone),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    init {
        refresh()
    }

    /** خواندن دوباره وضعیت واقعی مجوزها و مدیر دستگاه از سیستم. */
    fun refresh() {
        _systemStatuses.value = permissionHealth.checkAll()
        refreshAdminState()
    }

    /**
     * وضعیت زنده مدیر دستگاه را از سیستم می‌خواند (مثلاً بعد از برگشت
     * از صفحه فعال‌سازی) و اگر تلاش فعال‌سازی در جریان بود، نتیجه‌اش
     * را از همین وضعیت می‌سازد. اگر مدیر دستگاه فعال شده باشد، تنظیم
     * محافظت هم روشن و «فعال بوده» ثبت می‌شود تا نگهبان مسلح بماند و
     * غیرفعال‌سازی بعدی از تنظیمات سیستم قابل تشخیص باشد (همان
     * همگام‌سازی صفحه حالت بلاک).
     */
    fun refreshAdminState() {
        viewModelScope.launch {
            val active = runCatching { uninstallProtection.isAdminActive() }.getOrDefault(false)
            val available = runCatching { uninstallProtection.isActivationScreenAvailable() }.getOrNull()
            val listAvailable = runCatching { uninstallProtection.isDeviceAdminListAvailable() }.getOrNull()
            if (active) {
                val enabled = runCatching {
                    settingsStore.uninstallProtectionEnabled.first()
                }.getOrDefault(false)
                if (!enabled) {
                    runCatching { settingsStore.setUninstallProtectionEnabled(true) }
                }
                runCatching { settingsStore.setUninstallAdminWasActive(true) }
            }
            _adminState.update {
                // اگر تلاش فعال‌سازی در جریان بود، نتیجه‌اش را از وضعیت
                // زنده همین refresh می‌سازیم و پرچم را مصرف می‌کنیم؛
                // اگر تلاشی در جریان نبود و مدیر دستگاه فعال است،
                // نتیجه قدیمی دیگر معتبر نیست و پاک می‌شود.
                val result = when {
                    activationAttempted -> if (active) {
                        "فعال شد ✓ — حذف نصب «سد» مسدود است."
                    } else {
                        "فعال نشد — هر وقت خواستی از داشبورد ← حالت بلاک ← جلوگیری از حذف نصب انجامش بده."
                    }
                    active -> null
                    else -> it.activationResult
                }
                it.copy(
                    adminActive = active,
                    activationAvailable = available,
                    listAvailable = listAvailable,
                    activationResult = result,
                )
            }
            activationAttempted = false
        }
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

    /**
     * «فعال کن» در مرحله محافظت: تنظیم محافظت روشن و انتخاب ثبت
     * می‌شود (روشن‌کردن همیشه آزاد است). باز کردن صفحه فعال‌سازی
     * مدیر دستگاه در خود ضربه و در لایه UI انجام می‌شود، مثل صفحه
     * حالت بلاک — اینجا فقط تنظیم و انتخاب ثبت می‌شود تا refreshِ
     * بعد از آن پرچم تلاش را زودتر از برگشت کاربر مصرف نکند.
     */
    fun enableProtection() {
        viewModelScope.launch {
            settingsStore.setUninstallProtectionEnabled(true)
            settingsStore.setProtectionChoiceDone(true)
        }
    }

    /** «الان نه» در مرحله محافظت: فقط انتخاب ثبت می‌شود و ویزارد جلو می‌رود. */
    fun skipProtection() {
        viewModelScope.launch {
            settingsStore.setProtectionChoiceDone(true)
        }
    }

    /**
     * ثبت تلاش فعال‌سازی مدیر دستگاه — درست موقع ضربه روی دکمه صدا
     * زده می‌شود تا refresh بعد از برگشت از صفحه سیستمی نتیجه را نشان بدهد.
     */
    fun markActivationAttempted() {
        activationAttempted = true
        _adminState.update { it.copy(activationResult = null) }
    }

    /** باز کردن صفحه فعال‌سازی همان‌جا شکست خورد (Toast خطا هم نشان داده شد). */
    fun markActivationLaunchFailed() {
        activationAttempted = false
        _adminState.update {
            it.copy(
                activationResult = "صفحه فعال‌سازی باز نشد — از تنظیمات گوشی: امنیت ← " +
                    "Device admin apps ← «سد» را فعال کن.",
            )
        }
    }

    /**
     * ثبت پایان راه‌اندازی در DataStore (کلید موجود onboarding_done).
     * پرچم آموزش هم همین‌جا می‌خورد: آموزش فقط داخل جریان راه‌اندازی
     * است، پس با پایان راه‌اندازی (چه با دیدن آموزش، چه با ردکردن
     * کل ویزارد) دیگر نشان داده نمی‌شود.
     */
    suspend fun completeOnboarding() {
        settingsStore.setTutorialDone(true)
        settingsStore.setOnboardingDone(true)
    }
}
