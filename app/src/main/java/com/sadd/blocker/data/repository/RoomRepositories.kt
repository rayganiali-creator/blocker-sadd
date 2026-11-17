package com.sadd.blocker.data.repository

import androidx.room.withTransaction
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.db.AppDatabase
import com.sadd.blocker.data.db.BlockedAppDao
import com.sadd.blocker.data.db.BlockedAppEntity
import com.sadd.blocker.data.db.BlockedWebsiteDao
import com.sadd.blocker.data.db.BlockedWebsiteEntity
import com.sadd.blocker.data.db.BlockingSessionDao
import com.sadd.blocker.data.db.BlockingSessionEntity
import com.sadd.blocker.data.db.KeywordRuleDao
import com.sadd.blocker.data.db.KeywordRuleEntity
import com.sadd.blocker.data.db.ScheduleDao
import com.sadd.blocker.data.db.ScheduleEntity
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.RestoreTransaction
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** تراکنش Room برای ریستور بکاپ (M-3). */
class RoomRestoreTransaction @Inject constructor(
    private val db: AppDatabase,
) : RestoreTransaction {
    override suspend fun <T> run(block: suspend () -> T): T {
        return db.withTransaction { block() }
    }
}

/** پیاده‌سازی Room برای BlockedAppRepository. */
class RoomBlockedAppRepository @Inject constructor(
    private val dao: BlockedAppDao,
) : BlockedAppRepository {

    override suspend fun list(): List<BlockedApp> {
        return dao.listAll().map { it.toDomain() }
    }

    override fun observe(): Flow<List<BlockedApp>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getByPackage(packageName: String): BlockedApp? {
        return dao.getByPackage(packageName)?.toDomain()
    }

    override suspend fun add(app: BlockedApp) {
        dao.upsert(app.toEntity())
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) {
        dao.setEnabled(packageName, enabled)
    }

    override suspend fun setDelay(packageName: String, delaySeconds: Int?) {
        dao.setDelay(packageName, delaySeconds)
    }

    override suspend fun remove(packageName: String) {
        dao.delete(packageName)
    }

    override suspend fun countEnabled(): Int {
        return dao.countEnabled()
    }

    private fun BlockedAppEntity.toDomain(): BlockedApp {
        return BlockedApp(packageName, label, enabled, delaySeconds, createdAt)
    }

    private fun BlockedApp.toEntity(): BlockedAppEntity {
        return BlockedAppEntity(packageName, label, enabled, delaySeconds, createdAt)
    }
}

/** پیاده‌سازی Room برای WebsiteRepository. */
class RoomWebsiteRepository @Inject constructor(
    private val dao: BlockedWebsiteDao,
) : WebsiteRepository {

    override suspend fun list(): List<BlockedWebsite> {
        return dao.listAll().map { it.toDomain() }
    }

    override fun observe(): Flow<List<BlockedWebsite>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun findByHost(host: String): BlockedWebsite? {
        return dao.findByHost(host)?.toDomain()
    }

    override suspend fun add(site: BlockedWebsite): Long {
        return dao.insert(site.toEntity())
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    override suspend fun setDelay(id: Long, delaySeconds: Int?) {
        dao.setDelay(id, delaySeconds)
    }

    override suspend fun remove(id: Long) {
        dao.delete(id)
    }

    override suspend fun countEnabled(): Int {
        return dao.countEnabled()
    }

    private fun BlockedWebsiteEntity.toDomain(): BlockedWebsite {
        return BlockedWebsite(id, host, matchSubdomains, enabled, delaySeconds)
    }

    private fun BlockedWebsite.toEntity(): BlockedWebsiteEntity {
        return BlockedWebsiteEntity(id, host, matchSubdomains, enabled, delaySeconds)
    }
}

/** پیاده‌سازی Room برای KeywordRepository. */
class RoomKeywordRepository @Inject constructor(
    private val dao: KeywordRuleDao,
) : KeywordRepository {

    override suspend fun list(): List<KeywordRule> {
        return dao.listAll().map { it.toDomain() }
    }

    override fun observe(): Flow<List<KeywordRule>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun countByNormalized(normalized: String): Int {
        return dao.countByNormalized(normalized)
    }

    override suspend fun add(rule: KeywordRule): Long {
        return dao.insert(rule.toEntity())
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    override suspend fun setDelay(id: Long, delaySeconds: Int?) {
        dao.setDelay(id, delaySeconds)
    }

    override suspend fun remove(id: Long) {
        dao.delete(id)
    }

    override suspend fun countEnabled(): Int {
        return dao.countEnabled()
    }

    private fun KeywordRuleEntity.toDomain(): KeywordRule {
        return KeywordRule(id, rawText, normalizedText, category, isPhrase, enabled, delaySeconds)
    }

    private fun KeywordRule.toEntity(): KeywordRuleEntity {
        return KeywordRuleEntity(id, rawText, normalizedText, category, isPhrase, enabled, delaySeconds)
    }
}

/** پیاده‌سازی Room برای SessionRepository. */
class RoomSessionRepository @Inject constructor(
    private val dao: BlockingSessionDao,
) : SessionRepository {

    // کمربند دوم برای H-9: علاوه بر تراکنش DAO، شروع سشن در این پروسه
    // هم mutex دارد تا دو کوروتین هم‌زمان نتوانند هم‌زمان insert کنند.
    private val startMutex = Mutex()

    override suspend fun getActive(nowMs: Long): BlockingSession? {
        // اول سشن‌های منقضی‌شده را به expired تبدیل کن (H-5) تا تاریخچه درست بماند.
        dao.expireOld(nowMs)
        return dao.getActive(nowMs)?.toDomain()
    }

    override fun observeActive(): Flow<BlockingSession?> {
        // H-4: کوئری بدون پارامتر زمان؛ انقضا موقع emit با ساعت فعلی چک می‌شود.
        return dao.observeActive().map { entity ->
            entity?.takeIf { SessionCalculator.isActive(it.endEpochMs, System.currentTimeMillis()) }
                ?.toDomain()
        }
    }

    override suspend fun start(session: BlockingSession): Long {
        return dao.insert(session.toEntity())
    }

    override suspend fun startIfNoActive(session: BlockingSession, nowMs: Long): Long {
        return startMutex.withLock {
            dao.expireOld(nowMs)
            dao.insertIfNoActive(session.toEntity(), nowMs)
        }
    }

    override suspend fun expireOld(nowMs: Long): Int {
        return dao.expireOld(nowMs)
    }

    override suspend fun end(id: Long, reason: String) {
        dao.end(id, "ended", reason)
    }

    override suspend fun updateEnd(id: Long, newEndEpochMs: Long) {
        dao.updateEnd(id, newEndEpochMs)
    }

    override suspend fun history(limit: Int): List<BlockingSession> {
        return dao.history(limit).map { it.toDomain() }
    }

    private fun BlockingSessionEntity.toDomain(): BlockingSession {
        return BlockingSession(id, startEpochMs, endEpochMs, strict, ruleScope, status, endedReason, startElapsedMs)
    }

    private fun BlockingSession.toEntity(): BlockingSessionEntity {
        return BlockingSessionEntity(id, startEpochMs, endEpochMs, strict, ruleScope, status, endedReason, startElapsedRealtimeMs)
    }
}

/** پیاده‌سازی Room برای ScheduleRepository. */
class RoomScheduleRepository @Inject constructor(
    private val dao: ScheduleDao,
) : ScheduleRepository {

    override suspend fun list(): List<Schedule> {
        return dao.listAll().map { it.toDomain() }
    }

    override fun observe(): Flow<List<Schedule>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun add(schedule: Schedule): Long {
        return dao.insert(schedule.toEntity())
    }

    override suspend fun update(schedule: Schedule) {
        dao.update(schedule.toEntity())
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    override suspend fun remove(id: Long) {
        dao.delete(id)
    }

    private fun ScheduleEntity.toDomain(): Schedule {
        return Schedule(id, daysOfWeek, startMinute, endMinute, strict, ruleScope, enabled)
    }

    private fun Schedule.toEntity(): ScheduleEntity {
        return ScheduleEntity(id, daysOfWeek, startMinute, endMinute, strict, ruleScope, enabled)
    }
}
