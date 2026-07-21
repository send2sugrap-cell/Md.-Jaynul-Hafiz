package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "vendor_bills")
@Serializable
data class VendorBill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int,
    val invoiceNo: String,
    val date: String,
    val orderId: Int? = null,
    val description: String,
    val totalAmount: Double,
    val advancePaid: Double,
    val timestamp: Long = System.currentTimeMillis()
)
