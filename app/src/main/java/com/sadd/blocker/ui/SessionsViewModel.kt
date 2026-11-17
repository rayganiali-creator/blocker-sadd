package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.EndSessionUseCase
import com.sadd.blocker.domain.usecase.ExtendSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val active: BlockingSession? = null,
    val remainingText: String = "",
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val history: List<BlockingSession> = emptyList(),
    val pinRequired: Boolean = false,
    val message: String? = null,
)

/**
 * ViewModel سشن‌ها — فاز ۲.
 * پایان سشن Strict وقتی PIN تنظیم شده باشد، فقط با PIN درست انجام می‌شود؛
 * در غیر این صورت UI باید PIN بخواهد (pinRequired در state).
 */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val endSessionUseCase: EndSessionUseCase,
    private val extendSessionUseCase: ExtendSessionUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()
    private var tickCount = 0

    init {
        viewModelScope.launch {
            while (true) {
                tickCount++
                refresh()
                delay(1000)
            }
        }
    }

    fun extendSession() {
        viewModelScope.launch {
            val newEnd = extendSessionUseCase.execute()
            if (newEnd != null) {
                _uiState.update { it.copy(message = "سشن ۱۵ دقیقه تمدید شد.") }
            }
            refresh()
        }
    }

    /** درخواست پایان سشن. اگر Strict + PIN باشد، اول pinRequired می‌شود. */
    fun requestEndSession() {
        viewModelScope.launch {
            val active = _uiState.value.active
            if (active == null) return@launch
            val pinSet = settingsStore.pinSet.first()
            if (active.strict && pinSet) {
                _uiState.update { it.copy(pinRequired = true) }
                return@launch
            }
            endNow("user")
        }
    }

    /** پایان سشن با PIN (فقط وقتی pinRequired است). */
    fun endSessionWithPin(pin: String) {
        viewModelScope.launch {
            if (!settingsStore.checkPin(pin)) {
                _uiState.update { it.copy(message = "PIN اشتباه است.") }
                return@launch
            }
            _uiState.update { it.copy(pinRequired = false) }
            endNow("user")
        }
    }

    fun cancelPinDialog() {
        _uiState.update { it.copy(pinRequired = false) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun endNow(reason: String) {
        val ended = endSessionUseCase.execute(reason)
        if (ended) {
            _uiState.update { it.copy(message = "سشن پایان یافت.") }
        }
        refresh()
    }

    private suspend fun refresh() {
        val now = System.currentTimeMillis()
        val active = sessionRepository.getActive(now)
        val remaining = if (active != null) {
            SessionCalculator.remainingMs(active.endEpochMs, now)
        } else {
            0L
        }
        val total = if (active != null) {
            active.endEpochMs - active.startEpochMs
        } else {
            0L
        }
        // تاریخچه به‌ندرت تغییر می‌کند؛ فقط هر ۱۰ ثانیه خوانده می‌شود.
        val history = if (tickCount % 10 == 1) {
            sessionRepository.history(50)
        } else {
            _uiState.value.history
        }
        _uiState.update {
            it.copy(
                active = if (active != null && remaining > 0) active else null,
                remainingText = TimeFormat.formatRemainingMs(remaining),
                remainingMs = remaining,
                totalMs = total,
                history = history,
            )
        }
    }
}
