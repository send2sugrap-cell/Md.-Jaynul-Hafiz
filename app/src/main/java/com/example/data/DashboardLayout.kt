package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class DashboardWidget(
    val id: String,
    val title: String,
    val isVisible: Boolean = true,
    val heightMultiplier: Float = 1.0f,
    val widthSpan: Int = 12, // 1 to 12 columns
    val order: Int = 0
)

@Serializable
data class DashboardLayout(
    val widgets: List<DashboardWidget>
)
