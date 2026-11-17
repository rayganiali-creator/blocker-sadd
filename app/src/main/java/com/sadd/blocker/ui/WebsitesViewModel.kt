package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.AddWebsiteResult
import com.sadd.blocker.domain.usecase.AddWebsiteUseCase
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebsitesUiState(
    val websites: List<BlockedWebsite> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class WebsitesViewModel @Inject constructor(
    private val repository: WebsiteRepository,
    private val addUseCase: AddWebsiteUseCase,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebsitesUiState())
    val uiState: StateFlow<WebsitesUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: BlockedWebsite? = null

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(websites = list) }
            }
        }
    }

    fun addWebsite(rawHost: String, matchSubdomains: Boolean) {
        viewModelScope.launch {
            val result = addUseCase.execute(rawHost, matchSubdomains)
            if (result is AddWebsiteResult.Added) {
                _uiState.update { it.copy(message = "وب‌سایت ${result.site.host} اضافه شد.") }
            }
            if (result is AddWebsiteResult.Duplicate) {
                _uiState.update { it.copy(message = "این دامنه قبلاً در لیست است.") }
            }
            if (result is AddWebsiteResult.Invalid) {
                _uiState.update { it.copy(message = result.reason) }
            }
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد.
            if (!enabled && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان قانون را غیرفعال کرد.") }
                return@launch
            }
            repository.setEnabled(id, enabled)
        }
    }

    fun setDelay(id: Long, delaySeconds: Int?) {
        viewModelScope.launch {
            repository.setDelay(id, delaySeconds)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            val current = _uiState.value.websites.firstOrNull { it.id == id }
            // H-2: در سشن Strict نمی‌توان قانون فعال را حذف کرد.
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان قانون فعال را حذف کرد.") }
                return@launch
            }
            lastRemoved = current
            repository.remove(id)
            _uiState.update { it.copy(message = "${current?.host ?: ""} حذف شد.") }
        }
    }

    /** بازگردانی آخرین قانون حذف‌شده (M-12: undo). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            val newId = repository.add(removed.copy(id = 0))
            if (newId == -1L) {
                _uiState.update { it.copy(message = "این دامنه در لیست است.") }
            } else {
                _uiState.update { it.copy(message = "${removed.host} برگردانده شد.") }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
