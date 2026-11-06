package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LuneColors
import com.example.ui.theme.LocalIsDarkMode

enum class LuneTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ALARMS("Alarm", Icons.Filled.Alarm, Icons.Outlined.Alarm, "tab_alarms"),
    SLEEP("Sleep", Icons.Filled.Bedtime, Icons.Outlined.Bedtime, "tab_sleep"),
    TASKS("Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist, "tab_tasks"),
    SETTINGS("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_settings")
}

@Composable
fun LuneBottomNavigation(
    currentTab: LuneTab,
    onTabSelected: (LuneTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = LuneTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
    val haptics = com.example.alarm.rememberAppleHaptics()
    val isDark = LocalIsDarkMode.current

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navActiveIndex"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle outer glow shadow pill
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(76.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(38.dp),
                    spotColor = if (isDark) Color(0x60A78BFA) else Color(0x388E69AA),
                    ambientColor = if (isDark) Color(0x401D1726) else Color(0x288E69AA)
                )
        )

        // Frosted Glass Pill Bar Container
        Surface(
            shape = RoundedCornerShape(38.dp),
            color = if (isDark) Color(0xF21D1726) else Color(0xF8FFFFFF),
            border = BorderStroke(1.dp, if (isDark) Color(0x33A78BFA) else Color(0x1F000000)),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val tabCount = tabs.size
                val itemWidth = totalWidth / tabCount

                // Decorative Glass Sparkles
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(38.dp))
                ) {
                    // Top highlight line
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .fillMaxWidth(0.85f)
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        if (isDark) Color(0x40A78BFA) else Color(0xB3FFFFFF),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Sparkle dots
                    Box(
                        modifier = Modifier
                            .offset(x = 54.dp, y = 14.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x80A78BFA) else Color(0xE6FFFFFF))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-40).dp, y = 12.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x80FDE047) else Color(0xE6FFDDA7))
                    )
                }

                // Liquid Glow Pill Indicator sliding smoothly behind active tab
                Box(
                    modifier = Modifier
                        .offset(x = itemWidth * animatedIndex)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                if (isDark) {
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xE6332448),
                                            Color(0xD04C336A),
                                            Color(0xB85C3886)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xE6FFFFFF),
                                            Color(0xD0E8D7FF),
                                            Color(0xB8C9A8F4)
                                        )
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0x52A78BFA) else Color(0x80FFFFFF),
                                RoundedCornerShape(32.dp)
                            )
                    )
                }

                // Tab items
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == currentTab
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                            label = "tabScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                    if (tab != currentTab) {
                                        haptics.pulseAppleSelection()
                                    }
                                    onTabSelected(tab)
                                }
                                )
                                .testTag(tab.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.scale(scale)
                            ) {
                                // 31dp circular container for icon
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(31.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0x38A78BFA) else Color(0x52FFFFFF)
                                            } else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) {
                                                if (isDark) Color(0x80A78BFA) else Color(0x99FFFFFF)
                                            } else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) {
                                            if (isDark) Color(0xFFD4B3FF) else Color(0xFF7A4CB0)
                                        } else {
                                            if (isDark) Color(0xA6C8B8D4) else Color(0x8C4E3D32)
                                        },
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFFE2CCFF) else Color(0xFF6F46A3)
                                    } else {
                                        if (isDark) Color(0x99B8A8C0) else Color(0x995D4B40)
                                    },
                                    letterSpacing = 0.1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
