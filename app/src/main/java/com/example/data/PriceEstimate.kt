package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "price_estimates")
@Serializable
data class PriceEstimate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String = "Valued Customer",
    val clientMobile: String = "",
    val paperType: String,
    val printSize: String,
    val quantity: Int,
    val totalPrice: Double,
    val orderStatus: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
)
