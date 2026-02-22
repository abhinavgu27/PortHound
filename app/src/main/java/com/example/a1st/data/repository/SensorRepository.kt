package com.example.a1st.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

interface SensorRepository {
    fun getMagnetometerData(): Flow<Float>
}

@Singleton
class SensorRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorRepository {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Low-Pass Filter Alpha (0 < alpha < 1). 
    // Lower values = more smoothing, more lag.
    // 0.15 is a good balance for handheld devices like Realme P4 5G.
    private var alpha = 0.15f
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    // For LPF state
    private var smoothedValues = FloatArray(3) { 0f }

    override fun getMagnetometerData(): Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val rawValues = event.values

                    // 1. Apply Low-Pass Filter to raw Bx, By, Bz
                    // This helps mitigate jitters and internal interference
                    for (i in 0..2) {
                        smoothedValues[i] = smoothedValues[i] + alpha * (rawValues[i] - smoothedValues[i])
                    }

                    // 2. Calculate Magnitude: B = sqrt(Bx^2 + By^2 + Bz^2)
                    val bx = smoothedValues[0]
                    val by = smoothedValues[1]
                    val bz = smoothedValues[2]
                    val magnitude = sqrt((bx * bx + by * by + bz * bz).toDouble()).toFloat()

                    // Note: Baseline calibration on Realme P4 5G typically yields 
                    // a background EMF of 30-50µT in a clean environment.
                    trySend(magnitude)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        magnetometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
