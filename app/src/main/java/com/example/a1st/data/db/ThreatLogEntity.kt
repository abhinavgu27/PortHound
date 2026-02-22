package com.example.a1st.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_logs")
data class ThreatLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // EMF, IR, NETWORK
    val value: String,
    val timestamp: Long,
    val riskLevel: String // LOW, MEDIUM, HIGH
)
