package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "management_users")
@Serializable
data class ManagementUser(
    @PrimaryKey val email: String,
    val fullName: String,
    val mobileNumber: String,
    val passwordHash: String,
    val role: String // "admin" or "staff"
)
