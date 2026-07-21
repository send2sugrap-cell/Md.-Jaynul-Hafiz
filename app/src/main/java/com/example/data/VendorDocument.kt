package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "vendor_documents")
@Serializable
data class VendorDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int,
    val category: String, // e.g., "Receipt", "Contract", "Utility Bill", "Other"
    val title: String,
    val filePath: String, // Content URI as string
    val uploadTimestamp: Long = System.currentTimeMillis()
)
