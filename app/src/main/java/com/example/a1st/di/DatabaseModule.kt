package com.example.a1st.di

import android.content.Context
import androidx.room.Room
import com.example.a1st.data.db.ThreatDao
import com.example.a1st.data.db.ThreatDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideThreatDatabase(@ApplicationContext context: Context): ThreatDatabase {
        return Room.databaseBuilder(
            context,
            ThreatDatabase::class.java,
            "threat_database"
        ).build()
    }

    @Provides
    fun provideThreatDao(database: ThreatDatabase): ThreatDao {
        return database.threatDao()
    }
}
