package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "orders")
@Serializable
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val clientName: String = "",
    val jobName: String,
    val category: String,
    val specifications: String = "",
    val quantity: String = "",
    val perUnitCost: Double = 0.0,
    val totalAmount: Double,
    val profitAmount: Double = 0.0,
    val advanceAmount: Double = 0.0, // Added for billing
    val status: String = "New Job",
    val packageCount: Int = 0,
    val quantityPerPacket: Int = 0,
    val previousStage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
