package com.sadd.blocker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * دیتابیس Room — نسخه ۱.
 * مهاجرت‌ها از نسخه ۲ به بعد اینجا ثبت می‌شوند (Migration)؛
 * fallbackToDestructiveMigration ممنوع است تا قانون کاربر با آپدیت پاک نشود.
 */
@Database(
    entities = [
        BlockedAppEntity::class,
        BlockedWebsiteEntity::class,
        KeywordRuleEntity::class,
        BlockingSessionEntity::class,
        ScheduleEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun sessionDao(): BlockingSessionDao
}
