package com.sadd.blocker.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.data.system.UninstallProtection
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictSettingChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.UninstallProtectionGate
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.ui.ModesViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPink
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** تغییر Strict در انتظار بازگشایی (فقط در روش تایپ متن، در حافظه نگه داشته می‌شود). */
private sealed interface PendingStrictChange {
    data class Method(val method: StrictUnlockMethod) : PendingStrictChange
    data class Hours(val hours: Int) : PendingStrictChange
    data object ProtectionOff : PendingStrictChange
    /** تغییر یک تنظیم؛ فقط کلید blockMode مال همین ViewModel است. */
    data class SettingChange(val change: StrictSettingChange) : PendingStrictChange
}

data class StrictSettingsUiState(
    val blockMode: BlockMode = BlockMode.NORMAL,
    val unlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    val cooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    val cooldownRequest: StrictCooldownRequest? = null,
    val strictSessionActive: Boolean = false,
    val dialogVisible: Boolean = false,
    val message: String? = null,
    /** محافظت از حذف نصب روشن است یا نه (تنظیم). */
    val uninstallProtectionEnabled: Boolean = false,
    /** «سد» الان مدیر دستگاه فعال است یا نه (وضعیت زنده سیستم). */
    val uninstallAdminActive: Boolean = false,
    /** صفحه فعال‌سازی مدیر دستگاه روی این گوشی در دسترس است یا نه (تشخیص؛ null = هنوز بررسی نشده). */
    val adminActivationAvailable: Boolean? = null,
    /** فهرست «Device admin apps» روی این گوشی مستقیم باز می‌شود یا نه (null = هنوز بررسی نشده). */
    val deviceAdminListAvailable: Boolean? = null,
    /** سرویس Accessibility «سد» الان روشن است یا نه (وضعیت زنده سیستم). */
    val accessibilityEnabled: Boolean = false,
    /** نگهبان مسلح است یا نه: تنظیم روشن، یا مدیر دستگاه واقعاً فعال. */
    val guardArmed: Boolean = false,
    /** محافظت روشن بود ولی مدیر دستگاه از تنظیمات سیستم غیرفعال شد (تشخیص، فقط در کارت نشان داده می‌شود). */
    val adminDeactivatedNotice: Boolean = false,
    /** نتیجه آخرین تلاش فعال‌سازی مدیر دستگاه (فقط در حافظه؛ با تلاش بعدی پاک می‌شود). */
    val activationResult: String? = null,
)

/**
 * ViewModel تنظیمات بازگشایی Strict (ModesScreen).
 *
 * قانون اصلی: تغییر روش بازگشایی یا مدت انتظار، وقتی سشن Strict فعالی هست،
 * خودش نیازمند بازگشایی با روش فعلی است؛ وگرنه (سشن Strict فعالی نیست) آزاد است.
 * - روش فعلی PASSAGE: تغییر در حافظه می‌ماند و دیالوگ تایپ متن باز می‌شود؛
 *   با تطابق کامل، تغییر اعمال می‌شود.
 * - روش فعلی COOLDOWN: درخواست با زمان دیواری در DataStore ثبت می‌شود
 *   (درخواست جدید جای قبلی را می‌گیرد و زمان از اول شمرده می‌شود) و دیالوگ
 *   شمارش معکوس را نشان می‌دهد؛ با پایان انتظار، تغییر اعمال می‌شود.
 */
@HiltViewModel
class StrictSettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val sessionRepository: SessionRepository,
    private val uninstallProtection: UninstallProtection,
    private val permissionHealthChecker: PermissionHealthChecker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrictSettingsUiState())
    val uiState: StateFlow<StrictSettingsUiState> = _uiState.asStateFlow()
    private var pendingChange: PendingStrictChange? = null
    /** تلاش فعال‌سازی مدیر دستگاه در جریان است (فقط در حافظه؛ با refresh بعدی مصرف می‌شود). */
    private var activationAttempted = false

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictUnlockMethod.collect { method ->
                _uiState.update { it.copy(unlockMethod = method) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownHours.collect { hours ->
                _uiState.update { it.copy(cooldownHours = hours) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownRequest.collect { request ->
                _uiState.update { it.copy(cooldownRequest = request) }
            }
        }
        viewModelScope.launch {
            settingsStore.uninstallProtectionEnabled.collect { enabled ->
                _uiState.update { it.copy(uninstallProtectionEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            sessionRepository.observeActive().collect { active ->
                _uiState.update { it.copy(strictSessionActive = active?.strict == true) }
            }
        }
        refreshAdminState()
    }

    /**
     * وضعیت زنده مدیر دستگاه را از سیستم می‌خواند (مثلاً بعد از برگشت از صفحه فعال‌سازی).
     * همچنین تشخیص می‌دهد که مدیر دستگاه از تنظیمات سیستم غیرفعال شده است یا نه:
     * اگر محافظت روشن است و مدیر دستگاه قبلاً فعال بوده (در DataStore ثبت شده)
     * ولی الان فعال نیست، یعنی کاربر آن را از تنظیمات گوشی غیرفعال کرده است؛
     * در این صورت اعلان «غیرفعال شد» در کارت نشان داده می‌شود تا وقتی کاربر آن را ببندد
     * (با فعال‌شدن دوباره مدیر دستگاه هم اعلان پاک می‌شود). وقتی محافظت خاموش است،
     * وضعیتِ ثبت‌شده با وضعیت فعلی همگام می‌شود تا خاموش‌کردنِ خودِ اپ اشتباه گرفته نشود.
     *
     * همگام‌سازی مهم: اگر مدیر دستگاه دستی از تنظیمات سیستم فعال شده باشد،
     * تنظیم داخل اپ را هم روشن می‌کند — وگرنه نگهبان (که به تنظیم نگاه
     * می‌کرد) در وضعیت واقعی کاربر بی‌صدا خاموش می‌ماند. وضعیت
     * دسترسی‌پذیری و در دسترس بودن فهرست «Device admin apps» هم همین‌جا
     * تازه می‌شود تا خط «وضعیت محافظ» و خط تشخیص درست باشند.
     */
    fun refreshAdminState() {
        viewModelScope.launch {
            val active = runCatching { uninstallProtection.isAdminActive() }.getOrDefault(false)
            val available = runCatching { uninstallProtection.isActivationScreenAvailable() }.getOrNull()
            val listAvailable = runCatching { uninstallProtection.isDeviceAdminListAvailable() }.getOrNull()
            val accessibilityOn = runCatching { permissionHealthChecker.isAccessibilityEnabled() }.getOrDefault(false)
            var enabled = _uiState.value.uninstallProtectionEnabled
            // مدیر دستگاه دستی فعال شده ولی تنظیم خاموش است → تنظیم را روشن کن
            // تا نگهبان مسلح شود؛ بدون این همگام‌سازی، فعال‌سازی دستی نگهبان
            // را بیدار نمی‌کرد و حذف نصب از تنظیمات آزاد می‌ماند.
            if (active && !enabled) {
                runCatching { settingsStore.setUninstallProtectionEnabled(true) }
                enabled = true
            }
            val wasActive = runCatching { settingsStore.uninstallAdminWasActive.first() }.getOrDefault(false)
            val notice = UninstallProtectionGate.shouldShowDeactivatedNotice(enabled, wasActive, active)
            val newWasActive = when {
                !enabled -> active
                active -> true
                wasActive -> false
                else -> false
            }
            if (newWasActive != wasActive) {
                runCatching { settingsStore.setUninstallAdminWasActive(newWasActive) }
            }
            _uiState.update {
                // اگر تلاش فعال‌سازی در جریان بود، نتیجه‌اش را از وضعیت زنده
                // همین refresh می‌سازیم و پرچم را مصرف می‌کنیم؛ اگر تلاشی در
                // جریان نبود و مدیر دستگاه فعال است، نتیجه قدیمی (مثلاً
                // «فعال نشد») دیگر معتبر نیست و پاک می‌شود.
                val result = when {
                    activationAttempted -> if (active) {
                        "فعال شد ✓ — حذف نصب «سد» مسدود است."
                    } else {
                        "فعال نشد — از دکمه «فعال‌سازی مدیر دستگاه» زیر دستی انجام بده."
                    }
                    active -> null
                    else -> it.activationResult
                }
                it.copy(
                    uninstallAdminActive = active,
                    adminActivationAvailable = available,
                    deviceAdminListAvailable = listAvailable,
                    accessibilityEnabled = accessibilityOn,
                    guardArmed = UninstallProtectionGate.isGuardArmed(enabled, active),
                    adminDeactivatedNotice = if (active) {
                        false
                    } else {
                        notice || it.adminDeactivatedNotice
                    },
                    activationResult = result,
                )
            }
            activationAttempted = false
        }
    }

    /**
     * ثبت تلاش فعال‌سازی مدیر دستگاه — درست موقع ضربه روی دکمه/تاگل صدا
     * زده می‌شود تا refresh بعد از برگشت از صفحه سیستمی نتیجه را نشان بدهد.
     */
    fun markActivationAttempted() {
        activationAttempted = true
        _uiState.update { it.copy(activationResult = null) }
    }

    /** باز کردن صفحه فعال‌سازی همان‌جا شکست خورد (Toast خطا هم نشان داده شد). */
    fun markActivationLaunchFailed() {
        activationAttempted = false
        _uiState.update {
            it.copy(activationResult = "صفحه فعال‌سازی باز نشد — از دکمه «باز کردن تنظیمات امنیتی گوشی» زیر دستی انجام بده.")
        }
    }

    /** بستن نتیجه تلاش فعال‌سازی. */
    fun dismissActivationResult() {
        _uiState.update { it.copy(activationResult = null) }
    }

    /**
     * فهرست «Device admin apps» مستقیم باز نشد و به تنظیمات امنیتی رفت —
     * تا کاربر بداند از آنجا باید ادامه بدهد (خط تشخیص هم همین را می‌گوید).
     */
    fun noteDeviceAdminListFallback() {
        _uiState.update {
            it.copy(
                activationResult = "فهرست Device admin apps مستقیم باز نشد — تنظیمات امنیتی باز شد؛ از آنجا: Device admin apps ← «سد».",
            )
        }
    }

    /** بستن اعلان «محافظت از تنظیمات سیستم غیرفعال شد». */
    fun dismissAdminDeactivatedNotice() {
        _uiState.update { it.copy(adminDeactivatedNotice = false) }
    }

    /** درخواست تغییر حالت بلاک (پیش‌فرض شروع سشن). */
    fun requestBlockMode(mode: BlockMode) {
        val state = _uiState.value
        if (mode == state.blockMode) return
        viewModelScope.launch {
            if (!state.strictSessionActive) {
                settingsStore.setBlockMode(mode)
                return@launch
            }
            val change = StrictSettingChange.blockMode(mode.name)
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                pendingChange = PendingStrictChange.SettingChange(change)
                _uiState.update { it.copy(dialogVisible = true) }
            } else {
                // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
                val existing = state.cooldownRequest
                if (existing != null && existing.pendingKind == StrictPendingKind.SETTING_CHANGE &&
                    existing.pendingSettingChange == change
                ) {
                    _uiState.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.SETTING_CHANGE,
                    value = StrictSettingChange.encode(change),
                    durationHours = state.cooldownHours,
                )
                _uiState.update { it.copy(dialogVisible = true) }
            }
        }
    }

    /** درخواست تغییر روش بازگشایی. */
    fun requestUnlockMethod(method: StrictUnlockMethod) {
        val state = _uiState.value
        if (method == state.unlockMethod) return
        viewModelScope.launch {
            if (!state.strictSessionActive) {
                settingsStore.setStrictUnlockMethod(method)
                settingsStore.clearStrictCooldownRequest()
                return@launch
            }
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                pendingChange = PendingStrictChange.Method(method)
                _uiState.update { it.copy(dialogVisible = true) }
            } else {
                // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
                val existing = state.cooldownRequest
                if (existing != null && existing.pendingKind == StrictPendingKind.METHOD &&
                    existing.pendingMethod == method
                ) {
                    _uiState.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.METHOD,
                    value = method.name,
                    durationHours = state.cooldownHours,
                )
                _uiState.update { it.copy(dialogVisible = true) }
            }
        }
    }

    /** درخواست تغییر مدت انتظار. */
    fun requestCooldownHours(hours: Int) {
        val state = _uiState.value
        if (hours == state.cooldownHours) return
        viewModelScope.launch {
            if (!state.strictSessionActive) {
                settingsStore.setStrictCooldownHours(hours)
                settingsStore.clearStrictCooldownRequest()
                return@launch
            }
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                pendingChange = PendingStrictChange.Hours(hours)
                _uiState.update { it.copy(dialogVisible = true) }
            } else {
                val existing = state.cooldownRequest
                if (existing != null && existing.pendingKind == StrictPendingKind.HOURS &&
                    existing.pendingHours == hours
                ) {
                    _uiState.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.HOURS,
                    value = hours.toString(),
                    durationHours = state.cooldownHours,
                )
                _uiState.update { it.copy(dialogVisible = true) }
            }
        }
    }

    /**
     * روشن‌کردن تنظیم محافظت (بدون باز کردن صفحه و بدون refresh — refreshِ
     * بعد از آن نباید پرچم تلاش فعال‌سازی را زودتر از برگشت کاربر از صفحه
     * سیستمی مصرف کند). روشن‌کردن همیشه آزاد است.
     */
    fun ensureProtectionEnabled() {
        val state = _uiState.value
        if (state.uninstallProtectionEnabled) return
        viewModelScope.launch {
            settingsStore.setUninstallProtectionEnabled(true)
        }
    }

    /**
     * خاموش‌کردن محافظت — فقط بعد از دیالوگ تایید صریح «خاموش‌کردن محافظت؟»
     * در UI صدا زده می‌شود؛ هرگز اثر جانبی ضربه‌ای که برای فعال‌کردن بوده
     * نیست. خاموش‌کردن وسط سشن Strict فعال نیازمند بازگشایی Strict با روش
     * فعلی است؛ بیرون از سشن Strict آزاد است و همان‌جا «سد» را از مدیر
     * دستگاه هم حذف می‌کند.
     */
    fun requestProtectionOff() {
        val state = _uiState.value
        viewModelScope.launch {
            if (!UninstallProtectionGate.requiresUnlock(state.strictSessionActive, false)) {
                settingsStore.setUninstallProtectionEnabled(false)
                runCatching { uninstallProtection.removeAdmin() }
                refreshAdminState()
                return@launch
            }
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                pendingChange = PendingStrictChange.ProtectionOff
                _uiState.update { it.copy(dialogVisible = true) }
            } else {
                // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
                val existing = state.cooldownRequest
                if (existing != null && existing.pendingKind == StrictPendingKind.PROTECTION) {
                    _uiState.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.PROTECTION,
                    value = "",
                    durationHours = state.cooldownHours,
                )
                _uiState.update { it.copy(dialogVisible = true) }
            }
        }
    }

    /**
     * ثبت درخواست انتظار از دیالوگ (وقتی دیالوگ با درخواستی باز نشده باشد).
     * در جریان معمول ModesScreen درخواست موقع ضربه روی گزینه ثبت می‌شود،
     * پس این تابع بیشتر تور ایمنی است.
     */
    fun recordCooldownRequest() {
        val pending = pendingChange ?: return
        viewModelScope.launch {
            val state = _uiState.value
            when (pending) {
                is PendingStrictChange.Method -> {
                    settingsStore.recordStrictCooldownRequest(
                        kind = StrictPendingKind.METHOD,
                        value = pending.method.name,
                        durationHours = state.cooldownHours,
                    )
                }
                is PendingStrictChange.Hours -> {
                    settingsStore.recordStrictCooldownRequest(
                        kind = StrictPendingKind.HOURS,
                        value = pending.hours.toString(),
                        durationHours = state.cooldownHours,
                    )
                }
                is PendingStrictChange.ProtectionOff -> {
                    settingsStore.recordStrictCooldownRequest(
                        kind = StrictPendingKind.PROTECTION,
                        value = "",
                        durationHours = state.cooldownHours,
                    )
                }
                is PendingStrictChange.SettingChange -> {
                    settingsStore.recordStrictCooldownRequest(
                        kind = StrictPendingKind.SETTING_CHANGE,
                        value = StrictSettingChange.encode(pending.change),
                        durationHours = state.cooldownHours,
                    )
                }
            }
        }
    }

    /** بازگشایی موفق در دیالوگ: تغییر در انتظار اعمال می‌شود. */
    fun onUnlocked() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                val pending = pendingChange
                pendingChange = null
                when (pending) {
                    is PendingStrictChange.Method -> {
                        settingsStore.setStrictUnlockMethod(pending.method)
                    }
                    is PendingStrictChange.Hours -> {
                        settingsStore.setStrictCooldownHours(pending.hours)
                    }
                    is PendingStrictChange.ProtectionOff -> {
                        settingsStore.setUninstallProtectionEnabled(false)
                        runCatching { uninstallProtection.removeAdmin() }
                        refreshAdminState()
                    }
                    is PendingStrictChange.SettingChange -> {
                        if (pending.change.key == StrictSettingChange.KEY_BLOCK_MODE) {
                            runCatching { BlockMode.valueOf(pending.change.value) }
                                .onSuccess { settingsStore.setBlockMode(it) }
                        }
                    }
                    null -> Unit
                }
                _uiState.update {
                    it.copy(dialogVisible = false, message = "تنظیم Strict تغییر کرد.")
                }
            } else {
                applyRecordedRequestIfElapsed()
                _uiState.update { it.copy(dialogVisible = false) }
            }
        }
    }

    /**
     * اعمال تغییر ثبت‌شده در درخواست انتظار، اگر زمان کامل گذشته باشد.
     * درخواست پایان سشن (END_SESSION) و تغییر قانون (RULE_CHANGE) اینجا
     * اعمال نمی‌شوند؛ مال داشبورد/صفحه سشن‌ها و صفحه قانون مربوطه‌اند و
     * دست نخورده می‌مانند. خاموش‌کردن محافظت از حذف نصب (PROTECTION)
     * همین‌جا اعمال می‌شود: تنظیم خاموش و «سد» از مدیر دستگاه حذف می‌شود.
     */
    private suspend fun applyRecordedRequestIfElapsed(): Boolean {
        val request = _uiState.value.cooldownRequest ?: return false
        if (!StrictUnlock.isCooldownElapsed(request, System.currentTimeMillis())) {
            return false
        }
        when (request.pendingKind) {
            StrictPendingKind.METHOD -> {
                request.pendingMethod?.let { settingsStore.setStrictUnlockMethod(it) }
            }
            StrictPendingKind.HOURS -> {
                request.pendingHours?.let { settingsStore.setStrictCooldownHours(it) }
            }
            StrictPendingKind.PROTECTION -> {
                settingsStore.setUninstallProtectionEnabled(false)
                runCatching { uninstallProtection.removeAdmin() }
                refreshAdminState()
            }
            StrictPendingKind.END_SESSION -> return false
            StrictPendingKind.RULE_CHANGE -> return false
            // تغییر تنظیم: فقط کلید blockMode مال این صفحه است؛ بقیه کلیدها
            // (تاخیر سراسری، تم، رنگ‌بندی) مال ViewModel خودشان‌اند و درخواست
            // دست نمی‌خورد تا همان‌جا اعمال شود.
            StrictPendingKind.SETTING_CHANGE -> {
                val change = request.pendingSettingChange ?: return false
                if (change.key != StrictSettingChange.KEY_BLOCK_MODE) return false
                runCatching { BlockMode.valueOf(change.value) }
                    .onSuccess { settingsStore.setBlockMode(it) }
            }
            StrictPendingKind.EXTEND_SESSION -> return false
            StrictPendingKind.SCHEDULE_ADD -> return false
            StrictPendingKind.BACKUP_RESTORE -> return false
            StrictPendingKind.RULE_ADD -> return false
            // درخواست نگهبان اینجا اعمال نمی‌شود؛ مال BlockedActivity (حالت guard)
            // است و دست نخورده می‌ماند تا همان‌جا پنجره ۵دقیقه‌ای را تنظیم کند.
            StrictPendingKind.GUARD_UNLOCK -> return false
        }
        settingsStore.clearStrictCooldownRequest()
        return true
    }

    /** اعمال تغییرِ در انتظارِ پایان‌یافته، از کارت شمارش معکوس خود صفحه. */
    fun applyElapsedCooldownChange() {
        viewModelScope.launch {
            if (applyRecordedRequestIfElapsed()) {
                _uiState.update { it.copy(message = "تنظیم Strict تغییر کرد.") }
            }
        }
    }

    fun dismissDialog() {
        pendingChange = null
        _uiState.update { it.copy(dialogVisible = false) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/** توضیح فارسی کاری که درخواست انتظار برای آن ثبت شده است. */
private fun strictPendingDescription(request: StrictCooldownRequest): String {
    return when (request.pendingKind) {
        StrictPendingKind.END_SESSION -> "پایان سشن Strict"
        StrictPendingKind.METHOD -> when (request.pendingMethod) {
            StrictUnlockMethod.PASSAGE -> "تغییر روش بازگشایی به «تایپ متن طولانی»"
            StrictUnlockMethod.COOLDOWN -> "تغییر روش بازگشایی به «انتظار»"
            null -> "تغییر روش بازگشایی"
        }
        StrictPendingKind.HOURS -> {
            val hours = request.pendingHours ?: request.pendingValue
            "تغییر مدت انتظار به $hours ساعت"
        }
        StrictPendingKind.PROTECTION -> "خاموش‌کردن محافظت از حذف نصب"
        StrictPendingKind.GUARD_UNLOCK -> "بازگشایی محافظت (حذف نصب/توقف اجباری)"
        StrictPendingKind.SETTING_CHANGE -> {
            val change = request.pendingSettingChange
            when (change?.key) {
                StrictSettingChange.KEY_BLOCK_MODE -> "تغییر حالت بلاک"
                StrictSettingChange.KEY_THEME -> "تغییر تم"
                StrictSettingChange.KEY_COLOR_THEME -> "تغییر رنگ‌بندی"
                else -> "تغییر تنظیم"
            }
        }
        StrictPendingKind.EXTEND_SESSION -> "تمدید سشن Strict"
        StrictPendingKind.SCHEDULE_ADD -> "افزودن زمان‌بندی"
        StrictPendingKind.BACKUP_RESTORE -> "ریستور بکاپ"
        StrictPendingKind.RULE_ADD -> "افزودن قانون"
        StrictPendingKind.RULE_CHANGE -> {
            val change = request.pendingRuleChange
            if (change != null) {
                val actionText = when (change.action) {
                    StrictRuleChange.ACTION_TOGGLE -> if (change.value == "true") "فعال‌کردن" else "غیرفعال‌کردن"
                    StrictRuleChange.ACTION_REMOVE -> "حذف"
                    StrictRuleChange.ACTION_DELAY -> "تغییر تأخیر"
                    else -> "تغییر"
                }
                "$actionText قانون (${change.id})"
            } else {
                "تغییر قانون"
            }
        }
    }
}

/**
 * حالت بلاک.
 * Normal: صفحه مداخله با فرصت تجدیدنظر و خروج کنترل‌شده.
 * Strict: وسط سشن فعال هیچ چیزی در اپ قابل تغییر نیست — نه حالت بلاک،
 * نه تاخیر، نه تم و رنگ‌بندی، نه قانون (افزودن/غیرفعال/حذف/تاخیر)، نه
 * زمان‌بندی، نه تمدید سشن، نه ریستور بکاپ و نه پایان سشن — مگر با
 * بازگشایی Strict (تایپ متن طولانی یا انتظار ۱۲/۲۴/۴۸ ساعت). شروع سشن
 * Strict همیشه آزاد است؛ فعال‌کردن قانون (تقویت بلاک) و روشن‌کردن
 * محافظت هم آزادند. بلاک واقعی (تشخیص نیت و بلاک وب با Accessibility)
 * وصل است و فقط وسط سشن فعال کار می‌کند؛ حالت‌ها نحوه شروع سشن و قفل
 * قوانین وسط سشن را تعیین می‌کنند.
 * محدودیت صادقانه: اپ معمولی نمی‌تواند جلوی حذف نصب یا force-stop را قطعی بگیرد؛
 * مدیر دستگاه (پایین همین صفحه) حذف نصب را مسدود می‌کند و نگهبان، صفحه
 * غیرفعال‌سازی آن و حذف نصب/توقف اجباری را رهگیری می‌کند و بازگشایی
 * می‌خواهد — ولی خود صفحه سیستمی را نمی‌توان قفل کرد و اگر Accessibility
 * خاموش شود، نگهبان هم خاموش می‌شود.
 */
@Composable
fun ModesScreen(
    viewModel: ModesViewModel = hiltViewModel(),
    strictViewModel: StrictSettingsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strictState by strictViewModel.uiState.collectAsStateWithLifecycle()

    // وضعیت مدیر دستگاه را موقع برگشت به صفحه تازه می‌کنیم
    // (مثلاً بعد از فعال‌سازی در صفحه سیستمی).
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                strictViewModel.refreshAdminState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // تیکر شمارش معکوس کارت «تغییر در انتظار» — فقط وقتی درخواستی هست می‌دود.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val countdownActive = strictState.cooldownRequest != null && strictState.strictSessionActive
    LaunchedEffect(countdownActive) {
        if (!countdownActive) return@LaunchedEffect
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    if (strictState.dialogVisible) {
        StrictUnlockDialog(
            method = strictState.unlockMethod,
            cooldownHours = strictState.cooldownHours,
            cooldownRequest = strictState.cooldownRequest,
            onRecordCooldownRequest = { strictViewModel.recordCooldownRequest() },
            onUnlocked = { strictViewModel.onUnlocked() },
            onDismiss = { strictViewModel.dismissDialog() },
        )
    }

    // تایید صریح خاموش‌کردن محافظت — بدون آن، ضربه روی تاگل هرگز مدیر
    // دستگاه را حذف نمی‌کند.
    var showProtectionOffConfirm by remember { mutableStateOf(false) }
    if (showProtectionOffConfirm) {
        SaddDialog(
            title = "خاموش‌کردن محافظت؟",
            text = "مدیر دستگاه حذف می‌شود و حذف نصب «سد» دوباره ممکن می‌شود.",
            confirmText = "خاموش کن",
            onConfirm = {
                showProtectionOffConfirm = false
                strictViewModel.requestProtectionOff()
            },
            onDismiss = { showProtectionOffConfirm = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(
            "حالت بلاک",
            "پیش‌فرض شروع سشن؛ هر بار موقع شروع پرسیده می‌شود.",
            onBack = onBack,
        )
        MessageBar(message = strictState.message, onDismiss = { strictViewModel.dismissMessage() })
        ModeCard(
            selected = uiState.blockMode == BlockMode.NORMAL,
            title = "\u2066Normal\u2069 — عادی",
            description = "صفحه مداخله با فرصت تجدیدنظر؛ با بازگشت می‌توانی صرف‌نظر کنی.",
            icon = Icons.Filled.PlayArrow,
            color = ChipMint,
            onClick = { strictViewModel.requestBlockMode(BlockMode.NORMAL) },
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        ModeCard(
            selected = uiState.blockMode == BlockMode.STRICT,
            title = "\u2066Strict\u2069 — سخت‌گیرانه",
            description = "وسط سشن، تغییر قانون و پایان سشن فقط با بازگشایی Strict ممکن است. " +
                "جلوی حذف نصب را نمی‌گیرد؛ آن را پایین همین صفحه فعال کن. " +
                "\u2066force-stop\u2069 را هیچ اپ معمولی نمی‌تواند بگیرد.",
            icon = Icons.Filled.Lock,
            color = ChipPurple,
            onClick = { strictViewModel.requestBlockMode(BlockMode.STRICT) },
        )
        if (uiState.blockMode == BlockMode.STRICT) {
            Spacer(Modifier.height(SaddSpacing.Large))
            SectionTitle("بازگشایی Strict")
            SaddCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                    Text(
                        "پایان سشن و تغییر تنظیمات فقط با بازگشایی انجام می‌شود؛ شروع همیشه آزاد است.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    ModeCard(
                        selected = strictState.unlockMethod == StrictUnlockMethod.PASSAGE,
                        title = "تایپ متن طولانی",
                        description = "متن طولانی (حدود ۵۰۰ کاراکتر) را دقیقاً تایپ کن؛ تا کامل نشود، بازگشایی نمی‌شود.",
                        icon = Icons.Filled.TextFields,
                        color = ChipYellow,
                        onClick = { strictViewModel.requestUnlockMethod(StrictUnlockMethod.PASSAGE) },
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    ModeCard(
                        selected = strictState.unlockMethod == StrictUnlockMethod.COOLDOWN,
                        title = "انتظار",
                        description = "مدت انتخاب‌شده باید کامل بگذرد؛ بستن اپ یا ری‌استارت زمان را صفر نمی‌کند.",
                        icon = Icons.Filled.Timer,
                        color = ChipPink,
                        onClick = { strictViewModel.requestUnlockMethod(StrictUnlockMethod.COOLDOWN) },
                    )
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    Text(
                        "مدت انتظار (در روش انتظار):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    // مدت انتظار به سبک Detox: کنترل قطعه‌ای قرصی با قطعه انتخاب‌شده توپر.
                    SaddSegmentedControl(
                        options = StrictUnlock.COOLDOWN_HOUR_OPTIONS.map { "$it ساعت" },
                        selectedIndex = StrictUnlock.COOLDOWN_HOUR_OPTIONS
                            .indexOf(strictState.cooldownHours)
                            .coerceAtLeast(0),
                        onSelect = { index ->
                            strictViewModel.requestCooldownHours(StrictUnlock.COOLDOWN_HOUR_OPTIONS[index])
                        },
                    )
                    if (!strictState.strictSessionActive) {
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "سشن Strict فعالی نیست؛ تغییر این تنظیمات آزاد است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(SaddSpacing.Medium))
            SectionTitle("جلوگیری از حذف نصب")
            SaddCard(modifier = Modifier.fillMaxWidth(), hero = true) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                    // تاگل وضعیت واقعی مدیر دستگاه را نشان می‌دهد، نه فقط تنظیم
                    // ذخیره‌شده را: اگر تنظیم روشن ولی مدیر دستگاه فعال نباشد،
                    // تاگل خاموش است و ضربه روی آن فعال‌سازی را باز می‌کند —
                    // ضربه‌ای که برای فعال‌کردن است هرگز مدیر دستگاه را حذف نمی‌کند.
                    // ردیف محافظت به سبک Detox: چیپ آیکون سپر + همان ردیف سوییچ قبلی (رفتار دست نخورده).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SaddIconChip(icon = Icons.Filled.Shield, color = ChipMint)
                        Spacer(Modifier.size(SaddSpacing.Small))
                        SaddSwitchRow(
                        label = "جلوگیری از حذف نصب",
                        description = "«سد» مدیر دستگاه می‌شود و حذف نصب آن مسدود می‌شود.",
                        checked = strictState.uninstallAdminActive,
                        onCheckedChange = {
                            if (!strictState.uninstallAdminActive) {
                                // فعال‌کردن: مستقیم در همین ضربه، صفحه فعال‌سازی
                                // باز می‌شود (نه با تغییر وضعیت — اگر تنظیم از
                                // قبل روشن بود، هیچ انتقالی رخ نمی‌داد).
                                strictViewModel.ensureProtectionEnabled()
                                strictViewModel.markActivationAttempted()
                                if (UninstallProtectionGate.shouldLaunchActivation(strictState.uninstallAdminActive)) {
                                    if (!openAdminActivationPage(context)) {
                                        strictViewModel.markActivationLaunchFailed()
                                    }
                                }
                            } else {
                                // خاموش‌کردن: اول دیالوگ تایید، بعد (در صورت
                                // نیاز) بازگشایی Strict؛ هرگز حذف بی‌صدا نیست.
                                showProtectionOffConfirm = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        )
                    }
                    // اعلان: محافظت روشن بود ولی مدیر دستگاه از تنظیمات سیستم
                    // غیرفعال شد (اپ نمی‌تواند جلوی آن را بگیرد؛ فقط خبر می‌دهد).
                    if (strictState.adminDeactivatedNotice) {
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "محافظت از تنظیمات سیستم غیرفعال شد — برای فعال‌کردن دوباره، دکمه زیر را بزن.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        SaddButton(
                            text = "فهمیدم",
                            onClick = { strictViewModel.dismissAdminDeactivatedNotice() },
                            modifier = Modifier.fillMaxWidth(),
                            style = SaddButtonStyle.SECONDARY,
                        )
                    }
                    // نتیجه آخرین تلاش فعال‌سازی (بعد از برگشت از صفحه سیستمی).
                    strictState.activationResult?.let { result ->
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            result,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (strictState.uninstallAdminActive) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    Spacer(Modifier.height(SaddSpacing.Small))
                    if (strictState.uninstallAdminActive) {
                        Text(
                            "مدیر دستگاه فعال است؛ حذف نصب «سد» مسدود است.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(SaddSpacing.Medium))
                        SaddButton(
                            text = "رفتن به Device admin apps",
                            onClick = {
                                if (!openDeviceAdminAppsPage(context)) {
                                    strictViewModel.noteDeviceAdminListFallback()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = SaddButtonStyle.SECONDARY,
                        )
                    } else {
                        Text(
                            "مدیر دستگاه هنوز فعال نشده؛ تا فعال نشود، حذف نصب مسدود نیست.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(SaddSpacing.Medium))
                        // مسیر اصلی فعال‌سازی: همیشه و مستقیم صفحه فعال‌سازی
                        // را باز می‌کند، هرچه تاگل بگوید.
                        SaddButton(
                            text = "فعال‌سازی مدیر دستگاه",
                            onClick = {
                                strictViewModel.ensureProtectionEnabled()
                                strictViewModel.markActivationAttempted()
                                if (!openAdminActivationPage(context)) {
                                    strictViewModel.markActivationLaunchFailed()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        SaddButton(
                            text = "رفتن به Device admin apps",
                            onClick = {
                                if (!openDeviceAdminAppsPage(context)) {
                                    strictViewModel.noteDeviceAdminListFallback()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = SaddButtonStyle.SECONDARY,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        SaddButton(
                            text = "باز کردن تنظیمات امنیتی گوشی",
                            onClick = { openSecuritySettingsPage(context) },
                            modifier = Modifier.fillMaxWidth(),
                            style = SaddButtonStyle.SECONDARY,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "دستی: تنظیمات ← امنیت ← \u2066Device admin apps\u2069 ← «سد» را فعال کن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // خط تشخیص — برای عیب‌یابی از راه دور: وضعیت مدیر دستگاه
                    // + در دسترس بودن صفحه فعال‌سازی و فهرست Device admin apps.
                    strictState.adminActivationAvailable?.let { available ->
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            UninstallProtectionGate.adminDiagnosticLine(
                                strictState.uninstallAdminActive,
                                available,
                                strictState.deviceAdminListAvailable,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // خط وضعیت محافظ — نگهبان مسلح است یا نه، روی همین گوشی.
                    Spacer(Modifier.height(SaddSpacing.Small))
                    Text(
                        UninstallProtectionGate.guardStatusLine(
                            strictState.uninstallProtectionEnabled,
                            strictState.uninstallAdminActive,
                            strictState.accessibilityEnabled,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (strictState.guardArmed && strictState.accessibilityEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    Text(
                        "نگهبان صفحه غیرفعال‌سازی مدیر دستگاه و حذف نصب را رهگیری می‌کند و بازگشایی می‌خواهد؛ ولی خود صفحه سیستمی را نمی‌توان قفل کرد و اگر دسترسی‌پذیری خاموش شود، نگهبان هم خاموش می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val request = strictState.cooldownRequest
            if (request != null && strictState.strictSessionActive) {
                Spacer(Modifier.height(SaddSpacing.Medium))
                val remaining = StrictUnlock.cooldownRemainingMs(request, nowMs)
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        // سرتیتر کارت تغییر در انتظار به سبک Detox: چیپ آیکون تایمر + عنوان.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SaddIconChip(icon = Icons.Filled.Timer, color = ChipYellow)
                            Spacer(Modifier.size(SaddSpacing.Small))
                            Text(
                                "تغییر در انتظار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            strictPendingDescription(request),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        if (remaining > 0L) {
                            Text(
                                StrictUnlock.formatCooldownRemaining(remaining),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "باقی‌مانده تا پایان انتظار",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                "زمان انتظار تمام شد.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (request.pendingKind != StrictPendingKind.END_SESSION) {
                                Spacer(Modifier.height(SaddSpacing.Medium))
                                SaddButton(
                                    text = "اعمال تغییر",
                                    onClick = { strictViewModel.applyElapsedCooldownChange() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Spacer(Modifier.height(SaddSpacing.Small))
                                Text(
                                    "برای پایان سشن، از داشبورد یا صفحه سشن‌ها اقدام کن.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            // کارت اطلاع پایین صفحه به سبک Detox: چیپ آیکون اطلاع + متن.
            Row(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaddIconChip(icon = Icons.Filled.Info, color = ChipMint)
                Spacer(Modifier.size(SaddSpacing.Small))
                Text(
                    "بلاک فقط وسط سشن فعال کار می‌کند: جستجوی عمدی، ناوبری ممنوعه و باز کردن اپ ممنوعه. " +
                        "خواندن و تایپ عادی هرگز بلاک نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}

/** کارت انتخاب حالت به سبک Detox: رادیو + چیپ آیکون رنگی + عنوان و توضیح؛ انتخاب‌شده حاشیه primary توپر می‌گیرد. */
@Composable
private fun ModeCard(selected: Boolean, title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    SaddCard(
        modifier = Modifier.fillMaxWidth().then(
            if (selected) {
                Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(SaddRadius.Card),
                )
            } else {
                Modifier
            },
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.size(SaddSpacing.Small))
            SaddIconChip(icon = icon, color = color)
            Spacer(Modifier.size(SaddSpacing.Small))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
