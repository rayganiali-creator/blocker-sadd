package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionRulesUiState(
    val rules: List<SectionRule> = emptyList(),
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
 * ViewModel بخش‌های اپ — پیش‌فرض‌ها (۹ بخش: کل تلگرام/اینستاگرام/یوتیوب،
 * جستجوی هر سه، اکسپلور و ریلز اینستاگرام، شورتس یوتیوب) در اولین باز
 * شدن seed می‌شوند؛ هر بخش تاگل دارد.
 * مثل بقیه صفحه‌های قوانین، در سشن Strict فعال نمی‌توان قانون فعال را
 * غیرفعال کرد (H-2) — مگر بعد از بازگشایی Strict.
 */
@HiltViewModel
class SectionRulesViewModel @Inject constructor(
    private val repository: SectionRuleRepository,
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
