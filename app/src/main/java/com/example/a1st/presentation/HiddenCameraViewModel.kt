package com.example.a1st.presentation

import android.app.Activity
import android.graphics.Bitmap
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1st.data.camerax.CameraManager
import com.example.a1st.data.db.ThreatDao
import com.example.a1st.data.db.ThreatLogEntity
import com.example.a1st.data.network.NetworkRepository
import com.example.a1st.data.network.ScannedDevice
import com.example.a1st.data.repository.SensorRepository
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HiddenCameraViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val cameraManager: CameraManager,
    private val networkRepository: NetworkRepository,
    private val threatDao: ThreatDao
) : ViewModel() {

    private val _isIrVisionEnabled = MutableStateFlow(false)
    val isIrVisionEnabled: StateFlow<Boolean> = _isIrVisionEnabled.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    val threatLogs: StateFlow<List<ThreatLogEntity>> = threatDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredDevices: StateFlow<List<ScannedDevice>> = networkRepository.discoveredDevices

    private val _isScanningNetwork = MutableStateFlow(false)
    val isScanningNetwork: StateFlow<Boolean> = _isScanningNetwork.asStateFlow()

    private val lastFiveReadings = mutableListOf<Float>()
    private val _baselineEMF = MutableStateFlow(0f)
    val baselineEMF: StateFlow<Float> = _baselineEMF.asStateFlow()

    private var firstScanComplete = false

    val emfReading: StateFlow<Float> = sensorRepository.getMagnetometerData()
        .onEach { value ->
            synchronized(lastFiveReadings) {
                if (lastFiveReadings.size >= 5) lastFiveReadings.removeAt(0)
                lastFiveReadings.add(value)
            }
            val calibratedValue = value - _baselineEMF.value
            if (calibratedValue > 100f) {
                logThreat("EMF", "Spike: ${String.format("%.1f", calibratedValue)} µT", "HIGH")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    val calibratedEmfReading: StateFlow<Float> = combine(emfReading, _baselineEMF) { raw, baseline ->
        (raw - baseline).coerceAtLeast(0f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val processedBitmap: StateFlow<Bitmap?> = cameraManager.processedBitmap

    fun calibrateCurrentRoom() {
        synchronized(lastFiveReadings) {
            if (lastFiveReadings.isNotEmpty()) {
                _baselineEMF.value = lastFiveReadings.average().toFloat()
            }
        }
    }

    fun toggleIrVision() {
        _isIrVisionEnabled.value = !_isIrVisionEnabled.value
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        cameraManager.startCamera(lifecycleOwner, surfaceProvider)
    }

    fun scanNetwork(activity: Activity) {
        viewModelScope.launch {
            _isScanningNetwork.value = true
            networkRepository.scanNetwork()
            _isScanningNetwork.value = false
            
            discoveredDevices.value.filter { it.isThreat }.forEach { riskyDevice ->
                logThreat("NETWORK", "Found ${riskyDevice.resolvedName ?: riskyDevice.deviceCategory.name}", "MEDIUM")
            }

            if (!firstScanComplete) {
                firstScanComplete = true
                requestAppReview(activity)
            }
        }
    }

    fun upgradeToPremium() {
        _isPremium.value = true
    }

    private fun requestAppReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            threatDao.clearAllLogs()
        }
    }

    private fun logThreat(type: String, value: String, risk: String) {
        viewModelScope.launch {
            val log = ThreatLogEntity(
                type = type,
                value = value,
                timestamp = System.currentTimeMillis(),
                riskLevel = risk
            )
            threatDao.insertLog(log)
        }
    }
}
