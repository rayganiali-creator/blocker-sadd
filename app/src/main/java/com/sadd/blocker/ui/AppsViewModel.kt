package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.system.InstalledAppInfo
import com.sadd.blocker.data.system.InstalledAppsProvider
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.usecase.AddBlockedAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppsUiState(
    val installed: List<InstalledAppInfo> = emptyList(),
    val blocked: List<BlockedApp> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
    val selected: Set<String> = emptySet(),
    val message: String? = null,
)

/**
 * ViewModel بلاک اپ‌ها — فاز ۲.
 * لیست اپ‌های نصب‌شده از PackageManager خوانده می‌شود؛
 * قوانین در Room ذخیره می‌شوند و با Flow زنده به‌روز می‌شوند.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val repository: BlockedAppRepository,
    private val addUseCase: AddBlockedAppUseCase,
    private val installedAppsProvider: InstalledAppsProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(blocked = list) }
            }
        }
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) { installedAppsProvider.load() }
            _uiState.update { it.copy(installed = installed, loading = false) }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun toggleSelect(packageName: String) {
        _uiState.update { state ->
            val selected = state.selected.toMutableSet()
            if (selected.contains(packageName)) {
                selected.remove(packageName)
            } else {
                selected.add(packageName)
            }
            state.copy(selected = selected)
        }
    }

    fun addSelected() {
        viewModelScope.launch {
            val state = _uiState.value
            val toAdd = state.installed
                .filter { state.selected.contains(it.packageName) }
                .map { it.packageName to it.label }
            addUseCase.executeAll(toAdd)
            _uiState.update {
                it.copy(selected = emptySet(), message = "${toAdd.size} اپ به لیست بلاک اضافه شد.")
            }
        }
    }

    fun setEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(packageName, enabled)
        }
    }

    fun setDelay(packageName: String, delaySeconds: Int?) {
        viewModelScope.launch {
            repository.setDelay(packageName, delaySeconds)
        }
    }

    fun remove(packageName: String) {
        viewModelScope.launch {
            repository.remove(packageName)
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
