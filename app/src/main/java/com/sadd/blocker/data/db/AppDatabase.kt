package com.sadd.blocker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * دیتابیس Room — نسخه ۴.
 * مهاجرت‌ها از نسخه ۲ به بعد اینجا ثبت می‌شوند (Migration)؛
 * fallbackToDestructiveMigration ممنوع است تا قانون کاربر با آپدیت پاک نشود.
 *
 * نسخه ۲: ستون startElapsedMs به blocking_sessions اضافه شد (H-6:
 * راستی‌آزمایی سشن در برابر عقب کشیدن ساعت سیستم).
 * نسخه ۳: جدول‌های section_rules (بخش‌های اپ) و block_events (گزارش
 * بلاک‌ها) اضافه شدند؛ پیش‌فرض‌های بخش در مهاجرت seed می‌شوند.
 * نسخه ۴: بدون تغییر اسکیما — فقط ۵ پیش‌فرض جدید بخش seed می‌شوند
 * (کل تلگرام/اینستاگرام/یوتیوب، ریلز اینستاگرام، شورتس یوتیوب).
 * ثبت مهاجرت در AppModule لازم است: .addMigrations(..., MIGRATION_3_4).
 */
@Database(
    entities = [
        BlockedAppEntity::class,
        BlockedWebsiteEntity::class,
        KeywordRuleEntity::class,
        BlockingSessionEntity::class,
        ScheduleEntity::class,
        SectionRuleEntity::class,
        BlockEventEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun blockedWebsiteDao(): BlockedWebsiteDao
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun sessionDao(): BlockingSessionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sectionRuleDao(): SectionRuleDao
    abstract fun blockEventDao(): BlockEventDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE blocking_sessions " +
                "ADD COLUMN startElapsedMs INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `section_rules` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`packageName` TEXT NOT NULL, " +
                "`sectionKey` TEXT NOT NULL, " +
                "`labelFa` TEXT NOT NULL, " +
                "`enabled` INTEGER NOT NULL DEFAULT 1)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_section_rules_packageName_sectionKey` " +
                "ON `section_rules` (`packageName`, `sectionKey`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `block_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestampMs` INTEGER NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`detail` TEXT NOT NULL)",
        )
        // پیش‌فرض‌های بخش (همان SectionDetector.PRESETS)؛ کاربر بعداً تاگل می‌کند.
        // INSERT OR IGNORE: اگر کاربر پیش‌تر دستی ساخته باشد، دست نمی‌خورد.
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('org.telegram.messenger', 'telegram_search', 'جستجوی تلگرام', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.instagram.android', 'instagram_search', 'جستجوی اینستاگرام', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.instagram.android', 'instagram_explore', 'اکسپلور اینستاگرام', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.google.android.youtube', 'youtube_search', 'جستجوی یوتیوب', 1)",
        )
    }
}

/**
 * مهاجرت ۳ به ۴: بدون تغییر اسکیما — فقط ۵ پیش‌فرض جدید بخش برای
 * نصب‌های موجود seed می‌شوند (همان الگوی MIGRATION_2_3).
 * INSERT OR IGNORE: اگر کاربر پیش‌تر دستی ساخته باشد، دست نمی‌خورد؛
 * و اگر قانون را غیرفعال کرده باشد، enabled=1 بازنویسی نمی‌شود چون
 * سطر موجود نادیده گرفته می‌شود.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('org.telegram.messenger', 'telegram_app', 'خود تلگرام', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.instagram.android', 'instagram_app', 'خود اینستاگرام', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.instagram.android', 'instagram_reels', 'ریلز اینستاگرام', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.google.android.youtube', 'youtube_app', 'خود یوتیوب', 1)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO section_rules " +
                "(packageName, sectionKey, labelFa, enabled) VALUES " +
                "('com.google.android.youtube', 'youtube_shorts', 'شورتس یوتیوب', 1)",
        )
    }
}
