package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.usecase.AddKeywordResult
import com.sadd.blocker.domain.usecase.AddKeywordRuleUseCase
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeywordsUiState())
    val uiState: StateFlow<KeywordsUiState> = _uiState.asStateFlow()

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
