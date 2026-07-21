package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "invoice_records")
@Serializable
data class InvoiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val date: String,
    val totalAmount: Double,
    val advanceAmount: Double,
    val filePath: String
)
