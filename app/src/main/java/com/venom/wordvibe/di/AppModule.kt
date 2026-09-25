package com.venom.wordvibe.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.venom.analytics.AnalyticsManager
import com.venom.analytics.CrashlyticsManager
import com.venom.analytics.StubAnalyticsManager
import com.venom.analytics.StubCrashlyticsManager
import com.venom.data.local.RoomTransactionRunner
import com.venom.data.local.TransactionRunner
import com.venom.data.local.WordVibeDatabase
import com.venom.data.local.dao.QuizAttemptDao
import com.venom.data.local.dao.UsageDailyDao
import com.venom.data.local.dao.UserActivityDao
import com.venom.data.local.dao.UserIdentityDao
import com.venom.data.local.dao.UserProfileDao
import com.venom.data.local.dao.UserWordProgressDao
import com.venom.data.local.dao.WordMasterDao
import com.venom.data.local.dao.XpEventDao
import com.venom.data.repo.StubQuizRepository
import com.venom.data.repo.StubRemoteConfig
import com.venom.data.repo.StubWordMasterRepository
import com.venom.domain.repo.IQuizRepository
import com.venom.domain.repo.IRemoteConfig
import com.venom.domain.repo.IWordMasterRepository
import com.venom.domain.srs.SrsEngine
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
    fun provideDatabase(@ApplicationContext context: Context): WordVibeDatabase {
        return Room.databaseBuilder(
            context,
            WordVibeDatabase::class.java,
            "wordvibe.db"
        ).fallbackToDestructiveMigration(false).build()
    }

    @Provides
    fun provideUserIdentityDao(db: WordVibeDatabase): UserIdentityDao = db.userIdentityDao()

    @Provides
    fun provideUserProfileDao(db: WordVibeDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideUserActivityDao(db: WordVibeDatabase): UserActivityDao = db.userActivityDao()

    @Provides
    fun provideUserWordProgressDao(db: WordVibeDatabase): UserWordProgressDao = db.userWordProgressDao()

    @Provides
    fun provideWordMasterDao(db: WordVibeDatabase): WordMasterDao = db.wordMasterDao()

    @Provides
    fun provideXpEventDao(db: WordVibeDatabase): XpEventDao = db.xpEventDao()

    @Provides
    fun provideQuizAttemptDao(db: WordVibeDatabase): QuizAttemptDao = db.quizAttemptDao()

    @Provides
    fun provideUsageDailyDao(db: WordVibeDatabase): UsageDailyDao = db.usageDailyDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("wordvibe_settings")
        }
    }

    @Provides
    @Singleton
    fun provideSrsEngine(): SrsEngine = SrsEngine()

    @Provides
    @Singleton
    fun provideIRemoteConfig(impl: StubRemoteConfig): IRemoteConfig = impl

    @Provides
    @Singleton
    fun provideIQuizRepository(impl: StubQuizRepository): IQuizRepository = impl

    @Provides
    @Singleton
    fun provideIWordMasterRepository(impl: StubWordMasterRepository): IWordMasterRepository = impl

    @Provides
    @Singleton
    fun provideCrashlyticsManager(impl: StubCrashlyticsManager): CrashlyticsManager = impl

    @Provides
    @Singleton
    fun provideAnalyticsManager(impl: StubAnalyticsManager): AnalyticsManager = impl

    @Provides
    @Singleton
    fun provideTransactionRunner(impl: RoomTransactionRunner): TransactionRunner = impl
}
