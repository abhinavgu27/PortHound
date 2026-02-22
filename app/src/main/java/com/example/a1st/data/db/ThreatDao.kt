package com.example.a1st.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threat_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ThreatLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ThreatLogEntity)

    @Query("DELETE FROM threat_logs")
    suspend fun clearAllLogs()
}
