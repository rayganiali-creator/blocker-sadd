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
import com.sadd.blocker.domain.DelayMigration
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
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

/** رنگ‌بندی برنامه — مستقل از روشن/تیره؛ هر رنگ‌بندی روشن و تیره خودش را دارد. */
enum class ColorTheme {
    SADD,
    SUNSET,
    FOREST,
    OCEAN,
    GRAPE,
    ROSE,
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
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BLOCK_MODE = stringPreferencesKey("block_mode")
        val PIN_HASH = stringPreferencesKey("pin_hash_hex")
        val PIN_SALT = stringPreferencesKey("pin_salt_hex")
        val PIN_HASH_VERSION = intPreferencesKey("pin_hash_version")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKED_UNTIL = longPreferencesKey("pin_locked_until_ms")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val BATTERY_EXEMPTION_ASKED = booleanPreferencesKey("battery_exemption_asked")
        /** روش بازگشایی Strict: PASSAGE (تایپ متن) یا COOLDOWN (انتظار). */
        val STRICT_UNLOCK_METHOD = stringPreferencesKey("strict_unlock_method")
        /** مدت انتظار بازگشایی Strict (ساعت): ۱۲، ۲۴ یا ۴۸. */
        val STRICT_COOLDOWN_HOURS = intPreferencesKey("strict_cooldown_hours")
        /** زمان دیواری ثبت درخواست انتظار (۰ یعنی درخواستی نیست). */
        val STRICT_COOLDOWN_REQUESTED_AT = longPreferencesKey("strict_cooldown_requested_at_ms")
        /** کاری که درخواست انتظار برای آن ثبت شده (نام StrictPendingKind). */
        val STRICT_COOLDOWN_PENDING_KIND = stringPreferencesKey("strict_cooldown_pending_kind")
        /** مقدار درخواست انتظار: نام روش (METHOD) یا عدد ساعت (HOURS). */
        val STRICT_COOLDOWN_PENDING_VALUE = stringPreferencesKey("strict_cooldown_pending_value")
        /** مدت انتظار ثبت‌شده موقع درخواست (ساعت) — تا پایان درخواست ثابت است. */
        val STRICT_COOLDOWN_DURATION_HOURS = intPreferencesKey("strict_cooldown_duration_hours")
        /** محافظت از حذف نصب (مدیر دستگاه) روشن است یا نه؛ پیش‌فرض خاموش. */
        val UNINSTALL_PROTECTION_ENABLED = booleanPreferencesKey("uninstall_protection_enabled")
        /**
         * پایان پنجره بازگشایی محافظت (زمان دیواری، میلی‌ثانیه)؛ تا این لحظه
         * نگهبان حذف نصب/توقف اجباری (ProtectionGuardDetector) کاری نمی‌کند تا
         * کاربر بعد از بازگشایی Strict بتواند از تنظیمات ادامه بدهد. ۰ یعنی
         * پنجره‌ای باز نیست. با [ProtectionGuardDetector.PROTECTION_UNLOCK_WINDOW_MS]
         * (۵ دقیقه) بعد از بازگشایی تنظیم می‌شود.
         */
        val PROTECTION_UNLOCK_UNTIL_MS = longPreferencesKey("protection_unlock_until_ms")
        /**
         * آخرین وضعیتِ شناخته‌شده مدیر دستگاه (فعال بوده یا نه)؛ برای تشخیص
         * اینکه مدیر دستگاه از تنظیمات سیستم غیرفعال شده است. پیش‌فرض خاموش.
         */
        val UNINSTALL_ADMIN_WAS_ACTIVE = booleanPreferencesKey("uninstall_admin_was_active")
        /**
         * مرحله فعلی دنباله دسترسی‌پذیری ویزارد (۱ تا ۳؛ ۰ یعنی تمام شده).
         * ترتیب واقعی گوشی: ۱) زدن کلید خاکستری در دسترسی‌پذیری و بستن
         * پیام، ۲) روشن‌کردن Allow restricted settings در اطلاعات برنامه،
         * ۳) برگشت و فعال‌کردن «سد». با ری‌استارت اپ پاک نمی‌شود.
         */
        val ACCESSIBILITY_PHASE = intPreferencesKey("accessibility_phase")
        /**
         * پرچم مهاجرت یک‌باره تاخیر پیش‌فرض به فوری؛ اگر نخورده باشد،
         * اولین اجرا مقدار تاخیر سراسری را ۰ می‌کند و پرچم را می‌زند.
         */
        val DELAY_DEFAULT_MIGRATED = booleanPreferencesKey("delay_default_migrated")

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
     * مهاجرت یک‌باره تاخیر پیش‌فرض به فوری (۰ ثانیه).
     * نصب‌های قدیمی ممکن است مقدار ۵ ثانیه را ذخیره کرده باشند؛ تغییر
     * پیش‌فرض کد به‌تنهایی آن‌ها را عوض نمی‌کند. اولین اجرا بعد از
     * به‌روزرسانی، اگر پرچم نخورده باشد، مقدار را ۰ می‌کند و پرچم را
     * می‌زند؛ بعد از آن، انتخاب کاربر دست نمی‌خورد. Idempotent است و
     * فراخوانی هم‌زمان از دو جا هم بی‌ضرر است (همان مقدار نوشته می‌شود).
     */
    suspend fun migrateDelayDefaultIfNeeded() {
        val migrated = safeData.first()[DELAY_DEFAULT_MIGRATED] == true
        if (!DelayMigration.shouldMigrate(migrated)) return
        context.settingsDataStore.edit { prefs ->
            prefs[GLOBAL_DELAY] = DelayOptions.DEFAULT_SECONDS
            prefs[DELAY_DEFAULT_MIGRATED] = true
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

    /** رنگ‌بندی فعلی؛ پیش‌فرض «سد» (سرمه‌ای + مینت) است. */
    val colorTheme: Flow<ColorTheme> = safeData
        .map { prefs ->
            val raw = prefs[COLOR_THEME]
            if (raw == null) {
                ColorTheme.SADD
            } else {
                runCatching { ColorTheme.valueOf(raw) }.getOrDefault(ColorTheme.SADD)
            }
        }

    suspend fun setColorTheme(theme: ColorTheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[COLOR_THEME] = theme.name
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

    /** روش بازگشایی Strict؛ پیش‌فرض تایپ متن (PASSAGE) است. */
    val strictUnlockMethod: Flow<StrictUnlockMethod> = safeData
        .map { prefs ->
            val raw = prefs[STRICT_UNLOCK_METHOD]
            if (raw == null) {
                StrictUnlockMethod.PASSAGE
            } else {
                runCatching { StrictUnlockMethod.valueOf(raw) }
                    .getOrDefault(StrictUnlockMethod.PASSAGE)
            }
        }

    suspend fun setStrictUnlockMethod(method: StrictUnlockMethod) {
        context.settingsDataStore.edit { prefs ->
            prefs[STRICT_UNLOCK_METHOD] = method.name
        }
    }

    /** مدت انتظار بازگشایی Strict (ساعت)؛ فقط ۱۲/۲۴/۴۸ معتبر است. */
    val strictCooldownHours: Flow<Int> = safeData
        .map { prefs ->
            val hours = prefs[STRICT_COOLDOWN_HOURS] ?: StrictUnlock.DEFAULT_COOLDOWN_HOURS
            if (StrictUnlock.COOLDOWN_HOUR_OPTIONS.contains(hours)) {
                hours
            } else {
                StrictUnlock.DEFAULT_COOLDOWN_HOURS
            }
        }

    suspend fun setStrictCooldownHours(hours: Int) {
        if (!StrictUnlock.COOLDOWN_HOUR_OPTIONS.contains(hours)) return
        context.settingsDataStore.edit { prefs ->
            prefs[STRICT_COOLDOWN_HOURS] = hours
        }
    }

    /**
     * درخواست انتظار ثبت‌شده، یا null اگر درخواستی نیست.
     * در DataStore می‌ماند و با ری‌استارت اپ پاک نمی‌شود.
     */
    val strictCooldownRequest: Flow<StrictCooldownRequest?> = safeData
        .map { prefs ->
            val requestedAt = prefs[STRICT_COOLDOWN_REQUESTED_AT] ?: 0L
            if (requestedAt <= 0L) {
                null
            } else {
                val kindRaw = prefs[STRICT_COOLDOWN_PENDING_KIND]
                    ?: StrictPendingKind.END_SESSION.name
                val kind = runCatching { StrictPendingKind.valueOf(kindRaw) }
                    .getOrDefault(StrictPendingKind.END_SESSION)
                StrictCooldownRequest(
                    requestedAtMs = requestedAt,
                    pendingKind = kind,
                    pendingValue = prefs[STRICT_COOLDOWN_PENDING_VALUE] ?: "",
                    durationHours = prefs[STRICT_COOLDOWN_DURATION_HOURS]
                        ?: StrictUnlock.DEFAULT_COOLDOWN_HOURS,
                )
            }
        }

    /**
     * ثبت درخواست انتظار با زمان دیواری همین حالا.
     * درخواست جدید جای درخواست قبلی را می‌گیرد و زمان انتظار از اول شمرده می‌شود.
     */
    suspend fun recordStrictCooldownRequest(
        kind: StrictPendingKind,
        value: String,
        durationHours: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[STRICT_COOLDOWN_REQUESTED_AT] = nowMs
            prefs[STRICT_COOLDOWN_PENDING_KIND] = kind.name
            prefs[STRICT_COOLDOWN_PENDING_VALUE] = value
            prefs[STRICT_COOLDOWN_DURATION_HOURS] = durationHours
        }
    }

    suspend fun clearStrictCooldownRequest() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(STRICT_COOLDOWN_REQUESTED_AT)
            prefs.remove(STRICT_COOLDOWN_PENDING_KIND)
            prefs.remove(STRICT_COOLDOWN_PENDING_VALUE)
            prefs.remove(STRICT_COOLDOWN_DURATION_HOURS)
        }
    }

    /** محافظت از حذف نصب روشن است یا نه؛ پیش‌فرض خاموش است. */
    val uninstallProtectionEnabled: Flow<Boolean> = safeData
        .map { prefs -> prefs[UNINSTALL_PROTECTION_ENABLED] ?: false }

    suspend fun setUninstallProtectionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[UNINSTALL_PROTECTION_ENABLED] = enabled
        }
    }

    /** پایان پنجره بازگشایی محافظت (زمان دیواری)؛ پیش‌فرض ۰ یعنی پنجره‌ای باز نیست. */
    val protectionUnlockUntilMs: Flow<Long> = safeData
        .map { prefs -> prefs[PROTECTION_UNLOCK_UNTIL_MS] ?: 0L }

    suspend fun setProtectionUnlockUntil(untilMs: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[PROTECTION_UNLOCK_UNTIL_MS] = untilMs
        }
    }

    /** آخرین وضعیتِ شناخته‌شده مدیر دستگاه؛ پیش‌فرض «فعال نبوده». */
    val uninstallAdminWasActive: Flow<Boolean> = safeData
        .map { prefs -> prefs[UNINSTALL_ADMIN_WAS_ACTIVE] ?: false }

    suspend fun setUninstallAdminWasActive(wasActive: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[UNINSTALL_ADMIN_WAS_ACTIVE] = wasActive
        }
    }

    /** مرحله فعلی دنباله دسترسی‌پذیری ویزارد (۱ تا ۳)؛ پیش‌فرض ۱ است. */
    val accessibilityPhase: Flow<Int> = safeData
        .map { prefs -> prefs[ACCESSIBILITY_PHASE] ?: 1 }

    suspend fun setAccessibilityPhase(phase: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[ACCESSIBILITY_PHASE] = phase
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
