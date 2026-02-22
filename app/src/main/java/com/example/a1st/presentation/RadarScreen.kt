package com.example.a1st.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

@Composable
fun RadarScreen(viewModel: HiddenCameraViewModel, emfValue: Float, targetIp: String? = null) {
    val isIrVisionEnabled by viewModel.isIrVisionEnabled.collectAsState()
    val processedBitmap by viewModel.processedBitmap.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val isAlert = emfValue > 100f || targetIp != null
    val themeColor = if (isAlert) Color(0xFFFF0055) else Color(0xFF00FFFF)
    val animatedThemeColor by animateColorAsState(targetValue = themeColor, animationSpec = tween(500), label = "")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050B18), Color(0xFF1A0B2E))
                )
            )
    ) {
        // Camera Preview & Targeting HUD
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            viewModel.startCamera(lifecycleOwner, surfaceProvider)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isIrVisionEnabled) {
                processedBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.6f
                    )
                }
            }

            // Targeting HUD
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val scanLinePos by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val hudColor = if (targetIp != null) Color(0xFFFF003C) else Color(0xFF00FFFF)
                val strokeWidth = 2.dp.toPx()
                val cornerSize = 40.dp.toPx()

                // 4 L-shaped Corners
                // Top-Left
                drawLine(hudColor, Offset(50f, 50f), Offset(50f + cornerSize, 50f), strokeWidth)
                drawLine(hudColor, Offset(50f, 50f), Offset(50f, 50f + cornerSize), strokeWidth)
                // Top-Right
                drawLine(hudColor, Offset(size.width - 50f, 50f), Offset(size.width - 50f - cornerSize, 50f), strokeWidth)
                drawLine(hudColor, Offset(size.width - 50f, 50f), Offset(size.width - 50f, 50f + cornerSize), strokeWidth)
                // Bottom-Left
                drawLine(hudColor, Offset(50f, size.height - 50f), Offset(50f + cornerSize, size.height - 50f), strokeWidth)
                drawLine(hudColor, Offset(50f, size.height - 50f), Offset(50f, size.height - 50f - cornerSize), strokeWidth)
                // Bottom-Right
                drawLine(hudColor, Offset(size.width - 50f, size.height - 50f), Offset(size.width - 50f - cornerSize, size.height - 50f), strokeWidth)
                drawLine(hudColor, Offset(size.width - 50f, size.height - 50f), Offset(size.width - 50f, size.height - 50f - cornerSize), strokeWidth)

                // Scanning Line
                drawLine(
                    hudColor.copy(alpha = 0.4f),
                    Offset(0f, size.height * scanLinePos),
                    Offset(size.width, size.height * scanLinePos),
                    4.dp.toPx()
                )

                // Crosshair
                drawCircle(hudColor, radius = 4.dp.toPx(), center = center)
                drawCircle(hudColor, radius = 20.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            }
        }

        // Target Tracking Banner
        if (targetIp != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp)
                    .background(Color(0xFFFF003C).copy(alpha = 0.8f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GppBad, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRACKING TARGET: $targetIp",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Main Radar UI (Centered Overlay)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "CYBER SENTINEL",
                color = animatedThemeColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(2.dp)
                    .background(animatedThemeColor.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(60.dp))

            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ), label = ""
                )

                Canvas(modifier = Modifier.size(320.dp)) {
                    val center = this.center
                    val radius = size.minDimension / 2
                    drawCircle(animatedThemeColor.copy(alpha = 0.1f), radius = radius, style = Stroke(1.dp.toPx()))
                    rotate(rotation, center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color.Transparent, animatedThemeColor.copy(alpha = 0.5f), Color.Transparent),
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 90f,
                            useCenter = true,
                            size = size
                        )
                    }
                    drawCircle(color = animatedThemeColor, radius = radius, style = Stroke(width = 4.dp.toPx()))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", emfValue),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedThemeColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "µT",
                        fontSize = 20.sp,
                        color = animatedThemeColor.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Mode Selector (Bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .fillMaxWidth(0.9f)
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val emfSelected = !isIrVisionEnabled
                val irSelected = isIrVisionEnabled
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { if (isIrVisionEnabled) viewModel.toggleIrVision() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("EMF SCAN", color = if (emfSelected) Color(0xFF00FFFF) else Color.Gray, fontWeight = FontWeight.Bold)
                    if (emfSelected) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.6f).height(2.dp).background(Color(0xFF00FFFF)))
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { if (!isIrVisionEnabled) viewModel.toggleIrVision() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("IR VISION", color = if (irSelected) Color(0xFFFF00FF) else Color.Gray, fontWeight = FontWeight.Bold)
                    if (irSelected) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.6f).height(2.dp).background(Color(0xFFFF00FF)))
                    }
                }
            }
        }
    }
}
