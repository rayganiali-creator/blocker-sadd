package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.system.InstalledAppInfo
import com.sadd.blocker.data.system.InstalledAppsProvider
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.AddBlockedAppUseCase
import com.sadd.blocker.domain.usecase.StrictSessionGuard
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
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: BlockedApp? = null

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
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد.
            if (!enabled && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان قانون را غیرفعال کرد.") }
                return@launch
            }
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
            val current = _uiState.value.blocked.firstOrNull { it.packageName == packageName }
            // H-2: در سشن Strict نمی‌توان قانون فعال را حذف کرد.
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان قانون فعال را حذف کرد.") }
                return@launch
            }
            lastRemoved = current
            repository.remove(packageName)
            _uiState.update { it.copy(message = "«${current?.label ?: packageName}» حذف شد.") }
        }
    }

    /** بازگردانی آخرین قانون حذف‌شده (M-12: undo). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            repository.add(removed)
            _uiState.update { it.copy(message = "«${removed.label}» برگردانده شد.") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
