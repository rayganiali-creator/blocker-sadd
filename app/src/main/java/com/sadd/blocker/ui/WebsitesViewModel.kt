package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.AddWebsiteResult
import com.sadd.blocker.domain.usecase.AddWebsiteUseCase
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebsitesUiState())
    val uiState: StateFlow<WebsitesUiState> = _uiState.asStateFlow()

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
            repository.remove(id)
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
