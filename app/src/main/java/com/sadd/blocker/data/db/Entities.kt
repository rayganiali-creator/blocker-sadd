package com.sadd.blocker.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
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

    @Query("DELETE FROM keyword_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BlockingSessionDao {
    @Query("SELECT * FROM blocking_sessions WHERE status = 'active' AND endEpochMs > :nowMs ORDER BY endEpochMs DESC LIMIT 1")
    suspend fun getActive(nowMs: Long): BlockingSessionEntity?

    @Query("SELECT * FROM blocking_sessions WHERE status = 'active' AND endEpochMs > :nowMs ORDER BY endEpochMs DESC LIMIT 1")
    fun observeActive(nowMs: Long): Flow<BlockingSessionEntity?>

    @Query("SELECT * FROM blocking_sessions WHERE id = :id")
    suspend fun getById(id: Long): BlockingSessionEntity?

    @Query("SELECT * FROM blocking_sessions WHERE status != 'active' ORDER BY endEpochMs DESC LIMIT :limit")
    suspend fun history(limit: Int): List<BlockingSessionEntity>

    @Insert
    suspend fun insert(entity: BlockingSessionEntity): Long

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

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}
