package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.db.BlockingSessionDao
import com.sadd.blocker.data.db.BlockingSessionEntity
import com.sadd.blocker.data.db.KeywordRuleDao
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
    val activeRulesCount: Int = 0,
    val strict: Boolean = false,
)

/**
 * ViewModel داشبورد — اسکلت فاز ۱.
 * باقی‌مانده سشن هر ثانیه از endEpochMs محاسبه می‌شود (نه تایمر در حافظه).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionDao: BlockingSessionDao,
    private val keywordRuleDao: KeywordRuleDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(1000)
            }
        }
    }

    private suspend fun refresh() {
        val active = sessionDao.getActive()
        val rules = keywordRuleDao.getEnabled().size
        val remaining = if (active != null) {
            SessionCalculator.remainingMs(active.endEpochMs, System.currentTimeMillis())
        } else {
            0L
        }
        _uiState.update {
            it.copy(
                sessionActive = active != null && remaining > 0,
                remainingText = formatRemaining(remaining),
                activeRulesCount = rules,
                strict = active?.strict ?: false,
            )
        }
    }

    /** شروع سریع سشن ۱ ساعته (Normal). جزئیات کامل در فاز ۵. */
    fun startQuickSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            sessionDao.insert(
                BlockingSessionEntity(
                    startEpochMs = now,
                    endEpochMs = now + 60L * 60L * 1000L,
                    strict = false,
                ),
            )
            refresh()
        }
    }

    private fun formatRemaining(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
