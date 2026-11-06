package com.example.sleep

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Sleep Guidance Screen matching the exact user-attached screen:
 * - Deep starry midnight sky with constellations and gentle nebula
 * - Cinematic bedside scene showing a soft textured blue pillow on the bed,
 *   a modern smartphone resting on the wooden nightstand charging via a white cable,
 *   warm bedside lamp and potted houseplant
 * - Exact headline: "Place your phone\nnext to your pillow"
 * - Subtitle: "Keep it charging overnight"
 * - Glowing frosted-blue/white pill button: "I got it"
 */
@Composable
fun SleepGuidanceScreen(
    onGotIt: () -> Unit,
    onNeverShowAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040711))
    ) {
        // 1. FULL-SCREEN BEDROOM & STARRY SKY ILLUSTRATION
        Image(
            painter = painterResource(id = R.drawable.bg_sleep_guidance_full),
            contentDescription = "Place your phone next to your pillow scene",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. AMBIENT ATMOSPHERIC GLOWS (Matching screenshot light atmosphere)
        // Upper celestial starlight glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .size(360.dp)
                .blur(50.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Warm bedside lamp glow on the right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp, y = 80.dp)
                .size(240.dp)
                .blur(45.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE8A3).copy(alpha = 0.22f),
                            Color(0xFF38BDF8).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 3. FOREGROUND CONTENT & CONTROLS
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Place your phone next to your pillow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "Place your phone\nnext to your pillow",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Keep it charging overnight",
                    color = Color(0xFFC7D2FE),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.1.sp
                )
            }

            // Center: Clear Pillow & Phone charging graphic
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Soft blue halo glow
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .blur(36.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.28f),
                                    Color(0xFF0284C7).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Distinct, high-contrast Pillow & Phone illustration
                Image(
                    painter = painterResource(id = R.drawable.img_sleep_guidance_pillow),
                    contentDescription = "Pillow on bed with charging smartphone next to it",
                    modifier = Modifier
                        .size(300.dp)
                        .padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Bottom Actions: Glowing Pill Button ("I got it")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                // Soft outer neon glow behind the button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(64.dp)
                            .blur(18.dp)
                            .background(
                                color = Color(0xFF60A5FA).copy(alpha = 0.65f),
                                shape = RoundedCornerShape(32.dp)
                            )
                    )

                    // Glowing Pill Button matching screenshot
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFDBEAFE),
                                        Color(0xFFBFDBFE)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFF93C5FD)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .clickable(onClick = onGotIt)
                            .testTag("guidance_got_it_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "I got it",
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}
