package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.R

data class WallpaperItem(
    val id: String,
    val title: String,
    val soundLabel: String,
    val category: String,
    val quote: String? = null,
    val quoteSub: String? = null,
    val drawableRes: Int? = null,
    val customUri: String? = null,
    val gradientColors: List<Color>,
    val accentColor: Color = Color(0xFF38BDF8),
    val iconEmoji: String = "✨"
)

object WallpaperCatalog {

    const val DEFAULT_WALLPAPER_ID = "wp_nature_scenic"

    val NATURE_ITEMS = listOf(
        WallpaperItem(
            id = "wp_nature_scenic",
            title = "Scenic Nature",
            soundLabel = "Mountain Sunrise & Dawn Light",
            category = "Nature",
            quote = "MORNING HORIZON\nRise with golden light.",
            drawableRes = R.drawable.wp_nature_scenic,
            gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF78350F)),
            accentColor = Color(0xFFF59E0B),
            iconEmoji = "🏞️"
        ),
        WallpaperItem(
            id = "wp_mountain_lake",
            title = "Mountain Lake",
            soundLabel = "Alpine Reflection",
            category = "Nature",
            drawableRes = R.drawable.wp_mountain_lake,
            gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF0C4A6E)),
            accentColor = Color(0xFF38BDF8),
            iconEmoji = "🏔️"
        )
    )

    val TRENDING_ITEMS = listOf(
        WallpaperItem(
            id = "wp_y_u_no",
            title = "Y U NO Wake Up",
            soundLabel = "Rage Comic",
            category = "Trending",
            drawableRes = R.drawable.wp_y_u_no,
            gradientColors = listOf(Color(0xFF374151), Color(0xFF1F2937), Color(0xFF111827)),
            accentColor = Color(0xFFF59E0B),
            iconEmoji = "😤"
        ),
        WallpaperItem(
            id = "wp_wa0029",
            title = "Morning Motivation",
            soundLabel = "Rise & Shine",
            category = "Trending",
            quote = "RISE & SHINE\nMake today count.",
            drawableRes = null,
            gradientColors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A)),
            accentColor = Color(0xFFEAB308),
            iconEmoji = "✨"
        ),
        WallpaperItem(
            id = "wp_wa0030",
            title = "Dream Big",
            soundLabel = "Daily Inspiration",
            category = "Trending",
            quote = "DREAM BIG\nOutwork yesterday.",
            drawableRes = null,
            gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF172554), Color(0xFF0F172A)),
            accentColor = Color(0xFF38BDF8),
            iconEmoji = "🎯"
        ),
        WallpaperItem(
            id = "wp_whatsapp_screenshot",
            title = "Daily Focus",
            soundLabel = "Morning Chat",
            category = "Trending",
            quote = "FOCUS ON GOALS\nNever look back.",
            drawableRes = null,
            gradientColors = listOf(Color(0xFF27272A), Color(0xFF18181B), Color(0xFF09090B)),
            accentColor = Color(0xFF22C55E),
            iconEmoji = "💬"
        )
    )

    val MOTIVATION_ITEMS = listOf(
        WallpaperItem(
            id = "wp_inspire_green",
            title = "Mindset Shift",
            soundLabel = "Focus & Grow",
            category = "Motivation",
            drawableRes = R.drawable.wp_inspire_green,
            gradientColors = listOf(Color(0xFF166534), Color(0xFF14532D), Color(0xFF052E16)),
            accentColor = Color(0xFF4ADE80),
            iconEmoji = "🌱"
        ),
        WallpaperItem(
            id = "wp_minimal_quote",
            title = "Never Give Up",
            soundLabel = "Pure Grit",
            category = "Motivation",
            drawableRes = R.drawable.wp_minimal_quote,
            gradientColors = listOf(Color(0xFF18181B), Color(0xFF09090B), Color(0xFF000000)),
            accentColor = Color(0xFFFACC15),
            iconEmoji = "🔥"
        ),
        WallpaperItem(
            id = "wp_quote_badge",
            title = "Stay Persistent",
            soundLabel = "Courage",
            category = "Motivation",
            drawableRes = R.drawable.wp_quote_badge,
            gradientColors = listOf(Color(0xFF854D0E), Color(0xFF713F12), Color(0xFF422006)),
            accentColor = Color(0xFFFBBF24),
            iconEmoji = "💪"
        )
    )

    val SPACE_ITEMS = listOf(
        WallpaperItem(
            id = "wp_celestial_sky",
            title = "Celestial Night",
            soundLabel = "Cosmic Sky",
            category = "Space",
            drawableRes = R.drawable.wp_celestial_sky,
            gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81)),
            accentColor = Color(0xFFA855F7),
            iconEmoji = "🌌"
        )
    )

    val ALL_WALLPAPERS: List<WallpaperItem> by lazy {
        NATURE_ITEMS + TRENDING_ITEMS + MOTIVATION_ITEMS + SPACE_ITEMS
    }

    fun findById(id: String?): WallpaperItem {
        val defaultScenic = NATURE_ITEMS.first()
        if (id.isNullOrBlank() || id.equals("DEFAULT", ignoreCase = true)) return defaultScenic
        // If it starts with "custom_uri:" then it's a user photo picked from device album
        if (id.startsWith("content://") || id.startsWith("file://") || id.startsWith("custom_uri:")) {
            val uri = id.removePrefix("custom_uri:")
            return WallpaperItem(
                id = id,
                title = "My Album Photo",
                soundLabel = "Custom Photo",
                category = "My Photos",
                customUri = uri,
                gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
                accentColor = Color(0xFF38BDF8),
                iconEmoji = "📸"
            )
        }
        return ALL_WALLPAPERS.find { it.id.equals(id, ignoreCase = true) } ?: defaultScenic
    }
}
