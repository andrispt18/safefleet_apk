package com.example.ui.components

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.model.DrowsinessState
import com.example.domain.model.FacialMetrics
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafetyCritical
import com.example.ui.theme.SafetySafe
import com.example.ui.theme.SafetyWarning

@Composable
fun FaceOverlayCanvas(
    faceBox: Rect?,
    metrics: FacialMetrics?,
    state: DrowsinessState,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (faceBox == null || imageWidth <= 0 || imageHeight <= 0) {
            // Draw standby cockpit targeting grid
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val reticleRadius = size.minDimension * 0.22f

            drawCircle(
                color = ElectricBlue.copy(alpha = 0.25f),
                radius = reticleRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
            // Center crosshair lines
            drawLine(
                color = ElectricBlue.copy(alpha = 0.4f),
                start = Offset(centerX - 24f, centerY),
                end = Offset(centerX + 24f, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = ElectricBlue.copy(alpha = 0.4f),
                start = Offset(centerX, centerY - 24f),
                end = Offset(centerX, centerY + 24f),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val scaleX = size.width / imageWidth.toFloat()
        val scaleY = size.height / imageHeight.toFloat()

        // Handle front camera horizontal mirroring
        val left = if (isFrontCamera) {
            size.width - (faceBox.right * scaleX)
        } else {
            faceBox.left * scaleX
        }
        val right = if (isFrontCamera) {
            size.width - (faceBox.left * scaleX)
        } else {
            faceBox.right * scaleX
        }
        val top = faceBox.top * scaleY
        val bottom = faceBox.bottom * scaleY

        val boxWidth = (right - left).coerceAtLeast(10f)
        val boxHeight = (bottom - top).coerceAtLeast(10f)

        val strokeColor = when (state) {
            DrowsinessState.SAFE -> SafetySafe
            DrowsinessState.WARNING -> SafetyWarning
            DrowsinessState.DROWSY, DrowsinessState.CRITICAL -> SafetyCritical
            DrowsinessState.NO_FACE -> CyanAccent
        }

        // Draw bounding bracket box
        drawRoundRect(
            color = strokeColor.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 3f)
        )

        // Draw HUD corner accent ticks
        val cornerLen = (boxWidth * 0.2f).coerceAtMost(36f)
        val cornerStroke = 5f

        // Top Left
        drawLine(strokeColor, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
        drawLine(strokeColor, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)

        // Top Right
        drawLine(strokeColor, Offset(right, top), Offset(right - cornerLen, top), cornerStroke)
        drawLine(strokeColor, Offset(right, top), Offset(right, top + cornerLen), cornerStroke)

        // Bottom Left
        drawLine(strokeColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke)
        drawLine(strokeColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke)

        // Bottom Right
        drawLine(strokeColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke)
        drawLine(strokeColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke)
    }
}
