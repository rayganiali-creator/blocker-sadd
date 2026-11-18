package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleAdd
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.AddKeywordResult
import com.sadd.blocker.domain.usecase.AddKeywordRuleUseCase
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeywordsUiState(
    val rules: List<KeywordRule> = emptyList(),
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

@HiltViewModel
class KeywordsViewModel @Inject constructor(
    private val repository: KeywordRepository,
    private val addUseCase: AddKeywordRuleUseCase,
    sessionRepository: SessionRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeywordsUiState())
    val uiState: StateFlow<KeywordsUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: KeywordRule? = null
    /** تغییر قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingStrictRuleChange: StrictRuleChange? = null
    /** افزودن قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingRuleAdd: StrictRuleAdd? = null

    init {
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

    fun addKeyword(rawText: String, category: String) {
        viewModelScope.launch {
            // وسط سشن Strict، افزودن قانون هم تغییر است و فقط بعد از
            // بازگشایی Strict (تایپ متن / انتظار) انجام می‌شود.
            if (strictGuard.isStrictSessionActive()) {
                pendingRuleAdd = StrictRuleAdd(StrictRuleChange.SCOPE_KEYWORD, rawText, category)
                _uiState.update { it.copy(strictUnlockVisible = true) }
                return@launch
            }
            applyAddKeyword(rawText, category)
        }
    }

    /** افزودن کلمه (مسیر آزاد و مسیر بعد از بازگشایی Strict). */
    private suspend fun applyAddKeyword(rawText: String, category: String) {
        val result = addUseCase.execute(rawText, category)
        if (result is AddKeywordResult.Added) {
            _uiState.update { it.copy(message = "کلمه «${result.rule.rawText}» اضافه شد.") }
        }
        if (result is AddKeywordResult.Duplicate) {
            _uiState.update { it.copy(message = "این کلمه قبلاً در لیست است.") }
        }
        if (result is AddKeywordResult.Invalid) {
            _uiState.update { it.copy(message = result.reason) }
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (!enabled && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.toggle(StrictRuleChange.SCOPE_KEYWORD, id.toString(), false))
                return@launch
            }
            repository.setEnabled(id, enabled)
        }
    }

    fun setDelay(id: Long, delaySeconds: Int?) {
        viewModelScope.launch {
            // تغییر تاخیر قانون فعال در سشن Strict هم محافظت‌شده است.
            val current = _uiState.value.rules.firstOrNull { it.id == id }
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.delay(StrictRuleChange.SCOPE_KEYWORD, id.toString(), delaySeconds))
                return@launch
            }
            repository.setDelay(id, delaySeconds)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            val current = _uiState.value.rules.firstOrNull { it.id == id }
            // H-2: در سشن Strict نمی‌توان قانون فعال را حذف کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.remove(StrictRuleChange.SCOPE_KEYWORD, id.toString()))
                return@launch
            }
            lastRemoved = current
            repository.remove(id)
            _uiState.update { it.copy(message = "«${current?.rawText ?: ""}» حذف شد.") }
        }
    }

    /**
     * درخواست بازگشایی Strict برای یک تغییر قانون: دیالوگ باز می‌شود؛
     * در روش انتظار، ثبت درخواست از خود دیالوگ انجام می‌شود
     * ([recordStrictCooldownRequest])، پس ساعت از لحظه درخواست کاربر می‌شمرد.
     */
    private fun requestStrictUnlock(change: StrictRuleChange) {
        pendingStrictRuleChange = change
        _uiState.update { it.copy(strictUnlockVisible = true) }
    }

    /**
     * اعمال افزودن قانون یا تغییر قانون بعد از بازگشایی موفق در دیالوگ.
     * در روش انتظار، درخواست باید برای همین کار ثبت شده و کامل گذشته باشد؛
     * اگر انتظار برای کار دیگری ثبت شده باشد، اعمال نمی‌شود (و درخواست
     * دست نمی‌خورد تا همان کارِ خودش با آن انجام شود).
     */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            val add = pendingRuleAdd
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.RULE_ADD }?.pendingRuleAdd
            if (add != null) {
                if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                    val request = state.strictCooldownRequest
                    if (request == null || !StrictUnlock.isCooldownElapsed(request, now) ||
                        request.pendingRuleAdd != add
                    ) {
                        if (request != null && request.pendingRuleAdd != add) {
                            pendingRuleAdd = null
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
                pendingRuleAdd = null
                _uiState.update { it.copy(strictUnlockVisible = false) }
                if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.RULE_ADD) {
                    settingsStore.clearStrictCooldownRequest()
                }
                applyRuleAdd(add)
                return@launch
            }
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

    /** ثبت درخواست انتظار برای افزودن قانون یا تغییر قانون (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        val add = pendingRuleAdd
        if (add != null) {
            val existing = _uiState.value.strictCooldownRequest
            // درخواست تکراری همان افزودن: ساعت از اول شمرده نمی‌شود.
            if (existing?.pendingKind == StrictPendingKind.RULE_ADD &&
                existing.pendingRuleAdd == add
            ) return
            viewModelScope.launch {
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.RULE_ADD,
                    value = StrictRuleAdd.encode(add),
                    durationHours = _uiState.value.strictCooldownHours,
                )
            }
            return
        }
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
        pendingRuleAdd = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    /** افزودن قانونِ بازگشایی‌شده (فقط scope همین صفحه). */
    private suspend fun applyRuleAdd(add: StrictRuleAdd) {
        if (add.scope != StrictRuleChange.SCOPE_KEYWORD) return
        applyAddKeyword(add.id, add.value)
    }

    /** اعمال تغییر قانونِ بازگشایی‌شده (فقط scope همین صفحه). */
    private suspend fun applyStrictRuleChange(change: StrictRuleChange) {
        if (change.scope != StrictRuleChange.SCOPE_KEYWORD) return
        val id = change.id.toLongOrNull() ?: return
        when (change.action) {
            StrictRuleChange.ACTION_TOGGLE -> {
                repository.setEnabled(id, change.value.toBooleanStrictOrNull() ?: return)
            }
            StrictRuleChange.ACTION_REMOVE -> repository.remove(id)
            StrictRuleChange.ACTION_DELAY -> {
                repository.setDelay(id, if (change.value == "null") null else change.value.toIntOrNull())
            }
        }
    }

    /** بازگردانی آخرین قانون حذف‌شده (M-12: undo). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            val newId = repository.add(removed.copy(id = 0))
            if (newId == -1L) {
                _uiState.update { it.copy(message = "این قانون در لیست است.") }
            } else {
                _uiState.update { it.copy(message = "«${removed.rawText}» برگردانده شد.") }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
