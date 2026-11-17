package com.sadd.blocker.ui

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
import com.sadd.blocker.domain.usecase.EndSessionUseCase
import com.sadd.blocker.domain.usecase.StartSessionResult
import com.sadd.blocker.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

/**
 * ViewModel داشبورد — فاز ۲.
 * باقی‌مانده سشن هر ثانیه از endEpochMs محاسبه می‌شود (نه تایمر در حافظه)؛
 * تعداد قوانین و سلامت مجوزها از Room و سیستم خوانده می‌شوند، نه نمایشی.
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

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        viewModelScope.launch {
            while (true) {
                tickCount++
                refresh()
                delay(1000)
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(missingPermissions = permissionHealth.missingCount()) }
        }
    }

    fun startSession(durationMs: Long) {
        viewModelScope.launch {
            val strict = _uiState.value.blockMode == BlockMode.STRICT
            val result = startSessionUseCase.execute(durationMs, strict)
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

    fun endSession() {
        viewModelScope.launch {
            val ended = endSessionUseCase.execute("user")
            if (ended) {
                _uiState.update { it.copy(message = "سشن پایان یافت.") }
            }
            refresh()
        }
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
        val apps = appRepository.countEnabled()
        val websites = websiteRepository.countEnabled()
        val keywords = keywordRepository.countEnabled()
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
                appsCount = apps,
                websitesCount = websites,
                keywordsCount = keywords,
                activeRulesCount = apps + websites + keywords,
                missingPermissions = missing,
            )
        }
    }
}
