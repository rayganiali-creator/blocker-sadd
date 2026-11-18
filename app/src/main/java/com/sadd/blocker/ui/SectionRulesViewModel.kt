package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SectionDetector
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.InstalledAppInfo
import com.sadd.blocker.data.system.InstalledAppsProvider
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.model.SectionRule
import com.sadd.blocker.domain.repository.SectionRuleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SectionRulesUiState(
    val rules: List<SectionRule> = emptyList(),
    val installed: List<InstalledAppInfo> = emptyList(),
    val message: String? = null,
    /** دیالوگ بازگشایی Strict برای تغییر قانون محافظت‌شده باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده (اگر هست) برای دیالوگ. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
)

/**
 * ViewModel بخش‌های اپ — پیش‌فرض‌ها (۲۷ بخش: کل تلگرام/اینستاگرام/یوتیوب/
 * واتساپ/تیک‌تاک/ایکس/فیسبوک/اسنپ‌چت، جستجوی هر کدام، اکسپلور و ریلز
 * اینستاگرام، شورتس یوتیوب، وضعیت واتساپ، For You تیک‌تاک، خانه ایکس،
 * ریلز و واچ فیسبوک، اسپات‌لایت اسنپ‌چت) در اولین باز شدن seed می‌شوند؛
 * هر بخش تاگل دارد. کاربر می‌تواند هر اپ نصب‌شده دیگری را هم اضافه کند:
 * «خود اپ» (قطعی، تطابق پکیج) یا یک بخش سفارشی با متن نشانه (بهترین تلاش).
 * مثل بقیه صفحه‌های قوانین، در سشن Strict فعال نمی‌توان قانون فعال را
 * غیرفعال کرد (H-2) — مگر بعد از بازگشایی Strict. اضافه‌کردن قانون
 * (سخت‌تر کردن بلاک) وسط Strict آزاد است.
 */
@HiltViewModel
class SectionRulesViewModel @Inject constructor(
    private val repository: SectionRuleRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    sessionRepository: SessionRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionRulesUiState())
    val uiState: StateFlow<SectionRulesUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    /** تغییر قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingStrictRuleChange: StrictRuleChange? = null

    init {
        viewModelScope.launch {
            repository.ensurePresets()
        }
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) { installedAppsProvider.load() }
            _uiState.update { it.copy(installed = installed) }
        }
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(rules = list) }
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

    fun setEnabled(packageName: String, sectionKey: String, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (!enabled && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(
                    StrictRuleChange.toggle(
                        StrictRuleChange.SCOPE_SECTION,
                        "$packageName#$sectionKey",
                        false,
                    ),
                )
                return@launch
            }
            repository.setEnabled(packageName, sectionKey, enabled)
        }
    }

    /**
     * «خود اپ» را برای یک اپ نصب‌شده اضافه می‌کند (قطعی: باز شدن اپ
     * وسط سشن بلاک می‌شود). اگر آن اپ پیش‌فرض «خود اپ» را دارد،
     * تکراری ساخته نمی‌شود.
     */
    fun addWholeApp(packageName: String, appLabel: String) {
        viewModelScope.launch {
            if (SectionDetector.PRESETS.any { preset ->
                    preset.packageName == packageName &&
                        SectionDetector.isWholeAppSection(preset.sectionKey)
                }
            ) {
                _uiState.update { it.copy(message = "«خود $appLabel» از قبل وجود دارد؛ همان را فعال کن.") }
                return@launch
            }
            val added = repository.addRule(
                packageName,
                SectionDetector.SECTION_CUSTOM_APP,
                "خود $appLabel",
            )
            _uiState.update {
                it.copy(
                    message = if (added) "«خود $appLabel» اضافه شد." else "«خود $appLabel» از قبل وجود دارد.",
                )
            }
        }
    }

    /**
     * بخش سفارشی برای یک اپ نصب‌شده اضافه می‌کند: نام بخش را کاربر
     * می‌نویسد و متن نشانه (مثلاً «For You») را؛ وقتی آن متن روی
     * صفحه آن اپ دیده شود، وسط سشن بلاک می‌شود. بهترین تلاش است:
     * نشانه کوتاه یا عمومی بیش‌بلاک می‌کند و نشانه‌ای که در UI آن
     * اپ نیست هرگز بلاک نمی‌کند.
     */
    fun addCustomSection(packageName: String, appLabel: String, name: String, signal: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                _uiState.update { it.copy(message = "نام بخش را بنویس.") }
                return@launch
            }
            val sectionKey = SectionDetector.customSectionKey(signal)
            if (sectionKey == null) {
                _uiState.update { it.copy(message = "متن نشانه باید دست‌کم ۲ حرف باشد.") }
                return@launch
            }
            val added = repository.addRule(packageName, sectionKey, trimmedName)
            _uiState.update {
                it.copy(
                    message = if (added) {
                        "بخش «$trimmedName» برای $appLabel اضافه شد."
                    } else {
                        "این بخش از قبل وجود دارد."
                    },
                )
            }
        }
    }

    /** درخواست بازگشایی Strict برای یک تغییر قانون (دیالوگ باز می‌شود). */
    private fun requestStrictUnlock(change: StrictRuleChange) {
        pendingStrictRuleChange = change
        _uiState.update { it.copy(strictUnlockVisible = true) }
    }

    /**
     * اعمال تغییر قانون بعد از بازگشایی موفق در دیالوگ.
     * در روش انتظار، درخواست باید برای همین تغییر ثبت شده و کامل گذشته باشد.
     */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            val change = pendingStrictRuleChange
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.RULE_CHANGE }?.pendingRuleChange
            if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = state.strictCooldownRequest
                if (request == null || !StrictUnlock.isCooldownElapsed(request, now) ||
                    request.pendingRuleChange != change || change == null
                ) {
                    if (request != null && request.pendingRuleChange != change) {
                        pendingStrictRuleChange = null
                        _uiState.update {
                            it.copy(
                                strictUnlockVisible = false,
                                message = "این انتظار برای تغییر دیگری ثبت شده است؛ برای این تغییر دوباره درخواست بده.",
                            )
                        }
                    }
                    return@launch
                }
            }
            pendingStrictRuleChange = null
            _uiState.update { it.copy(strictUnlockVisible = false) }
            if (change == null) return@launch
            if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.RULE_CHANGE) {
                settingsStore.clearStrictCooldownRequest()
            }
            applyStrictRuleChange(change)
        }
    }

    /** ثبت درخواست انتظار برای تغییر قانون (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        val pending = pendingStrictRuleChange ?: return
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
        pendingStrictRuleChange = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    /** اعمال تغییر قانونِ بازگشایی‌شده (فقط scope همین صفحه). */
    private suspend fun applyStrictRuleChange(change: StrictRuleChange) {
        if (change.scope != StrictRuleChange.SCOPE_SECTION) return
        if (change.action != StrictRuleChange.ACTION_TOGGLE) return
        val parts = change.id.split("#")
        if (parts.size != 2) return
        repository.setEnabled(parts[0], parts[1], change.value.toBooleanStrictOrNull() ?: return)
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
