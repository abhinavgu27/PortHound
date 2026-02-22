package com.example.a1st.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hiddencameradetector.R
import kotlinx.coroutines.launch
import java.util.Locale

private val DeepSpaceBlack = Color(0xFF0A0A0F)
private val NeonCyan = Color(0xFF00F3FF)

@Composable
fun DashboardScreen(
    emfValue: Float,
    onCalibrate: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val isAlert = emfValue > 60f
    val statusColor by animateColorAsState(
        targetValue = if (isAlert) Color(0xFFFF0055) else NeonCyan,
        animationSpec = tween(500), label = ""
    )
    val scope = rememberCoroutineScope()
    val calibratedMsg = stringResource(R.string.msg_calibrated)
    val isTablet = isTabletDevice()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050B18), Color(0xFF1A0B2E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = if (isAlert) stringResource(R.string.status_threat) else stringResource(R.string.status_secure),
                color = statusColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.current_reading, emfValue),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.welcome_back),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    onCalibrate()
                    scope.launch {
                        snackbarHostState.showSnackbar(calibratedMsg)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth(if (isTablet) 0.4f else 0.85f)
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration, 
                    contentDescription = null, 
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.btn_calibrate), 
                    color = NeonCyan, 
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun isTabletDevice(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.screenWidthDp > 600
}
