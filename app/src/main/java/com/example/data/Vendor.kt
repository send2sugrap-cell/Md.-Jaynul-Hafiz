package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "vendors")
@Serializable
data class Vendor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val companyName: String,
    val phone: String,
    val address: String,
    val category: String,
    val creditLimit: Double = 0.0,
    val budgetLimit: Double = 0.0
)
