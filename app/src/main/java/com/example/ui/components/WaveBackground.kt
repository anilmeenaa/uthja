package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LuneBackground
import com.example.ui.theme.LuneColors
import com.example.ui.theme.LocalIsDarkMode

@Composable
fun WaveBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
    ) {
        // Ambient decorative glow blob at top left
        Box(
            modifier = Modifier
                .offset(x = (-90).dp, y = 80.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF4C2B6A) else LuneColors.surfaceLavender)
                .alpha(if (isDark) 0.12f else 0.06f)
        )

        // Ambient decorative glow blob at bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = (-120).dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF2E2452) else LuneColors.surfaceLavender)
                .alpha(if (isDark) 0.15f else 0.08f)
        )

        content()
    }
}
