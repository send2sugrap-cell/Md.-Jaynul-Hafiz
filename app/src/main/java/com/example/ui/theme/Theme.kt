package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.ContrastWrapper

val RoyalBlueCyanColorScheme = lightColorScheme(
    primary = RoyalBlue,
    secondary = Cyan,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

val DarkColorScheme = darkColorScheme(
    primary = LightGreen,
    secondary = MediumGreen,
    tertiary = DeepGreen,
    background = DarkestGreen,
    surface = DarkGreen,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val HighContrastColorScheme = lightColorScheme(
    primary = HighContrastYellow,
    secondary = Color.White,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun SucharuTheme(
    themeName: String = "ROYAL_BLUE",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "ROYAL_BLUE" -> RoyalBlueCyanColorScheme
        "DARK" -> DarkColorScheme
        "HIGH_CONTRAST" -> HighContrastColorScheme
        else -> RoyalBlueCyanColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        ContrastWrapper(backgroundColor = colorScheme.background) {
            content()
        }
    }
}
