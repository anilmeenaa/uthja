package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.rememberAppleHaptics
import kotlin.random.Random

data class ColorTileItem(
    val id: Int,
    val color: Color,
    val colorName: String,
    val isTarget: Boolean,
    var isFound: Boolean = false
)

/**
 * Color Tiles Mission UI Screen:
 * "Find Color Tiles: Find and tap the right colors"
 * - High-contrast aesthetic with glowing visual elements
 * - Dynamic color goal banner ("Tap all Emerald tiles")
 * - 3x3 grid of responsive tiles with animations and haptics
 * - Bottom exit bar matching the app's mission preview template
 */
@Composable
fun ColorTilesMissionScreen(
    onDismissConquered: () -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    isLockedMission: Boolean = true,
    onAttemptExitBlocked: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null
) {
    val haptics = rememberAppleHaptics()

    val colorPalette = listOf(
        Pair(Color(0xFF10B981), "Emerald"),
        Pair(Color(0xFF8B64DC), "Purple"),
        Pair(Color(0xFF0EA5E9), "Ocean Blue"),
        Pair(Color(0xFFF59E0B), "Amber"),
        Pair(Color(0xFFF43F5E), "Rose"),
        Pair(Color(0xFF14B8A6), "Teal")
    )

    // Choose target color
    val targetPair = remember { colorPalette.random() }
    val targetColor = targetPair.first
    val targetName = targetPair.second

    // Generate 9 tiles (3 of which are the target color)
    val tiles = remember {
        val list = mutableListOf<ColorTileItem>()
        // 3 targets
        repeat(3) { id ->
            list.add(ColorTileItem(id = id, color = targetColor, colorName = targetName, isTarget = true))
        }
        // 6 distractors
        val otherColors = colorPalette.filter { it.first != targetColor }
        repeat(6) { id ->
            val other = otherColors.random()
            list.add(ColorTileItem(id = 3 + id, color = other.first, colorName = other.second, isTarget = false))
        }
        list.shuffle()
        mutableStateListOf(*list.toTypedArray())
    }

    val remainingTargets = tiles.count { it.isTarget && !it.isFound }

    var hasCompleted by remember { mutableStateOf(false) }

    fun triggerCompletion() {
        if (!hasCompleted) {
            hasCompleted = true
            haptics.pulseAppleSuccess()
            onDismissConquered()
        }
    }

    BackHandler {
        if (isLockedMission) {
            haptics.pulseAppleButtonClick()
            onAttemptExitBlocked?.invoke()
        } else {
            triggerCompletion()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B18))
            .testTag("color_tiles_mission_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.9f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            if (isLockedMission) {
                                onAttemptExitBlocked?.invoke()
                            } else {
                                triggerCompletion()
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isLockedMission) Icons.Default.Lock else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = if (isLockedMission) "Mission locked" else "Back",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLockedMission) 22.dp else 28.dp)
                        )
                    }

                    // Target Count Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0D9488).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$remainingTargets tiles left",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "🎧 Gentle 30%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBAE6FD)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.pulseAppleSelection()
                                onToggleMute?.invoke()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. INSTRUCTION & GOAL BANNER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Find Color Tiles",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tap all matching color tiles to turn off alarm",
                    fontSize = 13.5.sp,
                    color = Color(0xFFA594BD)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Target Color Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1530))
                        .border(1.5.dp, Color(0xFF33234F), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(targetColor)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = "Target: $targetName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 3. 3x3 TILES GRID
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (col in 0 until 3) {
                            val index = row * 3 + col
                            val tile = tiles[index]

                            val scale by animateFloatAsState(
                                targetValue = if (tile.isFound) 0.92f else 1f,
                                animationSpec = spring(),
                                label = "tileScale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (tile.isFound) tile.color.copy(alpha = 0.35f) else tile.color
                                    )
                                    .border(
                                        width = if (tile.isFound) 2.5.dp else 1.dp,
                                        color = if (tile.isFound) Color.White else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        if (!tile.isFound) {
                                            if (tile.isTarget) {
                                                haptics.pulseAppleSelection()
                                                tiles[index] = tile.copy(isFound = true)
                                                if (tiles.count { it.isTarget && !it.isFound } == 0) {
                                                    // Conquered!
                                                    triggerCompletion()
                                                }
                                            } else {
                                                haptics.pulseShake()
                                            }
                                        }
                                    }
                                    .testTag("color_tile_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tile.isFound) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Found",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. BOTTOM EXIT PREVIEW BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isLockedMission) Color(0xFF1E232A) else Color(0xFFCFD5DE))
                    .navigationBarsPadding()
                    .height(54.dp)
                    .clickable {
                        haptics.pulseAppleButtonClick()
                        if (isLockedMission) {
                            onAttemptExitBlocked?.invoke()
                        } else {
                            triggerCompletion()
                        }
                    }
                    .testTag("exit_colortiles_preview_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLockedMission) Icons.Default.Lock else Icons.Default.Close,
                        contentDescription = if (isLockedMission) "Mission locked" else "Exit preview",
                        tint = if (isLockedMission) Color(0xFFFBBF24) else Color(0xFF1E293B),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (isLockedMission) "🔒 Power-off & Exit Protected · Find all tiles" else "Exit preview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLockedMission) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
