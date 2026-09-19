package com.overscroll.app.di

import android.content.Context
import androidx.room.Room
import com.overscroll.app.data.history.AppDatabase
import com.overscroll.app.data.history.DailyCountDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "overscroll_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }

    @Provides
    fun provideDailyCountDao(database: AppDatabase): DailyCountDao {
        return database.dailyCountDao()
    }
}
