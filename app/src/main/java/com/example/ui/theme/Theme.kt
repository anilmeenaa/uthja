package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val title: String, val description: String) {
    SYSTEM("System default", "Follows device dark/light setting"),
    LIGHT("Light mode", "Pure crisp white & modern aesthetic"),
    DARK("Dark mode", "Deep midnight palette, gentle on night eyes")
}

val LocalIsDarkMode = compositionLocalOf { false }

object LuneTheme {
    val isDark: Boolean
        @Composable
        get() = LocalIsDarkMode.current

    val background: Color
        @Composable
        get() = MaterialTheme.colorScheme.background

    val surface: Color
        @Composable
        get() = MaterialTheme.colorScheme.surface

    val surfaceVariant: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceVariant

    val textPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface

    val textSecondary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val border: Color
        @Composable
        get() = MaterialTheme.colorScheme.outline

    val borderLight: Color
        @Composable
        get() = MaterialTheme.colorScheme.outlineVariant

    val primary: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary
}

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF131018),
    primaryContainer = Color(0xFF382B4B),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF131017),
    onBackground = Color(0xFFFBF7FF),
    surface = Color(0xFF1E1825),
    onSurface = Color(0xFFFBF7FF),
    surfaceVariant = Color(0xFF282132),
    onSurfaceVariant = Color(0xFFB5A9C2),
    outline = Color(0xFF3D324D),
    outlineVariant = Color(0xFF2B2236),
    error = Color(0xFFF87171),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LuneAccent,
    onPrimary = LunePrimaryText,
    primaryContainer = LuneSoftAccent,
    onPrimaryContainer = LunePrimaryText,
    secondary = LunePrimaryText,
    onSecondary = LuneSurface,
    background = LuneBackground,
    onBackground = LunePrimaryText,
    surface = Color.White,
    onSurface = LunePrimaryText,
    surfaceVariant = LuneSurfaceVariant,
    onSurfaceVariant = LuneSecondaryText,
    outline = LuneBorder,
    outlineVariant = Color(0xFFF1F5F9),
    error = LuneError,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  CompositionLocalProvider(LocalIsDarkMode provides darkTheme) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

