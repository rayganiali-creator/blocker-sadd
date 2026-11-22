package com.sadd.blocker.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.pro.ProManager
import com.sadd.blocker.domain.BackupCodec
import com.sadd.blocker.domain.BackupData
import com.sadd.blocker.domain.ProEntitlement
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.RestoreBackupUseCase
import com.sadd.blocker.domain.usecase.RestoreResult
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val message: String? = null,
    val exporting: Boolean = false,
    /** دیالوگ بازگشایی Strict برای ریستور بکاپ باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده (اگر هست) برای دیالوگ. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
    /** سد پرو فعال است؟ (پشتیبان‌گیری و ریستور امکان پرو است.) */
    val isPro: Boolean = false,
    /** دیالوگ «این امکان در سد پرو است» باز است. */
    val proPromptVisible: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    private val scheduleRepository: ScheduleRepository,
    private val restoreUseCase: RestoreBackupUseCase,
    sessionRepository: SessionRepository,
    private val settingsStore: SettingsStore,
    private val proManager: ProManager,
) : ViewModel() {

    companion object {
        /** سقف حجم فایل بکاپ ورودی (M-10): خواندن فایل چندمگابایتی ریسک OOM دارد. */
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    /** JSON بکاپِ در انتظار بازگشایی Strict (فقط در حافظه). */
    private var pendingRestoreJson: String? = null

    init {
        viewModelScope.launch {
            settingsStore.strictUnlockMethod.collect { method ->
                _uiState.update { it.copy(strictUnlockMethod = method) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownHours.collect { hours ->
                _uiState.update { it.copy(strictCooldownHours = hours) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownRequest.collect { request ->
                _uiState.update { it.copy(strictCooldownRequest = request) }
            }
        }
        viewModelScope.launch {
            proManager.proState.collect { state ->
                _uiState.update {
                    it.copy(isPro = ProEntitlement.isPro(state, System.currentTimeMillis()))
                }
            }
        }
    }

    /** ساخت JSON بکاپ از وضعیت فعلی دیتابیس. */
    suspend fun buildExportJson(): String {
        val data = BackupData(
            apps = appRepository.list(),
            websites = websiteRepository.list(),
            keywords = keywordRepository.list(),
            schedules = scheduleRepository.list(),
        )
        return BackupCodec.encode(data, System.currentTimeMillis())
    }

    /**
     * خواندن فایل بکاپ با سقف حجم (M-10). خطای I/O دیگر بی‌صدا قورت داده
     * نمی‌شود؛ Result خطا را با پیام برمی‌گرداند.
     */
    suspend fun readBackupText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArray(MAX_BACKUP_BYTES + 1)
                var total = 0
                while (true) {
                    val read = stream.read(bytes, total, bytes.size - total)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BACKUP_BYTES) {
                        throw IllegalArgumentException("فایل بکاپ خیلی بزرگ است (حداکثر ۵ مگابایت).")
                    }
                }
                String(bytes, 0, total, Charsets.UTF_8)
            } ?: throw IllegalStateException("فایل بکاپ باز نشد.")
        }
    }

    /** نوشتن بکاپ؛ خطای I/O با پیام برمی‌گردد (M-10). */
    suspend fun writeBackupText(uri: Uri, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("فایل خروجی باز نشد.")
        }
    }

    /**
     * ریستور بکاپ از یک رشته JSON (خواندن فایل در BackupScreen انجام می‌شود).
     * خروجی گرفتن (export) خواندنی است و آزاد، ولی ریستور همه قوانین و
     * زمان‌بندی‌ها را عوض می‌کند؛ وسط سشن Strict فقط بعد از بازگشایی
     * Strict (تایپ متن / انتظار) انجام می‌شود. اگر اپ وسط انتظار ری‌استارت
     * شود، JSON در حافظه نمی‌ماند و با انتخاب دوباره فایل، بعد از گذشت
     * انتظار، مستقیم اعمال می‌شود.
     */
    fun importJson(json: String) {
        viewModelScope.launch {
            // ریستور امکان سد پرو است؛ در رایگان دیالوگ پرو باز می‌شود.
            if (!proManager.isProNow()) {
                _uiState.update { it.copy(proPromptVisible = true) }
                return@launch
            }
            if (strictGuard.isStrictSessionActive()) {
                // درخواست انتظارِ ثبت‌شده و گذشته (بعد از ری‌استارت اپ و
                // انتخاب دوباره فایل) مستقیم اعمال می‌شود.
                val request = settingsStore.strictCooldownRequest.firstOrNull()
                if (_uiState.value.strictUnlockMethod == StrictUnlockMethod.COOLDOWN &&
                    request?.pendingKind == StrictPendingKind.BACKUP_RESTORE &&
                    StrictUnlock.isCooldownElapsed(request, System.currentTimeMillis())
                ) {
                    settingsStore.clearStrictCooldownRequest()
                    applyRestore(json)
                    return@launch
                }
                pendingRestoreJson = json
                _uiState.update { it.copy(strictUnlockVisible = true) }
                return@launch
            }
            applyRestore(json)
        }
    }

    /** ریستور بعد از بازگشایی موفق در دیالوگ Strict. */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val json = pendingRestoreJson
            if (json == null) {
                _uiState.update {
                    it.copy(
                        strictUnlockVisible = false,
                        message = "فایل بکاپ را دوباره انتخاب کن؛ متن بکاپ بعد از ری‌استارت اپ نگه داشته نمی‌شود.",
                    )
                }
                return@launch
            }
            if (_uiState.value.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = _uiState.value.strictCooldownRequest
                if (request == null || request.pendingKind != StrictPendingKind.BACKUP_RESTORE ||
                    !StrictUnlock.isCooldownElapsed(request, System.currentTimeMillis())
                ) {
                    if (request != null && request.pendingKind != StrictPendingKind.BACKUP_RESTORE) {
                        pendingRestoreJson = null
                        _uiState.update {
                            it.copy(
                                strictUnlockVisible = false,
                                message = "این انتظار برای کار دیگری ثبت شده است؛ برای ریستور دوباره درخواست بده.",
                            )
                        }
                    }
                    return@launch
                }
                settingsStore.clearStrictCooldownRequest()
            }
            pendingRestoreJson = null
            _uiState.update { it.copy(strictUnlockVisible = false) }
            applyRestore(json)
        }
    }

    /** ثبت درخواست انتظار برای ریستور بکاپ (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        if (pendingRestoreJson == null) return
        val existing = _uiState.value.strictCooldownRequest
        // درخواست تکراری همان ریستور: ساعت از اول شمرده نمی‌شود.
        if (existing?.pendingKind == StrictPendingKind.BACKUP_RESTORE) return
        viewModelScope.launch {
            settingsStore.recordStrictCooldownRequest(
                kind = StrictPendingKind.BACKUP_RESTORE,
                value = "",
                durationHours = _uiState.value.strictCooldownHours,
            )
        }
    }

    fun cancelStrictUnlock() {
        pendingRestoreJson = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    /** نشان‌دادن دیالوگ پرو (از دکمه‌های خروجی/ورودی صفحه، قبل از بازکردن فایل‌گیر). */
    fun showProPrompt() {
        _uiState.update { it.copy(proPromptVisible = true) }
    }

    fun dismissProPrompt() {
        _uiState.update { it.copy(proPromptVisible = false) }
    }

    private suspend fun applyRestore(json: String) {
        val result = restoreUseCase.execute(json)
        if (result is RestoreResult.Restored) {
            val s = result.summary
            _uiState.update {
                it.copy(
                    message = "ریستور شد: ${s.appsAdded} اپ، ${s.websitesAdded} وب‌سایت، " +
                        "${s.keywordsAdded} کلمه، ${s.schedulesAdded} زمان‌بندی. " +
                        "${s.skipped} آیتم تکراری یا خراب skip شد.",
                )
            }
        }
        if (result is RestoreResult.Failed) {
            _uiState.update { it.copy(message = "ریستور ناموفق بود: ${result.reason}") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
