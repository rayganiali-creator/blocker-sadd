package com.sadd.blocker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sadd.blocker.core.PinCrypto
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.DelayOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore by preferencesDataStore(name = "blocker_settings")

/**
 * تنظیمات + امنیت PIN در DataStore.
 * PIN هرگز plaintext ذخیره نمی‌شود: فقط SHA-256(salt + PIN) + salt نگه داشته می‌شود
 * (منطق هش در PinCrypto است تا تست‌پذیر بماند).
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val GLOBAL_DELAY = intPreferencesKey("global_delay_seconds")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BLOCK_MODE = stringPreferencesKey("block_mode")
        val PIN_HASH = stringPreferencesKey("pin_hash_hex")
        val PIN_SALT = stringPreferencesKey("pin_salt_hex")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val BATTERY_EXEMPTION_ASKED = booleanPreferencesKey("battery_exemption_asked")
    }

    val globalDelaySeconds: Flow<Int> = context.settingsDataStore.data
        .map { prefs -> prefs[GLOBAL_DELAY] ?: DelayOptions.DEFAULT_SECONDS }

    suspend fun setGlobalDelay(seconds: Int) {
        if (!DelayOptions.VALUES.contains(seconds)) return
        context.settingsDataStore.edit { prefs ->
            prefs[GLOBAL_DELAY] = seconds
        }
    }

    val darkTheme: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[DARK_THEME] ?: true }

    suspend fun setDarkTheme(dark: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DARK_THEME] = dark
        }
    }

    val blockMode: Flow<BlockMode> = context.settingsDataStore.data
        .map { prefs ->
            val raw = prefs[BLOCK_MODE] ?: BlockMode.NORMAL.name
            runCatching { BlockMode.valueOf(raw) }.getOrDefault(BlockMode.NORMAL)
        }

    suspend fun setBlockMode(mode: BlockMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[BLOCK_MODE] = mode.name
        }
    }

    val pinSet: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[PIN_HASH] != null }

    val onboardingDone: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[ONBOARDING_DONE] = done
        }
    }

    /** ذخیره PIN جدید؛ false یعنی PIN معتبر نیست (۴ تا ۸ رقم ASCII). */
    suspend fun setPin(pin: String): Boolean {
        if (!PinCrypto.isValidPin(pin)) return false
        val salt = PinCrypto.generateSaltHex()
        val hash = PinCrypto.hashPin(pin, salt)
        context.settingsDataStore.edit { prefs ->
            prefs[PIN_HASH] = hash
            prefs[PIN_SALT] = salt
        }
        return true
    }

    suspend fun checkPin(pin: String): Boolean {
        val prefs = context.settingsDataStore.data.first()
        val hash = prefs[PIN_HASH] ?: return false
        val salt = prefs[PIN_SALT] ?: return false
        return PinCrypto.verifyPin(pin, salt, hash)
    }

    suspend fun clearPin() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(PIN_HASH)
            prefs.remove(PIN_SALT)
        }
    }
}
