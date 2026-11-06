package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.alarm.rememberAppleHaptics
import com.example.data.model.WallpaperCatalog
import com.example.data.model.WallpaperItem
import java.io.File
import java.io.FileOutputStream

/**
 * Alarm Wallpaper Selection Page matching the exact UI in the user attachment:
 * - Top back arrow with "Alarm wallpaper" centered title
 * - Filter chips: "📸 My Photos", "🌄 Motivation", "🪐 Space"
 * - Section 1 "📸 My Photos":
 *   - "Choose from Album" dashed card with '+' icon (Android Photo Picker integration)
 *   - Promo card: "Create your alarm with your favorite photos or videos"
 * - Section 3 "🌄 Motivation":
 *   - Horizontal cards (Wake up you..., Life gets better, Wake up & smile, Ambition)
 * - Floating action button for quick gallery photo picking
 */
@Composable
fun AlarmWallpaperPickerScreen(
    currentWallpaperId: String,
    onWallpaperSelected: (WallpaperItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberAppleHaptics()
    var selectedId by remember(currentWallpaperId) { mutableStateOf(currentWallpaperId) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Initialize customPickedUri from currently selected wallpaper if it's already a custom photo
    var customPickedUri by remember(currentWallpaperId) {
        val initialUri = if (currentWallpaperId.startsWith("custom_uri:")) {
            Uri.parse(currentWallpaperId.removePrefix("custom_uri:"))
        } else if (currentWallpaperId.startsWith("content://") || currentWallpaperId.startsWith("file://")) {
            Uri.parse(currentWallpaperId)
        } else {
            null
        }
        mutableStateOf(initialUri)
    }

    // Android Zero-Permission Visual Media Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // 1. Attempt to take persistable URI permission if applicable
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Not all providers support persistable flags; copy below guarantees persistence
            }

            // 2. Persist to internal app storage so alarm ringing service and receiver always have 100% access
            var finalUriString = uri.toString()
            try {
                val wallpapersDir = File(context.filesDir, "alarm_wallpapers").apply { mkdirs() }
                val targetFile = File(wallpapersDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    finalUriString = Uri.fromFile(targetFile).toString()
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmWallpaperPicker", "Failed copying wallpaper image to internal storage", e)
            }

            val savedUri = Uri.parse(finalUriString)
            customPickedUri = savedUri

            val customItem = WallpaperItem(
                id = "custom_uri:$finalUriString",
                title = "My Album Photo",
                soundLabel = "My Photo",
                category = "My Photos",
                customUri = finalUriString,
                gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
                accentColor = Color(0xFF38BDF8),
                iconEmoji = "📸"
            )
            selectedId = customItem.id
            haptics.pulseSuccess()
            onWallpaperSelected(customItem)
        }
    }

    val handleSaveAndDismiss = {
        val chosenItem = WallpaperCatalog.findById(selectedId)
        onWallpaperSelected(chosenItem)
    }

    BackHandler {
        handleSaveAndDismiss()
    }

    val onSelectCard: (WallpaperItem) -> Unit = { item ->
        haptics.pulseAppleSelection()
        if (selectedId == item.id) {
            onWallpaperSelected(item)
        } else {
            selectedId = item.id
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // 1. TOP BAR (< back arrow + "Alarm wallpaper" title + "Save" button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        haptics.pulseAppleButtonClick()
                        handleSaveAndDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp)
                        .testTag("wallpaper_picker_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Alarm wallpaper",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.align(Alignment.Center)
                )

                androidx.compose.material3.TextButton(
                    onClick = {
                        haptics.pulseAppleSuccess()
                        handleSaveAndDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("wallpaper_picker_save_top_button")
                ) {
                    Text(
                        text = "Save",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. HORIZONTAL FILTER CHIPS ROW
            val chipCategories = listOf(
                Pair("Nature", "🏞️ Scenic Nature"),
                Pair("My Photos", "📸 My Photos"),
                Pair("Motivation", "🌄 Motivation"),
                Pair("Space", "🪐 Space")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chipCategories.forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategoryFilter == catKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1E232A) else Color(0xFFF1F5F9))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF1E232A) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                haptics.pulseAppleSelection()
                                selectedCategoryFilter = if (isSelected) "All" else catKey
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = catLabel,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2.5 SECTION: 🏞️ Scenic Nature (Default)
            if (selectedCategoryFilter == "All" || selectedCategoryFilter == "Nature") {
                SectionHeader(title = "🏞️ Scenic Nature (Default)")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WallpaperCatalog.NATURE_ITEMS.forEach { item ->
                        WallpaperCard(
                            item = item,
                            isSelected = selectedId == item.id,
                            onClick = { onSelectCard(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. SECTION: 📸 My Photos
            if (selectedCategoryFilter == "All" || selectedCategoryFilter == "My Photos") {
                SectionHeader(title = "📸 My Photos")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Card 1: Choose from Album with dashed border and '+'
                    DashedAlbumCard(
                        customUri = customPickedUri,
                        isSelected = selectedId.startsWith("custom_uri:"),
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            if (customPickedUri != null && !selectedId.startsWith("custom_uri:")) {
                                val uriStr = customPickedUri.toString()
                                val customItem = WallpaperItem(
                                    id = "custom_uri:$uriStr",
                                    title = "My Album Photo",
                                    soundLabel = "My Photo",
                                    category = "My Photos",
                                    customUri = uriStr,
                                    gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
                                    accentColor = Color(0xFF38BDF8),
                                    iconEmoji = "📸"
                                )
                                selectedId = customItem.id
                                onWallpaperSelected(customItem)
                            } else {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    )

                    // Card 2: Promo banner card matching Screenshot
                    PromoAlbumBannerCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptics.pulseAppleButtonClick()
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }


            // 5. SECTION: 🌄 Motivation
            if (selectedCategoryFilter == "All" || selectedCategoryFilter == "Motivation") {
                SectionHeader(title = "🌄 Motivation")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WallpaperCatalog.MOTIVATION_ITEMS.forEach { item ->
                        WallpaperCard(
                            item = item,
                            isSelected = selectedId == item.id,
                            onClick = { onSelectCard(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6. SECTION: 🪐 Space
            if (selectedCategoryFilter == "All" || selectedCategoryFilter == "Space") {
                SectionHeader(title = "🪐 Space & Aesthetic")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WallpaperCatalog.SPACE_ITEMS.forEach { item ->
                        WallpaperCard(
                            item = item,
                            isSelected = selectedId == item.id,
                            onClick = { onSelectCard(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 7. FLOATING ACTION BUTTON (Photo gallery icon with '+' badge on bottom right)
        FloatingActionButton(
            onClick = {
                haptics.pulseAppleButtonClick()
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            containerColor = Color(0xFF1E232A),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 84.dp)
                .size(52.dp)
                .testTag("floating_gallery_picker_button")
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Pick custom wallpaper from album",
                modifier = Modifier.size(24.dp)
            )
        }

        // 8. FIXED BOTTOM APPLY & SAVE BAR
        val activeItem = remember(selectedId) {
            WallpaperCatalog.findById(selectedId)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.94f),
                            Color.White
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini preview thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(activeItem.gradientColors))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeItem.customUri != null) {
                        AsyncImage(
                            model = activeItem.customUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (activeItem.drawableRes != null) {
                        Image(
                            painter = painterResource(id = activeItem.drawableRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = activeItem.iconEmoji, fontSize = 15.sp)
                    }
                }

                androidx.compose.material3.Button(
                    onClick = {
                        haptics.pulseAppleSuccess()
                        onWallpaperSelected(activeItem)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("apply_wallpaper_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E232A),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save Wallpaper • ${activeItem.title}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E232A),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/**
 * Dashed Album Card matching Screenshot:
 * - Rounded rect with dashed border
 * - Plus icon in dark circular badge
 * - "Choose from Album" text
 */
@Composable
private fun DashedAlbumCard(
    customUri: Uri?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1)

    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFAFAFA))
            .drawBehind {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = stroke
                )
            }
            .clickable(onClick = onClick)
            .testTag("choose_from_album_card"),
        contentAlignment = Alignment.Center
    ) {
        if (customUri != null) {
            AsyncImage(
                model = customUri,
                contentDescription = "Custom Album Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E232A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Choose from\nAlbum",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Promo card with soft purple/lavender container:
 * "Create your alarm with your favorite photos or videos" with cute dog + portrait art
 */
@Composable
private fun PromoAlbumBannerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF3E8FF),
                        Color(0xFFEDE9FE),
                        Color(0xFFE0E7FF)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Create your alarm with your favorite photos or videos",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E232A),
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                // Cute Puppy Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🐶", fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Guy with silver hair Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF818CF8))
                        .border(1.5.dp, Color.White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🧑‍🦳", fontSize = 28.sp)
                }
            }
        }
    }
}

/**
 * Individual vertical wallpaper card (9:16 portrait):
 * - Displays wallpaper visual/gradient/drawable
 * - Centered circular checkmark if active/selected
 * - Music note subtitle at the bottom
 */
@Composable
private fun WallpaperCard(
    item: WallpaperItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptics = rememberAppleHaptics()

    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(colors = item.gradientColors)
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable {
                haptics.pulseAppleButtonClick()
                onClick()
            }
            .testTag("wallpaper_item_${item.id}")
    ) {
        // Background Artwork / Resource
        if (item.drawableRes != null) {
            Image(
                painter = painterResource(id = item.drawableRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // If card has quote text (like gradient cards without image)
        if (item.quote != null && item.drawableRes == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.quote,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }

        // Center Checkmark if selected (Matching Screenshot Card 1!)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Scrim with Music Note and Sound Label (Matching Screenshot: ♪ Meow Alarm, etc.)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = item.soundLabel,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
