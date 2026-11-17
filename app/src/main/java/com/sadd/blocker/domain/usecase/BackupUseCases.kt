package com.sadd.blocker.domain.usecase

import com.sadd.blocker.domain.BackupCodec
import com.sadd.blocker.domain.BackupDecodeResult
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.WebsiteRepository

/**
 * ریستور بکاپ — خالص Kotlin.
 * فقط آیتم‌های معتبر و غیرتکراری اضافه می‌شوند؛ نتیجه شمارش دقیق می‌دهد.
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
) {
    suspend fun execute(json: String): RestoreResult {
        val decoded = BackupCodec.decode(json)
        if (decoded is BackupDecodeResult.Failure) {
            return RestoreResult.Failed(decoded.reason)
        }
        val success = decoded as BackupDecodeResult.Success
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
        for (schedule in data.schedules) {
            schedules.add(schedule)
            schedulesAdded++
        }
        return RestoreResult.Restored(
            RestoreSummary(appsAdded, websitesAdded, keywordsAdded, schedulesAdded, skipped, notes),
        )
    }
}
