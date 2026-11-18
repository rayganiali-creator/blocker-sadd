package com.sadd.blocker.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.UninstallProtection
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
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
    /** دیالوگ بازگشایی Strict برای پایان سشن Strict باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده برای پایان سشن، اگر هست. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
    val blockMode: BlockMode = BlockMode.NORMAL,
    val message: String? = null,
    /** محافظت از حذف نصب روشن است یا نه (تنظیم). */
    val uninstallProtectionOn: Boolean = false,
    /** «سد» الان مدیر دستگاه فعال است یا نه (وضعیت زنده سیستم). */
    val uninstallAdminActive: Boolean = false,
)

/**
 * ViewModel سشن‌ها — فاز ۲ + رفع H-1 و M-5.
 * پایان سشن Strict فقط با بازگشایی Strict (تایپ متن / انتظار) انجام می‌شود؛
 * داشبورد و اینجا یک دیالوگ و یک مسیر را استفاده می‌کنند.
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود ([setTicking]).
 */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val endSessionUseCase: EndSessionUseCase,
    private val extendSessionUseCase: ExtendSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val settingsStore: SettingsStore,
    private val uninstallProtection: UninstallProtection,
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
        viewModelScope.launch {
            settingsStore.uninstallProtectionEnabled.collect { enabled ->
                _uiState.update { it.copy(uninstallProtectionOn = enabled) }
            }
        }
        refreshAdminState()
        setTicking(true)
    }

    /** وضعیت زنده مدیر دستگاه را از سیستم می‌خواند (مثلاً بعد از برگشت از صفحه فعال‌سازی). */
    fun refreshAdminState() {
        viewModelScope.launch {
            val active = runCatching { uninstallProtection.isAdminActive() }.getOrDefault(false)
            _uiState.update { it.copy(uninstallAdminActive = active) }
        }
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

    /**
     * درخواست پایان سشن.
     * سشن غیر Strict مثل قبل مستقیم پایان می‌یابد. سشن Strict فقط با
     * بازگشایی Strict (تایپ متن / انتظار) پایان می‌یابد؛ دیالوگ بازگشایی
     * باز می‌شود و پایان واقعی در [confirmStrictUnlocked] انجام می‌شود.
     * PIN دیگر پایان سشن Strict را گیت نمی‌کند (جایگزین آن بازگشایی Strict است).
     */
    fun requestEndSession() {
        viewModelScope.launch {
            val active = sessionRepository.getActive(System.currentTimeMillis())
            if (active == null) {
                refresh()
                return@launch
            }
            if (!active.strict) {
                when (val result = endSessionUseCase.execute("user")) {
                    is EndSessionResult.Ended -> {
                        _uiState.update { it.copy(message = "سشن پایان یافت.") }
                    }
                    is EndSessionResult.NoActive -> Unit
                    else -> Unit
                }
            } else {
                _uiState.update { it.copy(strictUnlockVisible = true) }
            }
            refresh()
        }
    }

    /**
     * پایان سشن Strict بعد از بازگشایی موفق در دیالوگ.
     * در روش انتظار، درخواست باید برای پایان سشن ثبت شده و کامل گذشته باشد؛
     * انتظارِ ثبت‌شده برای کار دیگری اینجا خرج نمی‌شود.
     * پایان دوباره از مسیر واحد UseCase انجام می‌شود (H-1).
     */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = state.strictCooldownRequest
                val kindMatches = request?.pendingKind == StrictPendingKind.END_SESSION
                if (request == null || !StrictUnlock.isCooldownElapsed(request, now) || !kindMatches) {
                    if (request != null && !kindMatches) {
                        _uiState.update {
                            it.copy(
                                strictUnlockVisible = false,
                                message = "این انتظار برای کار دیگری ثبت شده است؛ برای پایان سشن دوباره درخواست بده.",
                            )
                        }
                    }
                    return@launch
                }
            }
            val active = sessionRepository.getActive(now)
            if (active == null) {
                _uiState.update {
                    it.copy(strictUnlockVisible = false, message = "سشنی فعال نیست.")
                }
                refresh()
                return@launch
            }
            when (endSessionUseCase.execute("user")) {
                is EndSessionResult.Ended -> {
                    if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.END_SESSION) {
                        settingsStore.clearStrictCooldownRequest()
                    }
                    _uiState.update { it.copy(strictUnlockVisible = false, message = "سشن پایان یافت.") }
                }
                is EndSessionResult.NoActive -> {
                    _uiState.update { it.copy(strictUnlockVisible = false, message = "سشنی فعال نیست.") }
                }
            }
            refresh()
        }
    }

    /** ثبت درخواست انتظار برای پایان سشن Strict (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        viewModelScope.launch {
            settingsStore.recordStrictCooldownRequest(
                kind = StrictPendingKind.END_SESSION,
                value = "",
                durationHours = _uiState.value.strictCooldownHours,
            )
        }
    }

    fun cancelStrictUnlock() {
        _uiState.update { it.copy(strictUnlockVisible = false) }
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
