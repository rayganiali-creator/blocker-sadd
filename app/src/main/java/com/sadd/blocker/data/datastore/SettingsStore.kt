package com.sadd.blocker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sadd.blocker.core.PinCrypto
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.domain.repository.PinCheckResult
import com.sadd.blocker.domain.repository.PinGate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore by preferencesDataStore(name = "blocker_settings")

/** تم برنامه: روشن، تیره، یا پیروی از سیستم (M-6). */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * تنظیمات + امنیت PIN در DataStore.
 * PIN هرگز plaintext ذخیره نمی‌شود: فقط PBKDF2(salt, PIN) + salt نگه داشته
 * می‌شود (منطق هش در PinCrypto است تا تست‌پذیر بماند).
 *
 * خرابی فایل DataStore (M-9): همه Flowها خطای I/O را catch می‌کنند و
 * به پیش‌فرض‌ها برمی‌گردند تا اپ در استارت کرش نکند.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : PinGate {
    companion object {
        val GLOBAL_DELAY = intPreferencesKey("global_delay_seconds")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BLOCK_MODE = stringPreferencesKey("block_mode")
        val PIN_HASH = stringPreferencesKey("pin_hash_hex")
        val PIN_SALT = stringPreferencesKey("pin_salt_hex")
        val PIN_HASH_VERSION = intPreferencesKey("pin_hash_version")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKED_UNTIL = longPreferencesKey("pin_locked_until_ms")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val BATTERY_EXEMPTION_ASKED = booleanPreferencesKey("battery_exemption_asked")

        /** بعد از ۵ تلاش اشتباه، PIN قفل می‌شود (H-8). */
        const val PIN_MAX_ATTEMPTS = 5
        /** قفل اول ۳۰ ثانیه؛ هر تلاش اشتباه بعدی دو برابر، تا سقف ۱۵ دقیقه. */
        const val PIN_LOCK_BASE_SECONDS = 30L
        const val PIN_LOCK_MAX_SECONDS = 900L
    }

    /** Flow امن: خرابی DataStore به پیش‌فرض‌ها برمی‌گردد، نه کرش (M-9). */
    private val safeData: Flow<androidx.datastore.preferences.core.Preferences> =
        context.settingsDataStore.data
            .catch { e ->
                if (e is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw e
                }
            }

    val globalDelaySeconds: Flow<Int> = safeData
        .map { prefs -> prefs[GLOBAL_DELAY] ?: DelayOptions.DEFAULT_SECONDS }

    suspend fun setGlobalDelay(seconds: Int) {
        if (!DelayOptions.VALUES.contains(seconds)) return
        context.settingsDataStore.edit { prefs ->
            prefs[GLOBAL_DELAY] = seconds
        }
    }

    /**
     * تم فعلی. اگر کلید جدید نباشد، از کلید قدیمی dark_theme مهاجرت می‌کند
     * (true → DARK، false → LIGHT)؛ در غیر این صورت پیش‌فرض DARK است.
     */
    val theme: Flow<AppTheme> = safeData
        .map { prefs ->
            val raw = prefs[APP_THEME]
            if (raw != null) {
                runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.DARK)
            } else if (prefs[DARK_THEME] != null) {
                if (prefs[DARK_THEME] == true) AppTheme.DARK else AppTheme.LIGHT
            } else {
                AppTheme.DARK
            }
        }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_THEME] = theme.name
        }
    }

    val blockMode: Flow<BlockMode> = safeData
        .map { prefs ->
            val raw = prefs[BLOCK_MODE] ?: BlockMode.NORMAL.name
            runCatching { BlockMode.valueOf(raw) }.getOrDefault(BlockMode.NORMAL)
        }

    suspend fun setBlockMode(mode: BlockMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[BLOCK_MODE] = mode.name
        }
    }

    val pinSet: Flow<Boolean> = safeData
        .map { prefs -> prefs[PIN_HASH] != null }

    override suspend fun isPinSet(): Boolean {
        return safeData.first()[PIN_HASH] != null
    }

    val onboardingDone: Flow<Boolean> = safeData
        .map { prefs -> prefs[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[ONBOARDING_DONE] = done
        }
    }

    /** ذخیره PIN جدید؛ false یعنی PIN معتبر نیست (۴ تا ۸ رقم). قفل تلاش را صفر می‌کند. */
    suspend fun setPin(pin: String): Boolean {
        if (!PinCrypto.isValidPin(pin)) return false
        val salt = PinCrypto.generateSaltHex()
        val hash = PinCrypto.hashPin(pin, salt)
        context.settingsDataStore.edit { prefs ->
            prefs[PIN_HASH] = hash
            prefs[PIN_SALT] = salt
            prefs[PIN_HASH_VERSION] = PinCrypto.HASH_VERSION_V2_PBKDF2
            prefs.remove(PIN_FAILED_ATTEMPTS)
            prefs.remove(PIN_LOCKED_UNTIL)
        }
        return true
    }

    /**
     * بررسی PIN با شمارنده تلاش و قفل موقت تصاعدی (H-8).
     * هش‌های قدیمی نسخه ۱ (SHA-256) موقع ورود موفق، شفاف به PBKDF2
     * ارتقا پیدا می‌کنند.
     */
    override suspend fun verify(pin: String): PinCheckResult {
        val prefs = safeData.first()
        val hash = prefs[PIN_HASH] ?: return PinCheckResult.NotSet
        val salt = prefs[PIN_SALT] ?: return PinCheckResult.NotSet
        val now = System.currentTimeMillis()
        val lockedUntil = prefs[PIN_LOCKED_UNTIL] ?: 0L
        if (lockedUntil > now) return PinCheckResult.Locked(lockedUntil)
        val version = prefs[PIN_HASH_VERSION] ?: PinCrypto.HASH_VERSION_V1_SHA256
        val ok = if (version == PinCrypto.HASH_VERSION_V1_SHA256) {
            PinCrypto.verifyPinLegacy(pin, salt, hash)
        } else {
            PinCrypto.verifyPin(pin, salt, hash)
        }
        if (ok) {
            if (version == PinCrypto.HASH_VERSION_V1_SHA256) {
                // ارتقای شفاف هش قدیمی به PBKDF2.
                val newSalt = PinCrypto.generateSaltHex()
                val newHash = PinCrypto.hashPin(pin, newSalt)
                context.settingsDataStore.edit { edit ->
                    edit[PIN_HASH] = newHash
                    edit[PIN_SALT] = newSalt
                    edit[PIN_HASH_VERSION] = PinCrypto.HASH_VERSION_V2_PBKDF2
                    edit.remove(PIN_FAILED_ATTEMPTS)
                    edit.remove(PIN_LOCKED_UNTIL)
                }
            } else {
                context.settingsDataStore.edit { edit ->
                    edit.remove(PIN_FAILED_ATTEMPTS)
                    edit.remove(PIN_LOCKED_UNTIL)
                }
            }
            return PinCheckResult.Ok
        }
        val attempts = (prefs[PIN_FAILED_ATTEMPTS] ?: 0) + 1
        if (attempts >= PIN_MAX_ATTEMPTS) {
            val extra = (attempts - PIN_MAX_ATTEMPTS).coerceAtMost(5)
            val lockSeconds = (PIN_LOCK_BASE_SECONDS * (1L shl extra))
                .coerceAtMost(PIN_LOCK_MAX_SECONDS)
            val until = now + lockSeconds * 1000L
            context.settingsDataStore.edit { edit ->
                edit[PIN_FAILED_ATTEMPTS] = attempts
                edit[PIN_LOCKED_UNTIL] = until
            }
            return PinCheckResult.Locked(until)
        }
        context.settingsDataStore.edit { edit ->
            edit[PIN_FAILED_ATTEMPTS] = attempts
        }
        return PinCheckResult.Wrong(PIN_MAX_ATTEMPTS - attempts)
    }

    /** بررسی ساده (برای سازگاری): true یعنی PIN درست است. */
    suspend fun checkPin(pin: String): Boolean {
        return verify(pin) is PinCheckResult.Ok
    }

    suspend fun clearPin() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(PIN_HASH)
            prefs.remove(PIN_SALT)
            prefs.remove(PIN_HASH_VERSION)
            prefs.remove(PIN_FAILED_ATTEMPTS)
            prefs.remove(PIN_LOCKED_UNTIL)
        }
    }
}
