package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.Schedule
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchedulingUiState(
    val schedules: List<Schedule> = emptyList(),
    val message: String? = null,
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulingUiState())
    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: Schedule? = null

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(schedules = list) }
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
            // H-2: در سشن Strict نمی‌توان زمان‌بندی فعال را غیرفعال کرد.
            if (!enabled && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان زمان‌بندی را غیرفعال کرد.") }
                return@launch
            }
            repository.setEnabled(id, enabled)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            val current = _uiState.value.schedules.firstOrNull { it.id == id }
            // H-2: در سشن Strict نمی‌توان زمان‌بندی فعال را حذف کرد.
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان زمان‌بندی فعال را حذف کرد.") }
                return@launch
            }
            lastRemoved = current
            repository.remove(id)
            _uiState.update { it.copy(message = "زمان‌بندی حذف شد.") }
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
