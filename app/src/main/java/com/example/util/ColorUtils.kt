package com.example.util

import androidx.compose.ui.graphics.Color

fun getContrastColor(hexColor: String): String {
    val colorInt = android.graphics.Color.parseColor(hexColor)
    val color = Color(colorInt)
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return if (luminance > 0.5) "#000000" else "#FFFFFF"
}
