package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

enum class AppLogoSize(val dimension: Dp) {
    SM(42.dp),
    MD(64.dp),
    LG(94.dp)
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: AppLogoSize = AppLogoSize.MD,
    showWordmark: Boolean = true
) {
    val dimension = size.dimension

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App logo container using the uploaded Uthja logo
        Box(
            modifier = Modifier
                .size(dimension)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = Color(0x33FF9A77),
                    ambientColor = Color(0x22FF9A77)
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_uthja_logo),
                contentDescription = "Uthja Logo",
                modifier = Modifier.size(dimension),
                contentScale = ContentScale.Fit
            )
        }

        if (showWordmark) {
            AppText(
                text = "Uthja",
                variant = AppTextVariant.TITLE,
                tone = AppTextTone.DEFAULT,
                textAlign = TextAlign.Center
            )
        }
    }
}
