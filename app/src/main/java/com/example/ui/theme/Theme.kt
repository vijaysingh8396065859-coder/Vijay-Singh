package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MilkDiaryDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmeraldPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkAmberSecondary,
    onSecondary = DarkOnAmberSecondary,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = DarkOnAmberContainer,
    tertiary = DarkUpiTertiary,
    onTertiary = DarkOnUpiTertiary,
    tertiaryContainer = DarkUpiContainer,
    onTertiaryContainer = DarkOnUpiContainer,
    background = DarkForestBackground,
    onBackground = DarkInk,
    surface = DarkForestSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
    outlineVariant = DarkForestSurfaceVariant,
    error = DarkDueRed,
    onError = Color(0xFF450A0A),
    errorContainer = DarkDueContainer,
    onErrorContainer = Color(0xFFFEE2E2)
)

private val MilkDiaryLightColorScheme = lightColorScheme(
    primary = MeadowEmerald,
    onPrimary = Color.White,
    primaryContainer = MeadowEmeraldContainer,
    onPrimaryContainer = OnMeadowEmeraldContainer,
    secondary = BrassAmber,
    onSecondary = Color.White,
    secondaryContainer = BrassAmberContainer,
    onSecondaryContainer = OnBrassAmberContainer,
    tertiary = UpiIndigo,
    onTertiary = Color.White,
    tertiaryContainer = UpiIndigoContainer,
    onTertiaryContainer = OnUpiIndigoContainer,
    background = WarmMilkCream,
    onBackground = InkCharcoal,
    surface = PureMilkSurface,
    onSurface = InkCharcoal,
    surfaceVariant = WarmLinenSurface,
    onSurfaceVariant = WarmStone,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = DueTerracotta,
    onError = Color.White,
    errorContainer = DueTerracottaContainer,
    onErrorContainer = OnDueTerracottaContainer
)

val MilkDiaryShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MilkDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MilkDiaryDarkColorScheme else MilkDiaryLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MilkDiaryShapes,
        content = content
    )
}
