package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "system_activity_logs")
@Serializable
data class SystemActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String,
    val actor: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
