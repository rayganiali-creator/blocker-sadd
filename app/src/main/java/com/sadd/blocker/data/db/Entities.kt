package com.sadd.blocker.data.db

import androidx.room.Dao
import androidx.room.Entity
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

@Entity(tableName = "blocked_websites")
data class BlockedWebsiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val matchSubdomains: Boolean = true,
    val enabled: Boolean = true,
    val delaySeconds: Int? = null,
)

@Entity(tableName = "keyword_rules")
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

    @Query("SELECT * FROM blocked_apps WHERE enabled = 1")
    suspend fun getEnabled(): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<KeywordRuleEntity>

    // جلوگیری از قانون تکراری با مقایسه متن نرمال‌شده
    @Query("SELECT COUNT(*) FROM keyword_rules WHERE normalizedText = :normalized")
    suspend fun countByNormalized(normalized: String): Int

    @Insert
    suspend fun insert(entity: KeywordRuleEntity): Long

    @Update
    suspend fun update(entity: KeywordRuleEntity)

    @Query("DELETE FROM keyword_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BlockingSessionDao {
    @Query("SELECT * FROM blocking_sessions WHERE status = 'active' ORDER BY endEpochMs DESC LIMIT 1")
    suspend fun getActive(): BlockingSessionEntity?

    @Query("SELECT * FROM blocking_sessions WHERE status = 'active'")
    fun observeActive(): Flow<List<BlockingSessionEntity>>

    @Insert
    suspend fun insert(entity: BlockingSessionEntity): Long

    @Query("UPDATE blocking_sessions SET status = :status, endedReason = :reason WHERE id = :id")
    suspend fun end(id: Long, status: String, reason: String)
}
