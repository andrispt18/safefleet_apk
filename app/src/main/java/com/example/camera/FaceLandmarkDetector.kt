package com.example.camera

import android.graphics.Rect
import com.example.domain.model.FacialMetrics
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class FaceDetectionResult(
    val faceBoundingBox: Rect?,
    val metrics: FacialMetrics?,
    val hasFace: Boolean
)

class FaceLandmarkDetector {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setMinFaceSize(0.20f)
        .build()

    private val detector: FaceDetector = FaceDetection.getClient(detectorOptions)

    suspend fun processImage(inputImage: InputImage): FaceDetectionResult =
        suspendCancellableCoroutine { continuation ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    val primaryFace = faces.firstOrNull()
                    if (primaryFace != null) {
                        continuation.resume(createMetricsFromFace(primaryFace))
                    } else {
                        continuation.resume(FaceDetectionResult(null, null, false))
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(FaceDetectionResult(null, null, false))
                    }
                }
        }

    private fun createMetricsFromFace(face: Face): FaceDetectionResult {
        val rawLeftProb = face.leftEyeOpenProbability
        val rawRightProb = face.rightEyeOpenProbability

        val leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val leftEyeDetected = leftEyeLandmark != null || rawLeftProb != null
        val rightEyeDetected = rightEyeLandmark != null || rawRightProb != null
        val bothEyesVisible = leftEyeDetected && rightEyeDetected
        val anyEyeVisible = leftEyeDetected || rightEyeDetected

        // If one eye is obscured or unavailable, do NOT assume both are closed
        val effectiveEyeOpenProb: Float = when {
            rawLeftProb != null && rawRightProb != null -> (rawLeftProb + rawRightProb) / 2.0f
            rawLeftProb != null -> rawLeftProb
            rawRightProb != null -> rawRightProb
            else -> 0.85f // Default open if face is clearly tracked but open-prob classification unavailable
        }

        val leftEyeOpen = rawLeftProb ?: effectiveEyeOpenProb
        val rightEyeOpen = rawRightProb ?: effectiveEyeOpenProb

        val isClosed = when {
            rawLeftProb != null && rawRightProb != null -> rawLeftProb < 0.35f && rawRightProb < 0.35f
            rawLeftProb != null -> rawLeftProb < 0.35f
            rawRightProb != null -> rawRightProb < 0.35f
            else -> false
        }

        val earEstimate = (effectiveEyeOpenProb * 0.35f).coerceIn(0.0f, 0.45f)

        // Check mouth landmark if available
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val mouthTop = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val isYawning = if (mouthBottom != null && mouthTop != null) {
            (mouthBottom.y - mouthTop.y) > (face.boundingBox.height() * 0.28f)
        } else false

        val avgEyeProb = effectiveEyeOpenProb

        val headPitch = face.headEulerAngleX
        val headYaw = face.headEulerAngleY
        val headRoll = face.headEulerAngleZ
        val isHeadNodding = headPitch < -15.0f
        val isLookingAway = kotlin.math.abs(headYaw) > 28.0f || headPitch > 25.0f

        val landmarksMap = mutableMapOf<String, Pair<Float, Float>>()
        face.allLandmarks.forEach { lm ->
            landmarksMap[lm.landmarkType.toString()] = Pair(lm.position.x, lm.position.y)
        }

        val metrics = FacialMetrics(
            timestamp = System.currentTimeMillis(),
            faceDetected = true,
            leftEyeDetected = leftEyeDetected,
            rightEyeDetected = rightEyeDetected,
            bothEyesVisible = bothEyesVisible,
            anyEyeVisible = anyEyeVisible,
            leftEyeOpenProb = leftEyeOpen,
            rightEyeOpenProb = rightEyeOpen,
            avgEyeOpenProb = avgEyeProb,
            eyeAspectRatio = earEstimate,
            isEyeClosed = isClosed,
            mouthAspectRatio = if (isYawning) 0.65f else 0.15f,
            isYawning = isYawning,
            headPitch = headPitch,
            headYaw = headYaw,
            headRoll = headRoll,
            isHeadNodding = isHeadNodding,
            isLookingAway = isLookingAway,
            boundingBox = face.boundingBox,
            landmarks = landmarksMap
        )

        return FaceDetectionResult(
            faceBoundingBox = face.boundingBox,
            metrics = metrics,
            hasFace = true
        )
    }

    fun close() {
        try {
            detector.close()
        } catch (_: Exception) {
        }
    }
}
