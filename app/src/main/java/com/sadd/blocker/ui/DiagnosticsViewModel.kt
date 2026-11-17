package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.BuildConfig
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.data.system.PermissionStatus
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val permissions: List<PermissionStatus> = emptyList(),
    val appsCount: Int = 0,
    val websitesCount: Int = 0,
    val keywordsCount: Int = 0,
    val sessionActive: Boolean = false,
    val versionName: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val permissionHealth: PermissionHealthChecker,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val active = sessionRepository.getActive(now)
            _uiState.update {
                it.copy(
                    permissions = permissionHealth.checkAll(),
                    appsCount = appRepository.countEnabled(),
                    websitesCount = websiteRepository.countEnabled(),
                    keywordsCount = keywordRepository.countEnabled(),
                    sessionActive = active != null,
                )
            }
        }
    }
}
