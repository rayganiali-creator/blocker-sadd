package com.sadd.blocker.di

import android.content.Context
import androidx.room.Room
import com.sadd.blocker.data.db.AppDatabase
import com.sadd.blocker.data.db.BlockingSessionDao
import com.sadd.blocker.data.db.KeywordRuleDao
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
            // عمداً بدون fallbackToDestructiveMigration: قانون کاربر با آپدیت پاک نشود.
            .build()
    }

    @Provides
    fun provideSessionDao(db: AppDatabase): BlockingSessionDao = db.sessionDao()

    @Provides
    fun provideKeywordRuleDao(db: AppDatabase): KeywordRuleDao = db.keywordRuleDao()
}
