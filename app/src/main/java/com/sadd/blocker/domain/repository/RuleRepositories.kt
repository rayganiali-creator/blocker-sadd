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
    suspend fun end(id: Long, reason: String)
    suspend fun updateEnd(id: Long, newEndEpochMs: Long)
    suspend fun history(limit: Int = 50): List<BlockingSession>
}

interface ScheduleRepository {
    suspend fun list(): List<Schedule>
    fun observe(): Flow<List<Schedule>>
    suspend fun add(schedule: Schedule): Long
    suspend fun update(schedule: Schedule)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun remove(id: Long)
}
