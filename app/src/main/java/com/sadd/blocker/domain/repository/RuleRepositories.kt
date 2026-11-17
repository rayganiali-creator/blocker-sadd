package com.sadd.blocker.domain.repository

import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * قرارداد Repositoryها — خالص Kotlin.
 * پیاده‌سازی Room در data/repository است (وابسته به اندروید، گیت CI)؛
 * پیاده‌سازی حافظه‌ای فقط برای تست در تست‌ها هست.
 * قانون تکراری در سطح UseCase + DB (ایندکس یکتا) جلوگیری می‌شود، نه فقط UI.
 */
interface BlockedAppRepository {
    suspend fun list(): List<BlockedApp>
    fun observe(): Flow<List<BlockedApp>>
    suspend fun getByPackage(packageName: String): BlockedApp?
    suspend fun add(app: BlockedApp)
    suspend fun setEnabled(packageName: String, enabled: Boolean)
    suspend fun setDelay(packageName: String, delaySeconds: Int?)
    suspend fun remove(packageName: String)
    suspend fun countEnabled(): Int
}

interface WebsiteRepository {
    suspend fun list(): List<BlockedWebsite>
    fun observe(): Flow<List<BlockedWebsite>>
    suspend fun findByHost(host: String): BlockedWebsite?
    suspend fun add(site: BlockedWebsite): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun setDelay(id: Long, delaySeconds: Int?)
    suspend fun remove(id: Long)
    suspend fun countEnabled(): Int
}

interface KeywordRepository {
    suspend fun list(): List<KeywordRule>
    fun observe(): Flow<List<KeywordRule>>
    suspend fun countByNormalized(normalized: String): Int
    suspend fun add(rule: KeywordRule): Long
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun setDelay(id: Long, delaySeconds: Int?)
    suspend fun remove(id: Long)
    suspend fun countEnabled(): Int
}

interface SessionRepository {
    suspend fun getActive(nowMs: Long): BlockingSession?
    fun observeActive(): Flow<BlockingSession?>
    suspend fun start(session: BlockingSession): Long

    /**
     * شروع اتمی (H-9): فقط وقتی سشن فعالی نیست insert می‌کند؛
     * اگر سشن فعال باشد ‎-1‎ برمی‌گرداند. پیاده‌سازی Room این را در
     * یک تراکنش انجام می‌دهد تا دو شروع هم‌زمان دو سشن فعال نسازند.
     */
    suspend fun startIfNoActive(session: BlockingSession, nowMs: Long): Long
    suspend fun end(id: Long, reason: String)

    /**
     * سشن‌های منقضی‌شده (status=active ولی endEpochMs گذشته) را به
     * expired تبدیل می‌کند (H-5)؛ تعداد به‌روز شده را برمی‌گرداند.
     * هر getActive باید اول این را صدا بزند تا تاریخچه درست بماند.
     */
    suspend fun expireOld(nowMs: Long): Int
    suspend fun updateEnd(id: Long, newEndEpochMs: Long)
    suspend fun history(limit: Int = 50): List<BlockingSession>
}

/** نتیجه بررسی PIN (H-8: شمارنده تلاش + قفل موقت تصاعدی). */
sealed class PinCheckResult {
    object Ok : PinCheckResult()
    /** PIN اشتباه است؛ attemptsLeft تعداد تلاش باقی‌مانده تا قفل است. */
    data class Wrong(val attemptsLeft: Int) : PinCheckResult()
    /** PIN تا lockedUntilMs قفل است. */
    data class Locked(val lockedUntilMs: Long) : PinCheckResult()
    /** اصلاً PIN تنظیم نشده است. */
    object NotSet : PinCheckResult()
}

/** درگاه بررسی PIN برای UseCaseها — پیاده‌سازی در SettingsStore است. */
interface PinGate {
    suspend fun isPinSet(): Boolean
    suspend fun verify(pin: String): PinCheckResult
}

/**
 * تراکنش برای ریستور بکاپ (M-3): همه writeهای ریستور باید اتمی باشند تا
 * کرش وسط راه، ریستور نصفه نسازد. پیاده‌سازی Room از withTransaction
 * استفاده می‌کند؛ پیاده‌سازی تستی بلاک را مستقیم اجرا می‌کند.
 */
interface RestoreTransaction {
    suspend fun <T> run(block: suspend () -> T): T
}

interface ScheduleRepository {
    suspend fun list(): List<Schedule>
    fun observe(): Flow<List<Schedule>>
    suspend fun add(schedule: Schedule): Long
    suspend fun update(schedule: Schedule)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun remove(id: Long)
}
