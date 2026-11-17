package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.AddKeywordResult
import com.sadd.blocker.domain.usecase.AddKeywordRuleUseCase
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeywordsUiState(
    val rules: List<KeywordRule> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class KeywordsViewModel @Inject constructor(
    private val repository: KeywordRepository,
    private val addUseCase: AddKeywordRuleUseCase,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeywordsUiState())
    val uiState: StateFlow<KeywordsUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: KeywordRule? = null

    init {
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(rules = list) }
            }
        }
    }

    fun addKeyword(rawText: String, category: String) {
        viewModelScope.launch {
            val result = addUseCase.execute(rawText, category)
            if (result is AddKeywordResult.Added) {
                _uiState.update { it.copy(message = "کلمه «${result.rule.rawText}» اضافه شد.") }
            }
            if (result is AddKeywordResult.Duplicate) {
                _uiState.update { it.copy(message = "این کلمه قبلاً در لیست است.") }
            }
            if (result is AddKeywordResult.Invalid) {
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
            val current = _uiState.value.rules.firstOrNull { it.id == id }
            // H-2: در سشن Strict نمی‌توان قانون فعال را حذف کرد.
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان قانون فعال را حذف کرد.") }
                return@launch
            }
            lastRemoved = current
            repository.remove(id)
            _uiState.update { it.copy(message = "«${current?.rawText ?: ""}» حذف شد.") }
        }
    }

    /** بازگردانی آخرین قانون حذف‌شده (M-12: undo). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            val newId = repository.add(removed.copy(id = 0))
            if (newId == -1L) {
                _uiState.update { it.copy(message = "این قانون در لیست است.") }
            } else {
                _uiState.update { it.copy(message = "«${removed.rawText}» برگردانده شد.") }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
