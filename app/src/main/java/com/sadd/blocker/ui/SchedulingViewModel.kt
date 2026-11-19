package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.Schedule
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchedulingUiState(
    val schedules: List<Schedule> = emptyList(),
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
 * ViewModel زمان‌بندی — فاز ۲: ذخیره و مدیریت دوره‌ها در Room.
 * اجرای خودکار دوره‌ها با WorkManager در فاز ۵ وصل می‌شود؛
 * تا آن زمان، دوره‌ها فقط ذخیره و نمایش داده می‌شوند.
 */
@HiltViewModel
class SchedulingViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    sessionRepository: SessionRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulingUiState())
    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: Schedule? = null
    /** تغییر قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingStrictRuleChange: StrictRuleChange? = null

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(schedules = list) }
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
        // پاک‌سازی یک‌باره درخواست انتظارِ قدیمیِ «افزودن زمان‌بندی»:
        // افزودن زمان‌بندی دیگر گیت ندارد، پس درخواستِ ثبت‌شده قبل از
        // این تغییر در ModesScreen به‌صورت کارت شمارش معکوسِ گیرکرده
        // می‌ماند؛ درخواست RULE_CHANGE دست نمی‌خورد (هنوز گیت دارد).
        viewModelScope.launch {
            val request = settingsStore.strictCooldownRequest.firstOrNull()
            if (request?.pendingKind == StrictPendingKind.SCHEDULE_ADD) {
                settingsStore.clearStrictCooldownRequest()
            }
        }
    }

    fun addSchedule(daysOfWeek: Set<String>, startText: String, endText: String, strict: Boolean) {
        viewModelScope.launch {
            val start = TimeFormat.parseMinuteOfDay(startText)
            val end = TimeFormat.parseMinuteOfDay(endText)
            if (daysOfWeek.isEmpty()) {
                _uiState.update { it.copy(message = "حداقل یک روز هفته را انتخاب کن.") }
                return@launch
            }
            if (start == null || end == null) {
                _uiState.update { it.copy(message = "ساعت را به‌صورت HH:MM وارد کن (مثل 08:30).") }
                return@launch
            }
            if (start == end) {
                _uiState.update { it.copy(message = "ساعت شروع و پایان نمی‌توانند یکی باشند.") }
                return@launch
            }
            // M-15: پنجره شبانه (پایان کوچک‌تر/مساوی شروع) یعنی عبور از نیمه‌شب.
            val overnight = end < start
            val days = daysOfWeek.joinToString(",")
            val duplicate = repository.list().any {
                it.daysOfWeek == days && it.startMinute == start && it.endMinute == end
            }
            if (duplicate) {
                _uiState.update { it.copy(message = "این زمان‌بندی قبلاً ثبت شده است.") }
                return@launch
            }
            // افزودن زمان‌بندی همیشه آزاد است، حتی وسط سشن Strict:
            // زمان‌بندی‌ها فعلاً فقط ذخیره و نمایش داده می‌شوند (اجرای
            // خودکار با WorkManager در فاز ۵ وصل می‌شود) و افزودنِ یکی
            // نه سشن فعلی را کوتاه/تمام می‌کند و نه بلاک را ضعیف می‌کند؛
            // در آینده هم زمان‌بندی فقط سشن شروع می‌کند، نه پایان می‌دهد.
            // (غیرفعال/حذف زمان‌بندی فعال هنوز گیت Strict دارد — H-2.)
            repository.add(
                Schedule(
                    daysOfWeek = days,
                    startMinute = start,
                    endMinute = end,
                    strict = strict,
                ),
            )
            _uiState.update {
                it.copy(
                    message = if (overnight) {
                        "زمان‌بندی شبانه اضافه شد (تا روز بعد ادامه دارد)."
                    } else {
                        "زمان‌بندی اضافه شد."
                    },
                )
            }
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان زمان‌بندی فعال را غیرفعال کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (!enabled && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.toggle(StrictRuleChange.SCOPE_SCHEDULE, id.toString(), false))
                return@launch
            }
            repository.setEnabled(id, enabled)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            val current = _uiState.value.schedules.firstOrNull { it.id == id }
            // H-2: در سشن Strict نمی‌توان زمان‌بندی فعال را حذف کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(StrictRuleChange.remove(StrictRuleChange.SCOPE_SCHEDULE, id.toString()))
                return@launch
            }
            lastRemoved = current
            repository.remove(id)
            _uiState.update { it.copy(message = "زمان‌بندی حذف شد.") }
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
     * (افزودن زمان‌بندی دیگر از این مسیر نمی‌گذرد؛ آزاد است.)
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
        if (change.scope != StrictRuleChange.SCOPE_SCHEDULE) return
        val id = change.id.toLongOrNull() ?: return
        when (change.action) {
            StrictRuleChange.ACTION_TOGGLE -> {
                repository.setEnabled(id, change.value.toBooleanStrictOrNull() ?: return)
            }
            StrictRuleChange.ACTION_REMOVE -> repository.remove(id)
        }
    }

    /** بازگردانی آخرین زمان‌بندی حذف‌شده (M-12: undo). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            repository.add(removed.copy(id = 0))
            _uiState.update { it.copy(message = "زمان‌بندی برگردانده شد.") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
