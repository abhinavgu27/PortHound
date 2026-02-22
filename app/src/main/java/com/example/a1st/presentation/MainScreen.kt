package com.example.a1st.presentation

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hiddencameradetector.R

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Welcome : Screen("welcome", R.string.welcome_title, Icons.Default.Dashboard)
    object Dashboard : Screen("dashboard", R.string.dashboard_title, Icons.Default.Dashboard)
    object Scanner : Screen("scanner", R.string.scanner_title, Icons.Default.Radar)
    object Network : Screen("network", R.string.network_title, Icons.Default.CellTower)
    object Logs : Screen("logs", R.string.logs_title, Icons.Default.History)
}

@Composable
fun MainScreen(viewModel: HiddenCameraViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as Activity
    val sharedPrefs = remember { context.getSharedPreferences("cyber_sentinel_prefs", Context.MODE_PRIVATE) }
    
    var showPrivacyDialog by remember { 
        mutableStateOf(sharedPrefs.getBoolean("first_run_privacy", true)) 
    }
    
    val isFirstRun = remember { sharedPrefs.getBoolean("first_run", true) }
    val isPremium by viewModel.isPremium.collectAsState()
    
    val emfValue by viewModel.calibratedEmfReading.collectAsState()
    val threatLogs by viewModel.threatLogs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val items = listOf(
        Screen.Dashboard,
        Screen.Scanner,
        Screen.Network,
        Screen.Logs
    )

    if (showPrivacyDialog) {
        PrivacyComplianceDialog(
            onAccept = {
                showPrivacyDialog = false
                sharedPrefs.edit().putBoolean("first_run_privacy", false).apply()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route != Screen.Welcome.route

            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0A0A0F))
                        .border(
                            width = 0.5.dp, 
                            color = Color(0xFF00F3FF).copy(alpha = 0.3f), 
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(72.dp)
                    ) {
                        items.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        imageVector = screen.icon, 
                                        contentDescription = null,
                                        modifier = if (selected) Modifier.size(26.dp) else Modifier.size(24.dp)
                                    ) 
                                },
                                label = { 
                                    Text(
                                        text = stringResource(screen.labelRes),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00F3FF),
                                    selectedTextColor = Color(0xFF00F3FF),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0xFF00F3FF).copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isFirstRun) Screen.Welcome.route else Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(onFinished = {
                    sharedPrefs.edit().putBoolean("first_run", false).apply()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Dashboard.route) { 
                DashboardScreen(
                    emfValue = emfValue,
                    onCalibrate = { viewModel.calibrateCurrentRoom() },
                    snackbarHostState = snackbarHostState
                ) 
            }
            composable(Screen.Scanner.route) {
                if (isPremium) {
                    RadarScreen(viewModel = viewModel, emfValue = emfValue)
                } else {
                    PremiumPaywall(onUpgrade = { viewModel.upgradeToPremium() })
                }
            }
            composable(Screen.Network.route) {
                NetworkScreen(
                    viewModel = viewModel,
                    onNavigateToScanner = {
                        navController.navigate(Screen.Scanner.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Logs.route) { 
                LogsScreen(logs = threatLogs, onClearAll = { viewModel.clearLogs() }) 
            }
        }
    }
}

@Composable
fun PremiumPaywall(onUpgrade: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF00F3FF),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PORTHOUND PRO",
                color = Color(0xFF00F3FF),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unlock Physical EMF Sweep and IR Vision to pinpoint hidden lenses in your room.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F3FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("UPGRADE NOW - $4.99/mo", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PrivacyComplianceDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismiss */ },
        title = { Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.privacy_desc)) },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F3FF))
            ) {
                Text(stringResource(R.string.btn_accept), color = Color.Black)
            }
        },
        containerColor = Color(0xFF1A0B2E),
        titleContentColor = Color(0xFF00F3FF),
        textContentColor = Color.White.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp)
    )
}
