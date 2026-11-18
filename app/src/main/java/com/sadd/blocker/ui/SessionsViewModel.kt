package com.sadd.blocker.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.EndSessionResult
import com.sadd.blocker.domain.usecase.EndSessionUseCase
import com.sadd.blocker.domain.usecase.ExtendSessionUseCase
import com.sadd.blocker.domain.usecase.StartSessionResult
import com.sadd.blocker.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val blockMode: BlockMode = BlockMode.NORMAL,
    val message: String? = null,
)

/**
 * ViewModel سشن‌ها — فاز ۲ + رفع H-1 و M-5.
 * پایان سشن Strict وقتی PIN تنظیم شده باشد، فقط با PIN درست انجام می‌شود؛
 * بررسی PIN در UseCase است (مسیر واحد پایان — داشبورد و اینجا یکی‌اند).
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود ([setTicking]).
 */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val endSessionUseCase: EndSessionUseCase,
    private val extendSessionUseCase: ExtendSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()
    private var tickCount = 0
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        setTicking(true)
    }

    /** تیکر شمارش معکوس را روشن/خاموش می‌کند (M-5: فقط وقتی صفحه دیده می‌شود). */
    fun setTicking(active: Boolean) {
        tickerJob?.cancel()
        tickerJob = null
        if (!active) return
        tickerJob = viewModelScope.launch {
            while (true) {
                tickCount++
                refresh()
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    /**
     * شروع سشن — همان رفتاری که قبلاً داشبورد داشت (از داشبورد به اینجا منتقل شد):
     * در حالت Strict سشن سخت‌گیرانه شروع می‌شود؛ نتیجه از UseCase می‌آید.
     */
    fun startSession(durationMs: Long) {
        viewModelScope.launch {
            val strict = _uiState.value.blockMode == BlockMode.STRICT
            val result = startSessionUseCase.execute(
                durationMs,
                strict,
                nowElapsedMs = SystemClock.elapsedRealtime(),
            )
            if (result is StartSessionResult.Started) {
                _uiState.update { it.copy(message = "سشن ${TimeFormat.durationLabel(durationMs)} شروع شد.") }
            }
            if (result is StartSessionResult.AlreadyActive) {
                _uiState.update { it.copy(message = "یک سشن فعال وجود دارد؛ اول آن را پایان بده.") }
            }
            if (result is StartSessionResult.InvalidDuration) {
                _uiState.update { it.copy(message = result.reason) }
            }
            refresh()
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
            when (val result = endSessionUseCase.execute("user")) {
                is EndSessionResult.Ended -> {
                    _uiState.update { it.copy(message = "سشن پایان یافت.") }
                }
                is EndSessionResult.PinRequired -> {
                    _uiState.update { it.copy(pinRequired = true) }
                }
                is EndSessionResult.WrongPin -> {
                    _uiState.update {
                        it.copy(message = "PIN اشتباه است. ${result.attemptsLeft} تلاش باقی مانده است.")
                    }
                }
                is EndSessionResult.Locked -> {
                    val seconds = ((result.lockedUntilMs - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(1)
                    _uiState.update {
                        it.copy(message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.")
                    }
                }
                is EndSessionResult.NoActive -> Unit
            }
            refresh()
        }
    }

    /** پایان سشن با PIN (فقط وقتی pinRequired است). */
    fun endSessionWithPin(pin: String) {
        viewModelScope.launch {
            when (val result = endSessionUseCase.execute("user", pin)) {
                is EndSessionResult.Ended -> {
                    _uiState.update { it.copy(pinRequired = false, message = "سشن پایان یافت.") }
                }
                is EndSessionResult.WrongPin -> {
                    _uiState.update {
                        it.copy(message = "PIN اشتباه است. ${result.attemptsLeft} تلاش باقی مانده است.")
                    }
                }
                is EndSessionResult.Locked -> {
                    val seconds = ((result.lockedUntilMs - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            pinRequired = false,
                            message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.",
                        )
                    }
                }
                else -> Unit
            }
            refresh()
        }
    }

    fun cancelPinDialog() {
        _uiState.update { it.copy(pinRequired = false) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
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
