package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.domain.model.SectionRule
import com.sadd.blocker.domain.repository.SectionRuleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionRulesUiState(
    val rules: List<SectionRule> = emptyList(),
    val message: String? = null,
)

/**
 * ViewModel بخش‌های اپ — پیش‌فرض‌ها (جستجوی تلگرام/اینستاگرام/یوتیوب +
 * اکسپلور اینستاگرام) در اولین باز شدن seed می‌شوند؛ هر بخش تاگل دارد.
 * مثل بقیه صفحه‌های قوانین، در سشن Strict فعال نمی‌توان قانون فعال را
 * غیرفعال کرد (H-2).
 */
@HiltViewModel
class SectionRulesViewModel @Inject constructor(
    private val repository: SectionRuleRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionRulesUiState())
    val uiState: StateFlow<SectionRulesUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)

    init {
        viewModelScope.launch {
            repository.ensurePresets()
        }
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(rules = list) }
            }
        }
    }

    fun setEnabled(packageName: String, sectionKey: String, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد.
            if (!enabled && strictGuard.isStrictSessionActive()) {
                _uiState.update { it.copy(message = "در سشن Strict فعال نمی‌توان قانون را غیرفعال کرد.") }
                return@launch
            }
            repository.setEnabled(packageName, sectionKey, enabled)
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
