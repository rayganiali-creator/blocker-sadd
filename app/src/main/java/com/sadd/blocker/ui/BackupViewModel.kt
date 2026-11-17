package com.sadd.blocker.ui

import android.content.Context
import android.net.Uri
import com.sadd.blocker.domain.BackupCodec
import com.sadd.blocker.domain.BackupData
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.RestoreBackupUseCase
import com.sadd.blocker.domain.usecase.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val message: String? = null,
    val exporting: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    private val scheduleRepository: ScheduleRepository,
    private val restoreUseCase: RestoreBackupUseCase,
) : ViewModel() {

    companion object {
        /** سقف حجم فایل بکاپ ورودی (M-10): خواندن فایل چندمگابایتی ریسک OOM دارد. */
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

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

    fun importJson(json: String) {
        viewModelScope.launch {
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
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
