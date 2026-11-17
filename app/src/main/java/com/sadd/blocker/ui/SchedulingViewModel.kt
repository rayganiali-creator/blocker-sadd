package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.Schedule
import com.sadd.blocker.domain.repository.ScheduleRepository
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulingUiState())
    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()

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
            if (start >= end) {
                _uiState.update { it.copy(message = "ساعت پایان باید بعد از ساعت شروع باشد.") }
                return@launch
            }
            repository.add(
                Schedule(
                    daysOfWeek = daysOfWeek.joinToString(","),
                    startMinute = start,
                    endMinute = end,
                    strict = strict,
                ),
            )
            _uiState.update { it.copy(message = "زمان‌بندی اضافه شد.") }
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            repository.remove(id)
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
