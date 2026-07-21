package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "registered_customers")
@Serializable
data class RegisteredCustomer(
    val fullName: String,
    val mobile: String,
    val customerNumber: String,
    @PrimaryKey val username: String,
    val passwordHash: String
)
