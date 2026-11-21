package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.InstalledAppInfo
import com.sadd.blocker.data.system.InstalledAppsProvider
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.AddBlockedAppUseCase
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppsUiState(
    val installed: List<InstalledAppInfo> = emptyList(),
    val blocked: List<BlockedApp> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
    val selected: Set<String> = emptySet(),
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
 * ViewModel بلاک اپ‌ها — فاز ۲.
 * لیست اپ‌های نصب‌شده از PackageManager خوانده می‌شود؛
 * قوانین در Room ذخیره می‌شوند و با Flow زنده به‌روز می‌شوند.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val repository: BlockedAppRepository,
    private val addUseCase: AddBlockedAppUseCase,
    private val installedAppsProvider: InstalledAppsProvider,
    sessionRepository: SessionRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: BlockedApp? = null
    /** تغییر قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingStrictRuleChange: StrictRuleChange? = null

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(blocked = list) }
            }
        }
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) { installedAppsProvider.load() }
            _uiState.update { it.copy(installed = installed, loading = false) }
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
        // پاک‌سازی یک‌باره درخواست انتظارِ قدیمیِ «افزودن قانون»: افزودن
        // قانون دیگر گیت ندارد (فقط بلاک را قوی‌تر می‌کند)، پس
        // درخواستِ ثبت‌شده قبل از این تغییر نباید در ModesScreen به‌صورت
        // کارت شمارش معکوسِ گیرکرده بماند؛ درخواست RULE_CHANGE دست
        // نمی‌خورد (هنوز گیت دارد).
        viewModelScope.launch {
            val request = settingsStore.strictCooldownRequest.firstOrNull()
            if (request?.pendingKind == StrictPendingKind.RULE_ADD) {
                settingsStore.clearStrictCooldownRequest()
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun toggleSelect(packageName: String) {
        _uiState.update { state ->
            val selected = state.selected.toMutableSet()
            if (selected.contains(packageName)) {
                selected.remove(packageName)
            } else {
                selected.add(packageName)
            }
            state.copy(selected = selected)
        }
    }

    fun addSelected() {
        viewModelScope.launch {
            val packages = _uiState.value.selected.toList()
            // افزودن قانون وسط سشن Strict آزاد است: فقط بلاک را قوی‌تر
            // می‌کند (مثل تمدید سشن و افزودن زمان‌بندی). گیت Strict فقط
            // روی حذف/غیرفعال/ویرایش قانونِ موجود است.
            applyAddSelected(packages)
        }
    }

    /**
     * افزودن دسته‌ای اپ‌ها (مسیر آزاد و مسیر بعد از بازگشایی Strict).
     * برچسب‌ها موقع اعمال، از لیست اپ‌های نصب‌شده بازسازی می‌شوند.
     */
    private suspend fun applyAddSelected(packages: List<String>) {
        val state = _uiState.value
        val toAdd = state.installed
            .filter { packages.contains(it.packageName) }
            .map { it.packageName to it.label }
        addUseCase.executeAll(toAdd)
        _uiState.update {
            it.copy(selected = emptySet(), message = "${toAdd.size} اپ به لیست بلاک اضافه شد.")
        }
    }

    fun setEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (!enabled && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.toggle(StrictRuleChange.SCOPE_APP, packageName, false))
                return@launch
            }
            repository.setEnabled(packageName, enabled)
        }
    }

    fun setDelay(packageName: String, delaySeconds: Int?) {
        viewModelScope.launch {
            // تغییر تاخیر قانون فعال در سشن Strict هم محافظت‌شده است.
            val current = _uiState.value.blocked.firstOrNull { it.packageName == packageName }
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.delay(StrictRuleChange.SCOPE_APP, packageName, delaySeconds))
                return@launch
            }
            repository.setDelay(packageName, delaySeconds)
        }
    }

    fun remove(packageName: String) {
        viewModelScope.launch {
            val current = _uiState.value.blocked.firstOrNull { it.packageName == packageName }
            // H-2: در سشن Strict نمی‌توان قانون فعال را حذف کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.remove(StrictRuleChange.SCOPE_APP, packageName))
                return@launch
            }
            lastRemoved = current
            repository.remove(packageName)
            _uiState.update { it.copy(message = "«${current?.label ?: packageName}» حذف شد.") }
        }
    }

    /** درخواست بازگشایی Strict برای یک تغییر قانون (دیالوگ باز می‌شود). */
    private fun requestStrictUnlock(change: StrictRuleChange) {
        pendingStrictRuleChange = change
        _uiState.update { it.copy(strictUnlockVisible = true) }
    }

    /**
     * اعمال تغییر قانون بعد از بازگشایی موفق در دیالوگ.
     * در روش انتظار، درخواست باید برای همین کار ثبت شده و کامل گذشته باشد.
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
        if (change.scope != StrictRuleChange.SCOPE_APP) return
        when (change.action) {
            StrictRuleChange.ACTION_TOGGLE -> {
                repository.setEnabled(change.id, change.value.toBooleanStrictOrNull() ?: return)
            }
            StrictRuleChange.ACTION_REMOVE -> repository.remove(change.id)
            StrictRuleChange.ACTION_DELAY -> {
                repository.setDelay(change.id, if (change.value == "null") null else change.value.toIntOrNull())
            }
        }
    }

    /** بازگردانی آخرین قانون حذف‌شده (M-12: undo). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            repository.add(removed)
            _uiState.update { it.copy(message = "«${removed.label}» برگردانده شد.") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
