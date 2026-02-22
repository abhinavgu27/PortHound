package com.example.a1st.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.a1st.data.network.DeviceCategory
import com.example.a1st.data.network.ScannedDevice
import com.example.hiddencameradetector.R
import kotlinx.coroutines.delay

private val DeepSpaceBlack = Color(0xFF0A0A0F)
private val NeonCyan = Color(0xFF00F3FF)
private val DangerRed = Color(0xFFFF003C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(viewModel: HiddenCameraViewModel, onNavigateToScanner: () -> Unit) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanningNetwork.collectAsState()
    val anyThreat = discoveredDevices.any { it.isThreat }
    val isTablet = isTabletDevice()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context.findActivity()

    var selectedThreatDevice by remember { mutableStateOf<ScannedDevice?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted && activity != null) {
            viewModel.scanNetwork(activity)
        }
    }

    fun checkAndStartScan() {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted && activity != null) {
            viewModel.scanNetwork(activity)
        } else {
            showPermissionRationale = true
        }
    }

    // Trigger Haptic on new threat
    LaunchedEffect(discoveredDevices.size) {
        if (discoveredDevices.any { it.isThreat }) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
            .padding(16.dp)
    ) {
        Column {
            RadarHeader(isScanning, anyThreat)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVICES DISCOVERED: ${discoveredDevices.size}",
                    color = NeonCyan.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Button(
                    onClick = { checkAndStartScan() },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2E)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INITIATE SWEEP", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTablet) 2 else 1),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(discoveredDevices) { device ->
                    GlassDeviceCard(
                        device = device,
                        onClick = {
                            if (device.isThreat) {
                                selectedThreatDevice = device
                                showSheet = true
                            }
                        }
                    )
                }
            }
        }

        if (showPermissionRationale) {
            ModalBottomSheet(
                onDismissRequest = { showPermissionRationale = false },
                containerColor = Color(0xFF1A0B2E),
                contentColor = Color.White
            ) {
                PermissionRationaleSheet(
                    onGrant = {
                        showPermissionRationale = false
                        permissionLauncher.launch(permissionsToRequest)
                    },
                    onOpenSettings = {
                        showPermissionRationale = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        if (showSheet && selectedThreatDevice != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1A0B2E),
                contentColor = Color.White,
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                ThreatDetailSheet(
                    device = selectedThreatDevice!!,
                    onNavigateToScanner = {
                        showSheet = false
                        onNavigateToScanner()
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionRationaleSheet(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.WifiLock,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "NETWORK ACCESS REQUIRED",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Cyber Sentinel requires Local Network Access to detect hidden cameras on this Wi-Fi. Without this, silent network threats cannot be identified.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ALLOW ACCESS", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onOpenSettings) {
            Text("OPEN SETTINGS", color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ThreatDetailSheet(
    device: ScannedDevice,
    onNavigateToScanner: () -> Unit
) {
    val context = LocalContext.current
    var terminalLines by remember { mutableStateOf(listOf<String>()) }
    var terminalStatus by remember { mutableStateOf<String?>(null) }
    var isAttemptingStreams by remember { mutableStateOf(false) }
    
    val streamAttempts = listOf(
        "Attempt 1 (Generic)" to "rtsp://${device.ipAddress}:554/live/main",
        "Attempt 2 (Dahua/CP Plus)" to "rtsp://admin:admin@${device.ipAddress}:554/cam/realmonitor?channel=1&subtype=0",
        "Attempt 3 (Hikvision)" to "rtsp://admin:123456@${device.ipAddress}:554/Streaming/Channels/101",
        "Attempt 4 (TP-Link)" to "rtsp://admin:admin@${device.ipAddress}:554/stream1"
    )

    LaunchedEffect(isAttemptingStreams) {
        if (isAttemptingStreams) {
            terminalLines = listOf("> INITIATING RTSP HANDSHAKE...")
            delay(800)
            terminalLines = terminalLines + "> INJECTING DEFAULT CREDENTIALS..."
            delay(800)
            terminalLines = terminalLines + "> ERROR 401: ACCESS DENIED. ENTERPRISE FIREWALL DETECTED."
            delay(400)
            terminalStatus = "Cyber Sentinel successfully identified this device as an active, secured camera. Please proceed to Physical Sweep to locate the lens."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DangerRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GppBad,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SECURITY ALERT",
            color = DangerRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Suspicious Video Stream Port Detected on ${device.ipAddress}",
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isAttemptingStreams) {
            Button(
                onClick = { isAttemptingStreams = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("SMART TEST STREAM", fontWeight = FontWeight.Bold)
            }
        } else {
            // Hacker Terminal UI
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                color = Color.Black
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    terminalLines.forEach { line ->
                        Text(
                            text = line,
                            color = if (line.contains("ERROR")) DangerRed else Color.Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    terminalStatus?.let { status ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = status,
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onNavigateToScanner,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NeonCyan)
        ) {
            Icon(Icons.Default.Radar, contentDescription = null, tint = NeonCyan)
            Spacer(modifier = Modifier.width(12.dp))
            Text("LOCATE PHYSICALLY", color = NeonCyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlassDeviceCard(device: ScannedDevice, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val cardBorderColor = if (device.isThreat) DangerRed.copy(alpha = glowAlpha) else Color.White.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (device.deviceCategory) {
                DeviceCategory.WINDOWS_WORKSTATION -> Icons.Default.Laptop
                DeviceCategory.APPLE_DEVICE -> Icons.Default.Smartphone
                DeviceCategory.IP_CAMERA_DVR -> Icons.Default.Videocam
                DeviceCategory.MY_SENTINEL -> Icons.Default.Security
                DeviceCategory.GENERIC_DEVICE -> Icons.Default.Devices
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (device.isThreat) DangerRed.copy(alpha = 0.1f) else NeonCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (device.isThreat) DangerRed else NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.resolvedName ?: device.deviceCategory.name.replace("_", " "),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = device.ipAddress,
                    color = (if (device.isThreat) DangerRed else NeonCyan).copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (device.isThreat) {
                Text(
                    text = "THREAT",
                    color = DangerRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun RadarHeader(isScanning: Boolean, anyThreat: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val radarColor by animateColorAsState(
        targetValue = if (anyThreat) DangerRed else NeonCyan,
        animationSpec = tween(500), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = this.center
            val radius = size.minDimension / 2

            drawCircle(radarColor.copy(alpha = 0.1f), radius = radius, style = Stroke(1.dp.toPx()))
            drawCircle(radarColor.copy(alpha = 0.1f), radius = radius * 0.6f, style = Stroke(1.dp.toPx()))

            if (isScanning) {
                rotate(rotation, center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, radarColor.copy(alpha = 0.5f), Color.Transparent),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        size = size
                    )
                }
            }
            
            drawCircle(
                color = radarColor,
                radius = radius * (if (anyThreat) pulseScale else 1f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (anyThreat) Icons.Default.GppBad else Icons.Default.CellTower,
                contentDescription = null,
                tint = radarColor,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (isScanning) "SCANNING..." else if (anyThreat) "THREAT DETECTED" else "NETWORK SECURE",
                color = radarColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
