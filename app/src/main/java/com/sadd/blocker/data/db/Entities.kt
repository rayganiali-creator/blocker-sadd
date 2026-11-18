package com.sadd.blocker.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val enabled: Boolean = true,
    val delaySeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "blocked_websites",
    indices = [Index(value = ["host"], unique = true)],
)
data class BlockedWebsiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val matchSubdomains: Boolean = true,
    val enabled: Boolean = true,
    val delaySeconds: Int? = null,
)

@Entity(
    tableName = "keyword_rules",
    indices = [Index(value = ["normalizedText"], unique = true)],
)
data class KeywordRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawText: String,
    val normalizedText: String,
    val category: String = "عمومی",
    val isPhrase: Boolean = false,
    val enabled: Boolean = true,
    val delaySeconds: Int? = null,
)

@Entity(tableName = "blocking_sessions")
data class BlockingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val strict: Boolean = false,
    val ruleScope: String = "all",
    val status: String = "active",
    val endedReason: String? = null,
    /** مقدار SystemClock.elapsedRealtime() موقع شروع (H-6)؛ ۰ یعنی نامشخص. */
    val startElapsedMs: Long = 0,
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daysOfWeek: String = "",
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val strict: Boolean = false,
    val ruleScope: String = "all",
    val enabled: Boolean = true,
)

/**
 * قانون «بخشِ اپ» (نسخه ۳): یک بخش داخل یک اپ، مثلاً جستجوی تلگرام.
 * ترکیب (packageName, sectionKey) یکتاست؛ پیش‌فرض‌ها از
 * [com.sadd.blocker.core.SectionDetector.PRESETS] می‌آیند.
 */
@Entity(
    tableName = "section_rules",
    indices = [Index(value = ["packageName", "sectionKey"], unique = true)],
)
data class SectionRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val sectionKey: String,
    val labelFa: String,
    val enabled: Boolean = true,
)

/** رویداد بلاک (نسخه ۳): هر بلاک موتور، یک سطر؛ داشبورد «بلاک امروز» را از آن می‌شمارد. */
@Entity(tableName = "block_events")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val kind: String,
    val detail: String,
)

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY label")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps ORDER BY label")
    suspend fun listAll(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE enabled = 1")
    suspend fun getEnabled(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): BlockedAppEntity?

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE enabled = 1")
    suspend fun countEnabled(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockedAppEntity)

    @Update
    suspend fun update(entity: BlockedAppEntity)

    // M-16: آپدیت مستقیم به‌جای read-then-write (جلوگیری از lost update).
    @Query("UPDATE blocked_apps SET enabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE blocked_apps SET delaySeconds = :delaySeconds WHERE packageName = :packageName")
    suspend fun setDelay(packageName: String, delaySeconds: Int?)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface BlockedWebsiteDao {
    @Query("SELECT * FROM blocked_websites ORDER BY host")
    fun observeAll(): Flow<List<BlockedWebsiteEntity>>

    @Query("SELECT * FROM blocked_websites ORDER BY host")
    suspend fun listAll(): List<BlockedWebsiteEntity>

    @Query("SELECT * FROM blocked_websites WHERE id = :id")
    suspend fun findById(id: Long): BlockedWebsiteEntity?

    @Query("SELECT * FROM blocked_websites WHERE enabled = 1")
    suspend fun getEnabled(): List<BlockedWebsiteEntity>

    @Query("SELECT * FROM blocked_websites WHERE host = :host")
    suspend fun findByHost(host: String): BlockedWebsiteEntity?

    @Query("SELECT COUNT(*) FROM blocked_websites WHERE enabled = 1")
    suspend fun countEnabled(): Int

    // IGNORE: قانون تکراری (host یکتا) با -1 برگردانده می‌شود، نه exception.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BlockedWebsiteEntity): Long

    @Update
    suspend fun update(entity: BlockedWebsiteEntity)

    // M-16: آپدیت مستقیم به‌جای read-then-write.
    @Query("UPDATE blocked_websites SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE blocked_websites SET delaySeconds = :delaySeconds WHERE id = :id")
    suspend fun setDelay(id: Long, delaySeconds: Int?)

    @Query("DELETE FROM blocked_websites WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules ORDER BY id DESC")
    suspend fun listAll(): List<KeywordRuleEntity>

    @Query("SELECT * FROM keyword_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<KeywordRuleEntity>

    @Query("SELECT COUNT(*) FROM keyword_rules WHERE enabled = 1")
    suspend fun countEnabled(): Int

    @Query("SELECT * FROM keyword_rules WHERE id = :id")
    suspend fun getById(id: Long): KeywordRuleEntity?

    // جلوگیری از قانون تکراری با مقایسه متن نرمال‌شده
    @Query("SELECT COUNT(*) FROM keyword_rules WHERE normalizedText = :normalized")
    suspend fun countByNormalized(normalized: String): Int

    // IGNORE: متن نرمال‌شده تکراری با -1 برگردانده می‌شود، نه exception.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: KeywordRuleEntity): Long

    @Update
    suspend fun update(entity: KeywordRuleEntity)

    // M-16: آپدیت مستقیم به‌جای read-then-write.
    @Query("UPDATE keyword_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE keyword_rules SET delaySeconds = :delaySeconds WHERE id = :id")
    suspend fun setDelay(id: Long, delaySeconds: Int?)

    @Query("DELETE FROM keyword_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BlockingSessionDao {
    @Query("SELECT * FROM blocking_sessions WHERE status = 'active' AND endEpochMs > :nowMs ORDER BY endEpochMs DESC LIMIT 1")
    suspend fun getActive(nowMs: Long): BlockingSessionEntity?

    /**
     * H-4: بدون پارامتر زمان — nowMs فریز شده در Flow، سشن منقضی را برای
     * همیشه «فعال» نشان می‌داد. فقط status خوانده می‌شود؛ انقضا را
     * مصرف‌کننده با SessionCalculator چک می‌کند.
     */
    @Query("SELECT * FROM blocking_sessions WHERE status = 'active' ORDER BY endEpochMs DESC LIMIT 1")
    fun observeActive(): Flow<BlockingSessionEntity?>

    @Query("SELECT * FROM blocking_sessions WHERE id = :id")
    suspend fun getById(id: Long): BlockingSessionEntity?

    @Query("SELECT * FROM blocking_sessions WHERE status != 'active' ORDER BY endEpochMs DESC LIMIT :limit")
    suspend fun history(limit: Int): List<BlockingSessionEntity>

    @Insert
    suspend fun insert(entity: BlockingSessionEntity): Long

    /**
     * H-9: شروع اتمی در یک تراکنش — چک سشن فعال و insert با هم انجام
     * می‌شوند تا دو شروع هم‌زمان دو سشن فعال نسازند. ‎-1‎ یعنی سشن فعال هست.
     */
    @Transaction
    suspend fun insertIfNoActive(entity: BlockingSessionEntity, nowMs: Long): Long {
        if (getActive(nowMs) != null) return -1L
        return insert(entity)
    }

    /** H-5: سشن‌های active ولی منقضی‌شده را به expired تبدیل می‌کند. */
    @Query("UPDATE blocking_sessions SET status = 'expired', endedReason = 'expired' WHERE status = 'active' AND endEpochMs <= :nowMs")
    suspend fun expireOld(nowMs: Long): Int

    @Query("UPDATE blocking_sessions SET endEpochMs = :newEnd WHERE id = :id")
    suspend fun updateEnd(id: Long, newEnd: Long)

    @Query("UPDATE blocking_sessions SET status = :status, endedReason = :reason WHERE id = :id")
    suspend fun end(id: Long, status: String, reason: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY startMinute")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY startMinute")
    suspend fun listAll(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    suspend fun getEnabled(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Insert
    suspend fun insert(entity: ScheduleEntity): Long

    @Update
    suspend fun update(entity: ScheduleEntity)

    // M-16: آپدیت مستقیم به‌جای read-then-write.
    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SectionRuleDao {
    @Query("SELECT * FROM section_rules ORDER BY packageName, sectionKey")
    fun observeAll(): Flow<List<SectionRuleEntity>>

    @Query("SELECT * FROM section_rules ORDER BY packageName, sectionKey")
    suspend fun listAll(): List<SectionRuleEntity>

    @Query("SELECT * FROM section_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<SectionRuleEntity>

    @Query("SELECT * FROM section_rules WHERE packageName = :packageName AND sectionKey = :sectionKey")
    suspend fun getByPackageAndSection(packageName: String, sectionKey: String): SectionRuleEntity?

    // IGNORE: پیش‌فرض تکراری با -1 برگردانده می‌شود، نه exception.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SectionRuleEntity): Long

    // M-16: آپدیت مستقیم به‌جای read-then-write.
    @Query("UPDATE section_rules SET enabled = :enabled WHERE packageName = :packageName AND sectionKey = :sectionKey")
    suspend fun setEnabled(packageName: String, sectionKey: String, enabled: Boolean)

    @Query("DELETE FROM section_rules WHERE packageName = :packageName AND sectionKey = :sectionKey")
    suspend fun delete(packageName: String, sectionKey: String)
}

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(entity: BlockEventEntity): Long

    /** تعداد بلاک‌ها از [sinceMs] به بعد — داشبورد «بلاک امروز» را از آن می‌شمارد. */
    @Query("SELECT COUNT(*) FROM block_events WHERE timestampMs >= :sinceMs")
    fun countSince(sinceMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events WHERE timestampMs >= :sinceMs")
    suspend fun countSinceNow(sinceMs: Long): Int

    @Query("DELETE FROM block_events WHERE timestampMs < :beforeMs")
    suspend fun pruneOlderThan(beforeMs: Long): Int
}
