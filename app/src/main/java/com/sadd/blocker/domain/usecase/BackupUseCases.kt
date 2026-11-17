package com.sadd.blocker.domain.usecase

import com.sadd.blocker.domain.BackupCodec
import com.sadd.blocker.domain.BackupDecodeResult
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.RestoreTransaction
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.WebsiteRepository

/**
 * ریستور بکاپ — خالص Kotlin.
 * فقط آیتم‌های معتبر و غیرتکراری اضافه می‌شوند؛ نتیجه شمارش دقیق می‌دهد.
 * کل ریستور در یک تراکنش انجام می‌شود (M-3) تا کرش وسط راه،
 * ریستور نصفه نسازد. زمان‌بندی‌ها هم dedupe می‌شوند (M-3).
 */
data class RestoreSummary(
    val appsAdded: Int,
    val websitesAdded: Int,
    val keywordsAdded: Int,
    val schedulesAdded: Int,
    val skipped: Int,
    val notes: List<String>,
)

sealed class RestoreResult {
    data class Restored(val summary: RestoreSummary) : RestoreResult()
    data class Failed(val reason: String) : RestoreResult()
}

class RestoreBackupUseCase(
    private val apps: BlockedAppRepository,
    private val websites: WebsiteRepository,
    private val keywords: KeywordRepository,
    private val schedules: ScheduleRepository,
    private val transaction: RestoreTransaction? = null,
) {
    suspend fun execute(json: String): RestoreResult {
        val decoded = BackupCodec.decode(json)
        if (decoded is BackupDecodeResult.Failure) {
            return RestoreResult.Failed(decoded.reason)
        }
        val success = decoded as BackupDecodeResult.Success
        val run = transaction ?: object : RestoreTransaction {
            override suspend fun <T> run(block: suspend () -> T): T = block()
        }
        return run.run {
            restore(success)
        }
    }

    private suspend fun restore(success: BackupDecodeResult.Success): RestoreResult {
        val data = success.data
        var appsAdded = 0
        var websitesAdded = 0
        var keywordsAdded = 0
        var schedulesAdded = 0
        var skipped = success.skipped
        val notes = ArrayList(success.notes)
        for (app in data.apps) {
            if (apps.getByPackage(app.packageName) == null) {
                apps.add(app)
                appsAdded++
            } else {
                skipped++
            }
        }
        for (site in data.websites) {
            if (websites.findByHost(site.host) == null) {
                websites.add(site)
                websitesAdded++
            } else {
                skipped++
            }
        }
        for (rule in data.keywords) {
            if (keywords.countByNormalized(rule.normalizedText) == 0) {
                keywords.add(rule)
                keywordsAdded++
            } else {
                skipped++
            }
        }
        val existingSchedules = schedules.list().toMutableList()
        for (schedule in data.schedules) {
            val duplicate = existingSchedules.any {
                it.daysOfWeek == schedule.daysOfWeek &&
                    it.startMinute == schedule.startMinute &&
                    it.endMinute == schedule.endMinute &&
                    it.strict == schedule.strict &&
                    it.ruleScope == schedule.ruleScope
            }
            if (!duplicate) {
                schedules.add(schedule)
                // لیست دیده‌شده را به‌روز کن تا تکراری داخل همان فایل هم
                // گرفته شود (لیست کهنه، دومی را دوباره اضافه می‌کرد).
                existingSchedules.add(schedule)
                schedulesAdded++
            } else {
                skipped++
            }
        }
        return RestoreResult.Restored(
            RestoreSummary(appsAdded, websitesAdded, keywordsAdded, schedulesAdded, skipped, notes),
        )
    }
}
