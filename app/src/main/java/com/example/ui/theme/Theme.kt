package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrostedGlassColorScheme =
  darkColorScheme(
    primary = GlassPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0x333B82F6),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = GlassAccentBlue,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0x2638BDF8),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = GlassVioletOrb,
    onTertiary = Color.White,
    background = GlassBackground,
    onBackground = GlassTextPrimary,
    surface = GlassSurfaceDark,
    onSurface = GlassTextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = GlassTextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderSubtle,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = FrostedGlassColorScheme,
    typography = Typography,
    content = content,
  )
}


