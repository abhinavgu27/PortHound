package com.example.a1st.data.network

import kotlinx.coroutines.flow.StateFlow

enum class DeviceCategory {
    WINDOWS_WORKSTATION,
    APPLE_DEVICE,
    IP_CAMERA_DVR,
    MY_SENTINEL,
    GENERIC_DEVICE
}

data class ScannedDevice(
    val ipAddress: String,
    val resolvedName: String?,
    val deviceCategory: DeviceCategory,
    val isThreat: Boolean
)

interface NetworkRepository {
    val discoveredDevices: StateFlow<List<ScannedDevice>>
    suspend fun scanNetwork()
}
