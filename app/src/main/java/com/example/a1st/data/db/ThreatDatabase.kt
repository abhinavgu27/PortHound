package com.example.a1st.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ThreatLogEntity::class], version = 1, exportSchema = false)
abstract class ThreatDatabase : RoomDatabase() {
    abstract fun threatDao(): ThreatDao
}
