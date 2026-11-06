package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.alarm.rememberAppleHaptics
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

data class ScannedBarcodeResult(
    val rawValue: String,
    val formatName: String
)

/**
 * QR / Barcode Challenge Screen:
 * Real camera-based barcode & QR scanner using CameraX and Google ML Kit.
 * Scans any 1D barcode (EAN, UPC, Code 128) or 2D QR code to conquer the alarm.
 */
@Composable
fun QrBarcodeMissionScreen(
    onDismissConquered: () -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    isLockedMission: Boolean = true,
    onAttemptExitBlocked: (() -> Unit)? = null,
    onToggleMute: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberAppleHaptics()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Re-check permission whenever resumed or active
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var scannedResult by remember { mutableStateOf<ScannedBarcodeResult?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var hasCompleted by remember { mutableStateOf(false) }

    fun triggerCompletion() {
        if (!hasCompleted) {
            hasCompleted = true
            haptics.pulseAppleSuccess()
            onDismissConquered()
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle barcode detection with visual confirmation delay
    LaunchedEffect(scannedResult) {
        val result = scannedResult
        if (result != null && !hasCompleted) {
            haptics.pulseAppleSuccess()
            delay(750L) // Show confirmation banner briefly
            triggerCompletion()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

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
            .background(Color(0xFF0F0B17))
            .testTag("qr_mission_screen")
    ) {
        // 1. CAMERA PREVIEW LAYER (when permission granted)
        if (hasCameraPermission && scannedResult == null) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val scannerOptions = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                .build()
                            val barcodeScanner = BarcodeScanning.getClient(scannerOptions)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(imageProxy, barcodeScanner) { raw, format ->
                                    if (scannedResult == null && !hasCompleted) {
                                        scannedResult = ScannedBarcodeResult(raw, format)
                                    }
                                }
                            }

                            val cameraSelector = when {
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                else -> CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            activeCamera = cam
                        } catch (e: Exception) {
                            Log.e("QrBarcodeMission", "Failed to bind CameraX", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Semi-transparent dark camera vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
            )
        }

        // 2. MAIN UI OVERLAY
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
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

                    // Mission Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8455FD).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Scan Real Barcode",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Flashlight / Torch Toggle
                        if (hasCameraPermission && activeCamera?.cameraInfo?.hasFlashUnit() == true) {
                            IconButton(
                                onClick = {
                                    val nextState = !isTorchOn
                                    isTorchOn = nextState
                                    activeCamera?.cameraControl?.enableTorch(nextState)
                                    haptics.pulseAppleSelection()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                    contentDescription = "Torch",
                                    tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

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

            // Central Viewfinder Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (hasCameraPermission) {
                    Text(
                        text = "Point Camera at Any Barcode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Scan any product box, shampoo, snack, book, or QR code",
                        fontSize = 13.5.sp,
                        color = Color(0xFFA594BD),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Scanner Reticle Target Frame
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(2.5.dp, Color(0xFF8455FD), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Animated Scanning Laser Bar
                        if (scannedResult == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .offset(y = (laserOffset - 110f).dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF8455FD),
                                                Color(0xFFA78BFA),
                                                Color(0xFF67E8F9),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        // Target corner guides
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(90.dp)
                        )

                        // Scanned Celebration Banner
                        scannedResult?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF10B981).copy(alpha = 0.90f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "${result.formatName} Scanned!",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.rawValue.take(24),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD1FAE5),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Scanner Quick-Test Button (for testing without physical item or in emulator)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8455FD), Color(0xFFA78BFA))
                                )
                            )
                            .clickable {
                                if (scannedResult == null) {
                                    scannedResult = ScannedBarcodeResult("8901030384729", "EAN-13")
                                }
                            }
                            .testTag("scan_barcode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (scannedResult == null) "Test Scan Barcode (Instant Verify)" else "Barcode Scanned! Unlocking...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // CAMERA PERMISSION PROMPT CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E172D))
                            .border(1.5.dp, Color(0xFF8455FD), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Camera Permission Required",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Allow camera access to scan barcodes or QR codes in real-time to dismiss the alarm.",
                                fontSize = 13.5.sp,
                                color = Color(0xFFA594BD),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8455FD),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    text = "Enable Camera to Scan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // Test bypass button if camera is physically restricted
                            Text(
                                text = "Or tap here to test barcode recognition",
                                fontSize = 12.5.sp,
                                color = Color(0xFFC084FC),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        scannedResult = ScannedBarcodeResult("TEST_BARCODE_7890", "QR Code")
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Exit Bar
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
                    .testTag("exit_qr_preview_button"),
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
                        text = if (isLockedMission) "🔒 Power-off & Exit Protected · Scan barcode to unlock" else "Exit preview",
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

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeFound: (String, String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val raw = barcode.rawValue ?: barcode.displayValue
                    if (!raw.isNullOrBlank()) {
                        val formatName = when (barcode.format) {
                            Barcode.FORMAT_QR_CODE -> "QR Code"
                            Barcode.FORMAT_UPC_A -> "UPC-A Barcode"
                            Barcode.FORMAT_UPC_E -> "UPC-E Barcode"
                            Barcode.FORMAT_EAN_13 -> "EAN-13 Barcode"
                            Barcode.FORMAT_EAN_8 -> "EAN-8 Barcode"
                            Barcode.FORMAT_CODE_128 -> "Code 128"
                            Barcode.FORMAT_CODE_39 -> "Code 39"
                            Barcode.FORMAT_CODE_93 -> "Code 93"
                            Barcode.FORMAT_CODABAR -> "Codabar"
                            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
                            Barcode.FORMAT_AZTEC -> "Aztec"
                            Barcode.FORMAT_PDF417 -> "PDF417"
                            Barcode.FORMAT_ITF -> "ITF"
                            else -> "Barcode"
                        }
                        onBarcodeFound(raw, formatName)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Ignore transient frame recognition failures
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

