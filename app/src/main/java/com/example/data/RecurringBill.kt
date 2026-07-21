package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "recurring_bills")
@Serializable
data class RecurringBill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int,
    val description: String,
    val amount: Double,
    val interval: String, // "Weekly", "Monthly", "Yearly"
    val startDate: Long = System.currentTimeMillis(),
    val nextGenerationDate: Long,
    val isActive: Boolean = true
)
