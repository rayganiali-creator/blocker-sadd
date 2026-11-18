package com.sadd.blocker.di

import android.content.Context
import androidx.room.Room
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.db.AppDatabase
import com.sadd.blocker.data.db.BlockedAppDao
import com.sadd.blocker.data.db.BlockedWebsiteDao
import com.sadd.blocker.data.db.BlockingSessionDao
import com.sadd.blocker.data.db.KeywordRuleDao
import com.sadd.blocker.data.db.MIGRATION_1_2
import com.sadd.blocker.data.db.MIGRATION_2_3
import com.sadd.blocker.data.db.ScheduleDao
import com.sadd.blocker.data.repository.RoomBlockedAppRepository
import com.sadd.blocker.data.repository.RoomKeywordRepository
import com.sadd.blocker.data.repository.RoomRestoreTransaction
import com.sadd.blocker.data.repository.RoomScheduleRepository
import com.sadd.blocker.data.repository.RoomSessionRepository
import com.sadd.blocker.data.repository.RoomWebsiteRepository
import com.sadd.blocker.domain.repository.RestoreTransaction
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.ScheduleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.AddBlockedAppUseCase
import com.sadd.blocker.domain.usecase.AddKeywordRuleUseCase
import com.sadd.blocker.domain.usecase.AddWebsiteUseCase
import com.sadd.blocker.domain.usecase.EndSessionUseCase
import com.sadd.blocker.domain.usecase.ExtendSessionUseCase
import com.sadd.blocker.domain.usecase.RestoreBackupUseCase
import com.sadd.blocker.domain.usecase.StartSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "blocker.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // عمداً بدون fallbackToDestructiveMigration: قانون کاربر با آپدیت پاک نشود.
            .build()
    }

    @Provides
    fun provideBlockedAppDao(db: AppDatabase): BlockedAppDao = db.blockedAppDao()

    @Provides
    fun provideBlockedWebsiteDao(db: AppDatabase): BlockedWebsiteDao = db.blockedWebsiteDao()

    @Provides
    fun provideKeywordRuleDao(db: AppDatabase): KeywordRuleDao = db.keywordRuleDao()

    @Provides
    fun provideSessionDao(db: AppDatabase): BlockingSessionDao = db.sessionDao()

    @Provides
    fun provideScheduleDao(db: AppDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    @Singleton
    fun provideAppRepository(dao: BlockedAppDao): BlockedAppRepository {
        return RoomBlockedAppRepository(dao)
    }

    @Provides
    @Singleton
    fun provideWebsiteRepository(dao: BlockedWebsiteDao): WebsiteRepository {
        return RoomWebsiteRepository(dao)
    }

    @Provides
    @Singleton
    fun provideKeywordRepository(dao: KeywordRuleDao): KeywordRepository {
        return RoomKeywordRepository(dao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(dao: BlockingSessionDao): SessionRepository {
        return RoomSessionRepository(dao)
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(dao: ScheduleDao): ScheduleRepository {
        return RoomScheduleRepository(dao)
    }

    @Provides
    fun provideAddKeywordRuleUseCase(repo: KeywordRepository): AddKeywordRuleUseCase {
        return AddKeywordRuleUseCase(repo)
    }

    @Provides
    fun provideAddWebsiteUseCase(repo: WebsiteRepository): AddWebsiteUseCase {
        return AddWebsiteUseCase(repo)
    }

    @Provides
    fun provideAddBlockedAppUseCase(repo: BlockedAppRepository): AddBlockedAppUseCase {
        return AddBlockedAppUseCase(repo)
    }

    @Provides
    fun provideStartSessionUseCase(repo: SessionRepository): StartSessionUseCase {
        return StartSessionUseCase(repo)
    }

    @Provides
    fun provideEndSessionUseCase(
        repo: SessionRepository,
        settingsStore: SettingsStore,
    ): EndSessionUseCase {
        // SettingsStore درگاه PinGate است: بررسی PIN در UseCase انجام می‌شود (H-1).
        return EndSessionUseCase(repo, settingsStore)
    }

    @Provides
    fun provideExtendSessionUseCase(repo: SessionRepository): ExtendSessionUseCase {
        return ExtendSessionUseCase(repo)
    }

    @Provides
    fun provideRestoreTransaction(db: AppDatabase): RestoreTransaction {
        return RoomRestoreTransaction(db)
    }

    @Provides
    fun provideRestoreBackupUseCase(
        apps: BlockedAppRepository,
        websites: WebsiteRepository,
        keywords: KeywordRepository,
        schedules: ScheduleRepository,
        transaction: RestoreTransaction,
    ): RestoreBackupUseCase {
        return RestoreBackupUseCase(apps, websites, keywords, schedules, transaction)
    }
}
