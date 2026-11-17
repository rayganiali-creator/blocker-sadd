package com.sadd.blocker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * دیتابیس Room — نسخه ۲.
 * مهاجرت‌ها از نسخه ۲ به بعد اینجا ثبت می‌شوند (Migration)؛
 * fallbackToDestructiveMigration ممنوع است تا قانون کاربر با آپدیت پاک نشود.
 *
 * نسخه ۲: ستون startElapsedMs به blocking_sessions اضافه شد (H-6:
 * راستی‌آزمایی سشن در برابر عقب کشیدن ساعت سیستم).
 */
@Database(
    entities = [
        BlockedAppEntity::class,
        BlockedWebsiteEntity::class,
        KeywordRuleEntity::class,
        BlockingSessionEntity::class,
        ScheduleEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockedWebsiteDao(): BlockedWebsiteDao
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun sessionDao(): BlockingSessionDao
    abstract fun scheduleDao(): ScheduleDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE blocking_sessions " +
                "ADD COLUMN startElapsedMs INTEGER NOT NULL DEFAULT 0",
        )
    }
}
