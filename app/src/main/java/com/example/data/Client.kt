package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "clients")
@Serializable
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val company: String,
    val position: String,
    val mobile: String,
    val whatsapp: String,
    val email: String,
    val address: String,
    val specifications: String,
    val profilePicUri: String? = null
)
