package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "paper_types")
@Serializable
data class PaperType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val basePrice: Double = 0.0,
    val isDefault: Boolean = false
)
