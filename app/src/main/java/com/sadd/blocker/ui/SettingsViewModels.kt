package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.PinCrypto
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.data.datastore.ColorTheme
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.domain.repository.PinCheckResult
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModesUiState(
    val blockMode: BlockMode = BlockMode.NORMAL,
    val pinSet: Boolean = false,
)

@HiltViewModel
class ModesViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModesUiState())
    val uiState: StateFlow<ModesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsStore.pinSet.collect { pinSet ->
                _uiState.update { it.copy(pinSet = pinSet) }
            }
        }
    }

    fun setBlockMode(mode: BlockMode) {
        viewModelScope.launch {
            settingsStore.setBlockMode(mode)
        }
    }
}

/** تاخیر اختصاصی یک قانون؛ key شناسه بازنشانی است (پکیج اپ یا id عددی). */
data class RuleDelayOverride(val key: String, val label: String, val seconds: Int)

data class DelayUiState(
    val globalDelay: Int = DelayOptions.DEFAULT_SECONDS,
    val appOverrides: List<RuleDelayOverride> = emptyList(),
    val websiteOverrides: List<RuleDelayOverride> = emptyList(),
    val keywordOverrides: List<RuleDelayOverride> = emptyList(),
)

@HiltViewModel
class DelayViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DelayUiState())
    val uiState: StateFlow<DelayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.globalDelaySeconds.collect { seconds ->
                _uiState.update { it.copy(globalDelay = seconds) }
            }
        }
        viewModelScope.launch {
            appRepository.observe().collect { list ->
                val overrides = list
                    .filter { it.delaySeconds != null }
                    .map { RuleDelayOverride(it.packageName, it.label, it.delaySeconds!!) }
                _uiState.update { it.copy(appOverrides = overrides) }
            }
        }
        viewModelScope.launch {
            websiteRepository.observe().collect { list ->
                val overrides = list
                    .filter { it.delaySeconds != null }
                    .map { RuleDelayOverride(it.id.toString(), it.host, it.delaySeconds!!) }
                _uiState.update { it.copy(websiteOverrides = overrides) }
            }
        }
        viewModelScope.launch {
            keywordRepository.observe().collect { list ->
                val overrides = list
                    .filter { it.delaySeconds != null }
                    .map { RuleDelayOverride(it.id.toString(), it.rawText, it.delaySeconds!!) }
                _uiState.update { it.copy(keywordOverrides = overrides) }
            }
        }
    }

    fun setGlobalDelay(seconds: Int) {
        viewModelScope.launch {
            settingsStore.setGlobalDelay(seconds)
        }
    }

    fun resetAppDelay(packageName: String) {
        viewModelScope.launch {
            appRepository.setDelay(packageName, null)
        }
    }

    fun resetWebsiteDelay(key: String) {
        viewModelScope.launch {
            val id = key.toLongOrNull() ?: return@launch
            websiteRepository.setDelay(id, null)
        }
    }

    fun resetKeywordDelay(key: String) {
        viewModelScope.launch {
            val id = key.toLongOrNull() ?: return@launch
            keywordRepository.setDelay(id, null)
        }
    }
}

data class PinUiState(
    val pinSet: Boolean = false,
    val message: String? = null,
)

data class ThemeUiState(
    val theme: AppTheme = AppTheme.DARK,
    val colorTheme: ColorTheme = ColorTheme.SADD,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.theme.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            settingsStore.colorTheme.collect { colorTheme ->
                _uiState.update { it.copy(colorTheme = colorTheme) }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsStore.setTheme(theme)
        }
    }

    fun setColorTheme(colorTheme: ColorTheme) {
        viewModelScope.launch {
            settingsStore.setColorTheme(colorTheme)
        }
    }
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.pinSet.collect { pinSet ->
                _uiState.update { it.copy(pinSet = pinSet) }
            }
        }
    }

    /** تنظیم PIN اول. */
    fun setPin(newPin: String, confirmPin: String) {
        viewModelScope.launch {
            if (newPin != confirmPin) {
                _uiState.update { it.copy(message = "تکرار PIN با PIN جدید یکی نیست.") }
                return@launch
            }
            if (!PinCrypto.isValidPin(newPin)) {
                _uiState.update { it.copy(message = "PIN باید ۴ تا ۸ رقم باشد.") }
                return@launch
            }
            val ok = settingsStore.setPin(newPin)
            if (ok) {
                _uiState.update { it.copy(message = "PIN تنظیم شد.") }
            } else {
                _uiState.update { it.copy(message = "PIN معتبر نیست.") }
            }
        }
    }

    /** تغییر PIN — نیازمند PIN فعلی. */
    fun changePin(oldPin: String, newPin: String, confirmPin: String) {
        viewModelScope.launch {
            when (val check = settingsStore.verify(oldPin)) {
                is PinCheckResult.Ok -> Unit
                is PinCheckResult.Wrong -> {
                    _uiState.update {
                        it.copy(message = "PIN فعلی اشتباه است. ${check.attemptsLeft} تلاش باقی مانده است.")
                    }
                    return@launch
                }
                is PinCheckResult.Locked -> {
                    val seconds = ((check.lockedUntilMs - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(1)
                    _uiState.update {
                        it.copy(message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.")
                    }
                    return@launch
                }
                is PinCheckResult.NotSet -> {
                    _uiState.update { it.copy(message = "PIN تنظیم نشده است.") }
                    return@launch
                }
            }
            setPin(newPin, confirmPin)
        }
    }

    /** حذف PIN — نیازمند PIN فعلی. */
    fun removePin(oldPin: String) {
        viewModelScope.launch {
            when (val check = settingsStore.verify(oldPin)) {
                is PinCheckResult.Ok -> Unit
                is PinCheckResult.Wrong -> {
                    _uiState.update {
                        it.copy(message = "PIN فعلی اشتباه است. ${check.attemptsLeft} تلاش باقی مانده است.")
                    }
                    return@launch
                }
                is PinCheckResult.Locked -> {
                    val seconds = ((check.lockedUntilMs - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(1)
                    _uiState.update {
                        it.copy(message = "PIN موقتاً قفل است؛ $seconds ثانیه دیگر دوباره تلاش کن.")
                    }
                    return@launch
                }
                is PinCheckResult.NotSet -> {
                    _uiState.update { it.copy(message = "PIN تنظیم نشده است.") }
                    return@launch
                }
            }
            settingsStore.clearPin()
            _uiState.update { it.copy(message = "PIN حذف شد.") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
