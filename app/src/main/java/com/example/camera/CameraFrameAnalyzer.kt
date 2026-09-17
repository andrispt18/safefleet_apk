package com.example.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CameraFrameAnalyzer(
    private val detector: FaceLandmarkDetector,
    private val onResult: (FaceDetectionResult, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val isBusy = AtomicBoolean(false)
    private var lastAnalyzedTimestamp: Long = 0L
    private val frameIntervalMs = 90L // Throttle to ~11 FPS to save battery and prevent overheating
    private val analysisScope = CoroutineScope(Dispatchers.Default)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        // Frame skipping / backpressure protection
        if (currentTimestamp - lastAnalyzedTimestamp < frameIntervalMs || isBusy.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        if (!isBusy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height

        lastAnalyzedTimestamp = currentTimestamp

        analysisScope.launch {
            try {
                val result = detector.processImage(inputImage)
                onResult(result, imageWidth, imageHeight, rotationDegrees)
            } catch (_: Exception) {
            } finally {
                imageProxy.close()
                isBusy.set(false)
            }
        }
    }
}
