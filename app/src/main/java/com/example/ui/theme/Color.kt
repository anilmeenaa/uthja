package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Pure White & Crisp Lavender Palette
object LuneColors {
    val primary = Color(0xFF9A77FF)
    val primaryDeep = Color(0xFF6F46A3)
    val primaryLight = Color(0xFFC9A8F4)
    val primarySoft = Color(0xFFE8D7FF)

    val surface = Color(0xFFFFFFFF)
    val surfaceSoft = Color(0xFFF8FAFC)
    val surfaceMuted = Color(0xFFF1F5F9)
    val surfaceLavender = Color(0xFFF5F3FF)
    val surfacePeach = Color(0xFFFFF1F2)
    val surfaceSage = Color(0xFFF0FDF4)
    val surfaceGold = Color(0xFFFEFCE8)

    val accentPeach = Color(0xFFFFB598)
    val accentSage = Color(0xFF94BA95)
    val accentLavender = Color(0xFFBCA1F3)
    val accentGold = Color(0xFFE5B56E)

    val peachDark = Color(0xFFD97952)
    val sageDark = Color(0xFF4E7E50)
    val goldDark = Color(0xFFB87F2B)

    val text = Color(0xFF1E293B)
    val textSoft = Color(0xFF475569)
    val textMuted = Color(0xFF94A3B8)
    val white = Color(0xFFFFFFFF)
    val coral = Color(0xFFE25B5B)

    val border = Color(0xFFE2E8F0)
    val borderLight = Color(0xFFF1F5F9)
    val borderGlow = Color(0x529A77FF)
}

// Light Pure White Palette
val LuneBackground = Color(0xFFFFFFFF)
val LuneSurface = LuneColors.surface
val LuneSurfaceVariant = LuneColors.surfaceSoft
val LunePrimaryText = LuneColors.text
val LuneSecondaryText = LuneColors.textSoft
val LuneMutedText = LuneColors.textMuted
val LuneAccent = LuneColors.primaryDeep
val LuneSoftAccent = LuneColors.surfaceLavender
val LuneSuccess = LuneColors.sageDark
val LuneWarning = LuneColors.goldDark
val LuneError = LuneColors.coral
val LuneBorder = LuneColors.border

// Dark Night-Friendly Palette (Midnight Mode)
val LuneDarkBackground = Color(0xFF16120E)
val LuneDarkSurface = Color(0xFF221B15)
val LuneDarkSurfaceVariant = Color(0xFF2F261E)
val LuneDarkPrimaryText = Color(0xFFFBF6EE)
val LuneDarkSecondaryText = Color(0xFFB8A898)
val LuneDarkMutedText = Color(0xFF807062)
val LuneDarkAccent = Color(0xFFE5B67C)
val LuneDarkSoftAccent = Color(0xFF453629)
val LuneDarkBorder = Color(0xFF382D22)
