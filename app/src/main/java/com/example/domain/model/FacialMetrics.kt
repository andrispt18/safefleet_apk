package com.example.domain.model

import android.graphics.Rect

data class FacialMetrics(
    val timestamp: Long = System.currentTimeMillis(),
    val faceDetected: Boolean = true,
    val leftEyeDetected: Boolean = true,
    val rightEyeDetected: Boolean = true,
    val bothEyesVisible: Boolean = true,
    val anyEyeVisible: Boolean = true,
    val leftEyeOpenProb: Float = 1.0f,
    val rightEyeOpenProb: Float = 1.0f,
    val avgEyeOpenProb: Float = 1.0f,
    val eyeAspectRatio: Float = 0.35f,
    val isEyeClosed: Boolean = false,
    val mouthAspectRatio: Float = 0.10f,
    val isYawning: Boolean = false,
    val headPitch: Float = 0f,
    val headYaw: Float = 0f,
    val headRoll: Float = 0f,
    val isHeadNodding: Boolean = false,
    val isLookingAway: Boolean = false,
    val boundingBox: Rect? = null,
    val landmarks: Map<String, Pair<Float, Float>> = emptyMap()
) {
    // Backwards compatibility properties for existing engine & UI callers
    val ear: Float get() = eyeAspectRatio
    val mar: Float get() = mouthAspectRatio
    val isEyesClosed: Boolean get() = isEyeClosed
}
