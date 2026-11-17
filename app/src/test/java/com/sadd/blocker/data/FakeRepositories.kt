package com.sadd.blocker.data

import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.BlockingSession
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repositoryهای حافظه‌ای — فقط برای تست واحد.
 * رفتار مشاهده‌پذیر (Flow) را با StateFlow شبیه‌سازی می‌کنند.
 */
class FakeBlockedAppRepository : BlockedAppRepository {
    private val items = LinkedHashMap<String, BlockedApp>()
    private val flow = MutableStateFlow<List<BlockedApp>>(emptyList())

    private fun emit() {
        flow.value = items.values.sortedBy { it.label }
    }

    override suspend fun list(): List<BlockedApp> {
        return items.values.sortedBy { it.label }
    }

    override fun observe(): Flow<List<BlockedApp>> {
        return flow.asStateFlow()
    }

    override suspend fun getByPackage(packageName: String): BlockedApp? {
        return items[packageName]
    }

    override suspend fun add(app: BlockedApp) {
        items[app.packageName] = app
        emit()
    }

    override suspend fun setEnabled(packageName: String, enabled: Boolean) {
        val current = items[packageName] ?: return
        items[packageName] = current.copy(enabled = enabled)
        emit()
    }

    override suspend fun setDelay(packageName: String, delaySeconds: Int?) {
        val current = items[packageName] ?: return
        items[packageName] = current.copy(delaySeconds = delaySeconds)
        emit()
    }

    override suspend fun remove(packageName: String) {
        items.remove(packageName)
        emit()
    }

    override suspend fun countEnabled(): Int {
        return items.values.count { it.enabled }
    }
}

class FakeWebsiteRepository : WebsiteRepository {
    private val items = LinkedHashMap<Long, BlockedWebsite>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<BlockedWebsite>>(emptyList())

    private fun emit() {
        flow.value = items.values.sortedBy { it.host }
    }

    override suspend fun list(): List<BlockedWebsite> {
        return items.values.sortedBy { it.host }
    }

    override fun observe(): Flow<List<BlockedWebsite>> {
        return flow.asStateFlow()
    }

    override suspend fun findByHost(host: String): BlockedWebsite? {
        return items.values.firstOrNull { it.host == host }
    }

    override suspend fun add(site: BlockedWebsite): Long {
        val id = nextId++
        items[id] = site.copy(id = id)
        emit()
        return id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val current = items[id] ?: return
        items[id] = current.copy(enabled = enabled)
        emit()
    }

    override suspend fun setDelay(id: Long, delaySeconds: Int?) {
        val current = items[id] ?: return
        items[id] = current.copy(delaySeconds = delaySeconds)
        emit()
    }

    override suspend fun remove(id: Long) {
        items.remove(id)
        emit()
    }

    override suspend fun countEnabled(): Int {
        return items.values.count { it.enabled }
    }
}

class FakeKeywordRepository : KeywordRepository {
    private val items = LinkedHashMap<Long, KeywordRule>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<KeywordRule>>(emptyList())

    private fun emit() {
        flow.value = items.values.sortedByDescending { it.id }
    }

    override suspend fun list(): List<KeywordRule> {
        return items.values.sortedByDescending { it.id }
    }

    override fun observe(): Flow<List<KeywordRule>> {
        return flow.asStateFlow()
    }

    override suspend fun countByNormalized(normalized: String): Int {
        return items.values.count { it.normalizedText == normalized }
    }

    override suspend fun add(rule: KeywordRule): Long {
        val id = nextId++
        items[id] = rule.copy(id = id)
        emit()
        return id
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val current = items[id] ?: return
        items[id] = current.copy(enabled = enabled)
        emit()
    }

    override suspend fun setDelay(id: Long, delaySeconds: Int?) {
        val current = items[id] ?: return
        items[id] = current.copy(delaySeconds = delaySeconds)
        emit()
    }

    override suspend fun remove(id: Long) {
        items.remove(id)
        emit()
    }

    override suspend fun countEnabled(): Int {
        return items.values.count { it.enabled }
    }
}

class FakeSessionRepository : SessionRepository {
    private val items = LinkedHashMap<Long, BlockingSession>()
    private var nextId = 1L
    private val flow = MutableStateFlow<BlockingSession?>(null)
    private val mutex = Mutex()

    private fun emitActive(nowMs: Long) {
        flow.value = items.values.firstOrNull { it.status == "active" && it.endEpochMs > nowMs }
    }

    override suspend fun getActive(nowMs: Long): BlockingSession? {
        expireOld(nowMs)
        return items.values
            .filter { it.status == "active" && it.endEpochMs > nowMs }
            .maxByOrNull { it.endEpochMs }
    }

    override fun observeActive(): Flow<BlockingSession?> {
        return flow.asStateFlow()
    }

    override suspend fun start(session: BlockingSession): Long {
        val id = nextId++
        items[id] = session.copy(id = id)
        emitActive(session.startEpochMs)
        return id
    }

    override suspend fun startIfNoActive(session: BlockingSession, nowMs: Long): Long {
        return mutex.withLock {
            expireOld(nowMs)
            if (getActive(nowMs) != null) return@withLock -1L
            start(session)
        }
    }

    override suspend fun expireOld(nowMs: Long): Int {
        var count = 0
        for ((id, item) in items) {
            if (item.status == "active" && item.endEpochMs <= nowMs) {
                items[id] = item.copy(status = "expired", endedReason = "expired")
                count++
            }
        }
        if (count > 0) emitActive(nowMs)
        return count
    }

    override suspend fun end(id: Long, reason: String) {
        val current = items[id] ?: return
        items[id] = current.copy(status = "ended", endedReason = reason)
        emitActive(System.currentTimeMillis())
    }

    override suspend fun updateEnd(id: Long, newEndEpochMs: Long) {
        val current = items[id] ?: return
        items[id] = current.copy(endEpochMs = newEndEpochMs)
        emitActive(System.currentTimeMillis())
    }

    override suspend fun history(limit: Int): List<BlockingSession> {
        return items.values
            .filter { it.status != "active" }
            .sortedByDescending { it.endEpochMs }
            .take(limit)
    }
}

class FakeScheduleRepository : ScheduleRepository {
    private val items = LinkedHashMap<Long, Schedule>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<Schedule>>(emptyList())

    private fun emit() {
        flow.value = items.values.sortedBy { it.startMinute }
    }

    override suspend fun list(): List<Schedule> {
        return items.values.sortedBy { it.startMinute }
    }

    override fun observe(): Flow<List<Schedule>> {
        return flow.asStateFlow()
    }

    override suspend fun add(schedule: Schedule): Long {
        val id = nextId++
        items[id] = schedule.copy(id = id)
        emit()
        return id
    }

    override suspend fun update(schedule: Schedule) {
        if (!items.containsKey(schedule.id)) return
        items[schedule.id] = schedule
        emit()
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val current = items[id] ?: return
        items[id] = current.copy(enabled = enabled)
        emit()
    }

    override suspend fun remove(id: Long) {
        items.remove(id)
        emit()
    }
}
