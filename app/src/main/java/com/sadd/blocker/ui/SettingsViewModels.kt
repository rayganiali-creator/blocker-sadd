package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.PinCrypto
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.data.datastore.ColorTheme
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictSettingChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.domain.repository.PinCheckResult
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModesUiState(
    val blockMode: BlockMode = BlockMode.NORMAL,
    val pinSet: Boolean = false,
)

@HiltViewModel
class ModesViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModesUiState())
    val uiState: StateFlow<ModesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsStore.pinSet.collect { pinSet ->
                _uiState.update { it.copy(pinSet = pinSet) }
            }
        }
    }
}

/** تاخیر اختصاصی یک قانون؛ key شناسه بازنشانی است (پکیج اپ یا id عددی). */
data class RuleDelayOverride(
    val key: String,
    val label: String,
    val seconds: Int,
    /** قانون فعال است؟ بازنشانی تاخیر قانون غیرفعال وسط Strict آزاد است. */
    val enabled: Boolean = true,
)

data class DelayUiState(
    val globalDelay: Int = DelayOptions.DEFAULT_SECONDS,
    val appOverrides: List<RuleDelayOverride> = emptyList(),
    val websiteOverrides: List<RuleDelayOverride> = emptyList(),
    val keywordOverrides: List<RuleDelayOverride> = emptyList(),
    /** دیالوگ بازگشایی Strict برای تغییر تاخیر باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده (اگر هست) برای دیالوگ. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
)

@HiltViewModel
class DelayViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DelayUiState())
    val uiState: StateFlow<DelayUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    /** تغییر تاخیرِ در انتظار بازگشایی Strict (فقط در حافظه). */
    private var pendingDelayChange: StrictRuleChange? = null

    init {
        viewModelScope.launch {
            settingsStore.globalDelaySeconds.collect { seconds ->
                _uiState.update { it.copy(globalDelay = seconds) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictUnlockMethod.collect { method ->
                _uiState.update { it.copy(strictUnlockMethod = method) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownHours.collect { hours ->
                _uiState.update { it.copy(strictCooldownHours = hours) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownRequest.collect { request ->
                _uiState.update { it.copy(strictCooldownRequest = request) }
            }
        }
        viewModelScope.launch {
            appRepository.observe().collect { list ->
                val overrides = list
                    .filter { it.delaySeconds != null }
                    .map { RuleDelayOverride(it.packageName, it.label, it.delaySeconds!!, it.enabled) }
                _uiState.update { it.copy(appOverrides = overrides) }
            }
        }
        viewModelScope.launch {
            websiteRepository.observe().collect { list ->
                val overrides = list
                    .filter { it.delaySeconds != null }
                    .map { RuleDelayOverride(it.id.toString(), it.host, it.delaySeconds!!, it.enabled) }
                _uiState.update { it.copy(websiteOverrides = overrides) }
            }
        }
        viewModelScope.launch {
            keywordRepository.observe().collect { list ->
                val overrides = list
                    .filter { it.delaySeconds != null }
                    .map { RuleDelayOverride(it.id.toString(), it.rawText, it.delaySeconds!!, it.enabled) }
                _uiState.update { it.copy(keywordOverrides = overrides) }
            }
        }
    }

    /**
     * تغییر تاخیر سراسری.
     * وسط سشن Strict فقط بعد از بازگشایی Strict (تایپ متن / انتظار)
     * انجام می‌شود؛ بقیه وقت‌ها مستقیم اعمال می‌شود.
     */
    fun setGlobalDelay(seconds: Int) {
        viewModelScope.launch {
            if (seconds == _uiState.value.globalDelay) return@launch
            if (!strictGuard.isStrictSessionActive()) {
                settingsStore.setGlobalDelay(seconds)
                return@launch
            }
            requestDelayChange(
                StrictRuleChange.delay(StrictRuleChange.SCOPE_GLOBAL, "global", seconds),
            )
        }
    }

    /**
     * بازنشانی تاخیر اختصاصی اپ به سراسری. قانون فعال وسط Strict فقط بعد
     * از بازگشایی عوض می‌شود؛ قانون غیرفعال بلاک نمی‌کند و بازنشانی تاخیرش
     * آزاد است (وضعیت بلاک را ضعیف نمی‌کند).
     */
    fun resetAppDelay(packageName: String) {
        viewModelScope.launch {
            val enabled = _uiState.value.appOverrides
                .firstOrNull { it.key == packageName }?.enabled ?: true
            if (enabled && strictGuard.isStrictSessionActive()) {
                requestDelayChange(
                    StrictRuleChange.delay(StrictRuleChange.SCOPE_APP, packageName, null),
                )
                return@launch
            }
            appRepository.setDelay(packageName, null)
        }
    }

    /** بازنشانی تاخیر اختصاصی وب‌سایت به سراسری (گیت مثل [resetAppDelay]). */
    fun resetWebsiteDelay(key: String) {
        viewModelScope.launch {
            val enabled = _uiState.value.websiteOverrides
                .firstOrNull { it.key == key }?.enabled ?: true
            if (enabled && strictGuard.isStrictSessionActive()) {
                requestDelayChange(
                    StrictRuleChange.delay(StrictRuleChange.SCOPE_WEBSITE, key, null),
                )
                return@launch
            }
            val id = key.toLongOrNull() ?: return@launch
            websiteRepository.setDelay(id, null)
        }
    }

    /** بازنشانی تاخیر اختصاصی کلمه به سراسری (گیت مثل [resetAppDelay]). */
    fun resetKeywordDelay(key: String) {
        viewModelScope.launch {
            val enabled = _uiState.value.keywordOverrides
                .firstOrNull { it.key == key }?.enabled ?: true
            if (enabled && strictGuard.isStrictSessionActive()) {
                requestDelayChange(
                    StrictRuleChange.delay(StrictRuleChange.SCOPE_KEYWORD, key, null),
                )
                return@launch
            }
            val id = key.toLongOrNull() ?: return@launch
            keywordRepository.setDelay(id, null)
        }
    }

    /** درخواست تغییر تاخیر وسط سشن Strict: دیالوگ بازگشایی باز می‌شود. */
    private fun requestDelayChange(change: StrictRuleChange) {
        pendingDelayChange = change
        _uiState.update { it.copy(strictUnlockVisible = true) }
    }

    /** اعمال تغییر تاخیر بعد از بازگشایی موفق در دیالوگ Strict. */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            val change = pendingDelayChange
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.RULE_CHANGE }?.pendingRuleChange
                ?: return@launch
            if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = state.strictCooldownRequest
                if (request == null || !StrictUnlock.isCooldownElapsed(request, now) ||
                    request.pendingRuleChange != change
                ) {
                    if (request != null && request.pendingRuleChange != change) {
                        pendingDelayChange = null
                        _uiState.update { it.copy(strictUnlockVisible = false) }
                    }
                    return@launch
                }
                settingsStore.clearStrictCooldownRequest()
            }
            pendingDelayChange = null
            _uiState.update { it.copy(strictUnlockVisible = false) }
            applyStrictRuleChange(change)
        }
    }

    /** ثبت درخواست انتظار برای تغییر تاخیر (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        val pending = pendingDelayChange ?: return
        val existing = _uiState.value.strictCooldownRequest
        // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
        if (existing?.pendingKind == StrictPendingKind.RULE_CHANGE &&
            existing.pendingRuleChange == pending
        ) return
        viewModelScope.launch {
            settingsStore.recordStrictCooldownRequest(
                kind = StrictPendingKind.RULE_CHANGE,
                value = StrictRuleChange.encode(pending),
                durationHours = _uiState.value.strictCooldownHours,
            )
        }
    }

    fun cancelStrictUnlock() {
        pendingDelayChange = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    /** اعمال تغییر تاخیرِ بازگشایی‌شده (سراسری یا اختصاصی یک قانون). */
    private suspend fun applyStrictRuleChange(change: StrictRuleChange) {
        if (change.action != StrictRuleChange.ACTION_DELAY) return
        when (change.scope) {
            StrictRuleChange.SCOPE_GLOBAL -> {
                change.value.toIntOrNull()?.let { settingsStore.setGlobalDelay(it) }
            }
            StrictRuleChange.SCOPE_APP -> {
                appRepository.setDelay(change.id, if (change.value == "null") null else change.value.toIntOrNull())
            }
            StrictRuleChange.SCOPE_WEBSITE -> {
                val id = change.id.toLongOrNull() ?: return
                websiteRepository.setDelay(id, if (change.value == "null") null else change.value.toIntOrNull())
            }
            StrictRuleChange.SCOPE_KEYWORD -> {
                val id = change.id.toLongOrNull() ?: return
                keywordRepository.setDelay(id, if (change.value == "null") null else change.value.toIntOrNull())
            }
        }
    }
}

data class PinUiState(
    val pinSet: Boolean = false,
    val message: String? = null,
)

data class ThemeUiState(
    val theme: AppTheme = AppTheme.DARK,
    val colorTheme: ColorTheme = ColorTheme.SADD,
    /** دیالوگ بازگشایی Strict برای تغییر تم/رنگ‌بندی باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده (اگر هست) برای دیالوگ. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    /** تغییر تم/رنگ‌بندیِ در انتظار بازگشایی Strict (فقط در حافظه). */
    private var pendingSettingChange: StrictSettingChange? = null

    init {
        viewModelScope.launch {
            settingsStore.theme.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            settingsStore.colorTheme.collect { colorTheme ->
                _uiState.update { it.copy(colorTheme = colorTheme) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictUnlockMethod.collect { method ->
                _uiState.update { it.copy(strictUnlockMethod = method) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownHours.collect { hours ->
                _uiState.update { it.copy(strictCooldownHours = hours) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownRequest.collect { request ->
                _uiState.update { it.copy(strictCooldownRequest = request) }
            }
        }
    }

    /**
     * تغییر تم (روشن/تیره/سیستمی). وسط سشن Strict فقط بعد از بازگشایی
     * Strict (تایپ متن / انتظار) انجام می‌شود.
     */
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            if (theme == _uiState.value.theme) return@launch
            if (!strictGuard.isStrictSessionActive()) {
                settingsStore.setTheme(theme)
                return@launch
            }
            requestSettingChange(StrictSettingChange.theme(theme.name))
        }
    }

    /** تغییر رنگ‌بندی (گیت مثل [setTheme]). */
    fun setColorTheme(colorTheme: ColorTheme) {
        viewModelScope.launch {
            if (colorTheme == _uiState.value.colorTheme) return@launch
            if (!strictGuard.isStrictSessionActive()) {
                settingsStore.setColorTheme(colorTheme)
                return@launch
            }
            requestSettingChange(StrictSettingChange.colorTheme(colorTheme.name))
        }
    }

    /** درخواست تغییر تم/رنگ‌بندی وسط سشن Strict: دیالوگ بازگشایی باز می‌شود. */
    private fun requestSettingChange(change: StrictSettingChange) {
        pendingSettingChange = change
        _uiState.update { it.copy(strictUnlockVisible = true) }
    }

    /** اعمال تغییر تم/رنگ‌بندی بعد از بازگشایی موفق در دیالوگ Strict. */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            val change = pendingSettingChange
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.SETTING_CHANGE }?.pendingSettingChange
                ?: return@launch
            if (change.key != StrictSettingChange.KEY_THEME &&
                change.key != StrictSettingChange.KEY_COLOR_THEME
            ) return@launch
            if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = state.strictCooldownRequest
                if (request == null || !StrictUnlock.isCooldownElapsed(request, now) ||
                    request.pendingSettingChange != change
                ) {
                    if (request != null && request.pendingSettingChange != change) {
                        pendingSettingChange = null
                        _uiState.update { it.copy(strictUnlockVisible = false) }
                    }
                    return@launch
                }
                settingsStore.clearStrictCooldownRequest()
            }
            pendingSettingChange = null
            _uiState.update { it.copy(strictUnlockVisible = false) }
            applySettingChange(change)
        }
    }

    /** ثبت درخواست انتظار برای تغییر تم/رنگ‌بندی (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        val pending = pendingSettingChange ?: return
        val existing = _uiState.value.strictCooldownRequest
        // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
        if (existing?.pendingKind == StrictPendingKind.SETTING_CHANGE &&
            existing.pendingSettingChange == pending
        ) return
        viewModelScope.launch {
            settingsStore.recordStrictCooldownRequest(
                kind = StrictPendingKind.SETTING_CHANGE,
                value = StrictSettingChange.encode(pending),
                durationHours = _uiState.value.strictCooldownHours,
            )
        }
    }

    fun cancelStrictUnlock() {
        pendingSettingChange = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    /** اعمال تغییر تم/رنگ‌بندیِ بازگشایی‌شده (فقط کلیدهای مال این ViewModel). */
    private suspend fun applySettingChange(change: StrictSettingChange) {
        when (change.key) {
            StrictSettingChange.KEY_THEME -> {
                runCatching { AppTheme.valueOf(change.value) }
                    .onSuccess { settingsStore.setTheme(it) }
            }
            StrictSettingChange.KEY_COLOR_THEME -> {
                runCatching { ColorTheme.valueOf(change.value) }
                    .onSuccess { settingsStore.setColorTheme(it) }
            }
        }
    }
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.pinSet.collect { pinSet ->
                _uiState.update { it.copy(pinSet = pinSet) }
            }
        }
    }

    /** تنظیم PIN اول. */
    fun setPin(newPin: String, confirmPin: String) {
        viewModelScope.launch {
            if (newPin != confirmPin) {
                _uiState.update { it.copy(message = "تکرار PIN با PIN جدید یکی نیست.") }
                return@launch
            }
            if (!PinCrypto.isValidPin(newPin)) {
                _uiState.update { it.copy(message = "PIN باید ۴ تا ۸ رقم باشد.") }
                return@launch
            }
            val ok = settingsStore.setPin(newPin)
            if (ok) {
                _uiState.update { it.copy(message = "PIN تنظیم شد.") }
            } else {
                _uiState.update { it.copy(message = "PIN معتبر نیست.") }
            }
        }
    }

    /** تغییر PIN — نیازمند PIN فعلی. */
    fun changePin(oldPin: String, newPin: String, confirmPin: String) {
        viewModelScope.launch {
            when (val check = settingsStore.verify(oldPin)) {
                is PinCheckResult.Ok -> Unit
                is PinCheckResult.Wrong -> {
                    _uiState.update {
                        it.copy(message = "PIN فعلی اشتباه است. ${check.attemptsLeft} تلاش باقی مانده است.")
                    }
                    return@launch
                }
                is PinCheckResult.Locked -> {
                    val seconds = ((check.lockedUntilMs - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(1)
                    _uiState.update {
                        it.copy(message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.")
                    }
                    return@launch
                }
                is PinCheckResult.NotSet -> {
                    _uiState.update { it.copy(message = "PIN تنظیم نشده است.") }
                    return@launch
                }
            }
            setPin(newPin, confirmPin)
        }
    }

    /** حذف PIN — نیازمند PIN فعلی. */
    fun removePin(oldPin: String) {
        viewModelScope.launch {
            when (val check = settingsStore.verify(oldPin)) {
                is PinCheckResult.Ok -> Unit
                is PinCheckResult.Wrong -> {
                    _uiState.update {
                        it.copy(message = "PIN فعلی اشتباه است. ${check.attemptsLeft} تلاش باقی مانده است.")
                    }
                    return@launch
                }
                is PinCheckResult.Locked -> {
                    val seconds = ((check.lockedUntilMs - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(1)
                    _uiState.update {
                        it.copy(message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.")
                    }
                    return@launch
                }
                is PinCheckResult.NotSet -> {
                    _uiState.update { it.copy(message = "PIN تنظیم نشده است.") }
                    return@launch
                }
            }
            settingsStore.clearPin()
            _uiState.update { it.copy(message = "PIN حذف شد.") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
