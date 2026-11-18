package com.sadd.blocker.di

import com.sadd.blocker.data.db.AppDatabase
import com.sadd.blocker.data.db.BlockEventDao
import com.sadd.blocker.data.db.SectionRuleDao
import com.sadd.blocker.data.repository.RoomBlockEventRepository
import com.sadd.blocker.data.repository.RoomSectionRuleRepository
import com.sadd.blocker.domain.repository.BlockEventRepository
import com.sadd.blocker.domain.repository.SectionRuleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * بایندینگ‌های موتور بلاک بدون VPN (بخش‌های اپ + گزارش بلاک).
 * جدا از AppModule نگه داشته شده تا مالک آن فایل درگیر نشود.
 *
 * توجه مالک AppModule: ساخت دیتابیس آنجا باید
 * `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)` شود؛ در غیر این صورت
 * Room موقع آپدیت از نسخه ۳ به ۴ (seed پنج پیش‌فرض جدید بخش)
 * خطای «no migration path» می‌دهد و اپ در استارت کرش می‌کند.
 */
@Module
@InstallIn(SingletonComponent::class)
object SectionRulesModule {

    @Provides
    fun provideSectionRuleDao(db: AppDatabase): SectionRuleDao = db.sectionRuleDao()

    @Provides
    fun provideBlockEventDao(db: AppDatabase): BlockEventDao = db.blockEventDao()

    @Provides
    @Singleton
    fun provideSectionRuleRepository(dao: SectionRuleDao): SectionRuleRepository {
        return RoomSectionRuleRepository(dao)
    }

    @Provides
    @Singleton
    fun provideBlockEventRepository(dao: BlockEventDao): BlockEventRepository {
        return RoomBlockEventRepository(dao)
    }
}
