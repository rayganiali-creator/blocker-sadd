package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.KeyboardThemes
import com.sadd.blocker.data.datastore.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel تب «کیبورد هوشمند»: فقط تم رنگی کیبورد را نگه می‌دارد.
 * همان یک ترجیح DataStore است که خود کیبورد (دکمه رنگ نوار ابزار)
 * هم می‌نویسد — انتخاب از هر دو جا یکی است.
 */
@HiltViewModel
class SmartKeyboardViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /** کلید تم رنگی فعلی کیبورد (KeyboardTheme.key). */
    val keyboardThemeKey: StateFlow<String> = settingsStore.keyboardTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeyboardThemes.DEFAULT.key)

    fun setKeyboardTheme(key: String) {
        viewModelScope.launch {
            settingsStore.setKeyboardTheme(key)
        }
    }
}
