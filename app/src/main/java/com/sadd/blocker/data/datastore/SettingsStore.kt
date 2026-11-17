package com.sadd.blocker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore by preferencesDataStore(name = "blocker_settings")

/**
 * تنظیمات + امنیت PIN در DataStore.
 * PIN هرگز plaintext ذخیره نمی‌شود: فقط SHA-256(salt + PIN) + salt نگه داشته می‌شود.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val GLOBAL_DELAY = intPreferencesKey("global_delay_seconds")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val PIN_HASH = stringPreferencesKey("pin_hash_hex")
        val PIN_SALT = stringPreferencesKey("pin_salt_hex")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val BATTERY_EXEMPTION_ASKED = booleanPreferencesKey("battery_exemption_asked")
    }

    val globalDelaySeconds: Flow<Int> = context.settingsDataStore.data
        .map { prefs -> prefs[GLOBAL_DELAY] ?: 5 }

    val darkTheme: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[DARK_THEME] ?: true }

    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256Hex(salt + pin.toByteArray(Charsets.UTF_8))
        context.settingsDataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(PIN_HASH, hash)
                set(PIN_SALT, salt.toHex())
            }
        }
    }

    suspend fun checkPin(pin: String): Boolean {
        val prefs = context.settingsDataStore.data.first()
        val hash = prefs[PIN_HASH] ?: return false
        val salt = prefs[PIN_SALT]?.hexToBytes() ?: return false
        return sha256Hex(salt + pin.toByteArray(Charsets.UTF_8)) == hash
    }

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
