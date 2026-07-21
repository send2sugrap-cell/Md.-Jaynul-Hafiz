package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "vendor_payments")
@Serializable
data class VendorPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int,
    val date: String,
    val amount: Double,
    val paymentMethod: String,
    val mrNo: String? = null,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)
