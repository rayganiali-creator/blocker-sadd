package com.sadd.blocker.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.EndSessionResult
import com.sadd.blocker.domain.usecase.EndSessionUseCase
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

data class DashboardUiState(
    val sessionActive: Boolean = false,
    val remainingText: String = "",
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val strict: Boolean = false,
    val blockMode: BlockMode = BlockMode.NORMAL,
    val appsCount: Int = 0,
    val websitesCount: Int = 0,
    val keywordsCount: Int = 0,
    val activeRulesCount: Int = 0,
    val missingPermissions: Int = 0,
    val message: String? = null,
    /** پایان سشن Strict نیازمند PIN است (H-1). */
    val endPinRequired: Boolean = false,
)

/**
 * ViewModel داشبورد — فاز ۲ + رفع H-1 و M-5.
 * باقی‌مانده سشن هر ثانیه از endEpochMs محاسبه می‌شود (نه تایمر در حافظه)؛
 * تعداد قوانین و سلامت مجوزها از Room و سیستم خوانده می‌شوند، نه نمایشی.
 *
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود (M-5): صفحه با
 * [setTicking] آن را در onResume روشن و در onPause خاموش می‌کند.
 * تعداد قوانین با Flow تغییرمحور خوانده می‌شود، نه در هر تیک.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val endSessionUseCase: EndSessionUseCase,
    private val permissionHealth: PermissionHealthChecker,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var tickCount = 0
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        // تعداد قوانین: فقط موقع تغییر جدول به‌روز می‌شود، نه هر ثانیه.
        viewModelScope.launch {
            appRepository.observe().collect { list ->
                val enabled = list.count { it.enabled }
                _uiState.update {
                    it.copy(appsCount = enabled, activeRulesCount = enabled + it.websitesCount + it.keywordsCount)
                }
            }
        }
        viewModelScope.launch {
            websiteRepository.observe().collect { list ->
                val enabled = list.count { it.enabled }
                _uiState.update {
                    it.copy(websitesCount = enabled, activeRulesCount = it.appsCount + enabled + it.keywordsCount)
                }
            }
        }
        viewModelScope.launch {
            keywordRepository.observe().collect { list ->
                val enabled = list.count { it.enabled }
                _uiState.update {
                    it.copy(keywordsCount = enabled, activeRulesCount = it.appsCount + it.websitesCount + enabled)
                }
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

    fun refreshPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(missingPermissions = permissionHealth.missingCount()) }
        }
    }

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

    /**
     * درخواست پایان سشن — مسیر واحد پایان (H-1): بررسی PIN در UseCase
     * انجام می‌شود، نه در ViewModel؛ داشبورد و صفحه سشن‌ها یکی‌اند.
     */
    fun requestEndSession() {
        viewModelScope.launch {
            when (val result = endSessionUseCase.execute("user")) {
                is EndSessionResult.Ended -> {
                    _uiState.update { it.copy(message = "سشن پایان یافت.") }
                }
                is EndSessionResult.PinRequired -> {
                    _uiState.update { it.copy(endPinRequired = true) }
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

    /** پایان سشن Strict با PIN (فقط وقتی endPinRequired است). */
    fun confirmEndSession(pin: String) {
        viewModelScope.launch {
            when (val result = endSessionUseCase.execute("user", pin)) {
                is EndSessionResult.Ended -> {
                    _uiState.update { it.copy(endPinRequired = false, message = "سشن پایان یافت.") }
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
                            endPinRequired = false,
                            message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.",
                        )
                    }
                }
                else -> Unit
            }
            refresh()
        }
    }

    fun cancelEndPin() {
        _uiState.update { it.copy(endPinRequired = false) }
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
        // سلامت مجوزها (شامل فراخوانی سیستمی VpnService.prepare) فقط هر ۱۰ ثانیه چک می‌شود.
        val missing = if (tickCount % 10 == 1) {
            permissionHealth.missingCount()
        } else {
            _uiState.value.missingPermissions
        }
        _uiState.update {
            it.copy(
                sessionActive = active != null && remaining > 0,
                remainingText = TimeFormat.formatRemainingMs(remaining),
                remainingMs = remaining,
                totalMs = total,
                strict = active?.strict ?: false,
                missingPermissions = missing,
            )
        }
    }
}
