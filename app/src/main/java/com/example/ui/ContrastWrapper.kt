package com.example.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.util.getContrastColor

@Composable
fun ContrastWrapper(backgroundColor: Color, content: @Composable () -> Unit) {
    val colorInt = backgroundColor.toArgb()
    val hexColor = String.format("#%06X", (0xFFFFFF and colorInt))
    val contentColorHex = getContrastColor(hexColor)
    val contentColor = Color(android.graphics.Color.parseColor(contentColorHex))
    
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        content()
    }
}
