package com.example.a1st.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {

    private val _discoveredDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<ScannedDevice>> = _discoveredDevices.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scanDispatcher = Dispatchers.IO.limitedParallelism(50)

    override suspend fun scanNetwork() {
        withContext(Dispatchers.IO) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

            val ipv4Address = getIpv4Address(linkProperties)
            val prefixLength = getPrefixLength(linkProperties) ?: 24

            if (ipv4Address == null) return@withContext

            val ipInt = addressToInt(ipv4Address)
            val mask = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
            val startIpInt = ipInt and mask
            val endIpInt = startIpInt or mask.inv()
            
            val startRange = startIpInt + 1
            val endRange = endIpInt - 1
            
            _discoveredDevices.value = emptyList()

            val hostCount = (endRange - startRange + 1).coerceAtLeast(0)
            val scanLimit = if (hostCount > 254) 254 else hostCount

            (0 until scanLimit).map { i ->
                async {
                    val currentIpInt = startRange + i
                    val testIp = intToIp(currentIpInt)
                    
                    if (testIp == ipv4Address.hostAddress) {
                        addDevice(ScannedDevice(testIp, "My Sentinel Device", DeviceCategory.MY_SENTINEL, false))
                        return@async
                    }

                    fingerprintDevice(testIp)
                }
            }.awaitAll()
        }
    }

    private suspend fun fingerprintDevice(ip: String) {
        val ports = listOf(135, 445, 62078, 554, 1935, 8080)
        var category = DeviceCategory.GENERIC_DEVICE
        var isThreat = false
        var isDeviceActive = false

        for (port in ports) {
            try {
                withTimeout(150) {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(ip, port), 150)
                    socket.close()
                    isDeviceActive = true
                    
                    when (port) {
                        135, 445 -> category = DeviceCategory.WINDOWS_WORKSTATION
                        62078 -> category = DeviceCategory.APPLE_DEVICE
                        554, 1935, 8080 -> {
                            category = DeviceCategory.IP_CAMERA_DVR
                            isThreat = true
                        }
                    }
                }
            } catch (e: Exception) {
                val msg = e.message?.lowercase() ?: ""
                if (msg.contains("refused") || msg.contains("reset")) {
                     isDeviceActive = true
                }
            }
            if (isThreat) break
        }

        if (isDeviceActive) {
            val resolvedName = try {
                withContext(Dispatchers.IO) {
                    InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
                }
            } catch (e: Exception) { null }

            addDevice(ScannedDevice(ip, resolvedName, category, isThreat))
        }
    }

    private fun addDevice(device: ScannedDevice) {
        _discoveredDevices.update { current ->
            if (current.none { it.ipAddress == device.ipAddress }) {
                (current + device).sortedBy { it.ipAddress }
            } else {
                current
            }
        }
    }

    private fun getIpv4Address(linkProperties: LinkProperties?): Inet4Address? {
        return linkProperties?.linkAddresses?.map { it.address }?.filterIsInstance<Inet4Address>()?.firstOrNull { !it.isLoopbackAddress }
    }

    private fun getPrefixLength(linkProperties: LinkProperties?): Int? {
        return linkProperties?.linkAddresses?.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }?.prefixLength
    }

    private fun addressToInt(address: Inet4Address): Int {
        val bytes = address.address
        return ((bytes[0].toInt() and 0xFF) shl 24) or
               ((bytes[1].toInt() and 0xFF) shl 16) or
               ((bytes[2].toInt() and 0xFF) shl 8) or
               (bytes[3].toInt() and 0xFF)
    }

    private fun intToIp(i: Int): String {
        return "${(i shr 24) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 8) and 0xFF}.${i and 0xFF}"
    }
}
