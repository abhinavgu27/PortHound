package com.example.a1st.data.camerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraManager {

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    override val processedBitmap: StateFlow<Bitmap?> = _processedBitmap

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraManager", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        // Use the built-in toBitmap() if available, otherwise we need a proper converter.
        // For RGBA_8888, we can manually create a bitmap.
        val bitmap = imageProxy.toBitmap()
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val width = mutableBitmap.width
        val height = mutableBitmap.height
        val pixels = IntArray(width * height)
        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            // Simple luminance calculation
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            // Filter for high-intensity bright spots (potential IR)
            if (luminance > 245) {
                // Neon Magenta: R:255, G:0, B:255
                pixels[i] = Color.rgb(255, 0, 255)
            } else {
                // Make non-bright spots transparent so we can overlay on preview
                pixels[i] = Color.TRANSPARENT
            }
        }

        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        _processedBitmap.value = mutableBitmap
        imageProxy.close()
    }
}
