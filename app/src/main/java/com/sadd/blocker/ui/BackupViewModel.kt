package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.BackupCodec
import com.sadd.blocker.domain.BackupData
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.RestoreBackupUseCase
import com.sadd.blocker.domain.usecase.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val message: String? = null,
    val exporting: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    private val scheduleRepository: ScheduleRepository,
    private val restoreUseCase: RestoreBackupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** ساخت JSON بکاپ از وضعیت فعلی دیتابیس. */
    suspend fun buildExportJson(): String {
        val data = BackupData(
            apps = appRepository.list(),
            websites = websiteRepository.list(),
            keywords = keywordRepository.list(),
            schedules = scheduleRepository.list(),
        )
        return BackupCodec.encode(data, System.currentTimeMillis())
    }

    fun importJson(json: String) {
        viewModelScope.launch {
            val result = restoreUseCase.execute(json)
            if (result is RestoreResult.Restored) {
                val s = result.summary
                _uiState.update {
                    it.copy(
                        message = "ریستور شد: ${s.appsAdded} اپ، ${s.websitesAdded} وب‌سایت، " +
                            "${s.keywordsAdded} کلمه، ${s.schedulesAdded} زمان‌بندی. " +
                            "${s.skipped} آیتم تکراری یا خراب skip شد.",
                    )
                }
            }
            if (result is RestoreResult.Failed) {
                _uiState.update { it.copy(message = "ریستور ناموفق بود: ${result.reason}") }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
